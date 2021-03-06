import { Services } from 'czechidm-core';
import { Domain, Utils } from 'czechidm-core';
import SystemEntityTypeEnum from '../domain/SystemEntityTypeEnum';

export default class ProvisioningOperationService extends Services.AbstractService {

  constructor() {
    super();
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return `${(entity._embedded && entity._embedded.system) ? entity._embedded.system.name : entity.system}:${SystemEntityTypeEnum.getNiceLabel(entity.entityType)}:${entity.systemEntityUid}`;
  }

  getApiPath() {
    return '/provisioning-operations';
  }

  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(Domain.SearchParameters.NAME_QUICK).clearSort().setSort('created', 'DESC');
  }

  /**
   * Cancel all filtered provisioning operation from queue
   *
   * @param  {Object} filter
   * @return {Promise}
   */
  cancelAll(searchParameters) {
    return Services.RestApiService
      .put(Services.RestApiService.getUrl(this.getApiPath() + `/action/bulk/cancel` + searchParameters.toUrl()))
      .then(response => {
        if (response.status === 403) {
          throw new Error(403);
        }
        if (response.status === 404) {
          throw new Error(404);
        }
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

  /**
   * Delete all provisioning operation from queue for the given system identifier.
   *
   * @param  {string} system identifier
   * @return {Promise}
   */
  deleteAll(system) {
    return Services.RestApiService
      .delete(Services.RestApiService.getUrl(this.getApiPath() + `/action/bulk/delete?system=${encodeURIComponent(system)}`))
      .then(response => {
        if (response.status === 403) {
          throw new Error(403);
        }
        if (response.status === 404) {
          throw new Error(404);
        }
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

  /**
   * Retry or cancel provisioning operation
   *
   * @param  {string} operation id
   * @param  {string} action 'retry' or 'cancel'
   * @return {Promise}
   */
  retry(id, action = 'retry') {
    return Services.RestApiService
      .put(this.getApiPath() + `/${id}/${action}`)
      .then(response => {
        if (response.status === 403) {
          throw new Error(403);
        }
        if (response.status === 404) {
          throw new Error(404);
        }
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

  /**
   * Retry or cancel provisioning batch
   *
   * @param  {string} batch id
   * @param  {string} action 'retry' or 'cancel'
   * @return {Promise}
   */
  retryBatch(id, action = 'retry') {
    return Services.RestApiService
      .put(Services.RestApiService.getUrl(`/provisioning-batches/${id}/${action}`))
      .then(response => {
        if (response.status === 403) {
          throw new Error(403);
        }
        if (response.status === 404) {
          throw new Error(404);
        }
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
