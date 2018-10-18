import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import {SecurityManager, IdentityManager, WorkflowTaskInstanceManager} from '../../redux';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';

const workflowTaskInstanceManager = new WorkflowTaskInstanceManager();
/**
 * Table of universal requests
 *
 * @author Vít Švanda
 */
export class RequestTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
  }

  getUiKey() {
    return this.props.uiKey;
  }

  getManager() {
    return this.props.manager;
  }

  getContentKey() {
    return 'content.requests';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  reload() {
    this.refs.table.getWrappedInstance().reload();
  }

  showDetail(entity) {
    this.context.router.push(`/requests/${entity.id}/detail`);
  }

  reload() {
    this.refs.table.getWrappedInstance().reload();
  }

  _getCandidatesCell({ rowIndex, data, property}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    return (
      <Advanced.IdentitiesInfo identities={entity._embedded.wfProcessId[property]} maxEntry={2} />
    );
  }

  _renderTargetCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity && entity._embedded) {
      return '';
    }
    const owner = entity._embedded.ownerId;
    const types = entity.ownerType.split('.');
    const entityType = types[types.length - 1];
    // If owner does not exists (was delete/not exists yet), the name will be returned;
    if (!owner) {
      return entity.name;
    }
    return (
      <Advanced.EntityInfo
        entityType={ entityType }
        entity={owner}
        showLink
        face="popover"/>
    );
  }

  _getCurrentActivitiCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    const task = {taskName: entity._embedded.wfProcessId.currentActivityName,
                  processDefinitionKey: entity._embedded.wfProcessId.processDefinitionKey,
                  definition: {id: entity._embedded.wfProcessId.activityId}
                };
    return (
      workflowTaskInstanceManager.localize(task, 'name')
    );
  }

  _getWfProcessCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    return (
      <Advanced.WorkflowProcessInfo entity={entity._embedded.wfProcessId}/>
    );
  }

  _renderDetailCell({ rowIndex, data }) {
    return (
      <Advanced.DetailButton
        title={this.i18n('button.detail')}
        onClick={this.showDetail.bind(this, data[rowIndex])}/>
    );
  }

  render() {
    const { _showLoading, uiKey, startRequestFunc, columns, forceSearchParameters, showFilter} = this.props;
    const innerShowLoading = _showLoading;
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          showFilter={showFilter}
          showLoading={innerShowLoading}
          forceSearchParameters={forceSearchParameters}
          manager={this.getManager()}
          showRowSelection={SecurityManager.hasAuthority('REQUEST_UPDATE')}
          actions={
            [{ value: 'delete', niceLabel: this.i18n('action.delete.action'),
               action: this.onDelete.bind(this), disabled: false }]
          }
          filterOpened
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate ref="fromTill"/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={ this.i18n('filter.text.placeholder') }
                      help={ Advanced.Filter.getTextHelp() }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="states"
                      placeholder={ this.i18n('filter.states.placeholder') }
                      enum={ RoleRequestStateEnum }
                      multiSelect/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          _searchParameters={ this.getSearchParameters() }>
          <Advanced.Column
            property=""
            header=""
            rendered={_.includes(columns, 'detail')}
            className="detail-button"
            cell={this._renderDetailCell.bind(this)}/>
          <Advanced.Column
            property="state"
            rendered={_.includes(columns, 'state')}
            sort
            face="enum"
            enumClass={RoleRequestStateEnum}/>
          <Advanced.Column
            property="name"
            rendered={false}
            face="text"
            />
          <Advanced.Column
            property="ownerId"
            header={ this.i18n('entity.RequestItem.ownerId') }
            face="text"
            rendered={_.includes(columns, 'targetObject')}
            cell={this._renderTargetCell}/>
          <Advanced.Column
            property="candicateUsers"
            face="text"
            rendered={_.includes(columns, 'wf')}
            cell={this._getCandidatesCell}
            />
          <Advanced.Column
            property="executeImmediately"
            rendered={_.includes(columns, 'executeImmediately')}
            sort
            face="boolean"/>
          <Advanced.Column
            property="currentActivity"
            rendered={_.includes(columns, 'wf')}
            face="text"
            cell={this._getCurrentActivitiCell}
            />
          <Advanced.Column
            property="wfProcessId"
            rendered={_.includes(columns, 'wf_name')}
            cell={this._getWfProcessCell}
            sort
            face="text"/>
          <Advanced.Column
            property="result"
            face="text"
            rendered={_.includes(columns, 'result')}
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.OperationResult value={ entity.result }/>
                );
              }
            }/>
          <Advanced.Column
            property="created"
            rendered={_.includes(columns, 'created')}
            sort
            face="datetime"/>
          <Advanced.Column
            property=""
            header=""
            rendered={_.includes(columns, 'startRequest')}
            width="55px"
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                const state = data[rowIndex].state;
                const canBeStart = (state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.CONCEPT))
                || (state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXCEPTION))
                || (state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.DUPLICATED));
                return (
                  <span>
                    <Basic.Button
                      ref="startButton"
                      type="button"
                      level="success"
                      rendered={SecurityManager.hasAnyAuthority(['REQUEST_UPDATE']) && canBeStart}
                      style={{marginRight: '2px'}}
                      title={this.i18n('button.start')}
                      titlePlacement="bottom"
                      onClick={startRequestFunc && startRequestFunc.bind(this, [data[rowIndex].id])}
                      className="btn-xs">
                      <Basic.Icon type="fa" icon="play"/>
                    </Basic.Button>
                  </span>
                );
              }
            }/>
        </Advanced.Table>
      </div>
    );
  }
}

RequestTable.propTypes = {
  _showLoading: PropTypes.bool,
  showFilter: PropTypes.bool,
  forceSearchParameters: PropTypes.object,
  startRequestFunc: PropTypes.func,
  columns: PropTypes.arrayOf(PropTypes.string)
};

RequestTable.defaultProps = {
  _showLoading: false,
  showFilter: true,
  columns: ['state', 'targetObject', 'created', 'modified', 'wf', 'executeImmediately',
   'startRequest', 'detail', 'wf_name', 'result']
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(RequestTable);
