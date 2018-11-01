import FlashMessagesManager from '../flash/FlashMessagesManager';
import * as Utils from '../../utils';
/**
 * action types
 */
export const REQUEST_DATA = 'REQUEST_DATA';
export const STOP_REQUEST = 'STOP_REQUEST';
export const RECEIVE_DATA = 'RECEIVE_DATA';
export const CLEAR_DATA = 'CLEAR_DATA';
export const RECEIVE_ERROR = 'RECEIVE_ERROR';

/**
 * Encapsulate redux action for form data (create, edit) etc.
 *
 * @author Radek Tomiška
 */
export default class DataManager {

  constructor() {
    this.flashMessagesManager = new FlashMessagesManager();
  }

  /**
   * Request data from store - simply sets loading flag fot given uiKey
   *
   * @param  {string} uiKey - access ui key
   * @param  {any} data - stored data
   * @return {action} - action
   */
  requestData(uiKey) {
    return {
      type: REQUEST_DATA,
      uiKey
    };
  }

  /**
   * Stops request
   *
   * @param  {string} uiKey - ui key for loading indicator etc
   * @param  {object} error - received error
   * @return {action}
   */
  stopRequest(uiKey, error = null, cb = null) {
    if (cb) {
      cb(null, error);
    }
    return {
      type: STOP_REQUEST,
      uiKey,
      error
    };
  }

  /**
   * Add data to store - data can be read by other components (receiveData alias)
   *
   * @param  {string} uiKey - access ui key
   * @param  {any} data - stored data
   * @return {action} - action
   */
  storeData(uiKey, data) {
    return this.receiveData(uiKey, data);
  }

  /**
   * Add data to store - data can be read by other components
   *
   * @param  {string} uiKey - access ui key
   * @param  {any} data - stored data
   * @return {action} - action
   */
  receiveData(uiKey, data, cb = null) {
    if (cb) {
      cb(data, null);
    }
    return {
      type: RECEIVE_DATA,
      uiKey,
      data
    };
  }

  /**
   * Clear (remove) data from store
   *
   * @param  {string} uiKey - access ui key
   * @return {action} - action
   */
  clearData(uiKey) {
    return {
      type: CLEAR_DATA,
      uiKey
    };
  }

  /**
   * Receive error from server call
   *
   * @param  {string|number} id - entity identifier (could be null)
   * @param  {object} entity - received entity
   * @param  {string} uiKey - ui key for loading indicator etc
   * @param  {object} error - received error
   * @return {object} - action
   */
  receiveError(data, uiKey, error = null, cb = null) {
    return (dispatch) => {
      if (cb) {
        cb(null, error);
      } else {
        dispatch(this.flashMessagesManager.addErrorMessage({
          key: 'error-' + uiKey
        }, error));
      }
      dispatch({
        type: RECEIVE_ERROR,
        uiKey,
        error
      });
    };
  }

  /**
   * Returns data associated with the given key
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - access ui key
   * @return {any} - stored data
   */
  static getData(state, uiKey) {
    if (!state || !uiKey) {
      return null;
    }
    if (!state.data.data.has(uiKey)) {
      return null;
    }
    return state.data.data.get(uiKey);
  }

  /**
   * Returns true, when loading for given uiKey proceed
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - access ui key
   * @return {any} - stored data
   * @since 9.0.0
   */
  static isShowLoading(state, uiKey) {
    return Utils.Ui.isShowLoading(state, uiKey);
  }
}
