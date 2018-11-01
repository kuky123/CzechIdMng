import Immutable from 'immutable';
//
import { getNavigationItem } from './actions';
import { Actions, Properties } from './constants';

const INITIAL_STATE = new Immutable.Map({
  [Properties.PROPERTIES]: null, // configuration propereties
  [Properties.NAVIGATION]: null,  // all navigation items from enabled modules as Map
  selectedNavigationItems: ['home'], // homepage by default
  i18nReady: null,              // localization language is ready
  modulesReady: false,            // modules loaders is ready
  navigationReady: false,
  configurationReady: false,
  appReady: false,
  appUnavailable: false,
  hideFooter: false
});

/**
 * Config / layout storage
 *
 * @author Radek Tomiška
 */
export function config(state = INITIAL_STATE, action) {
  switch (action.type) {
    case Actions.SELECT_NAVIGATION_ITEMS: {
      const prevState = state.get('selectedNavigationItems');
      const newState = [];
      for (let i = 0; i < action.selectedNavigationItems.length; i++) {
        newState[i] = action.selectedNavigationItems[i] || (prevState.length > i ? prevState[i] : null);
      }
      return state.set('selectedNavigationItems', newState);
    }
    case Actions.SELECT_NAVIGATION_ITEM: {
      const newState = [];
      // traverse to item parent
      let itemId = action.selectedNavigationItemId;
      while (itemId !== null) {
        const item = getNavigationItem(state, itemId);
        if (!item) {
          break;
        }
        newState.splice(0, 0, item.id); // insert at start
        itemId = item.parentId;
      }
      return state.set('selectedNavigationItems', newState);
    }
    case Actions.I18N_INIT: {
      return state.set('i18nReady', null);
    }
    case Actions.I18N_READY: {
      LOGGER.debug('i18n ready [' + action.lng + ']');
      return state.set('i18nReady', action.lng);
    }
    case Actions.MODULES_INIT: {
      return state.set('modulesReady', false);
    }
    case Actions.MODULES_READY: {
      LOGGER.debug('modules ready [' + action.ready + ']');
      return state.set('modulesReady', action.ready);
    }
    case Actions.NAVIGATION_INIT: {
      return state.set('navigationReady', false);
    }
    case Actions.NAVIGATION_READY: {
      LOGGER.debug('navigation ready [' + action.ready + ']');
      let newState = state.set(Properties.NAVIGATION, action.navigation);
      newState = newState.set('navigationReady', action.ready);
      return newState;
    }
    case Actions.CONFIGURATION_INIT: {
      return state.set('configurationReady', false);
    }
    case Actions.CONFIGURATION_READY: {
      LOGGER.debug('configuration ready [' + action.ready + ']');
      const newState = state.set('configurationReady', action.ready);
      return newState;
    }
    case Actions.APP_INIT: {
      let newState = state.set('appReady', false);
      newState = newState.set('appUnavailable', false);
      return newState;
    }
    case Actions.APP_READY: {
      LOGGER.debug('app ready [' + action.ready + ']');
      return state.set('appReady', action.ready);
    }
    case Actions.APP_UNAVAILABLE: {
      let newState = state.set('appReady', false);
      newState = newState.set('appUnavailable', true);
      return newState;
    }
    case Actions.CONFIGURATION_RECEIVED: {
      const configs = state.get(Properties.PROPERTIES);
      if (configs) {
        // put new values
        return state.set(Properties.PROPERTIES, configs.merge(action.data));
      }
      return state.set(Properties.PROPERTIES, action.data);
    }
    case Actions.HIDE_FOOTER: {
      return state.set(Properties.HIDE_FOOTER, action.hideFooter);
    }
    default:
      return state;
  }
}
