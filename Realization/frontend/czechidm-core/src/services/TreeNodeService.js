import FormableEntityService from './FormableEntityService';
import SearchParameters from '../domain/SearchParameters';
import TreeTypeService from './TreeTypeService';
import RestApiService from './RestApiService';
import * as Utils from '../utils';

/**
 * Tree nodes  - structure items.
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class TreeNodeService extends FormableEntityService {

  constructor() {
    super();
    this.treeTypeService = new TreeTypeService();
  }

  getApiPath() {
    return '/tree-nodes';
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
    return 'TREENODE';
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
   * Returns search parameters by parent ID, used in tree
   */
  getTreeSearchParameters() {
    return this.getDefaultSearchParameters().setName(TreeNodeService.TREE_SEARCH).setSize(50);
  }

  /**
   * Returns search parameters for search roots
   */
  getRootSearchParameters() {
    return this.getDefaultSearchParameters().setName(TreeNodeService.ROOT_SEARCH).setSize(50);
  }

  /**
   * Returns dafult tree node
   *
   * @return {promise}
   */
  getDefaultTreeNode() {
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
}

/**
 * Search by parent ID for tree
 * @type {Number}
 */
TreeNodeService.TREE_SEARCH = 'children';

/**
 * Search roots
 */
TreeNodeService.ROOT_SEARCH = 'roots';

export default TreeNodeService;
