import AbstractService from './AbstractService';
import SearchParameters from '../domain/SearchParameters';
/**
 * Abstract form values
 *
 * @author Roman Kučera
 * @author Radek Tomiška
 */
export default class FormValueService extends AbstractService {

  getApiPath() {
    return '/form-values';
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return `${entity.id}`; // TODO: attribute name + value?
  }

  supportsBulkAction() {
    return true;
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('formAttribute.code', 'asc');
  }
}
