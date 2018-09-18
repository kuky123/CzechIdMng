import _ from 'lodash';
import UiUtils from './UiUtils';

/**
 * Helper methods for ui permissions
 *
 * @author Radek Tomiška
 */
export default class PermissionUtils {

  /**
   * What logged user can do with ui key and underlying entity.
   *
   * @param  {state} state - application state
   * @param  {string} uiKey - ui key for loading indicator etc.
   * @return {arrayOf(permission)} what logged user can do with ui key and underlying entity or null, if permissions are not loaded
   */
  static getPermissions(state, uiKey) {
    const uiState = UiUtils.getUiState(state, uiKey);
    if (!uiState) {
      return undefined;
    }
    return uiState.permissions;
  }

  /**
   * Returns true, if "currently logged user has given permission" - respectively if given permissions contains given permission.
   *
   * @param  {arrayOf(permission)} permissions - loaded permissions - see getPermissions method.
   * @param  {string}  permission base permission
   * @return {Boolean} Returns true, if "currently logged user has given permission".
   */
  static hasPermission(permissions, permission) {
    if (!permissions || !permission) {
      return false;
    }
    return _.includes(permissions, permission) || _.includes(permissions, 'ADMIN');
  }

}
