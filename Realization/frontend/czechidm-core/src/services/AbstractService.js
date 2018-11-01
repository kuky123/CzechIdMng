import _ from 'lodash';
//
import RestApiService from './RestApiService';
import SearchParameters from '../domain/SearchParameters';
import * as Utils from '../utils';

/**
 * Contains basic CRUD + search operation
 *
 * @author Radek Tomiška
 */
export default class AbstractService {

  constructor() {
    if (this.getApiPath === undefined) {
      throw new TypeError('Must override method getApiPath()');
    }
    this.getById = this.getById.bind(this);
  }

  /**
   * Returns absolute endpoint patch
   *
   * @return {[type]} [description]
   */
  getAbsoluteApiPath() {
    return RestApiService.getUrl(this.getApiPath());
  }

  /**
   * Textual entity reprezentation (~entity.toString())
   * @param  {entity} entity
   * @return {string]}
   */
  getNiceLabel(entity) {
    return entity.id;
  }

  /**
   * Returns true, if `patch`  method is supported
   *
   * Added for enddpoints with dto - dto's doesn't support `patch` method for now
   *
   * @return {bool} Returns true, if `patch`  method is supported
   */
  supportsPatch() {
    return true;
  }

  /**
   * Added for enddpoints with authorization policies evaluation
   *
   * @return {bool} Returns true, when endpoint suppors uthorization policies evaluation
   */
  supportsAuthorization() {
    return this.getGroupPermission() !== null;
  }

  /**
   * Added for enddpoints with backend bulk action support.
   *
   * @return {bool}
   */
  supportsBulkAction() {
    return false;
  }

  /**
   * Returns group permission for given manager / agenda
   *
   * @return {string} GroupPermission name
   */
  getGroupPermission() {
    return null;
  }

  /**
  * Resolve JSON parse exception. Replaces it for syntax error localized exception.
  */
  _resolveException(ex) {
    if (ex && ex.stack && ex.stack.indexOf('SyntaxError') === 0) {
      return {message: 'JSON parse error', module: 'core', statusEnum: 'SYNTAX_ERROR', statusCode: 300};
    }
    return ex;
  }

