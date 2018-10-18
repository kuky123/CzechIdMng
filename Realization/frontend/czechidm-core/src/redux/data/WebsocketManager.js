import EntityManager from './EntityManager';
import { WebsocketService } from '../../services';

/**
 * Websocket logs
 *
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
export default class WebsocketManager extends EntityManager {

  constructor() {
    super();
    this.service = new WebsocketService();
  }

  getService() {
    return this.service;
  }

  getEntityType() {
    return 'WebsocketLog';
  }

  getCollectionType() {
    return 'websocketLogs';
  }
}
