import EntityManager from './EntityManager';
import { FormValueService } from '../../services';

/**
 * Abstract form values
 *
 * @author Roman Kučera
 * @author Radek Tomiška
 */
export default class FormValueManager extends EntityManager {

  constructor() {
    super();
    this.service = new FormValueService();
  }

  getService() {
    return this.service;
  }

  getEntityType() {
    return 'FormValue';
  }

  getCollectionType() {
    return 'formValues';
  }

  deleteValue(value, uiKey, cb) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().deleteValue(value)
          .then(() => {
            if (cb) {
              cb();
            }
            dispatch(this.dataManager.stopRequest(uiKey));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
    };
  }
}
