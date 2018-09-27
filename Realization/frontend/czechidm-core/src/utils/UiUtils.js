import EntityUtils from './EntityUtils';
import Joi from 'joi';
import _ from 'lodash';

/**
 * Helper methods for ui state
 *
 * @author Radek Tomiška
 */
export default class UiUtils {

  /**
   * Returns state associated with given key or null if state for given key does not exist
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {object} - ui state
   */
  static getUiState(state, uiKey) {
    if (!state || !uiKey || !state.data.ui[uiKey]) {
      return null;
    }
    return state.data.ui[uiKey];
  }

  /**
   * Returns true, when loading for given uiKey proceed
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {boolean} - true, when loading for given uiKey proceed
   */
  static isShowLoading(state, uiKey) {
    const uiState = UiUtils.getUiState(state, uiKey);
    if (!uiState) {
      return false;
    }
    return uiState.showLoading;
  }

  /**
   * Returns error asigned to given uiKey or null, if no error is found
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {object} error
   */
  static getError(state, uiKey) {
    const uiState = UiUtils.getUiState(state, uiKey);
    if (!uiState) {
      return null;
    }
    if (!uiState.error) {
      return null;
    }
    return uiState.error;
  }

  /**
   * Returns search parameters associated with given ui key. If state for given key does not exist, then returns defaultSearchParameters
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {boolean} - true, when loading for given uiKey proceed
   */
  static getSearchParameters(state, uiKey, defaultSearchParameters = null) {
    const uiState = UiUtils.getUiState(state, uiKey);
    if (!uiState) {
      return false;
    }
    return uiState.searchParameters ? uiState.searchParameters : defaultSearchParameters;
  }

  /**
   * Read entities associarted by given uiKey items from ui store
   *
   * @param  {state} state [description]
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {array[entity]}
   */
  static getEntities(state, uiKey) {
    const uiState = UiUtils.getUiState(state, uiKey);
    if (!uiState) {
      return [];
    }
    return EntityUtils.getEntitiesByIds(state, uiState.entityType, uiState.items, uiState.trimmed);
  }

  /**
   * Returns css row class for given entity
   * - when entity is disabled - returns `disabled`
   * - when entity is invalid - returns `disabled`
   * - otherwise: empty string
   *
   * @param  {object} entity
   * @return {string} css row class
   */
  static getRowClass(entity) {
    if (!entity) {
      return '';
    }
    if (EntityUtils.isDisabled(entity)) {
      return 'disabled';
    }
    if (!EntityUtils.isValid(entity)) {
      return 'disabled';
    }
    return '';
  }

  /**
   * Returns css row class for given entity
   * - when entity is disabled - returns `disabled`
   * - otherwise: empty string
   *
   * @param  {object} entity
   * @return {string} css row class
   */
  static getDisabledRowClass(entity) {
    if (!entity) {
      return '';
    }
    if (EntityUtils.isDisabled(entity)) {
      return 'disabled';
    }
    return '';
  }

  /**
   * Returns css row class for given entity
   * - when entity is disabled - returns `disabled`
   * - otherwise: empty string
   *
   * @param  {object} entity
   * @return {string} css row class
   */
  static getRequestRowClass(entity) {
    if (!entity || !entity._embedded || !entity._embedded.requestItem) {
      return '';
    }
    if (entity._embedded.requestItem.operation === 'ADD') {
      return 'success';
    }

    if (entity._embedded.requestItem.operation === 'UPDATE') {
      return 'warning';
    }

    if (entity._embedded.requestItem.operation === 'REMOVE') {
      return 'danger';
    }
    return '';
  }

  /**
   * Returns random css level
   *
   * @return {string}
   */
  static getRandomLevel() {
    const min = Math.ceil(0);
    const max = Math.floor(6);
    const levelNumber = Math.floor(Math.random() * (max - min)) + min;

    switch (levelNumber) {
      case 5: {
        return 'danger';
      }
      case 4: {
        return 'warning';
      }
      case 3: {
        return 'info';
      }
      case 2: {
        return 'success';
      }
      default: {
        return 'primary';
      }
    }
  }

  /**
   * Return simple class name
   *
   * @param  {string} taskType cannonical class name
   * @return {string}
   */
  static getSimpleJavaType(javaType) {
    if (!javaType) {
      return null;
    }
    return javaType.split('.').pop(-1);
  }

