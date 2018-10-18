import * as Utils from '../utils';
import FormableEntityService from './FormableEntityService';
import RestApiService from './RestApiService';
import SearchParameters from '../domain/SearchParameters';
import PasswordPolicyService from './PasswordPolicyService';

const passwordPolicyService = new PasswordPolicyService();

/**
 * Identities endpoint
 *
 * @author Radek Tomiška
 */
class IdentityService extends FormableEntityService {

  getApiPath() {
    return '/identities';
  }

  supportsPatch() {
    return false;
  }

  supportsBulkAction() {
    return true;
  }

  getGroupPermission() {
    return 'IDENTITY';
  }

  getNiceLabel(entity) {
    let toString = '';
    if (entity) {
      if (!entity.lastName) {
        return entity.username;
      }
      toString += this.getFullName(entity);
      toString += (entity.username ? ` (${entity.username})` : '');
      toString += (entity.externalCode ? ` (${entity.externalCode})` : '');
    }
    return toString;
  }

  getFullName(entity) {
    let toString = '';
    if (entity) {
      toString += (entity.titleBefore ? (entity.titleBefore + ' ') : '');
      toString += (entity.firstName ? (entity.firstName + ' ') : '');
      toString += (entity.lastName ? entity.lastName : '');
      toString += (entity.titleAfter ? `, ${entity.titleAfter}` : '');
    }
    return toString;
  }

  /**
   * Change users pasword in given systems (by json)
   *
   * @param username {string}
   * @param passwordChangeDto {object}
   * @param token {string} - if token has to be used (from public page is is not needed, then *false* could be given)
   * @return {Promise}
   */
  passwordChange(username, passwordChangeDto, token = null) {
    return RestApiService.put(RestApiService.getUrl(`/public${this.getApiPath()}/${encodeURIComponent(username)}/password-change`), passwordChangeDto, token);
  }

  /**
   * Shows password policies rules on selected systems via exception
   *
   * @param passwordChangeDto {object}
   * @param token {string} - if token has to be used (from public page is is not needed, then *false* could be given)
   * @return {Promise}
   */
  preValidate(passwordChangeDto, token = null) {
    return RestApiService.put(RestApiService.getUrl(`/public${this.getApiPath()}/prevalidate`), passwordChangeDto, token);
  }

  /**
   * Generates password based on configured idm policies
   *
   * @return {Promise}
   */
  generatePassword() {
    return passwordPolicyService.generatePassword();
  }

  /**
   * Required idm password change for given user without login
   *
   * @param username {string}
   * @param oldPassword {string}
   * @param newPassword {string}
   * @return {Promise}
   */
  passwordMustChange(username, oldPassword, newPassword) {
    return RestApiService.put(this.getApiPath() + `/password/must-change`, {
      identity: username,
      oldPassword,
      newPassword
    });
  }

  /**
   * Get given user's roles
   *
   * @param username {string}
   * @param token {string}
   * @return {Promise}
   */
  getRoles(username, token = null) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(username)}/roles`, token)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('username');
  }

  /**
   * Deactivates given identity
   *
   * @param  {String} identity
   * @return {Promise}
   */
  deactivate(username) {
    return RestApiService
      .patch(this.getApiPath() + `/${encodeURIComponent(username)}/disable`, {})
      .then(response => {
        return response.json();
      })
      .then(jsonResponse => {
        if (Utils.Response.hasError(jsonResponse)) {
          throw Utils.Response.getFirstError(jsonResponse);
        }
        return jsonResponse;
      });
  }

  /**
   * Activates given identity
   *
   * @param  {String} identity
   * @return {Promise}
   */
  activate(username) {
    return RestApiService
      .patch(this.getApiPath() + `/${encodeURIComponent(username)}/enable`, {})
      .then(response => {
        return response.json();
      })
      .then(jsonResponse => {
        if (Utils.Response.hasError(jsonResponse)) {
          throw Utils.Response.getFirstError(jsonResponse);
        }
        return jsonResponse;
      });
  }

  /**
   * Returns identity accounts
   *
   * @param  {String} username
   * @return {Promise}
   */
  getAccounts(username) {
    return RestApiService.get(this.getApiPath() + `/${encodeURIComponent(username)}/accounts`);
  }

  /**
   * Get given identity's main position in organization
   *
   * @param username {string}
   * @param token {string}
   * @return {Promise}
   */
  getWorkPosition(username) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(username)}/work-position`)
    .then(response => {
      if (response.status === 204) {
        // no work position was found
        return {};
      }
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  getAuthorities(username) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(username)}/authorities`)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  /**
   * Start workflow for change permissions
   * @param  id Identity id
   * @return Promise  task instance
   */
  changePermissions(id) {
    return RestApiService.put(this.getApiPath() + `/${encodeURIComponent(id)}/change-permissions`, null).then(response => {
      if (response.status === 403) {
        throw new Error(403);
      }
      if (response.status === 404) {
        throw new Error(404);
      }
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  /**
   * Fetch profile
   *
   * @param  {string, number} identityId entity id
   * @param {string} token CIDMST token
   * @return {Promise}
   */
  getProfile(identityId, token = null) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile`, token)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  /**
   * Save (create + patch) profile
   *
   * @param  {string, number} identityId entity id
   * @param {Profile} profile
   * @return {Promise}
   */
  patchProfile(identityId, profile) {
    return RestApiService
    .patch(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile`, profile)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }

  /**
   * Upload image to BE
   */
  uploadProfileImage(identityId, formData) {
    return RestApiService
      .upload(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile/image`, formData)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (Utils.Response.hasInfo(json)) {
          throw Utils.Response.getFirstInfo(json);
        }
        return json;
      });
  }

  /**
   * Get image from BE
   */
  downloadProfileImage(identityId) {
    return RestApiService.download(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile/image`);
  }

  /**
   * Delete image from BE
   */
  deleteProfileImage(identityId) {
    return RestApiService
      .delete(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile/image`)
      .then(response => {
        if (response.status === 204) { // no content - ok
          return null;
        }
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (Utils.Response.hasInfo(json)) {
          throw Utils.Response.getFirstInfo(json);
        }
        return json;
      });
  }

  /**
   * Fetch profile permissions
   *
   * @param  {string, number} identityId entity id
   * @return {Promise}
   */
  getProfilePermissions(identityId) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(identityId)}/profile/permissions`)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    });
  }
}

export default IdentityService;
