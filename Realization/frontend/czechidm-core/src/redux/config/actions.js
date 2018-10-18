import { formatPattern } from 'react-router/lib/PatternUtils';
import Immutable from 'immutable';
// reused actions
import FlashMessagesManager from '../../redux/flash/FlashMessagesManager';
// api
import ConfigLoader from '../../utils/ConfigLoader';
import ComponentLoader from '../../utils/ComponentLoader';
import { LocalizationService } from '../../services';
import SecurityManager from '../security/SecurityManager';
import ConfigurationManager from '../data/ConfigurationManager';
import { Actions, Properties } from './constants';

/**
 * Config / layout actions
 *
 * @author Radek Tomiška
 */

/**
 * Select navigation items
 *
 * @param  {array} selectedNavigationItems Array of selected navigation item. Can contains null values for select specified navigation level
 */
export function selectNavigationItems(selectedNavigationItems) {
  return {
    type: Actions.SELECT_NAVIGATION_ITEMS,
    selectedNavigationItems
  };
}
/**
 * Select navigation item by id
 * @return {action}
 */
export function selectNavigationItem(selectedNavigationItemId) {
  return {
    type: Actions.SELECT_NAVIGATION_ITEM,
    selectedNavigationItemId
  };
}

/**
 * Collapse navigation
 *
 * @param  {bool} collapse
 * @return {action}
 */
export function collapseNavigation(collapsed = false) {
  return {
    type: Actions.COLLAPSE_NAVIGATION,
    collapsed
  };
}

/**
 * Reloads whole navigation
 */
function navigationInit(cb) {
  return (dispatch) => {
    dispatch({
      type: Actions.NAVIGATION_INIT
    });
    dispatch({
      type: Actions.NAVIGATION_READY,
      navigation: ConfigLoader.getNavigation(),
      ready: true
    });
    dispatch({
      type: Actions.APP_READY,
      ready: true
    });
    cb();
  };
}

/**
 * Loads BE public configuration and store them into config storage
 *
 * @param  {func} cb callback
 * @return {action}
 */
export function backendConfigurationInit(cb = () => {}) {
  return (dispatch, getState) => {
    const configurationManager = new ConfigurationManager();
    dispatch(configurationManager.fetchPublicConfigurations((data, error) => {
      if (!error) {
        // disable modules by configuration
        ConfigLoader.getModuleDescriptors().forEach(moduleDescriptor => {
          if (moduleDescriptor.backendId) { // FE module depends on be module
            const isEnabled = ConfigurationManager.isModuleEnabled(getState(), moduleDescriptor.backendId) || false;
            //
            // FE module can be disabed sepatelly
            if (isEnabled && (moduleDescriptor.backendId === moduleDescriptor.id || ConfigurationManager.isModuleEnabled(getState(), moduleDescriptor.id))) {
              ConfigLoader.enable(moduleDescriptor.id, true);
            } else {
              ConfigLoader.enable(moduleDescriptor.id, false);
            }
          } else {
            const isEnabled = ConfigurationManager.isModuleEnabled(getState(), moduleDescriptor.id);
            ConfigLoader.enable(moduleDescriptor.id, isEnabled === null || isEnabled);
          }
        });
        ComponentLoader.reloadComponents();
        //
        dispatch({
          type: Actions.CONFIGURATION_READY,
          ready: true
        });
        dispatch(navigationInit(cb));
      } else {
        dispatch({
          type: Actions.APP_UNAVAILABLE
        });
        cb(error);
      }
    }));
  };
}

/**
* After localization is initied - set to ready, before inicialization will be false
*/
function i18nReady(lng, cb, reloadBackendConfiguration) {
  return (dispatch) => {
    dispatch({
      type: Actions.I18N_READY,
      lng
    });
    if (reloadBackendConfiguration) {
      dispatch(backendConfigurationInit(cb));
    } else if (cb) {
      cb();
    }
  };
}

/**
* Init i18n localization service
*/
function i18nInit(cb) {
  return (dispatch) => {
    LocalizationService.init(ConfigLoader,
      (error) => {
        if (error) {
          // FIXME: locale is broken ... but en message will be better
          cb({ level: 'error', title: 'Nepodařilo se iniciovat lokalizaci', message: error });
        } else {
          dispatch(i18nReady(LocalizationService.getCurrentLanguage(), cb, true));
        }
      }
    );
  };
}

