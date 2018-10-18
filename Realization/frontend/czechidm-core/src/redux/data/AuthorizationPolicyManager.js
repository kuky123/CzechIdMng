import Immutable from 'immutable';
//
import AbstractRequestManager from './AbstractRequestManager';
import DataManager from './DataManager';
import { AuthorizationPolicyService } from '../../services';

/**
 * Role's granted authorities
 *
 * @author Radek Tomiška
 */
export default class AuthorizationPolicyManager extends AbstractRequestManager {

  constructor() {
    super();
    this.service = new AuthorizationPolicyService();
  }

  getService() {
    return this.service;
  }

  getEntitySubType() {
    return 'AuthorizationPolicy';
  }

  getCollectionType() {
    return 'authorizationPolicies';
  }

  /**
   * Loads all registered evaluators (available for authorization policies)
   *
   * @return {action}
   */
  fetchSupportedEvaluators() {
    const uiKey = AuthorizationPolicyManager.UI_KEY_SUPPORTED_EVALUATORS;
    //
    return (dispatch, getState) => {
      const loaded = DataManager.getData(getState(), uiKey);
      if (loaded) {
        // we dont need to load them again - change depends on BE restart
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getSupportedEvaluators()
          .then(json => {
            let evaluators = new Immutable.Map();
            if (json._embedded && json._embedded.authorizationEvaluators) {
              json._embedded.authorizationEvaluators.forEach(item => {
                evaluators = evaluators.set(item.evaluatorType, item);
              });
            }
            dispatch(this.dataManager.receiveData(uiKey, evaluators));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
      }
    };
  }

  /**
   * Loads all authorizable types (available for authorization policies)
   *
   * @return {action}
   */
  fetchAuthorizableTypes() {
    const uiKey = AuthorizationPolicyManager.UI_KEY_AUTHORIZABLE_TYPES;
    //
    return (dispatch, getState) => {
      const loaded = DataManager.getData(getState(), uiKey);
      if (loaded) {
        // we dont need to load them again - change depends on BE restart
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getAuthorizableTypes()
          .then(json => {
            let authorizableTypes = new Immutable.Map();
            if (json._embedded && json._embedded.authorizableTypes) {
              json._embedded.authorizableTypes.forEach(item => {
                authorizableTypes = authorizableTypes.set(item.id, item);
              });
            }
            dispatch(this.dataManager.receiveData(uiKey, authorizableTypes));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
      }
    };
  }
}

AuthorizationPolicyManager.UI_KEY_SUPPORTED_EVALUATORS = 'authorization-supported-evaluators';
AuthorizationPolicyManager.UI_KEY_AUTHORIZABLE_TYPES = 'authorization-authorizable-types';