  /**
   * Do substring on given data by max length. Substring is not on char byt on word.
   * Last word will be whole. Get begining part.
   * If data is cutted then substring is extended by suffix from right.
   * Examples:
   * UiUtils.substringBegin('This is too long text', 5, '');
   * UiUtils.substringBegin('hello/j/', 6, '/', '...'); --> 'hello...'
   *
   * @param  {String} data
   * @param  {Number} maxLength
   * @param  {String} cutChar Character cutting words
   * @param  {String} suffix String which extends cutted data from right
   * @return {String}
   */
  static substringBegin(data, maxLength, cutChar, suffix = '') {
    if (data === null || data === undefined) {
      return null;
    }
    if (data.length <= maxLength) {
      return data;
    }
    if (data.charAt(maxLength) === cutChar) {
      const result = data.substr(0, maxLength) + suffix;
      return result;
    }
    //
    data = data + cutChar;
    let result = data.replace(/<(?:.|\n)*?>/gm, '').substr(0, maxLength);
    result = result.substr(0, Math.min(result.length, result.lastIndexOf(cutChar)));
    result = result + suffix;
    return result;
  }
  /**
   * Do substring on given data by max length. Substring is not on char byt on word.
   * Last word will be whole. Get ending part.
   * If data is cutted then substring is extended by suffix from left.
   * Examples:
   * UiUtils.substringEnd('this/is/path', 5, '/');
   * UiUtils.substringEnd('hello/j/', 4, '/', '...')); -->'.../j/'
   *
   * @param  {String} data
   * @param  {Number} maxLength
   * @param  {String} cutChar Character cutting words
   * @param  {String} suffix String which extends cutted data from left
   * @return {String}
   */
  static substringEnd(data, maxLength, cutChar, suffix = '') {
    if (data === null || data === undefined) {
      return null;
    }
    if (data.length <= maxLength) {
      return data;
    }
    //
    data = cutChar + data;
    let result = data.replace(/<(?:.|\n)*?>/gm, '').substr(data.length - maxLength, data.length);
    result = result.substr(result.indexOf(cutChar), result.length);
    result = suffix + result;
    return result;
  }

  /**
  * Do substring on given data by max length. Substring is not on char byt on word.
  * Last word will be whole.
  *
  * @param  {String} data
  * @param  {Number} maxLength
  * @return {String}
  */
  static substringByWord(data, maxLength, suffix = null) {
    return this.substringBegin(data, maxLength, ' ', suffix);
  }

  /**
   * Encode string in utf-8 to base 64
   *
   * @param  {String}
   * @return {String}
   */
  static utf8ToBase64(data) {
    if (data != null) {
      return window.btoa(unescape(encodeURIComponent(data)));
    }
  }

  /**
   * Decode given string in base 64 to utf-8 substringBegin
   *
   * @param  {String}
   * @return {String}
   */
  static base64ToUtf8(data) {
    if (data != null) {
      return decodeURIComponent(escape(window.atob(data)));
    }
  }

  /**
   * Method return validation for only signed integer with maximum defined
   * in constant MAX_VALUE_INTEGER or you can define this value by parameter.
   * Null values and zero are allowed.
   *
   * @param {Integer}
   * @return {Integer}
   */
  static getIntegerValidation(max) {
    if (max) {
      return Joi.number().integer().allow(null).allow(0).positive().max(max);
    }
    return Joi.number().integer().allow(null).allow(0).positive().max(UiUtils.MAX_VALUE_INTEGER);
  }

  /**
   * Parse given text and search localization parameter. This paramaters are wrapped in {{value}}.
   *
   * @param  {String} text
   * @return {Ordered array, withs parsed values}
   */
  static parseLocalizationParams(text) {
    if (!text) {
      return text;
    }
    const regex = new RegExp('\{{([^{}])*}}', 'g');
    let params = text.match(regex);

    if (!params) {
      params = [];
    }
    const results = {};
    for (let i = 0; i < params.length; i++) {
      const result = params[i].replace(/{{/g, '').replace(/}}/g, '');
      if (result.startsWith('context_')) {
        results.context = result.replace(/context_/g, '');
      } else {
        results[i] = result;
      }
    }
    // Set default value (original text without [{{}}])
    results.defaultValue = text.replace(/{{/g, '').replace(/}}/g, '');
    return results;
  }

  /**
   * Spinal case string transformation
   *
   * @param  {string} text
   * @return {string}
   * @since 8.1.2
   */
  static spinalCase(text) {
    if (!text) {
      return null;
    }
    //
    return _.kebabCase(text.toLowerCase());
  }

  /**
   * Escapes all occurencesof double quotes
   *
   * @param  {string} text
   * @return {string}
   * @since 8.1.2
   */
  static escapeDoubleQuotes(text) {
    if (!text) {
      return null;
    }
    return text.split('"').join('\\"');
  }


  /**
   * Transforma object value into string - can be rendered
   *
   * @param  {object} objectValue
   * @return {string}
   * @since 8.2.0
   */
  static toStringValue(objectValue) {
    if (_.isArray(objectValue)) {
      return objectValue.join(', ');
    } else if (_.isObject(objectValue)) {
      return JSON.stringify(objectValue);
    }
    return objectValue + '';
  }

  /**
   * Returns true, if given value is filled - usable for boolean values - false is not empty.
   *
   * @param  {object}  objectValue
   * @return {Boolean}
   */
  static isEmpty(objectValue) {
    // null value
    if (objectValue === null) {
      return true;
    }
    // undefined value
    if (objectValue === undefined) {
      return true;
    }
    // empty string
    if (objectValue === '') {
      return true;
    }
    return false;
  }
}

UiUtils.MAX_VALUE_INTEGER = 2147483647;
