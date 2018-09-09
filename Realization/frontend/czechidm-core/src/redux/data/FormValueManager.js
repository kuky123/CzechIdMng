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

}
