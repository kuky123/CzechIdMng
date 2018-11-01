import Immutable from 'immutable';
//
import EntityManager from './EntityManager';
import { SchedulerService } from '../../services';
import DataManager from './DataManager';

/**
 * Scheduler administration
 *
 * @author Radek Tomiška
 */
export default class SchedulerManager extends EntityManager {

  constructor() {
    super();
    this.dataManager = new DataManager();
    this.service = new SchedulerService();
  }

  getService() {
    return this.service;
  }

  getNiceLabel(entity, showDescription = true) {
    return this.getService().getNiceLabel(entity, showDescription);
  }

  getEntityType() {
    return 'SchedulerTask';
  }

  getCollectionType() {
    return 'tasks';
  }

  /**
   * Return simple class name
   *
   * @param  {string} taskType cannonical class name
   * @return {string}
   */
  getSimpleTaskType(taskType) {
    return this.getService().getSimpleTaskType(taskType);
  }

  /**
   * Loads registeered scheduled tasks
   *
   * @return {action}
   */
  fetchSupportedTasks() {
    const uiKey = SchedulerManager.UI_KEY_SUPPORTED_TASKS;
    //
    return (dispatch, getState) => {
      const loaded = DataManager.getData(getState(), uiKey);
      if (loaded) {
        // we dont need to load them again - change depends on BE restart
      } else {
        dispatch(this.dataManager.requestData(uiKey));
        this.getService().getSupportedTasks()
          .then(json => {
            let tasks = new Immutable.Map();
            if (json._embedded && json._embedded.tasks) {
              json._embedded.tasks.forEach(item => {
                tasks = tasks.set(item.taskType, item);
              });
            }
            dispatch(this.dataManager.receiveData(uiKey, tasks));
          })
          .catch(error => {
            // TODO: data uiKey
            dispatch(this.dataManager.receiveError(null, uiKey, error));
          });
      }
    };
  }

  /**
   * Run given task manually
   *
   * @param  {string} taskId
   * @param  {func} callback
   * @return {action}
   */
  runTask(taskId, cb) {
    const uiKey = SchedulerManager.UI_KEY_TASKS;
    //
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().runTask(taskId)
        .then(() => {
          dispatch(this.dataManager.stopRequest(uiKey, null, cb));
        })
        .catch(error => {
          // TODO: data uiKey
          dispatch(this.dataManager.receiveError(null, uiKey, error));
        });
    };
  }

  dryRunTask(taskId, cb) {
    const uiKey = SchedulerManager.UI_KEY_TASKS;
    //
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().dryRunTask(taskId)
        .then(() => {
          dispatch(this.dataManager.stopRequest(uiKey, null, cb));
        })
        .catch(error => {
          // TODO: data uiKey
          dispatch(this.dataManager.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Deletes given trigger
   *
   * @param  {string} taskId
   * @param  {func} callback
   * @return {action}
   */
  deleteTrigger(trigger, cb) {
    const uiKey = SchedulerManager.UI_KEY_TASKS;
    //
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().deleteTrigger(trigger)
        .then(() => {
          dispatch(this.dataManager.stopRequest(uiKey, null, cb));
        })
        .catch(error => {
          // TODO: data uiKey
          dispatch(this.dataManager.receiveError(null, uiKey, error));
        });
    };
  }

  /**
   * Deletes given trigger
   *
   * @param  {string} taskId
   * @param  {func} callback
   * @return {action}
   */
  createTrigger(trigger, cb) {
    const uiKey = SchedulerManager.UI_KEY_TASKS;
    //
    return (dispatch) => {
      dispatch(this.dataManager.requestData(uiKey));
      this.getService().createTrigger(trigger)
        .then(() => {
          dispatch(this.dataManager.stopRequest(uiKey, null, cb));
        })
        .catch(error => {
          // TODO: data uiKey
          dispatch(this.dataManager.receiveError(null, uiKey, error));
        });
    };
  }
}

SchedulerManager.UI_KEY_TASKS = 'scheduler-tasks';
SchedulerManager.UI_KEY_SUPPORTED_TASKS = 'scheduler-supported-tasks';
