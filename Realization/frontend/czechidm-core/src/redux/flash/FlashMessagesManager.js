import React from 'react';
import _ from 'lodash';
import { routeActions } from 'react-router-redux';
// import moment from 'moment';
// api
import { LocalizationService, AuthenticateService } from '../../services';
// import SecurityManager from '../Security/SecurityManager';
// import SettingManager from '../data/SettingManager';

/*
 * action types
 */

export const ADD_MESSAGE = 'ADD_MESSAGE';
export const HIDE_MESSAGE = 'HIDE_MESSAGE';
export const HIDE_ALL_MESSAGES = 'HIDE_ALL_MESSAGES';
export const REMOVE_MESSAGE = 'REMOVE_MESSAGE';
export const REMOVE_ALL_MESSAGES = 'REMOVE_ALL_MESSAGES';
//
export const DEFAULT_SERVER_UNAVAILABLE_TIMEOUT = 3000;

const _DEFAULT_MESSAGE = {
  id: null, // internal id
  key: null, // key for unique checking
  title: null,
  message: null,
  level: 'success', // PropTypes.oneOf(['success', 'info', 'warning', 'error']),
  position: 'tr', // PropTypes.oneOf(['tr', 'tc']),
  autoDismiss: 5,
  dismissible: true,
  action: null,
  hidden: false,
  date: new Date()
};

/**
 * Flash messages
 *
 * @author Radek Tomiška
 */
export default class FlashMessagesManager {

  constructor() {
    this.serverUnavailableTimeout = DEFAULT_SERVER_UNAVAILABLE_TIMEOUT;
  }

  getServerUnavailableTimeout() {
    return this.serverUnavailableTimeout;
  }

  setServerUnavailableTimeout(serverUnavailableTimeout) {
    this.serverUnavailableTimeout = serverUnavailableTimeout;
  }

  addMessage(message) {
    return dispatch => {
      if (message.key) {
        dispatch(this.hideMessage(message.key));
      }
      dispatch({
        type: ADD_MESSAGE,
        message: _.merge({}, message, { date: new Date() })
      });
    };
  }

  addError(error, context = null) {
    return this.addErrorMessage({}, error, context);
  }

  /**
  * Transforms options to message and adds default props
  */
  createMessage(options) {
    if (!options) {
      return null;
    }
    let message = options;
    if (message == null) {
      message = LocalizationService.i18n('message.success.common', { defaultValue: 'The operation was successfully completed' });
    }
    if (typeof message === 'string') {
      message = _.merge({}, _DEFAULT_MESSAGE, { message });
    }
    // errors are shown centered by default
    if (message.level && (message.level === 'error' /* || message.level === 'warning' */) && !message.position) {
      message.position = 'tc';
    }
    // add default
    message = _.merge({}, _DEFAULT_MESSAGE, message);
    if (!message.title && !message.message) {
      message.message = LocalizationService.i18n('message.success.common', { defaultValue: 'The operation was successfully completed' });
    }
    if (message.title && typeof message.title === 'object') {
      message.title = JSON.stringify(message.title);
    }
    if (message.message && typeof message.message === 'object') {
      message.message = JSON.stringify(message.message);
    }
    return message;
  }

  /**
   * Converts result model to flash message
   *
   * @param  {ResultModel} resultModel
   * @return {FlashMessage}
   */
  convertFromResultModel(resultModel) {
    if (!resultModel) {
      return null;
    }
    // automatic localization
    const messageTitle = LocalizationService.i18n(resultModel.module + ':error.' + resultModel.statusEnum + '.title', this._prepareParams(resultModel.parameters, `${resultModel.statusEnum} (${resultModel.statusCode}:${resultModel.id})`));
    let defaultMessage = resultModel.message;
    if (!_.isEmpty(resultModel.parameters)) {
      defaultMessage += ' (';
      let first = true;
      for (const parameterKey in resultModel.parameters) {
        if (!first) {
          defaultMessage += ', ';
        } else {
          first = false;
        }
        defaultMessage += `${parameterKey}:${resultModel.parameters[parameterKey]}`;
      }
      defaultMessage += ')';
    }
    const messageText = LocalizationService.i18n(resultModel.module + ':error.' + resultModel.statusEnum + '.message', this._prepareParams(resultModel.parameters, defaultMessage));
    //
    const levelStatusCode = parseInt(resultModel.statusCode, 10);
    let level; // 4xx - warning message, 5xx - error message
    if (levelStatusCode >= 500) {
      level = 'error';
    } else if (levelStatusCode >= 200 && levelStatusCode < 300) {
      if (levelStatusCode === 202) {
        // accepted - info
        level = 'info';
      } else {
        level = 'success';
      }
    } else {
      level = 'warning';
      // TODO: info level?
    }
    //
    return this.createMessage({
      key: resultModel.statusEnum,
      level,
      title: messageTitle,
      message: messageText,
    });
  }

  convertFromError(error) {
    if (this.isServerUnavailableError(error)) {
      return {
        key: 'error-app-load',
        level: 'error',
        title: LocalizationService.i18n('content.error.503.description'),
        message: LocalizationService.i18n('content.error.503.note'),
        dismissible: true/* ,
        action: {
          label: 'Odeslat na podporu',
          callback: () => {
            alert('Comming soon.')
          }
        }*/
      };
    }
    //
    let errorMessage;
    if (error.statusEnum) { // our error message
      errorMessage = this.convertFromResultModel(error);
    } else { // system error - only contain message
      errorMessage = {
        level: error.level || 'error',
        title: error.title,
        message: error.message
      };
    }
    return errorMessage;
  }