/**
 * Changes locale
 *
 * @param  {string} lng locale (e.g. en)
 * @param  {func} cb callback fnction
 * @return {action}
 */
export function i18nChange(lng, cb = () => {}) {
  return (dispatch) => {
    LocalizationService.changeLanguage(lng,
      (error) => {
        if (error) {
          const flashMessagesManager = new FlashMessagesManager();
          // FIXME: locale is broken ... but en message will be better
          dispatch(flashMessagesManager.addMessage({level: 'error', title: 'Nepodařilo se iniciovat lokalizaci', message: error }));
        } else {
          dispatch(i18nReady(LocalizationService.getCurrentLanguage(), cb, false));
        }
      }
    );
  };
}

/**
* Init modules
*/
function modulesInit(config, moduleDescriptors, componentDescriptors, cb) {
  return (dispatch) => {
    dispatch({ type: Actions.MODULES_INIT });
    dispatch({ type: Actions.CONFIGURATION_INIT });
    ConfigLoader.init(config, moduleDescriptors);
    ComponentLoader.initComponents(componentDescriptors);
    dispatch({
      type: Actions.MODULES_READY,
      ready: true
    });
    dispatch(i18nInit(cb));
  };
}

export function appInit(config, moduleDescriptors, componentDescriptors, cb = () => {}) {
  return (dispatch) => {
    // init application - start asynchronous inicialization: modules / localization / configuration / navigation
    dispatch(modulesInit(config, moduleDescriptors, componentDescriptors, cb));
  };
}

/**
 * return parameters used in redirections
 */
export function resolveNavigationParameters(userContext = null, params = null) {
  let parameterValues;
  if (params) {
    parameterValues = new Immutable.Map(params);
  } else {
    parameterValues = new Immutable.Map({});
  }
  if (userContext) {
    //
    parameterValues = parameterValues.set('loggedUsername', userContext.username || 'guest');
    if (!parameterValues.has('entityId')) {
      parameterValues = parameterValues.set('entityId', userContext.username || 'guest');
    }
  }
  return parameterValues.toJS();
}

/**
 * Return navigation items for given level (parentId).
 * If parentId isn't set, then returns root (top) navigation items
 * Check user access, conditions etc ...
 * construct target links by given parameters
 */
export function getNavigationItems(navigation, parentId = null, section = null, userContext = null, params = null, onlyDynamic = false) {
  if (!navigation) {
    return [];
  }
  let navigationItems = [];
  if (!parentId) {
    navigationItems = navigation.get(ConfigLoader.NAVIGATION_BY_PARENT).get('').toArray();
  } else {
    if (!navigation.get(ConfigLoader.NAVIGATION_BY_PARENT).has(parentId)) {
      return [];
    }
    navigationItems = navigation.get(ConfigLoader.NAVIGATION_BY_PARENT).get(parentId).toArray();
  }

  return navigationItems.filter(item => {
    if (onlyDynamic && item.type !== 'DYNAMIC') {
      return false;
    }
    if (section && section !== item.section) {
      return false;
    }
    if (item.disabled) {
      return false;
    }
    // security check
    if (!SecurityManager.hasAccess(item.access, userContext)) {
      return false;
    }
    // construct target to link from path and parameters
    if (item.path) {
      item.to = formatPattern(item.path, resolveNavigationParameters(userContext, params));
    }

    return true;
  });
}

/**
 * Returns navigation item by given id from given configState immutable map
 *
 * @param  {object} configState
 * @param  {string} id
 * @return {navigationItem}
 */
export function getNavigationItem(configState, id) {
  if (!configState || !id) {
    return null;
  }
  const navigation = configState.get(Properties.NAVIGATION);
  //
  if (!navigation) {
    return null;
  }
  if (!navigation.get(ConfigLoader.NAVIGATION_BY_ID).has(id)) {
    return null;
  }
  return navigation.get(ConfigLoader.NAVIGATION_BY_ID).get(id);
}

/**
 * Hide footer on some contents.
 *
 * @return {bool} true - content footer will not be rendered
 */
export function hideFooter(hidden = false) {
  return {
    type: Actions.HIDE_FOOTER,
    [Properties.HIDE_FOOTER]: hidden
  };
}
