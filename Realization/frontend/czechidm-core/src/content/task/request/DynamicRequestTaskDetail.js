import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
//
import * as Basic from '../../../components/basic';
import DecisionButtons from '../DecisionButtons';
import DynamicTaskDetail from '../DynamicTaskDetail';
import RequestDetail from '../../request/RequestDetail';
import { connect } from 'react-redux';
import SearchParameters from '../../../domain/SearchParameters';
import WorkflowTaskInfo from '../../../components/advanced/WorkflowTaskInfo/WorkflowTaskInfo';

/**
 * Custom task detail designed for use with universal request.
 * Extended from DynamicTaskDetail (it is standard task detail renderer)
 */
class DynamicRequestTaskDetail extends DynamicTaskDetail {

  getContentKey() {
    return 'content.task.instance';
  }

  componentDidMount() {
    super.componentDidMount();
  }

  _completeTask(decision) {
    const formDataValues = this.refs.formData.getData();
    const task = this.refs.form.getData();
    const formDataConverted = this._toFormData(formDataValues, task.formData);
    this.setState({
      showLoading: true
    });
    const formData = {'decision': decision.id, 'formData': formDataConverted};
    const { taskManager} = this.props;
    this.context.store.dispatch(taskManager.completeTask(task, formData, this.props.uiKey, this._afterComplete.bind(this)));
  }

  render() {
    const {task, canExecute, taskManager} = this.props;
    const { showLoading} = this.state;
    const showLoadingInternal = task ? showLoading : true;
    let force = new SearchParameters();
    if (task) {
      force = force.setFilter('username', task.applicant);
    }
    const formDataValues = this._toFormDataValues(task.formData);
    const taskName = taskManager.localize(task, 'name');

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm"/>

        <Basic.PageHeader>{taskName}
          <small> {this.i18n('header')}</small>
        </Basic.PageHeader>
        <Basic.Alert
          title={this.i18n('description')}
          text={<WorkflowTaskInfo entity={task} showLink={false} showLoading={!task} className="no-margin"/>}
        />
        <Basic.AbstractForm style={{display: 'none'}} className="panel-body" ref="form" data={task}/>
        <Basic.Panel style={{display: 'none'}}>
          <Basic.AbstractForm ref="formData" data={formDataValues} style={{ padding: '15px 15px 0px 15px' }}>
            {this._getFormDataComponents(task)}
          </Basic.AbstractForm>
        </Basic.Panel>
        <RequestDetail
          ref="requestDetail"
          uiKey="request-detail"
          entityId={task.variables.entityEvent.content.id}
          showLoading={showLoadingInternal}
          editableInStates={['IN_PROGRESS']}
          simpleMode
          additionalButtons={
            <DecisionButtons
              task={task}
              onClick={this._validateAndCompleteTask.bind(this)}
              readOnly={!canExecute || showLoadingInternal}
              showBackButton={false}
            />}
          canExecute={canExecute}/>
      </div>
    );
  }
}

DynamicRequestTaskDetail.propTypes = {
  task: PropTypes.object,
  readOnly: PropTypes.bool,
  taskManager: PropTypes.object.isRequired,
  canExecute: PropTypes.bool
};

DynamicRequestTaskDetail.defaultProps = {
  task: null,
  readOnly: false,
  canExecute: true
};
function select(state, component) {
  const task = component.task;
  if (task) {
    return {
      _showLoading: false
    };
  }
  return {
    _showLoading: true
  };
}

export default connect(select, null, null, { withRef: true })(DynamicRequestTaskDetail);
