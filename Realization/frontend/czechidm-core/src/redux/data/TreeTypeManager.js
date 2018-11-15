import Immutable from 'immutable';
//
import EntityManager from './EntityManager';
import { TreeTypeService } from '../../services';
import DataManager from './DataManager';

/**
 * Tree structures
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
export default class TreeTypeManager extends EntityManager {

  constructor() {
    super();
    this.service = new TreeTypeService();
    this.dataManager = new DataManager();
  }

  getService() {
    return this.service;
  }

  getEntityType() {
    return 'TreeType';
  }

  getCollectionType() {
    return 'treeTypes';
  }

  /**
   * Returns default tree type => organization structure
   *
   * @return {action}
   */
  fetchDefaultTreeType(cb = null) {
    const uiKey = TreeTypeManager.UI_KEY_DEFAULT_TREE_TYPE;
    //
    return (dispatch, getState) => {
      const defaultTreeType = DataManager.getData(getState(), uiKey);
      if (defaultTreeType || defaultTreeType === false) {
        // default tree type is already loaded
        // TODO: clear redux state, after default tree type is changed
        if (cb) {
          cb(defaultTreeType, null);
        }
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getDefaultTreeType()
          .then(json => {
            dispatch(this.receiveEntity(json.id, json, uiKey));
            dispatch(this.dataManager.receiveData(uiKey, json, cb));
          })
          .catch(error => {
            if (error.statusCode === 404) {
              dispatch(this.dataManager.receiveData(uiKey, false, cb));
            } else {
              // TODO: data uiKey
              dispatch(this.dataManager.receiveError(null, uiKey, error, cb));
            }
          });
      }
    };
  }

  /**
   * Get given treeType's configuration properties
   *
   * @param  {string} treeTypeId
   * @param  {string} uiKey
   * @return {action}
   */
  fetchConfigurations(treeTypeId, uiKey) {
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().getConfigurations(treeTypeId)
        .then(json => {
          let configurations = new Immutable.Map();
          json.forEach(item => {
            configurations = configurations.set(item.name.split('.').pop(), item);
          });
          dispatch(this.dataManager.receiveData(uiKey, configurations));
        })
        .catch(error => {
          dispatch(this.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Rebuild forest index for given tree type
   *
   * @param  {string} treeTypeId
   * @param  {string} uiKey
   * @param {func} callback
   * @return {action}
   */
  rebuildIndex(treeTypeId, uiKey) {
    return (dispatch, getState) => {
      dispatch(this.dataManager.requestData(`${uiKey}-rebuild`));
      this.getService().rebuildIndex(treeTypeId)
        .then(() => {
          dispatch(this.dataManager.stopRequest(`${uiKey}-rebuild`));
          dispatch(this.flashMessagesManager.addMessage({ message: this.i18n('content.tree.types.configuration.action.rebuild.success', { record: this.getNiceLabel(this.getEntity(getState(), treeTypeId)) }) }));
          dispatch(this.fetchConfigurations(treeTypeId, uiKey));
        })
        .catch(error => {
          // TODO: data uiKey
          dispatch(this.dataManager.receiveError(null, `${uiKey}-rebuild`, error));
        });
    };
  }
}

TreeTypeManager.UI_KEY_DEFAULT_TREE_TYPE = 'default-tree-type';
