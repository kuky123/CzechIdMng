import { Managers } from 'czechidm-core';
import { AttributeControlledValueService } from '../services';

const service = new AttributeControlledValueService();

export default class AttributeControlledValueManager
 extends Managers.EntityManager {

  constructor() {
    super();
  }

  getService() {
    return service;
  }

  getEntityType() {
    return 'AttributeControlledValue';
  }

  getCollectionType() {
    return 'controlledValues';
  }
}