  /**
   * Returns resource by given id
   *
   * @param  {string|number} id resource identifier
   * @return {Promise} promise with response
   */
  getById(id) {
    return RestApiService
      .get(this.getApiPath() + `/${encodeURIComponent(id)}`)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  /**
   * Create new record
   *
   * @param  {entity} json created entity
   * @return {promise}
   */
  create(json) {
    return RestApiService
      .post(this.getApiPath(), json)
      .then(response => {
        if (response.status === 204) { // no content - ok
          return null;
        }
        return response.json();
      })
      .then(jsonResponse => {
        if (Utils.Response.hasError(jsonResponse)) {
          throw Utils.Response.getFirstError(jsonResponse);
        }
        if (Utils.Response.hasInfo(jsonResponse)) {
          throw Utils.Response.getFirstInfo(jsonResponse);
        }
        return jsonResponse;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  upload(formData) {
    return RestApiService
      .upload(this.getApiPath(), formData)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (Utils.Response.hasInfo(json)) {
          throw Utils.Response.getFirstInfo(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  deleteById(id) {
    return RestApiService
      .delete(this.getApiPath() + `/${encodeURIComponent(id)}`)
      .then(response => {
        if (response.status === 204) { // no content - ok
          return null;
        }
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (Utils.Response.hasInfo(json)) {
          throw Utils.Response.getFirstInfo(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  updateById(id, json) {
    return RestApiService
      .put(this.getApiPath() + `/${encodeURIComponent(id)}`, json)
      .then(response => {
        return response.json();
      })
      .then(jsonResponse => {
        if (Utils.Response.hasError(jsonResponse)) {
          throw Utils.Response.getFirstError(jsonResponse);
        }
        if (Utils.Response.hasInfo(jsonResponse)) {
          throw Utils.Response.getFirstInfo(jsonResponse);
        }
        return jsonResponse;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  patchById(id, json) {
    return RestApiService
      .patch(this.getApiPath() + `/${encodeURIComponent(id)}`, json)
      .then(response => {
        return response.json();
      })
      .then(jsonResponse => {
        if (Utils.Response.hasError(jsonResponse)) {
          throw Utils.Response.getFirstError(jsonResponse);
        }
        return jsonResponse;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  search(searchParameters) {
    if (!searchParameters) {
      searchParameters = this.getDefaultSearchParameters();
    }
    if (!(searchParameters instanceof SearchParameters)) {
      // TODO: try construct SearchParameters.fromJs object
      LOGGER.warn('Search parameters is not instance of SearchParameters - using default', searchParameters);
      searchParameters = this.getDefaultSearchParameters();
    }
    return RestApiService
      .get(this.getApiPath() + searchParameters.toUrl())
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  count(searchParameters) {
    if (!searchParameters) {
      searchParameters = this.getDefaultSearchParameters().setName(SearchParameters.NAME_COUNT);
    }
    if (!(searchParameters instanceof SearchParameters)) {
      // TODO: try construct SearchParameters.fromJs object
      LOGGER.warn('Search parameters is not instance of SearchParameters - using default', searchParameters);
      searchParameters = this.getDefaultSearchParameters().setName(SearchParameters.NAME_COUNT);
    }
    return RestApiService
      .get(this.getApiPath() + searchParameters.toUrl())
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return _.clone(new SearchParameters().setSort('id'));
  }

  /**
   * Merge search parameters - second sp has higher priority
   *
   * @param  {object} previousSearchParameters
   * @param  {object} newSearchParameters
   * @return {object} resultSearchParameters
   */
  mergeSearchParameters(previousSearchParameters, newSearchParameters) {
    if (!newSearchParameters) { // nothing to merge with
      return previousSearchParameters;
    }
    if (!previousSearchParameters) { // nothing to merge with
      return newSearchParameters;
    }
    // merge filters
    let _searchParameters = _.clone(previousSearchParameters);
    newSearchParameters.getFilters().forEach((filter, property) => {
      _searchParameters = _searchParameters.setFilter(property, filter);
    });
    // override sorts if needed
    if (!newSearchParameters.getSorts().isEmpty()) {
      _searchParameters = _searchParameters.clearSort();
      newSearchParameters.getSorts().forEach((ascending, property) => {
        _searchParameters = _searchParameters.setSort(property, ascending);
      });
    }
    // override pagination
    if (newSearchParameters.getPage() !== null) {
      _searchParameters = _searchParameters.setPage(newSearchParameters.getPage());
    }
    // override size
    if (newSearchParameters.getSize() !== null) {
      _searchParameters = _searchParameters.setSize(newSearchParameters.getSize());
    }
    // override name
    if (newSearchParameters.getName() !== null && newSearchParameters.getName() !== SearchParameters.NAME_QUICK) {
      _searchParameters = _searchParameters.setName(newSearchParameters.getName());
    }
    return _searchParameters;
  }

  /**
   * Get given audits revisions for apiPath and username, role name, tree nodes ids
   *
   * @param apiPath {string}
   * @param entityId {string, number}
   * @return {Promise}
   */
  getRevisions(entityId) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(entityId)}/revisions`)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    }).catch(ex => {
      throw this._resolveException(ex);
    });
  }

  /**
   * Get single revision for username, role name, tree nodes ids and id revision
   *
   * @param entityId {string, number}
   * @param revId {number}
   * @return {Promise}
   */
  getRevision(entityId, revId) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(entityId)}/revisions/${encodeURIComponent(revId)}`)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    }).catch(ex => {
      throw this._resolveException(ex);
    });
  }

  /**
   * Fetch permissions
   * @param  {string, number} id entity id
   * @return {Promise}
   */
  getPermissions(id) {
    return RestApiService
    .get(this.getApiPath() + `/${encodeURIComponent(id)}/permissions`)
    .then(response => {
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        throw Utils.Response.getFirstError(json);
      }
      return json;
    }).catch(ex => {
      throw this._resolveException(ex);
    });
  }

  /**
   * Common put (=> update) action on given entity
   *
   * @param  {string} method - action name (e.g. PUT / POST / DELETE )
   * @param  {string} actionName  - action name (e.g. activate / deactivate / archivate )
   * @param  {string} id        entity id or alias (codeable)
   * @return {promise}
   */
  action(method, actionName, id) {
    return RestApiService
      .action(method, this.getApiPath() + `/${encodeURIComponent(id)}${actionName ? '/' + actionName : ''}`)
      .then(response => {
        if (response.status === 204) { // no content - ok
          return null;
        }
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (Utils.Response.hasInfo(json)) {
          throw Utils.Response.getFirstInfo(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  /**
   * Returns all bulk actions in given api path
   *
   * @return Promise
   */
  getAvailableBulkActions() {
    return RestApiService
      .get(this.getApiPath() + `/bulk/actions`)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  /**
   * Execute bulk action
   *
   * @param  {object}   action
   * @param  {Function} cb
   * @return {Promise}
   */
  processBulkAction(action, cb) {
    return RestApiService
      .post(this.getApiPath() + `/bulk/action`, action)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (cb) {
          cb(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }

  /**
   * Validate action before execution
   *
   * @param  {object}   action
   * @param  {Function} cb
   * @return {Promise}
   */
  prevalidateBulkAction(action, cb) {
    return RestApiService
      .post(this.getApiPath() + `/bulk/prevalidate`, action)
      .then(response => {
        return response.json();
      })
      .then(json => {
        if (Utils.Response.hasError(json)) {
          throw Utils.Response.getFirstError(json);
        }
        if (cb) {
          cb(json);
        }
        return json;
      }).catch(ex => {
        throw this._resolveException(ex);
      });
  }
}
