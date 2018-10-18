import EntityManager from './EntityManager';
//
import { ProfileService } from '../../services';

/**
 * Profiles
 *
 * @author Radek Tomiška
 */
export default class ProfileManager extends EntityManager {

  constructor() {
    super();
    this.service = new ProfileService();
  }

  getService() {
    return this.service;
  }

  getEntityType() {
    return 'Profile';
  }

  getCollectionType() {
    return 'profiles';
  }
}
