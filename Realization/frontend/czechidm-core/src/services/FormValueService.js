import AbstractService from './AbstractService';
import SearchParameters from '../domain/SearchParameters';
import RestApiService from './RestApiService';
import * as Utils from '../utils';
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

  deleteValue(value) {
    return RestApiService
        .post(this.getApiPath() + '/' + value.id + '/delete', value._embedded.formAttribute._embedded.formDefinition)
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
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('formAttribute.code', 'asc');
  }
}
