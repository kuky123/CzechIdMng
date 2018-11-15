import React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { WorkflowHistoricProcessInstanceManager, WorkflowHistoricTaskInstanceManager, WorkflowTaskInstanceManager, SecurityManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';
import HistoricProcessInstanceTable from './HistoricProcessInstanceTable';

/**
* Workflow process historic detail
*/
const workflowHistoricProcessInstanceManager = new WorkflowHistoricProcessInstanceManager();
const workflowHistoricTaskInstanceManager = new WorkflowHistoricTaskInstanceManager();
const workflowTaskInstanceManager = new WorkflowTaskInstanceManager();

const MAX_CANDICATES = 3;

/**
 * Detail for instance of the workflow process
 *
 * @author Vít Švanda
 */
class HistoricProcessInstanceDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {};
  }

  getContentKey() {
    return 'content.workflow.history.process';
  }

  /**
   * componentDidMount call only _initComponent for initial form and download diagram.
   */
  componentDidMount() {
    this._initComponent(this.props);
  }

  /**
   * componentWillReceiveProps call _initComponent only if is
   * historicProcessInstanceId is different from next props historicProcessInstanceId.
   */
  componentWillReceiveProps(nextProps) {
    const { historicProcessInstanceId } = nextProps.params;
    if (historicProcessInstanceId && historicProcessInstanceId !== this.props.params.historicProcessInstanceId) {
      this._initComponent(nextProps);
    }
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const { historicProcessInstanceId } = props.params;
    this.context.store.dispatch(workflowHistoricProcessInstanceManager.fetchEntity(historicProcessInstanceId));
    this.selectNavigationItem('workflow-historic-processes');
    workflowHistoricProcessInstanceManager.getService().downloadDiagram(historicProcessInstanceId, this.reciveDiagram.bind(this));
  }

  reciveDiagram(blob) {
    const objectURL = URL.createObjectURL(blob);
    this.setState({diagramUrl: objectURL});
  }

  _showFullDiagram() {
    this.setState({showModalDiagram: true});
  }

  _closeModalDiagram() {
    this.setState({showModalDiagram: false});
  }

  _getAssigneCell({ rowIndex, data, property}) {
    const entity = data[rowIndex];
    if (!entity || !entity[property]) {
      return '';
    }
    return (
      <Advanced.IdentityInfo entityIdentifier={entity[property]} face="link"/>
    );
  }

  _getCandidatesCell({ rowIndex, data, property}) {
    const entity = data[rowIndex];
    if (!entity || !entity[property]) {
      return '';
    }
    return (
      <Advanced.IdentitiesInfo identities={entity[property]} maxEntry={MAX_CANDICATES} />
    );
  }

  _getProcessInfo(process) {
    if (process) {
      return (
        <div>
          <Basic.LabelWrapper readOnly ref="name" label={this.i18n('name')}>
            <Advanced.WorkflowProcessInfo entity={process} showLink={false} showLoading={!process} className="no-margin"/>
          </Basic.LabelWrapper>
        </div>
      );
    }
  }

  _getWfTaskCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.id) {
      return '';
    }
    entity.taskName = entity.name;
    entity.taskDescription = entity.description;
    return (
      workflowTaskInstanceManager.localize(entity, 'name')
    );
  }

  render() {
    const {showLoading, diagramUrl, showModalDiagram} = this.state;
    const {_historicProcess} = this.props;
    const { historicProcessInstanceId } = this.props.params;


    const showLoadingInternal = showLoading || !_historicProcess;
    let force = new SearchParameters();
    force = force.setFilter('processInstanceId', historicProcessInstanceId);
    let forceSubprocess = new SearchParameters();
    forceSubprocess = forceSubprocess.setFilter('superProcessInstanceId', historicProcessInstanceId);

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.PageHeader>
          {this.i18n('header')}
        </Basic.PageHeader>

        <Basic.Panel showLoading={showLoadingInternal}>
          <Basic.AbstractForm ref="form" data={_historicProcess} readOnly style={{ padding: '15px 15px 0 15px' }}>
            {this._getProcessInfo(_historicProcess)}
            <Basic.TextField ref="id" label={this.i18n('id')}/>
            <Basic.TextField ref="processDefinitionKey" label={this.i18n('processDefinitionKey')}/>
            <Basic.TextField ref="superProcessInstanceId" label={this.i18n('superProcessInstanceId')}/>
            <Basic.DateTimePicker ref="startTime" label={this.i18n('startTime')}/>
            <Basic.DateTimePicker ref="endTime" label={this.i18n('endTime')}/>
            <Basic.TextArea ref="deleteReason" label={this.i18n('deleteReason')}/>
            <Basic.ScriptArea ref="_processVariablesJson" mode="json" readOnly rows={6} label={this.i18n('processVariables')} rendered={ SecurityManager.isAdmin() }/>
          </Basic.AbstractForm>
          <Basic.PanelFooter>
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>
              {this.i18n('button.back')}
            </Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
        <Basic.Panel>
          <Basic.PanelHeader>
            {this.i18n('tasks')}
          </Basic.PanelHeader>
          <Advanced.Table
            ref="tableTasks"
            uiKey="table-tasks"
            pagination={false}
            forceSearchParameters={force}
            manager={workflowHistoricTaskInstanceManager}>
            <Advanced.Column
              header=""
              property="name"
              cell={this._getWfTaskCell}
              sort={false}/>
            <Advanced.Column
              property="assignee"
              sort={false}
              cell={this._getAssigneCell}/>
            <Advanced.Column
                property="candicateUsers"
                sort={false}
                cell={this._getCandidatesCell}/>
            <Advanced.Column property="createTime" sort face="datetime"/>
            <Advanced.Column property="endTime" sort face="datetime"/>
            <Advanced.Column property="completeTaskDecision" sort={false} face="text"/>
            <Advanced.Column property="completeTaskMessage" sort={false} face="text"/>
            <Advanced.Column property="deleteReason" sort={false} face="text"/>
          </Advanced.Table>
        </Basic.Panel>
        <Basic.Panel>
          <Basic.PanelHeader>
            {this.i18n('subprocesses')}
          </Basic.PanelHeader>
          <HistoricProcessInstanceTable uiKey="historic_subprocess_instance_table" ref="subprocessTable"
            workflowHistoricProcessInstanceManager={workflowHistoricProcessInstanceManager}
            forceSearchParameters={forceSubprocess}
            filterOpened={false}/>
        </Basic.Panel>
        <Basic.Panel showLoading={!diagramUrl}>
          <Basic.PanelHeader>
            {this.i18n('diagram')} <div className="pull-right">
            <Basic.Button type="button" className="btn-sm" level="success" onClick={this._showFullDiagram.bind(this)}>
              <Basic.Icon icon="fullscreen"/>
            </Basic.Button>
          </div>
        </Basic.PanelHeader>
        <div style={{textAlign: 'center', marginBottom: '40px'}}>
          <img style={{maxWidth: '70%'}} src={diagramUrl}/>
        </div>
        </Basic.Panel>
        <Basic.Modal
           show={showModalDiagram}
           dialogClassName="modal-large"
           onHide={this._closeModalDiagram.bind(this)}
           style={{width: '90%'}} keyboard={!diagramUrl}>
          <Basic.Modal.Header text={this.i18n('fullscreenDiagram')}/>
          <Basic.Modal.Body style={{overflow: 'scroll'}}>
            <img src={diagramUrl}/>
          </Basic.Modal.Body>
          <Basic.Modal.Footer>
            <Basic.Button level="link" disabled={showLoading} onClick={this._closeModalDiagram.bind(this)}>{this.i18n('button.close')}</Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </div>
    );
  }
}

HistoricProcessInstanceDetail.propTypes = {
};
HistoricProcessInstanceDetail.defaultProps = {

};

function select(state, component) {
  const { historicProcessInstanceId } = component.params;
  const historicProcess = workflowHistoricProcessInstanceManager.getEntity(state, historicProcessInstanceId, false);
  if (historicProcess && !historicProcess.trimmed) {
    historicProcess._processVariablesJson = JSON.stringify(historicProcess.processVariables, null, 4);
  }
  return {
    _historicProcess: historicProcess
  };
}

export default connect(select, null, null, { withRef: true})(HistoricProcessInstanceDetail);