  /**
   * Converts message from BE message
   *
   * @param  {IdmMessage} notificationMessage
   * @return {FlashMessage}
   */
  convertFromWebsocketMessage(notificationMessage) {
    if (!notificationMessage.model) {
      return notificationMessage;
    }
    //
    const message = this.convertFromResultModel(notificationMessage.model);
    // TODO: registrable message converters
    if (notificationMessage.key === 'core:event' && notificationMessage.model && notificationMessage.model.parameters && notificationMessage.model.parameters.processors) {
      message.children = (
        <div>
          <ol style={{ listStylePosition: 'inside' }}>
            {
              notificationMessage.model.parameters.processors.map(processor => {
                return (
                  <li>{ LocalizationService.i18n(`${processor.module}:processor.${processor.name}.title`, { defaultValue: processor.id }) } </li>
                );
              })
            }
          </ol>
        </div>
      );
    }
    if (notificationMessage.position) {
      message.position = notificationMessage.position;
    }
    if (notificationMessage.level) {
      message.level = notificationMessage.level;
    }
    return message;
  }

  addErrorMessage(message, error) {
    return (dispatch, getState) => {
      if (DEBUG) {
        getState().logger.error(message, error);
      }
      const errorMessage = _.merge({}, this.convertFromError(error), message);
      // error redirect handler
      if (this.isServerUnavailableError(error)) {
        // timeout to prevent showing message on ajax call interuption
        setTimeout(
          () => {
            dispatch(this.addMessage(errorMessage));
            dispatch(this._clearConfiguations());
          }
          , this.getServerUnavailableTimeout());
      // TODO: module defined exception handlers
      } else if (this._isLoginError(error)) {
        this._logoutImmediatelly(); // we dont want to propagate LOGOUT dispatch event ... we want propagate new event:
        dispatch({
          type: 'RECEIVE_LOGIN_EXPIRED'
        });
      } else if (this._isPasswordChangeError(error)) {
        dispatch(this._logoutImmediatelly());
        const username = error.parameters.identity;
        dispatch(routeActions.push(`/password/change?name=${username}`));
      } else {
        dispatch(this.addMessage(errorMessage));
      }
    };
  }

  // TODO: cyclic dependency in security manager
  _logoutImmediatelly() {
    const authenticateService = new AuthenticateService();
    authenticateService.clearStorage();
    return {
      type: 'LOGOUT'
    };
  }

  // TODO: cyclic dependency in configuration manager - refactor action constants to separate file
  _clearConfiguations() {
    return {
      type: 'APP_UNAVAILABLE'
    };
  }

  /**
   * Returns true, if new login is needed
   */
  _isLoginError(error) {
    if (!error.statusEnum) {
      return false;
    }
    if (error.statusEnum === 'XSRF'
        || error.statusEnum === 'LOG_IN'
        || error.statusEnum === 'AUTH_EXPIRED'
        || error.statusEnum === 'AUTHORITIES_CHANGED') {
      return true;
    }
    return false;
  }

  /**
   * Refurn true, if pasword has to be changed before usage of application
   */
  _isPasswordChangeError(error) {
    if (!error.statusEnum) {
      return false;
    }
    if (error.statusEnum === 'MUST_CHANGE_IDM_PASSWORD') {
      return true;
    }
    return false;
  }

  /**
   * Returns true, if BE is not available
   */
  isServerUnavailableError(error) {
    if (error
        && error.message
        && (error.message.indexOf('NetworkError') > -1 || error.message.indexOf('Failed to fetch') > -1 || (error.statusCode && (error.statusCode === '400' || error.statusCode === '503')))) {
      return true;
    }
    return false;
  }

  _prepareParams(params, defaultMessage) {
    let results = {};
    if (params) {
      // iterate over all params do necessary converts
      // TODO converter for date
      // for (const key in params) {
      //   if (params.hasOwnProperty(key)) {
      //     console.log(params[key], 111);
      //     if (!isNaN(Number(params[key]))) {
      //       // nothing, just number
      //     } else if (params[key] instanceof Date && !isNaN(new Date(params[key])) && params) {
      //       // convert to date
      //       const date = moment(new Date(params[key]));
      //       params[key] = date.format(LocalizationService.i18n('format.date'));
      //     }
      //   }
      // }
      results = _.merge({}, params);
    }
    if (defaultMessage) {
      results.defaultValue = defaultMessage;
    }
    return results;
  }

  hideMessage(id) {
    return {
      type: HIDE_MESSAGE,
      id
    };
  }

  removeMessage(id) {
    return {
      type: REMOVE_MESSAGE,
      id
    };
  }

  hideAllMessages() {
    return {
      type: HIDE_ALL_MESSAGES
    };
  }

  removeAllMessages() {
    return {
      type: REMOVE_ALL_MESSAGES
    };
  }

  redirect() {
    return dispatch => {
      dispatch(routeActions.push(`/error/403`));
    };
  }

}
