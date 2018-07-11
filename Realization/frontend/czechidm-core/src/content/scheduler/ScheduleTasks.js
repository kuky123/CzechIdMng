import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import moment from 'moment';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { SchedulerManager, DataManager, ConfigurationManager, SecurityManager } from '../../redux';
import * as Utils from '../../utils';
import TriggerTypeEnum from '../../enums/TriggerTypeEnum';

const manager = new SchedulerManager();

/**
 * Scheduler administration
 *
 * @author Radek Tomiška
 */
class ScheduleTasks extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      triggerDetail: {
        show: false,
        entity: {}
      },
      triggerType: TriggerTypeEnum.findKeyBySymbol(TriggerTypeEnum.SIMPLE),
      taskType: null
    };
  }

  getContentKey() {
    return 'content.scheduler.schedule-tasks';
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return SchedulerManager.UI_KEY_TASKS;
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'scheduler', 'scheduler-schedule-tasks']);
    this.context.store.dispatch(manager.fetchSupportedTasks());
  }

  /**
   * Open modal task detail
   */
  showDetail(entity) {
    // transform parameters to Form
    if (entity.parameters) {
      _.keys(entity.parameters).map(parameterName => {
        entity[`parameter-${parameterName}`] = entity.parameters[parameterName];
      });
    }
    if (entity.parameters && entity.parameters.dryRun) {
      entity.dryRun = true;
    }
    super.showDetail(entity, () => {
      this.refs.taskType.focus();
    });
  }

  /**
   * Close modal task detail
   */
  closeDetail() {
    this.setState({
      detail: {
        show: false,
        entity: {}
      },
      taskType: null
    });
  }

  onChangeTaskType(taskType) {
    this.setState({
      taskType
    });
  }

  /**
   * Saves give entity
   */
  save(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const { taskType } = this.state;
    const entity = this.refs.form.getData();
    const parameters = entity.parameters;
    // transform parameters
    if (taskType) {
      if (taskType.parameters) {
        entity.parameters = {};
        _.keys(taskType.parameters).map(parameterName => {
          entity.parameters[parameterName] = this.refs[`parameter-${parameterName}`].getValue();
        });
      }
    } else {
      if (parameters) {
        _.keys(parameters).map(parameterName => {
          parameters[parameterName] = entity[`parameter-${parameterName}`];
        });
      }
    }
    //
    if (entity.dryRun) {
      entity.parameters.dryRun = true;
    }
    //
    if (Utils.Entity.isNew(entity)) {
      this.context.store.dispatch(this.getManager().createEntity(entity, this.getUiKey(), this.afterSave.bind(this)));
    } else {
      this.context.store.dispatch(this.getManager().updateEntity(entity, this.getUiKey(), this.afterSave.bind(this)));
    }
  }

  /**
   * Callback after save
   */
  afterSave(entity, error) {
    if (error) {
      this.addError(error);
      this.refs.form.processEnded();
      return;
    }
    this.addMessage({ message: this.i18n('action.save.success', { record: this.getManager().getNiceLabel(entity) }) });
    this.refs.table.getWrappedInstance().reload();
    this.closeDetail();
  }

  onRun(entity, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs['confirm-task-run'].show(
      this.i18n(`action.task-run.message`, { record: this.getManager().getNiceLabel(entity) }),
      this.i18n(`action.task-run.header`)
    ).then(() => {
      this.context.store.dispatch(this.getManager().runTask(entity.id, () => {
        this.addMessage({ message: this.i18n('action.task-run.success', { count: 1, record: this.getManager().getNiceLabel(entity) }) });
      }));
    }, () => {
      // Rejected
    });
  }

  onDryRun(entity, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs['confirm-task-dry-run'].show(
      this.i18n(`action.task-dry-run.message`, { record: this.getManager().getNiceLabel(entity) }),
      this.i18n(`action.task-dry-run.header`)
    ).then(() => {
      this.context.store.dispatch(this.getManager().dryRunTask(entity.id, () => {
        this.addMessage({ message: this.i18n('action.task-dry-run.success', { count: 1, record: this.getManager().getNiceLabel(entity) }) });
      }));
    }, () => {
      // Rejected
    });
  }

  onTriggerDelete(trigger) {
    const record = moment(trigger.nextFireTime).format(this.i18n('format.datetime'));
    // show confirm message for deleting entity or entities
    this.refs['confirm-delete'].show(
      this.i18n(`action.trigger-delete.message`, { record }),
      this.i18n(`action.trigger-delete.header`)
    ).then(() => {
      this.context.store.dispatch(manager.deleteTrigger(trigger, () => {
        this.addMessage({ message: this.i18n(`action.trigger-delete.success`) });
        this.refs.table.getWrappedInstance().reload();
      }));
    }, () => {
      //
    });
  }

  saveTrigger(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.triggerForm.isFormValid()) {
      return;
    }
    const formEntity = this.refs.triggerForm.getData();
    //
    this.context.store.dispatch(this.getManager().createTrigger(formEntity, () => {
      this.addMessage({ message: this.i18n('action.trigger-create.success') });
      this.closeTriggerDetail();
      this.refs.table.getWrappedInstance().reload();
    }));
  }


  onChangeTriggerType(triggerType) {
    this.setState({
      triggerType: triggerType.value
    });
  }

  /**
   * Shows modal detail with given entity
   */
  showTriggerDetail(entity) {
    this.setState({
      triggerDetail: {
        show: true,
        entity
      }
    }, () => {
      this.refs.triggerForm.setData(entity);
      this.refs.type.focus();
    });
  }

  /**
   * Close modal detail
   */
  closeTriggerDetail() {
    this.setState({
      triggerDetail: {
        show: false,
        entity: {}
      }
    });
  }

  getLevel(data) {
    if (data.parameters && data.parameters.dryRun) {
      return 'info';
    }
    return 'success';
  }

  render() {
    const { supportedTasks, showLoading, showLoadingDetail, instanceId } = this.props;
    const { detail, triggerDetail, triggerType, taskType } = this.state;
    //
    const _supportedTasks = [];
    if (supportedTasks) {
      supportedTasks.forEach(task => {
        _supportedTasks.push({
          niceLabel: this.getManager().getSimpleTaskType(task.taskType),
          value: task.taskType,
          description: task.description,
          parameters: task.parameters
        });
      });
    }
    _supportedTasks.sort((one, two) => {
      return one.niceLabel > two.niceLabel;
    });
    const entityParameterNames = [];
    if (detail.entity.parameters) {
      _.keys(detail.entity.parameters).forEach(parameterName => {
        if (parameterName.lastIndexOf('core:', 0) !== 0) {
          entityParameterNames.push(parameterName);
        }
      });
    }
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Confirm ref="confirm-task-run" level="success"/>
        <Basic.Confirm ref="confirm-task-dry-run" level="info"/>

        <Advanced.Table
          ref="table"
          uiKey="schedule-task-table"
          manager={ manager }
          showRowSelection={ SecurityManager.hasAnyAuthority(['SCHEDULER_DELETE']) }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={ this.showDetail.bind(this, _supportedTasks.length === 0 ? {} : { name: _supportedTasks[0], instanceId }) }
                rendered={ _supportedTasks.length > 0 && SecurityManager.hasAnyAuthority(['SCHEDULER_CREATE'])}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          _searchParameters={ this.getSearchParameters() }>
          <Advanced.Column
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.showDetail.bind(this, data[rowIndex])}/>
                );
              }
            }/>
          <Advanced.Column
            property="taskType"
            sort
            width={150}
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data, property }) => {
                const propertyValue = data[rowIndex][property];
                return (
                  <span title={propertyValue}>{ this.getManager().getSimpleTaskType(propertyValue) }</span>
                );
              }
            }/>
          <Advanced.Column property="description" sort />
          <Advanced.Column property="instanceId" sort />
          <Basic.Column
            header={this.i18n('action.task-edit.parameters')}
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                if (!entity.parameters) {
                  return null;
                }
                return _.keys(entity.parameters).map(parameterName => {
                  if (parameterName.lastIndexOf('core:', 0) === 0) {
                    return null;
                  }
                  if (Utils.Ui.isEmpty(entity.parameters[parameterName])) {
                    return null;
                  }
                  return (<div>{parameterName}: { Utils.Ui.toStringValue(entity.parameters[parameterName]) }</div>);
                });
              }
            }/>
          <Basic.Column
            property="triggers"
            header={this.i18n('entity.SchedulerTask.triggers')}
            cell={
              ({ data, rowIndex, property}) => {
                const triggers = data[rowIndex][property];
                return (
                  <div>
                    {
                      triggers.map(trigger => {
                        if (!trigger.initiatorTaskId && (!trigger.nextFireTime || (trigger.fireTime && moment(trigger.fireTime).isBefore(moment())))) {
                          // only plan
                          return null;
                        }
                        return (
                          <div>
                            {
                              trigger.initiatorTaskId
                              ?
                              <Advanced.SchedulerTaskInfo entityIdentifier={ trigger.initiatorTaskId } face="popover"/>
                              :
                              <Advanced.DateValue value={trigger.nextFireTime} showTime />
                            }
                            {' '}
                            <Basic.Button
                              level="link"
                              className="btn-xs"
                              onClick={ this.onTriggerDelete.bind(this, trigger) }
                              rendered={ SecurityManager.hasAnyAuthority(['SCHEDULER_DELETE']) }>
                              <Basic.Icon value="remove" color="red"/>
                              </Basic.Button>
                          </div>
                        );
                      })
                    }
                    <Basic.Button
                      level="success"
                      className="btn-xs"
                      onClick={this.showTriggerDetail.bind(this, { type: triggerType, taskId: data[rowIndex].id })}
                      rendered={SecurityManager.hasAnyAuthority(['SCHEDULER_CREATE'])}>
                      <Basic.Icon value="plus"/>
                      {' '}
                      { this.i18n('button.add') }
                    </Basic.Button>
                  </div>
                );
              }
            }/>
            <Basic.Column
              header={this.i18n('label.action')}
              className="action"
              cell={
                ({rowIndex, data}) => {
                  return (
                    <div>
                      <Basic.Button
                        level= "info"
                        onClick={this.onDryRun.bind(this, data[rowIndex])}
                        className="btn-xs"
                        title={ data[rowIndex].supportsDryRun ? this.i18n('button.dryRun') : this.i18n('error.SCHEDULER_DRY_RUN_NOT_SUPPORTED.title') }
                        titlePlacement="bottom"
                        style={{ marginLeft: 3 }}
                        rendered={ SecurityManager.hasAnyAuthority(['SCHEDULER_EXECUTE']) }
                        disabled={ !data[rowIndex].supportsDryRun }
                        icon="play"/>
                      <Basic.Button
                        level= "success"
                        onClick={this.onRun.bind(this, data[rowIndex])}
                        className="btn-xs"
                        title={this.i18n('button.run')}
                        titlePlacement="bottom"
                        style={{ marginLeft: 3 }}
                        rendered={ SecurityManager.hasAnyAuthority(['SCHEDULER_EXECUTE']) }>
                        <Basic.Icon icon="play"/>
                      </Basic.Button>
                    </div>
                  );
                }
              }/>
        </Advanced.Table>

        <Basic.Modal
          show={detail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static"
          keyboard={!showLoading}>

          <form onSubmit={this.save.bind(this)}>
            <Basic.Modal.Header
              closeButton={ !showLoadingDetail }
              text={ !Utils.Entity.isNew(detail.entity) ? this.i18n('action.task-edit.header') : this.i18n('action.task-create.header')}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm
                ref="form"
                showLoading={ showLoadingDetail }
                readOnly={ !this.getManager().canSave(detail.entity) }>
                <Basic.EnumSelectBox
                  ref="taskType"
                  label={this.i18n('entity.SchedulerTask.taskType')}
                  options={_supportedTasks}
                  onChange={this.onChangeTaskType.bind(this)}
                  required
                  searchable
                  readOnly={ !Utils.Entity.isNew(detail.entity) }/>
                <Basic.TextArea
                  ref="description"
                  placeholder={taskType ? taskType.description : null}
                  label={this.i18n('entity.SchedulerTask.description')}
                  max={255}/>
                <Basic.TextField
                  ref="instanceId"
                  label={this.i18n('entity.SchedulerTask.instanceId.label')}
                  helpBlock={this.i18n('entity.SchedulerTask.instanceId.help')}
                  required/>
                {
                  (Utils.Entity.isNew(detail.entity) && taskType && taskType.parameters && _.keys(taskType.parameters).length > 0)
                  ?
                  <div>
                    <Basic.ContentHeader text={this.i18n('action.task-edit.parameters')} />
                    {
                      _.keys(taskType.parameters).map(parameterName => {
                        return (
                          <Basic.TextField
                            label={parameterName}
                            ref={`parameter-${parameterName}`}
                            value={taskType.parameters[parameterName]}
                            max={255}/>
                        );
                      })
                    }
                  </div>
                  :
                  <div>
                    {
                      entityParameterNames.length === 0
                      ||
                      <div>
                        <Basic.ContentHeader text={this.i18n('action.task-edit.parameters')} />
                          {
                            entityParameterNames.map(parameterName => {
                              return (
                                <Basic.TextField
                                  label={parameterName}
                                  ref={`parameter-${parameterName}`}
                                  max={255}/>
                              );
                            })
                          }
                      </div>
                    }
                  </div>
                }
              </Basic.AbstractForm>
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={ this.closeDetail.bind(this) }
                showLoading={ showLoadingDetail }>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={ showLoadingDetail }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ this.getManager().canSave(detail.entity) }>
                { this.i18n('button.save') }
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>

        <Basic.Modal
          show={triggerDetail.show}
          onHide={this.closeTriggerDetail.bind(this)}
          backdrop="static"
          keyboard={!showLoading}>

          <form onSubmit={this.saveTrigger.bind(this)}>
            <Basic.Modal.Header closeButton={!showLoading} text={this.i18n('action.trigger-create.header')}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="triggerForm" showLoading={showLoading}>
                <Basic.EnumSelectBox
                  ref="type"
                  enum={ TriggerTypeEnum }
                  label={ this.i18n('entity.SchedulerTask.trigger._type.label') }
                  required
                  onChange={this.onChangeTriggerType.bind(this)}/>
                <Basic.DateTimePicker
                  ref="fireTime"
                  label={ this.i18n('entity.SchedulerTask.trigger.fireTime') }
                  hidden={ triggerType !== 'SIMPLE' }
                  required={ triggerType === 'SIMPLE' }/>
                <Basic.TextField
                  ref="cron"
                  label={ this.i18n('entity.SchedulerTask.trigger.cron.label') }
                  helpBlock={
                    <span>
                      { this.i18n('entity.SchedulerTask.trigger.cron.help') }
                      {' '}
                      <Basic.Link href={ this.i18n('entity.SchedulerTask.trigger.cron.link.href') } text={ this.i18n('entity.SchedulerTask.trigger.cron.link.text') }/>
                    </span>
                  }
                  hidden={ triggerType !== 'CRON' }
                  required={ triggerType === 'CRON' }/>
                <Advanced.CronTab
                  hidden={ triggerType !== 'CRON' }
                  required={ triggerType === 'CRON' } />
                <Basic.SelectBox
                  ref="initiatorTaskId"
                  manager={ manager }
                  label={ this.i18n('entity.SchedulerTask.trigger.dependent.initiatorTaskId.label') }
                  helpBlock={ this.i18n('entity.SchedulerTask.trigger.dependent.initiatorTaskId.help') }
                  hidden={ triggerType !== 'DEPENDENT' }
                  required={ triggerType === 'DEPENDENT' }/>
              </Basic.AbstractForm>
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.closeTriggerDetail.bind(this)}
                showLoading={showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

ScheduleTasks.propTypes = {
  instanceId: PropTypes.string,
  userContext: PropTypes.object,
  supportedTasks: PropTypes.object,
  showLoading: PropTypes.bool,
  showLoadingDetail: PropTypes.bool
};
ScheduleTasks.defaultProps = {
  instanceId: null,
  userContext: null,
  supportedTasks: null,
  showLoading: true,
  showLoadingDetail: false
};

function select(state) {
  return {
    instanceId: ConfigurationManager.getPublicValue(state, 'idm.pub.app.instanceId'),
    userContext: state.security.userContext,
    supportedTasks: DataManager.getData(state, SchedulerManager.UI_KEY_SUPPORTED_TASKS),
    showLoading: Utils.Ui.isShowLoading(state, SchedulerManager.UI_KEY_SUPPORTED_TASKS),
    showLoadingDetail: Utils.Ui.isShowLoading(state, SchedulerManager.UI_KEY_TASKS),
    _searchParameters: Utils.Ui.getSearchParameters(state, SchedulerManager.UI_KEY_TASKS)
  };
}

export default connect(select)(ScheduleTasks);
