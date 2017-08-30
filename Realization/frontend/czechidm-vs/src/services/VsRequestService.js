import { Services } from 'czechidm-core';
import { Domain } from 'czechidm-core';
import { Utils } from 'czechidm-core';

/**
 * Service controlls request for virtual systems
 *
 * @author Vít Švanda
 */
export default class VsRequestService extends Services.AbstractService {

  getApiPath() {
    return '/vs/requests';
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return `${entity.uid} (${entity.operationType} - ${entity.state})`;
  }

  /**
   * Agenda supports authorization policies
   */
  supportsAuthorization() {
    return true;
  }

  /**
   * Group permission - all base permissions (`READ`, `WRITE`, ...) will be evaluated under this group
   */
  getGroupPermission() {
    return 'VSREQUEST';
  }

  /**
   * Almost all dtos doesn§t support rest `patch` method
   */
  supportsPatch() {
    return false;
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(Domain.SearchParameters.NAME_QUICK).clearSort().setSort('uid');
  }

  /**
  * Mark virtual system request as realized (changes will be propagated to VsAccount)
  */
  realize(id) {
    return Services.RestApiService
      .put(this.getApiPath() + `/${encodeURIComponent(id)}/realize`)
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
      });
  }

  /**
  * Cancel virtual system request
  */
  cancel(id) {
    return Services.RestApiService
      .put(this.getApiPath() + `/${encodeURIComponent(id)}/cancel`)
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
      });
  }
}
