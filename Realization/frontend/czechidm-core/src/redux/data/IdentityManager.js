import FormableEntityManager from './FormableEntityManager';
import SecurityManager, { RECEIVE_PROFILE } from '../security/SecurityManager';
import { IdentityService } from '../../services';
import DataManager from './DataManager';
import ProfileManager from './ProfileManager';
import * as Utils from '../../utils';
import { RECEIVE_PERMISSIONS } from './EntityManager';

/**
 * Manager for identities fetching
 *
 * @author Radek Tomiška
 */
export default class IdentityManager extends FormableEntityManager {

  constructor() {
    super();
    this.identityService = new IdentityService();
    this.dataManager = new DataManager();
    this.profileManager = new ProfileManager();
  }

  getService() {
    return this.identityService;
  }

  getEntityType() {
    return 'Identity'; // TODO: constant or enumeration
  }

  getCollectionType() {
    return 'identities';
  }

  /**
   * Return resource identifier on FE (see BE - IdentifiableByName)
   *
   * @return {string} secondary identifier (unique property)
   */
  getIdentifierAlias() {
    return 'username';
  }

  getFullName(identity) {
    return this.getService().getFullName(identity);
  }

  /**
   * Sets user activity
   *
   * @param {array[string]} usernames selected usernames
   * @param {string} bulkActionName activate|deactivate
   */
  setUsersActivity(entities, bulkActionName) {
    return (dispatch) => {
      dispatch(
        this.startBulkAction(
          {
            name: bulkActionName,
            title: this.i18n(`content.identities.action.${bulkActionName}.header`, { count: entities.length })
          },
          entities.length
        )
      );
      const successEntities = [];
      entities.reduce((sequence, identity) => {
        return sequence.then(() => {
          if (bulkActionName === 'activate') {
            return this.getService().activate(identity.id);
          }
          return this.getService().deactivate(identity.id);
        }).then(json => {
          dispatch(this.updateBulkAction());
          successEntities.push(identity);
          // new entity to redux trimmed store
          json._trimmed = true;
          dispatch(this.receiveEntity(identity.id, json));
        }).catch(error => {
          dispatch(this.flashMessagesManager.addErrorMessage({ title: this.i18n(`content.identities.action.${bulkActionName}.error`, { username: this.getNiceLabel(identity) }) }, error));
          throw error;
        });
      }, Promise.resolve())
      .catch(() => {
        // nothing - message is propagated before
        // catch is before then - we want execute nex then clausule
      })
      .then(() => {
        if (successEntities.length > 0) {
          dispatch(this.flashMessagesManager.addMessage({
            level: successEntities.length === entities.length ? 'success' : 'info',
            message: this.i18n(`content.identities.action.${bulkActionName}.success`, { usernames: this.getNiceLabels(successEntities).join(', ') })
          }));
        }
        dispatch(this.stopBulkAction());
      });
    };
  }

  /**
   * Load username authorities from BE
   *
   * @param  {string} username
   * @param  {string} uiKey
   * @return {array[object]}
   */
  fetchAuthorities(username, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getAuthorities(username)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Get given identity's main position in organization
   *
   * @param  {string} username
   * @param  {string} uiKey
   * @return {array[object]}
   */
  fetchWorkPosition(username, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getWorkPosition(username)
        .then(json => {
          dispatch(this.dataManager.receiveData(uiKey, json));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Return true when currently logged user can change password
   *
   * @param  {string} passwordChangeType see consts bottom
   * @param  {arrayOf(string)} permissions logged identity's base permissions on selected identity
   * @return {bool}
   */
  canChangePassword(passwordChangeType, permissions) {
    if (!passwordChangeType || passwordChangeType === IdentityManager.PASSWORD_DISABLED) {
      // password cannot be changed by environment configuration
      return false;
    }
    if (SecurityManager.isAdmin()) {
      // admin
      return true;
    }
    return Utils.Permission.hasPermission(permissions, 'PASSWORDCHANGE');
  }

  /**
   * Profile ui key
   *
   * @param  {[type]} identityId [description]
   * @return {[type]}            [description]
   */
  resolveProfileUiKey(identityId) {
    return `${IdentityManager.UIKEY_PROFILE}${identityId}`;
  }

  /**
   * Save logged identity profile metadata.
   * Refresh logged user context profile.
   *
   * @param  {Profile} profile metadata
   * @return {action}
   */
  saveCurrentProfile(identityId, profile) {
    return (dispatch) => {
      this.getService().patchProfile(identityId, profile)
        .then((entity) => {
          dispatch({
            type: RECEIVE_PROFILE,
            profile: entity
          });
        })
        .catch(error => {
          // log error only into message history (e.g. 403 - authorization policies are wrong configured)
          // saving profile is optional - logged identity couldn't have permission for read profile (or profile not found)
          this.flashMessagesManager.addErrorMessage({ hidden: true, level: 'info' }, error);
        });
    };
  }

  /**
   * Upload image to BE
   */
  uploadProfileImage(identityId, formData) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().uploadProfileImage(identityId, formData)
        .then(() => {
          dispatch(this.dataManager.receiveData(uiKey, { imageUrl: null })); // enforce reload
          dispatch(this.downloadProfileImage(identityId));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  downloadProfileImage(identityId) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch, getState) => {
      const profile = DataManager.getData(getState(), uiKey);
      if (profile && (profile.imageUrl || profile.imageUrl === false)) {
        // profile already loaded or image not found before (imageUrl === false)
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().downloadProfileImage(identityId)
          .then(response => {
            if (response.status === 404) {
              return null;
            } else if (response.status === 200) {
              return response.blob();
            }
            const json = response.json();
            if (Utils.Response.hasError(json)) {
              throw Utils.Response.getFirstError(json);
            }
            if (Utils.Response.hasInfo(json)) {
              throw Utils.Response.getFirstInfo(json);
            }
          })
          .then(blob => {
            let imageUrl = false;
            if (blob) {
              imageUrl = URL.createObjectURL(blob);
            }
            dispatch(this.dataManager.receiveData(uiKey, {
              imageUrl
            }));
          })
          .catch(error => {
            dispatch(this.receiveError(null, uiKey, error));
          });
      }
    };
  }

  deleteProfileImage(identityId) {
    const uiKey = this.resolveProfileUiKey(identityId);
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().deleteProfileImage(identityId)
        .then(() => {
          dispatch(this.dataManager.receiveData(uiKey, { imageUrl: false }));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Fetch profile permissions
   *
   * Lookout: permissions will be stored under identity identifier in reducer.
   */
  fetchProfilePermissions(id) {
    return (dispatch, getState) => {
      if (getState().security.userContext.isExpired) {
        return;
      }
      //
      this.getService().getProfilePermissions(id)
      .then(permissions => {
        const profileKey = this.profileManager.resolveUiKey(null, id);
        //
        dispatch({
          type: RECEIVE_PERMISSIONS,
          id,
          entityType: this.profileManager.getEntityType(),
          permissions,
          uiKey: profileKey
        });
      });
    };
  }

  /**
   * PreValidates password
   *
   * @param  {string} requestData
   * @return
   */
  preValidate(requestData) {
    return this.identityService.preValidate(requestData);
  }
}

IdentityManager.PASSWORD_DISABLED = 'DISABLED';
IdentityManager.PASSWORD_ALL_ONLY = 'ALL_ONLY';
IdentityManager.PASSWORD_CUSTOM = 'CUSTOM';
IdentityManager.UIKEY_PROFILE = 'identity-profile-';
