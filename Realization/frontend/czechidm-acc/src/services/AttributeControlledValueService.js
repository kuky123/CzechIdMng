import { Services, Domain } from 'czechidm-core';

export default class AttributeControlledValueService extends Services.AbstractService {

  constructor() {
    super();
  }

  // dto
  supportsPatch() {
    return false;
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return entity.value;
  }

  getApiPath() {
    return '/system-attribute-controlled-values';
  }

  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(Domain.SearchParameters.NAME_QUICK).clearSort().setSort('created', 'DESC');
  }
}
