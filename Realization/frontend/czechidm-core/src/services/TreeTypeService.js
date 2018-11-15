import RestApiService from './RestApiService';
import AbstractService from './AbstractService';
import * as Utils from '../utils';
import SearchParameters from '../domain/SearchParameters';

/**
 * Tree type administration
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class TreeTypeService extends AbstractService {

  const
  getApiPath() {
    return '/tree-types';
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    if (entity.name === entity.code) {
      return entity.name;
    }
    return `${entity.name} (${entity.code})`;
  }

  supportsAuthorization() {
    return true;
  }

  getGroupPermission() {
    return 'TREETYPE';
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('name');
  }

  /**
   * Returns dafult tree Type
   *
   * @return {promise}
   */
  getDefaultTreeType() {
    return RestApiService
    .get(this.getApiPath() + '/search/default')
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
   * Returns configuration properties for given tree
   *
   * @param  {string} treeTypeId
   * @return {promise}
   */
  getConfigurations(treeTypeId) {
    return RestApiService
    .get(this.getApiPath() + `/${treeTypeId}/configurations`)
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
   * Rebuild forest index for given tree type
   *
   * @param  {string} treeTypeId
   * @return {promise}        [description]
   */
  rebuildIndex(treeTypeId) {
    return RestApiService
    .put(this.getApiPath() + `/${treeTypeId}/index/rebuild`)
    .then(response => {
      if (response.status === 204) {
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
}

export default TreeTypeService;
