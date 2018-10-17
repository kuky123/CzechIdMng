//
// Aplication entry point
//

// global babel polyfill - IE Symbol support, Object.assign etc.
import 'babel-polyfill';
import React from 'react';
import ReactDOM from 'react-dom';
// https://github.com/rackt/react-router/blob/master/upgrade-guides/v2.0.0.md#changes-to-thiscontext
// TODO: serving static resources requires different approach - https://github.com/rackt/react-router/blob/master/docs/guides/basics/Histories.md#createbrowserhistory
import { Router, hashHistory } from 'react-router';
import { Provider } from 'react-redux';
import merge from 'object-assign';
import Immutable from 'immutable';
import _ from 'lodash';
import { combineReducers, compose, createStore, applyMiddleware } from 'redux';
import thunkMiddleware from 'redux-thunk';
import promiseMiddleware from 'redux-promise';
import Promise from 'es6-promise';
import log4js from 'log4js';
//
import persistState, { mergePersistedState } from 'redux-localstorage';
import filter from 'redux-localstorage-filter';
//
import { syncHistory, routeReducer } from 'react-router-redux';
//
import { Reducers, Managers, Basic, ConfigActions } from 'czechidm-core';
import ConfigLoader from 'czechidm-core/src/utils/ConfigLoader';
//
// this parts are genetater dynamicaly to dist - after build will be packed by browserify to sources
import config from '../dist/config.json';
import { moduleDescriptors } from '../dist/modules/moduleAssembler';
import { componentDescriptors } from '../dist/modules/componentAssembler';
//
// application routes root
import App from './layout/App';
//
//
// global promise init
// TODO: https://github.com/qubyte/fetch-ponyfill
Promise.polyfill();
//
// logger setting e.g. http://stritti.github.io/log4js/docu/users-guide.html
log4js.configure({
  appenders: [
    {
      type: 'console',
      layout: {
        type: 'pattern',
        // pattern: '%d{ISO8601} [%-5p%] %c %m'
        // pattern: '%d{ISO8601} [%p] %n%m',
        pattern: '[%p] %m'
      }
    }
  ],
  // replaceConsole: true
});
const logger = log4js.getLogger();
const level = ConfigLoader.getConfig('logger.level', !config.logger || !config.logger.level ? 'DEBUG' : config.logger.level);
logger.setLevel(level);
global.LOGGER = logger;

// debug setting
// global DEBUG is true only if is application compiled/runned via watchify task. When is application only build, then is always DEBUG set on FALSE.
if (typeof DEBUG === 'undefined') {
  global.DEBUG = true;
}

/**
 * viz. import adapter from 'redux-localstorage/lib/adapters/localStorage';
 * TODO: move to utils
 */
function adapter(storage) {
  return {
    0: storage,

    put: function put(key, value, callback) {
      try {
        //
        value.messages.messages = value.messages.messages.toArray();
        value.messages.messages.forEach(message => {
          // prevent to persist react elements
          // FIXME: restore react fragment from text
          message.children = null;
        });
        callback(null, storage.setItem(key, JSON.stringify(value)));
      } catch (e) {
        callback(e);
      }
    },

    get: function get(key, callback) {
      try {
        callback(null, JSON.parse(storage.getItem(key)));
      } catch (e) {
        callback(e);
      }
    },

    del: function del(key, callback) {
      try {
        callback(null, storage.removeItem(key));
      } catch (e) {
        callback(e);
      }
    }
  };
}

