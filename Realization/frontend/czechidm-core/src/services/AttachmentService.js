import AbstractRequestService from './AbstractRequestService';
import SearchParameters from '../domain/SearchParameters';
import RestApiService from './RestApiService';
import * as Utils from '../utils';

/**
 * AttachmentService
 *
 * @author Radek Tomiška
 */
export default class AttachmentService extends AbstractRequestService {

  constructor() {
    super();
  }

  getSubApiPath() {
    return '/attachments';
  }

  getNiceLabel(entity) {
    if (!entity || !entity._embedded) {
      return '';
    }
    return `${entity.name}`;
  }

  supportsPatch() {
    return true;
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('created', 'desc');
  }

  /**
   * Upload attachment
   *
   * @param  {form} formData body
   * @return {object} attachment metadata
   */
  upload(formData) {
    return RestApiService
      .upload(this.getApiPath() + `/upload`, formData)
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
      });
  }
}
