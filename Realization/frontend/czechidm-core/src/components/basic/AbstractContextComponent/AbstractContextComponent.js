import React from 'react';
import PropTypes from 'prop-types';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import { FlashMessagesManager, ConfigurationManager } from '../../../redux';
import { i18n } from '../../../services/LocalizationService';

/**
 * Automatically injects redux context (store) to component context,
 * localization,
 * add message to context.
 *
 * @author Radek Tomiška
 */
class AbstractContextComponent extends AbstractComponent {

  constructor(props, context) {
    super(props, context);
    this.flashMessagesManager = new FlashMessagesManager();
  }

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return null;
  }

  /**
   * Add flash message, see more in FlashMessages component
   *
   * @param {Message} message
   * @param {Event} event
   */
  addMessage(message, event) {
    if (event) {
      event.preventDefault();
    }
    this.context.store.dispatch(this.flashMessagesManager.addMessage(message));
  }

  /**
   * Add error flash message, see more in FlashMessages component
   *
   * @param {Error} error message (json failure)
   * @param {Event} event
   */
  addError(error, event) {
    if (event) {
      event.preventDefault();
    }
    this.context.store.dispatch(this.flashMessagesManager.addError(error, this.context));
  }

  /**
   * Add error flash message, see more in FlashMessages component
   *
   * @param {Message} message
   * @param {Error} error message (json failure)
   * @param {Event} event
   */
  addErrorMessage(message, error, event) {
    if (event) {
      event.preventDefault();
    }
    this.context.store.dispatch(this.flashMessagesManager.addErrorMessage(message, error, this.context));
  }

  /**
   * Hide all flash message, see more in FlashMessages component
   */
  hideAllMessages() {
    this.context.store.dispatch(this.flashMessagesManager.hideAllMessages());
  }

  /**
   * Hide flash message by id or key
   *
   * @param  {string} idOrKey message id or key
   */
  hideMessage(idOrKey) {
    this.context.store.dispatch(this.flashMessagesManager.hideMessage(idOrKey));
  }

  /**
   * Returns localized message
   * - for supported options see http://i18next.com/pages/doc_features.html
   *
   * @param  {string} key     localization key
   * @param  {object} options parameters
   * @return {string}         localized message
   */
  _i18n(key, options) {
    let result = i18n(key, options);
    // escape html
    if (options && options.escape === false && key !== result) {
      result = (<span dangerouslySetInnerHTML={{__html: i18n(key, options)}}/>);
    }
    return result;
  }

  /**
   * Automatically prepend component prefix to localization key
   * If overridened key isn't found in localization, then previous key is used
   *
   * @param  {string} key     localization key
   * @param  {object} options parameters
   * @return {string}         localized message
   */
  i18n(key, options) {
    if (!key) {
      return '';
    }
    //
    const componentKey = this.getComponentKey();
    //
    const resultKeyWithModule = (key.indexOf(':') > -1 || !componentKey) ? key : `${componentKey}.${key}`;
    const resultKeyWithoutModule = (resultKeyWithModule.indexOf(':') > -1) ? resultKeyWithModule.split(':')[1] : resultKeyWithModule;
    const i18nValue = this._i18n(resultKeyWithModule, options);
    if (i18nValue === resultKeyWithModule || i18nValue === resultKeyWithoutModule) {
      return this._i18n(key, options);
    }
    return i18nValue;
  }

  /**
   * Returns logger, which is configured for whole app in redux store
   *
   * @return {object} logger
   */
  getLogger() {
    if (this.context.store) {
      return this.context.store.getState().logger;
    }
    return LOGGER;
  }

  /**
   * Returns initialized flash message manager
   *
   * @return {FlashMessageManager}
   */
  getFlashManager() {
    return this.flashMessagesManager;
  }

  /**
   * Returns true, when application (BE) is in development stage
   *
   * @return {Boolean}
   */
  isDevelopment() {
    return ConfigurationManager.getEnvironmentStage(this.context.store.getState()) === 'development';
  }
}

AbstractContextComponent.propTypes = {
  ...AbstractComponent.propTypes
};

AbstractContextComponent.defaultProps = {
  ...AbstractComponent.defaultProps
};

AbstractContextComponent.contextTypes = {
  store: PropTypes.object
};

// Wrap the component to inject dispatch and state into it
export default AbstractContextComponent;