const reducersApp = combineReducers({
  config: Reducers.config,
  messages: Reducers.messages,
  data: Reducers.data,
  security: Reducers.security,
  routing: routeReducer,
  logger: (state = logger) => {
    // TODO: can be moved to separate redecuer - now is inline
    return state;
  }
});
//
// persistent local storage
const reducer = compose(
  mergePersistedState((initialState, persistedState) => {
    // constuct immutable maps
    const result = merge({}, initialState, persistedState);
    let composedMessages = new Immutable.OrderedMap({});
    persistedState.messages.messages.map(message => {
      composedMessages = composedMessages.set(message.id, message);
    });
    result.messages.messages = composedMessages;
    //
    return result;
  })
)(reducersApp);
//
const storage = compose(
  filter([
    'messages.messages',       // flash messages
    'security.userContext'     // logged user context {username, token, etc}
  ])
)(adapter(window.localStorage));
//
//
const createPersistentStore = compose(
  persistState(storage, 'czechidm-storage')
)(createStore);
//
// Sync dispatched route actions to the history
const reduxRouterMiddleware = syncHistory(hashHistory);
//
// before dispatch handler
function dispatchTrace({ getState }) {
  return (next) => (action) => {
    logger.trace('will dispatch', action);
    // Call the next dispatch method in the middleware chain.
    const returnValue = next(action);
    logger.trace('state after dispatch', getState());
    // This will likely be the action itself, unless
    // a middleware further in chain changed it.
    return returnValue;
  };
}
//
// redux queue middle ware, inpired by https://github.com/zackargyle/redux-async-queue
// TODO: move to utils
function reduxQueue({ dispatch, getState }) {
  const THREAD_COUNT = 3;
  const queues = {}; // queued actions
  const running = {}; // running queue ids

  function dequeue(key) {
    const action = queues[key].find(a => {
      return !running[key].has(a.id);
    });
    if (!action) {
      return;
    }
    running[key] = running[key].add(action.id);
    //
    // execute action
    action.callback(function next() {
      queues[key].shift();
      running[key] = running[key].delete(action.id);
      if (queues[key].length > 0 && running[key].size < THREAD_COUNT) {
        dequeue(key);
      }
    }, dispatch, getState);
  }
  //
  return next => action => {
    const { queue: key, callback, id } = action || {};
    if (key) {
      if (typeof callback !== 'function') {
        throw new Error('Queued actions must have a <callback> property');
      }
      if (!id) {
        throw new Error('Queued actions must have a <id> property');
      }
      // Verify array at <key>
      queues[key] = queues[key] || [];
      running[key] = running[key] || new Immutable.Set();
      // Add new queued callback
      queues[key].push(action);
      //
      // If it's the only one, sync call it.
      if (queues[key].length === 1 || running[key].size < THREAD_COUNT) {
        dequeue(key);
      }
    } else {
      return next(action);
    }
  };
}
//
// apply middleware
let midlewares = [];
if (logger.isTraceEnabled()) {
  midlewares.push(dispatchTrace);
}
midlewares = [...midlewares, thunkMiddleware, promiseMiddleware, reduxRouterMiddleware, reduxQueue];
const createStoreWithMiddleware = applyMiddleware(...midlewares)(createPersistentStore);
// redux store
const store = createStoreWithMiddleware(reducer);
// const store = createStoreWithMiddleware(reducer, window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__());
// Required for replaying actions from devtools to work
reduxRouterMiddleware.listenForReplays(store);
//

/**
 * Returns unique route id by path (or component if path is not defined)
 *
 * @param  {string} parentRouteId
 * @param  {route} route
 * @return {string}
 */
function getRouteId(parentRouteId, route) {
  let id = '';
  if (parentRouteId) {
    id += parentRouteId;
  }
  if (route.path) {
    id += route.path;
  } else {
    id += route.component;
  }
  return id;
}

/**
 * Transform route tree to flat map.
 * Adds route dafeul values.
 * Removes routes from disabled module.
 *
 * @param  {immutable} routesMap result routes
 * @param  {string} moduleId module identifier
 * @param  {string} parentRouteId parent route identifier (we need them for unique route id)
 * @param  {route} route processing route
 * @return {immutable} result routes
 */
