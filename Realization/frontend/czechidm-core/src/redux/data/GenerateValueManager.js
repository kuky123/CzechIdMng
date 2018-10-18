import Immutable from 'immutable';
//
import EntityManager from './EntityManager';
import { GenerateValueService } from '../../services';
import DataManager from './DataManager';

/**
 * Manager for Generated attributes
 *
 * @author Ondřej Kopr
 */
export default class GenerateValueManager extends EntityManager {

  constructor() {
    super();
    this.service = new GenerateValueService();
  }

  getService() {
    return this.service;
  }

  getEntityType() {
    return 'GenerateValue';
  }

  getCollectionType() {
    return 'generateValues';
  }

  getGroupPermission() {
    return 'GENERATEVALUE';
  }

  /**
   * Loads all entities wich supports generating
   *
   * @return {action}
   */
  fetchSupportedTypes() {
    const uiKey = GenerateValueManager.UI_KEY_SUPPORTED_TYPES;
    //
    return (dispatch, getState) => {
      const loaded = DataManager.getData(getState(), uiKey);
      if (loaded) {
        // we dont need to load them again - change depends on BE restart
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getSupportedTypes()
          .then(json => {
            dispatch(this.dataManager.receiveData(uiKey, json));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
      }
    };
  }

  /**
   * Loads all available generators
   *
   * @return {action}
   */
  fetchAvailableGenerators(dtoType = null) {
    const uiKey = GenerateValueManager.UI_KEY_AVAILABLE_GENERATORS;
    //
    return (dispatch, getState) => {
      const loaded = DataManager.getData(getState(), uiKey);
      if (loaded) {
        // we dont need to load them again - change depends on BE restart
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getAvailableGenerators(dtoType)
          .then(json => {
            let generators = new Immutable.Map();
            if (json._embedded && json._embedded.valueGenerators) {
              json._embedded.valueGenerators.forEach(item => {
                generators = generators.set(item.generatorType, item);
              });
            }
            dispatch(this.dataManager.receiveData(uiKey, generators));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
      }
    };
  }
}

GenerateValueManager.UI_KEY_SUPPORTED_TYPES = 'generate-attr-supported-types';
GenerateValueManager.UI_KEY_AVAILABLE_GENERATORS = 'generate-attr-available-generators';
