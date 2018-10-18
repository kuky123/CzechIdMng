import _ from 'lodash';
import {
  REQUEST_LOGIN,
  RECEIVE_LOGIN,
  RECEIVE_LOGIN_EXPIRED,
  RECEIVE_LOGIN_ERROR,
  RECEIVE_PROFILE, // @since 9.3.0
  LOGOUT,
  RECEIVE_REMOTE_LOGIN_ERROR,
  REQUEST_REMOTE_LOGIN
} from './SecurityManager';
import { Actions } from '../config/constants';

// TODO: integrate immutable map with redux-localstorage
const INITIAL_STATE = {
  userContext: { // logged userContext {id, username, token, isGuest etc .}
    id: null, // logged identity id
    showLoading: false,
    isExpired: false,
    username: null, // logged identity username
    isAuthenticated: false,
    isTryRemoteLogin: true,
    tokenCSRF: null,
    tokenCIDMST: null,
    authorities: [], // identity authorities
    profile: null, // identity profile @since 9.3.0
    navigationCollapsed: false
  }
};

/**
 * Security context storage
 *
 * @author Radek Tomiška
 */
export function security(state = INITIAL_STATE, action) {
  switch (action.type) {
    case REQUEST_LOGIN: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          showLoading: true
        })
      });
    }
    case REQUEST_REMOTE_LOGIN: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          showLoading: true,
          isTryRemoteLogin: false
        })
      });
    }
    case RECEIVE_LOGIN: {
      return _.merge({}, state, {
        userContext: _.merge(
          {
            isAuthenticated: true,
            showLoading: false,
            isExpired: false
          },
          action.userContext
        )
      });
    }
    case RECEIVE_LOGIN_ERROR: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          showLoading: false,
          tokenCIDMST: null,
          isAuthenticated: false
        })
      });
    }
    case RECEIVE_REMOTE_LOGIN_ERROR: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          showLoading: false,
          isTryRemoteLogin: false
        })
      });
    }
    case RECEIVE_LOGIN_EXPIRED: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          isAuthenticated: false,
          isExpired: true,
          showLoading: false,
          isTryRemoteLogin: true,
          tokenCIDMST: null
        })
      });
    }
    case RECEIVE_PROFILE: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          profile: action.profile
        })
      });
    }
    case Actions.COLLAPSE_NAVIGATION: {
      return _.merge({}, state, {
        userContext: _.merge({}, state.userContext, {
          navigationCollapsed: action.collapsed
        })
      });
    }
    case LOGOUT: {
      return _.merge({}, INITIAL_STATE, {
        isTryRemoteLogin: false
      });
    }
    default:
      return state;
  }
}