function fillRouteMap(routesMap, moduleId, parentRouteId, route) {
  const routeId = getRouteId(parentRouteId, route);
  // fill module to route from parent route
  if (route.module !== undefined) {
    moduleId = route.module;
  }
  // module is disabled - skip whole subtree
  if (moduleId && !ConfigLoader.isEnabledModule(moduleId)) {
    return routesMap;
  }
  // cloned route with filled default values
  const clonedRoute = {
    id: routeId,
    parentId: parentRouteId,
    module: route.module || moduleId,
    access: route.access || [{ type: 'IS_AUTHENTICATED' }],
    onEnter: route.onEnter || Managers.SecurityManager.checkAccess,
    component: route.component,
    path: route.path,
    priority: route.priority || 0,
    order: route.order || 0
  };
  // add route to flat map by priority and order
  if (!routesMap.has(routeId)
      || routesMap.get(routeId).priority < clonedRoute.priority // higher priority
      || (routesMap.get(routeId).priority === clonedRoute.priority && routesMap.get(routeId).order > clonedRoute.order)) { // lower order
    routesMap = routesMap.set(routeId, clonedRoute);
  }
  // children of route with less priority are forgotten
  if (route.childRoutes && (routesMap.get(routeId).priority <= clonedRoute.priority)) {
    route.childRoutes.forEach(childRoute => {
      routesMap = fillRouteMap(routesMap, moduleId, routeId, childRoute);
    });
  }
  return routesMap;
}

/**
 * Rebuild tree from flat map and original route tree.
 *
 * @param  {immutable} routesMap flat routes map (see fillRouteMap)
 * @param  {route} targetRoute cloned route
 * @param  {string} parentRouteId parent route identifier (we need them for unique route id)
 * @param  {route} route original route (wee need to rebuild tree in the same structure)
 */
function fillRouteTree(routesMap, targetRoute, parentRouteId, route) {
  const routeId = getRouteId(parentRouteId, route);
  //
  if (routesMap.has(routeId)) {
    _.merge(targetRoute, routesMap.get(routeId));
  } else {
    // route was not found - is disabled etc.
    return;
  }
  if (route.childRoutes) {
    targetRoute.childRoutes = [];
    route.childRoutes.forEach(childRoute => {
      const targetChildRoute = {};
      targetRoute.childRoutes.push(targetChildRoute);
      fillRouteTree(routesMap, targetChildRoute, routeId, childRoute);
    });
    // sort routes by order
    route.childRoutes = route.childRoutes.sort((one, two) => {
      return one.order - two.order;
    });
  }
}

store.dispatch(ConfigActions.appInit(config, moduleDescriptors, componentDescriptors, (error) => {
  if (!error) {
    const routeAssembler = require('../dist/modules/routeAssembler');
    //
    // prepare routes in flat map
    let routeMap = new Immutable.OrderedMap();
    routeAssembler.childRoutes.forEach(moduleRoute => { // wee need to skip "decorator" routes
      if (moduleRoute.childRoutes) {
        moduleRoute.childRoutes.forEach(route => {
          routeMap = fillRouteMap(routeMap, moduleRoute.module, null, route);
        });
      }
    });
    //
    // rebuild target routes
    let resultRoutes = [];
    routeAssembler.childRoutes.forEach(moduleRoute => { // wee need to skip "decorator" routes
      if (moduleRoute.childRoutes) {
        moduleRoute.childRoutes.forEach(route => {
          const resultRoute = {};
          resultRoutes.push(resultRoute);
          fillRouteTree(routeMap, resultRoute, null, route);
        });
      }
    });
    // sort routes by order
    //
    resultRoutes = resultRoutes.filter(i => {
      return i.id !== null && i.id !== undefined;
    }).sort((one, two) => {
      return one.order - two.order;
    });
    //
    const routes = {
      childRoutes: [
        {
          path: '/',
          getComponent: (location, cb) => {
            cb(null, App );
          },
          indexRoute: {
            component: require('./layout/Dashboard'),
            onEnter: Managers.SecurityManager.checkAccess,
            access: [{ type: 'IS_AUTHENTICATED' }]
          },
          childRoutes: resultRoutes
        }
      ]
    };
    // init websocket for user messages (after F5 etc.)
    // @deprecated @since 9.2.0, will be removed (move websocket notification support to your custom module if needed)
    // store.dispatch(Managers.SecurityManager.connectStompClient());
    //
    // app entry point
    ReactDOM.render(
      <Provider store={store}>
        <Router history={hashHistory} routes={routes}/>
      </Provider>
      ,
      document.getElementById('content')
    );
  } else {
    const flashManager = new Managers.FlashMessagesManager();
    ReactDOM.render(
      <div style={{ margin: 15 }}>
        <Basic.FlashMessage
          icon="exclamation-sign"
          message={flashManager.convertFromError(error)}/>
      </div>
      ,
      document.getElementById('content')
    );
  }
}));
