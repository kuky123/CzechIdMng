import React from 'react';
import PropTypes from 'prop-types';
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
 * Role request table
 * @author Vít Švanda
 */
class RoleRequestTable extends Advanced.AbstractTableContent {

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
    return 'content.roleRequests';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }


  showDetail(entity, add) {
    const {createNewRequestFunc } = this.props;
    if (add) {
      createNewRequestFunc(entity);
    } else {
      this.context.router.push(`/role-requests/${entity.id}/detail`);
    }
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
      <Advanced.IdentitiesInfo identities={entity._embedded.wfProcessId[property]} maxEntry={5} />
    );
  }

  _getCurrentActivitiCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }

    const task = {};
    task.definition = {id: entity._embedded.wfProcessId.activityId};
    task.processDefinitionKey = entity._embedded.wfProcessId.processDefinitionKey;
    task.taskName = entity._embedded.wfProcessId.currentActivityName;
    return (
      workflowTaskInstanceManager.localize(task, 'name')
    );
  }

  _getWfProcessCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.wfProcessId) {
      return '';
    }
    return (
      <Advanced.WorkflowProcessInfo entityIdentifier={ entity.wfProcessId } entity={ entity._embedded ? entity._embedded.wfProcessId : null }/>
    );
  }

  render() {
    const { _showLoading, uiKey, startRequestFunc, createNewRequestFunc, columns, forceSearchParameters, showFilter, className, rendered } = this.props;
    const innerShowLoading = _showLoading;
    //
    if (!rendered) {
      return null;
    }
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          showLoading={innerShowLoading}
          forceSearchParameters={forceSearchParameters}
          manager={this.getManager()}
          showRowSelection={SecurityManager.hasAuthority('ROLEREQUEST_UPDATE')}
          actions={
            [{ value: 'delete', niceLabel: this.i18n('action.delete.action'),
               action: this.onDelete.bind(this), disabled: false }]
          }
          filterOpened
          showFilter={ showFilter }
          filter={
            !showFilter
            ?
            null
            :
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="applicant"
                      placeholder={this.i18n('filter.applicant.placeholder')}/>
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
                <Basic.Row className="last">
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.SelectBox
                      ref="applicants"
                      placeholder={this.i18n('filter.applicants.placeholder')}
                      multiSelect
                      manager={this.identityManager}/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate
                      ref="fromTill"
                      fromProperty="createdFrom"
                      fromPlaceholder={this.i18n('filter.dateCreatedFrom.placeholder')}
                      tillProperty="createdTill"
                      tillPlaceholder={this.i18n('filter.dateCreatedTill.placeholder')}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={this.showDetail.bind(this, { }, true)}
                rendered={
                  _.includes(columns, 'createNew')
                    && createNewRequestFunc
                    && SecurityManager.hasAnyAuthority(['ROLEREQUEST_ADMIN'])
                  }>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }
          className={ className }>
          <Advanced.Column
            property=""
            header=""
            rendered={_.includes(columns, 'detail')}
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.showDetail.bind(this, data[rowIndex], false)}/>
                );
              }
            }/>
          <Advanced.Column
            property="state"
            rendered={_.includes(columns, 'state')}
            sort
            face="enum"
            enumClass={RoleRequestStateEnum}/>
          <Advanced.Column
            property="applicant"
            rendered={_.includes(columns, 'applicant')}
            face="text"
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.IdentityInfo entityIdentifier={ entity.applicant } entity={ entity._embedded.applicant } face="popover" />
                );
              }
            }/>
          <Advanced.Column
            property="currentActivity"
            rendered={_.includes(columns, 'wf')}
            face="text"
            cell={this._getCurrentActivitiCell}
            />
          <Advanced.Column
            property="candicateUsers"
            rendered={_.includes(columns, 'wf')}
            face="text"
            cell={this._getCandidatesCell}
            />
          <Advanced.Column
            property="executeImmediately"
            rendered={_.includes(columns, 'executeImmediately')}
            sort
            face="boolean"/>
          <Advanced.Column
            property="modified"
            rendered={_.includes(columns, 'modified')}
            sort
            face="datetime"/>
          <Advanced.Column
            property="created"
            rendered={_.includes(columns, 'created')}
            sort
            face="datetime"/>
          <Advanced.Column
            property="wfProcessId"
            rendered={_.includes(columns, 'wf')}
            cell={this._getWfProcessCell}
            sort
            face="text"/>
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
                      rendered={startRequestFunc && SecurityManager.hasAnyAuthority(['ROLEREQUEST_UPDATE']) && canBeStart}
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

RoleRequestTable.propTypes = {
  _showLoading: PropTypes.bool,
  showFilter: PropTypes.bool,
  forceSearchParameters: PropTypes.object,
  startRequestFunc: PropTypes.func,
  createNewRequestFunc: PropTypes.func,
  columns: PropTypes.arrayOf(PropTypes.string),
  /**
   * Css
   */
  className: PropTypes.string
};

RoleRequestTable.defaultProps = {
  rendered: true,
  _showLoading: false,
  showFilter: true,
  columns: ['state', 'created', 'modified', 'wf', 'applicant', 'executeImmediately', 'startRequest', 'createNew', 'detail']
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(RoleRequestTable);
