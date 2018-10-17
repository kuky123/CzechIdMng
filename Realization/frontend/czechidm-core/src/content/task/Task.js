import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import {WorkflowTaskInstanceManager } from '../../redux';
import DynamicTaskDetail from './DynamicTaskDetail';
import ComponentService from '../../services/ComponentService';

const workflowTaskInstanceManager = new WorkflowTaskInstanceManager();
const componentService = new ComponentService();

/**
 * Component for render detail of workflow task. Is responsible for choose correct task detil component.
 * As default is DynamicTaskDetail, when have task secificate custom task detail (in formKey),
 * than is loaded component by name and used it for task detail render.
 */
class Task extends Basic.AbstractContent {

  constructor(props) {
    super(props);
    this.state = {
      showLoading: props.showLoading,
      nonExistentTask: false // true if task isn't found or is solved
    };
  }

  componentDidMount() {
    this.selectNavigationItem('tasks');
    const { taskID } = this.props.params;
    this.context.store.dispatch(workflowTaskInstanceManager.fetchPermissions(taskID, null));
    this.context.store.dispatch(workflowTaskInstanceManager.fetchEntityIfNeeded(taskID, null, (json, error) => {
      if (error) {
        // task isn't exists or is solved
        if (error && error.statusCode === 404) {
          this.setState({
            nonExistentTask: true
          });
        } else {
          this.handleError(error);
        }
      }
    }));
  }

  componentDidUpdate() {
    this.selectNavigationItem('tasks');
  }

  getContentKey() {
    return 'content.task';
  }

  _goBack() {
    if (this.context.router.goBack()) {
      // nothig, router just can go back
    } else {
      // transmition to /task, history doesnt exist
      // we havn't task or another information we must go to dashboard
      this.context.router.push('/');
    }
  }

  _canExecute(task) {
    if (task) {
      const decisions = task.decisions;
      if (decisions && decisions.length > 0) {
        return true;
      }
    }
    return false;
  }

  render() {
    const { readOnly, task } = this.props;
    const { nonExistentTask } = this.state;
    let DetailComponent;
    if (task && task.formKey) {
      DetailComponent = componentService.getComponent(task.formKey);
      if (!DetailComponent) {
        this.addMessage({title: this.i18n('message.task.detailNotFound'), level: 'warning'});
        this.context.router.goBack();
        return null;
      }
    }
    if (!DetailComponent) {
      DetailComponent = DynamicTaskDetail;
    }
    if (nonExistentTask) {
      return (
        <div>
          <Basic.PageHeader>
            {this.i18n('instance.header')}
          </Basic.PageHeader>
          <Basic.Panel >
            <Basic.Alert level="info" text={this.i18n('message.task.taskSolvedOrNotFound')} />
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={this._goBack.bind(this)}>
                {this.i18n('button.back')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </div>
      );
    }
    return (
      <div>
        {task ?
        <DetailComponent
          task={task}
          canExecute={this._canExecute(task)}
          uiKey="dynamic-task-detail"
          taskManager={workflowTaskInstanceManager}
          readOnly={readOnly}/>
        :
        <Basic.Well showLoading/>
        }
      </div>
    );
  }
}

Task.propTypes = {
  task: PropTypes.object,
  readOnly: PropTypes.bool
};

Task.defaultProps = {
  task: null,
  readOnly: false
};

function select(state, component) {
  const { taskID } = component.params;
  const task = workflowTaskInstanceManager.getEntity(state, taskID);
  return {
    task
  };
}

export default connect(select)(Task);
