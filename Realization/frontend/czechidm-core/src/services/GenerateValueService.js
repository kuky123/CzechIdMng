import AbstractService from './AbstractService';
import SearchParameters from '../domain/SearchParameters';
import RestApiService from './RestApiService';
import * as Utils from '../utils';

/**
 * Generate values service
 *
 * @author Ondřej Kopr
 */
class GenerateValueService extends AbstractService {

  getApiPath() {
    return '/generate-values';
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return entity.generatorType;
  }

  // dto
  supportsPatch() {
    return false;
  }

  /**
   * Returns default searchParameters for scripts
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('generatorType');
  }

  /**
   * Loads all registered entities wich supports generating
   *
   * @return {promise}
   */
  getSupportedTypes() {
    return RestApiService
    .get(this.getApiPath() + '/search/supported-types')
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
   * Loads all available generators
   *
   * @return {promise}
   */
  getAvailableGenerators() {
    return RestApiService
    .get(this.getApiPath() + '/search/available-generators')
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

export default GenerateValueService;
