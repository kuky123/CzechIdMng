import _ from 'lodash';
//
import Stomp from 'stompjs';
import SockJS from 'sockjs-client';
//
import { AuthenticateService, IdentityService, LocalizationService } from '../../services';
import FlashMessagesManager from '../flash/FlashMessagesManager';
import ConfigLoader from '../../utils/ConfigLoader';
import { Actions } from '../config/constants';

/**
 * action types
 */
export const REQUEST_LOGIN = 'REQUEST_LOGIN';
export const RECEIVE_LOGIN = 'RECEIVE_LOGIN';
export const RECEIVE_LOGIN_EXPIRED = 'RECEIVE_LOGIN_EXPIRED';
export const RECEIVE_LOGIN_ERROR = 'RECEIVE_LOGIN_ERROR';
export const REQUEST_REMOTE_LOGIN = 'REQUEST_REMOTE_LOGIN';
export const RECEIVE_REMOTE_LOGIN_ERROR = 'RECEIVE_REMOTE_LOGIN_ERROR';
export const RECEIVE_PROFILE = 'RECEIVE_PROFILE';
export const LOGOUT = 'LOGOUT';
//
const TOKEN_COOKIE_NAME = 'XSRF-TOKEN';
const PERMISSION_SEPARATOR = '_';
const ADMIN_PERMISSION = 'ADMIN';
const ADMIN_AUTHORITY = `APP${PERMISSION_SEPARATOR}${ADMIN_PERMISSION}`;

const authenticateService = new AuthenticateService();
const identityService = new IdentityService();
const flashMessagesManager = new FlashMessagesManager();
let stompClient = null;

/**
 * Encapsulate user context / authentication and authorization
 *
 * @author Radek Tomiška
 */
export default class SecurityManager {

  constructor() {
  }

  /**
   * If authentication token's expiration time has been extended or modified,
   * it is stored in AuthenticateService#lastToken. The method checks if
   * new exteded token is available and in case it is, the new token
   * is set into userContext.
   */
  checkRefreshedToken() {
    return (dispatch, getState) => {
      const userContext = getState().security.userContext;
      const lt = AuthenticateService.getLastToken();
      if (lt && userContext.tokenCIDMST && userContext.tokenCIDMST !== lt) {
        userContext.tokenCIDMST = lt;
        dispatch({
          type: RECEIVE_LOGIN,
          userContext
        });
      }
    };
  }

  /**
   * Login user
   *
   * @param  {string} username
   * @param  {string} password
   * @param  {function} redirect
   * @return {action}
   */
  login(username, password, redirect) {
    return (dispatch, getState) => {
      dispatch(this.requestLogin());
      dispatch(flashMessagesManager.hideAllMessages());
      //
      authenticateService.login(username, password)
        .then(json => this._handleUserAuthSuccess(dispatch, getState, redirect, json))
        .catch(error => dispatch(this.receiveLoginError(error, redirect)));
    };
  }

  /**
   * Tries to authenticate by remote authority token.
   * In case of successful authentication sets the tokenCIDMST into userContext.
   */
  remoteLogin(redirect) {
    return (dispatch, getState) => {
      dispatch(this.requestRemoteLogin());
      //
      authenticateService.remoteLogin()
        .then(json => this._handleUserAuthSuccess(dispatch, getState, redirect, json))
        .catch(error => dispatch(this.receiveRemoteLoginError(error, redirect)));
    };
  }

  _handleUserAuthSuccess(dispatch, getState, redirect, json) {
    const decoded = AuthenticateService.decodeToken(json.token);
    const userName = decoded.currentUsername;
    // load identity profile
    identityService.getProfile(userName, json.token)
      .catch(error => {
        // profile is optional - logged identity couldn't have permission for read profile (or profile not found)
        flashMessagesManager.addErrorMessage({ hidden: true, level: 'info' }, error);
        return null;
      })
      .then(profile => {
        // construct logged user context
        const userContext = {
          id: json.authentication.currentIdentityId,
          isAuthenticated: true,
          username: userName,
          tokenCIDMST: json.token,
          tokenCSRF: authenticateService.getCookie(TOKEN_COOKIE_NAME),
          authorities: json.authorities.map(authority => authority.authority),
          profile
        };
        //
        // remove all messages (only logout could be fond in messages after logout)
        dispatch(flashMessagesManager.removeAllMessages());
        //
        // init FE by saved profile on BE
        if (profile) {
          // collapse navigation
          dispatch({
            type: Actions.COLLAPSE_NAVIGATION,
            collapsed: profile.navigationCollapsed
          });
          //
          // change locale by profile
          if (profile.preferredLanguage) {
            LocalizationService.changeLanguage(profile.preferredLanguage, (error) => {
              if (error) {
                // FIXME: locale is broken ... but en message will be better
                dispatch(flashMessagesManager.addMessage({level: 'error', title: 'Nepodařilo se iniciovat lokalizaci', message: error }));
              } else {
                dispatch({
                  type: Actions.I18N_READY,
                  lng: profile.preferredLanguage
                });
              }
            });
          }
        }
        //
        // send userContext to state
        dispatch(this.receiveLogin(userContext, redirect));
      });
  }

