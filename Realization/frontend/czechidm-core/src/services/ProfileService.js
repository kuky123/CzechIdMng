import AbstractService from './AbstractService';
import SearchParameters from '../domain/SearchParameters';

/**
 * Profiles
 *
 * @author Radek Tomiška
 */
class ProfileService extends AbstractService {

  constructor() {
    super();
  }

  getApiPath() {
    return '/profiles';
  }

  supportsPatch() {
    return true;
  }

  supportsBulkAction() {
    return true;
  }

  getGroupPermission() {
    return 'PROFILE';
  }

  /**
   * Nice label
   *
   * @param  {entity} entity
   * @return {string}
   */
  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return entity.id; // TODO ... Profile - identity nice label
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('identity.username', 'asc');
  }
}

export default ProfileService;