  /*
  * Request data
  */
  requestLogin() {
    return {
      type: REQUEST_LOGIN
    };
  }

  requestRemoteLogin() {
    return {
      type: REQUEST_REMOTE_LOGIN
    };
  }

  receiveLogin(userContext, redirect) {
    return dispatch => {
      // login to websocket
      // @deprecated @since 9.2.0, will be removed (move websocket notification support to your custom module if needed)
      // dispatch(SecurityManager.connectStompClient(userContext));
      // getState().logger.debug('received login', userContext);
      //
      // redirect after login, if needed
      if (redirect) {
        redirect(userContext.isAuthenticated);
      }
      dispatch({
        type: RECEIVE_LOGIN,
        userContext
      });
    };
  }

  receiveLoginError(error, redirect) {
    return dispatch => {
      authenticateService.clearStorage();
      // add error message
      if (error) {
        dispatch(flashMessagesManager.addErrorMessage({ position: 'tc' }, error));
      }
      // redirect after login, if needed
      if (redirect) {
        redirect(false);
      }
      dispatch({
        type: RECEIVE_LOGIN_ERROR
      });
    };
  }

  receiveRemoteLoginError(error) {
    return (dispatch, getState) => {
      // add error message
      if (error) {
        getState().logger.warn('Remote login error occurred:', error);
      }
      dispatch({
        type: RECEIVE_REMOTE_LOGIN_ERROR
      });
    };
  }

  /**
   * Logout from BE and FE. Adds logout message after BE returns response
   *
   * @param  {function} redirect Callback, when loggout is done
   * @return {action}
   */
  logout(redirect) {
    return dispatch => {
      authenticateService
        .logout()
        .then(() => {
          dispatch(flashMessagesManager.removeAllMessages());
          /* RT: i think this message is not needed
          dispatch(flashMessagesManager.addMessage({
            key: 'logout',
            message: LocalizationService.i18n('content.logout.message.logout'),
            level: 'info',
            position: 'tc'
          }));*/
          dispatch(this.receiveLogout());
          if (redirect) {
            redirect();
          }
        })
        .catch(error => {
          if (error.statusCode === 401 || error.statusCode === 403) {
            // logout was already called - clean up token from FE
            dispatch(this.receiveLogout());
            if (redirect) {
              redirect();
            }
          } else {
            // other error
            dispatch(this.receiveLoginError(error, redirect));
          }
        });
    };
  }

  /**
   * Logout without wait for promise result
   */
  logoutImmediatelly() {
    authenticateService.logout();
    return dispatch => {
      dispatch(this.receiveLogout());
    };
  }

  receiveLogout() {
    return dispatch => {
      // logout from web sockets
      // dispatch(SecurityManager.disconectStompClient());
      dispatch({
        type: LOGOUT
      });
    };
  }

  getCookie(cookieName) {
    return authenticateService.getCookie(cookieName);
  }

  /**
   * Puts new token to userContext
   */
  reloadToken() {
    return (dispatch, getState) => {
      dispatch(this.receiveLogin(
        _.merge({}, getState().security.userContext, {
          tokenCSRF: this.getCookie(TOKEN_COOKIE_NAME)
        })
      ));
    };
  }

  /**
   * Returns true, if user is logged in
   */
  static isAuthenticated(userContext = null) {
    if (!userContext) {
      return AuthenticateService.isAuthenticated();
    }
    return userContext.isAuthenticated;
  }

  /**
   * Returns true, if user is "big boss"
   */
  static isAdmin(userContext = null) {
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    if (!this.isAuthenticated(userContext) || !userContext.authorities) {
      return false;
    }
    return _.includes(userContext.authorities, ADMIN_AUTHORITY);
  }

  /**
   * Returns true, if given username equals authenticated username
   */
  static equalsAuthenticated(username, userContext = null) {
    if (!username) {
      return false;
    }
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    if (!this.isAuthenticated(userContext)) {
      return false;
    }
    return username === userContext.username;
  }

  /**
   * Returns true, if user has given authority
   */
  static hasAuthority(authority, userContext = null) {
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    if (!this.isAuthenticated(userContext) || !userContext.authorities || !authority) {
      return false;
    }
    return this.isAdmin(userContext) // admin
      || _.includes(userContext.authorities, `${authority.split(PERMISSION_SEPARATOR)[0]}${PERMISSION_SEPARATOR}${ADMIN_PERMISSION}`) // group admin
      || _.includes(userContext.authorities, authority); // single authority
  }

  /**
   * Returns true, if user has any of given authorities, false otherwise.
   *
   * @param  {arrayOf(string)}  authorities
   * @param  {userContext}  userContext
   * @return {Boolean}
   */
  static hasAnyAuthority(authorities, userContext = null) {
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    if (!this.isAuthenticated(userContext) || !userContext.authorities || !authorities) {
      return false;
    }
    if (this.isAdmin(userContext)) {
      return true;
    }
    return authorities.some(authority => {
      return this.hasAuthority(authority, userContext);
    });
  }

  /**
   * Returns true, if user has all of given authorities, false otherwise.
   *
   * @param  {arrayOf(string)}  authorities
   * @param  {userContext}  userContext
   * @return {Boolean}
   */
  static hasAllAuthorities(authorities, userContext = null) {
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    if (!this.isAuthenticated(userContext) || !userContext.authorities || !authorities) {
      return false;
    }
    if (this.isAdmin(userContext)) {
      return true;
    }
    return authorities.every(authority => {
      return this.hasAuthority(authority, userContext);
    });
  }

  /**
   * Return true, if user fits in at least one access item - {@see ConfigLoader} for available access types
   */
  static hasAccess(accessItems, userContext) {
    if (!accessItems) {
      return false;
    }
    if (!_.isArray(accessItems)) {
      accessItems = [accessItems];
    }

    if (accessItems.length === 0) {
      return false;
    }

    const hasDenyAll = this.isDenyAll(accessItems);
    if (hasDenyAll) {
      return false;
    }

    const hasPermitAll = accessItems.some(accessItem => {
      if (accessItem.type && accessItem.type === 'PERMIT_ALL') {
        return true;
      }
    });
    if (hasPermitAll) {
      return true;
    }

    return accessItems.every(accessItem => {
      if (accessItem.type) {
        switch (accessItem.type) {
          case 'NOT_AUTHENTICATED': {
            return !this.isAuthenticated(userContext);
          }
          case 'IS_AUTHENTICATED': {
            return this.isAuthenticated(userContext);
          }
          case 'HAS_ANY_AUTHORITY': {
            return this.hasAnyAuthority(accessItem.authorities, userContext);
          }
          case 'HAS_ALL_AUTHORITIES': {
            return this.hasAllAuthorities(accessItem.authorities, userContext);
          }
          default : {
            return false;
          }
        }
      }
    });
  }

  static isDenyAll(accessItems) {
    if (!accessItems) {
      return false;
    }
    if (accessItems.length === 0) {
      return false;
    }
    if (!_.isArray(accessItems)) {
      accessItems = [accessItems];
    }
    return accessItems.some(accessItem => {
      if (accessItem.type && accessItem.type === 'DENY_ALL') {
        return true;
      }
    });
  }

  /**
   * Checks access from routes
   */
  static checkAccess(nextState, replace) {
    const userContext = AuthenticateService.getUserContext();
    const lastRoute = nextState.routes.slice(-1)[0];
    // warn: SecurityManager can not be defined as this - is caled from router
    if (!SecurityManager.hasAccess(lastRoute.access, userContext)) {
      replace({
        pathname: (SecurityManager.isAuthenticated(userContext)) ? '/error/403' : '/login',
        state: { nextPathname: nextState.location.pathname }
      });
    }
  }

  /**
   * Connect websocket client to receiving flashmessages from BE
   *
   * @param  {UserContext} userContext
   * @deprecated @since 9.2.0, will be removed
   */
  static connectStompClient(userContext = null) {
    return (dispatch, getState) => {
      dispatch(this.disconectStompClient());
      if (!userContext) {
        userContext = AuthenticateService.getUserContext();
      }
      // logged user only
      if (userContext && userContext.username) {
        stompClient = Stomp.over(new SockJS(`${ConfigLoader.getServerUrl()}/websocket-info`));
        const headers = {
          login: userContext.username,
          CIDMST: userContext.tokenCIDMST
        };
        stompClient.connect(headers, () => {
          getState().logger.debug(`stomp client for messages - identity [${userContext.username}] is logged.`);
          stompClient.subscribe(`/user/${userContext.username}/queue/messages`, (stompMessage) => {
            const rawMessage = JSON.parse(stompMessage.body);
            dispatch(flashMessagesManager.addMessage(flashMessagesManager.convertFromWebsocketMessage(rawMessage)));
          }, headers);
        }, () => {
          // try reconnect
          // setTimeout(SecurityManager.connectStompClient, 10000);
        });
      }
    };
  }

  /**
   * Disconnect websocket client to receiving flashmessages from BE
   *
   * @return {[type]} [description]
   * @deprecated @since 9.2.0, will be removed
   */
  static disconectStompClient() {
    return (dispatch, getState) => {
      if (stompClient) {
        try {
          stompClient.disconnect(() => {
            getState().logger.debug(`stomp client for messages - websocket successfully closed.`);
          });
        } catch (err) {
          LOGGER.debug(`[SecurityManager]: closing stomp client failed.`, err.message);
        }
      }
    };
  }

  static getTokenExpiration(userContext = null) {
    if (!userContext) {
      userContext = AuthenticateService.getUserContext();
    }
    const decodedToken = AuthenticateService.decodeToken(userContext.tokenCIDMST);
    //
    return AuthenticateService.getTokenExpiration(decodedToken);
  }
}

SecurityManager.ADMIN_PERMISSION = ADMIN_PERMISSION;
SecurityManager.ADMIN_AUTHORITY = ADMIN_AUTHORITY;
