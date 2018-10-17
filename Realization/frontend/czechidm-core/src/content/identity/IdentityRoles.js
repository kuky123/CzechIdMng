import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import SearchParameters from '../../domain/SearchParameters';
import { IdentityRoleManager, IdentityContractManager, IdentityManager, WorkflowTaskInstanceManager, WorkflowProcessInstanceManager, SecurityManager, RoleRequestManager } from '../../redux';
import RoleRequestTable from '../requestrole/RoleRequestTable';
import IdentityRoleTableComponent, { IdentityRoleTable } from './IdentityRoleTable';

const uiKey = 'identity-roles';
const uiKeyContracts = 'role-identity-contracts';
const identityRoleManager = new IdentityRoleManager();
const identityManager = new IdentityManager();
const identityContractManager = new IdentityContractManager();
const workflowProcessInstanceManager = new WorkflowProcessInstanceManager();
const roleRequestManager = new RoleRequestManager();
const workflowTaskInstanceManager = new WorkflowTaskInstanceManager();

/**
 * Assigned identity roles
 * Created role requests
 * Roles in approval (wf)
 *
 * @author VS
 * @author Radek Tomiška
 */
class IdentityRoles extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      activeKey: 1,
    };
  }

  getContentKey() {
    return 'content.identity.roles';
  }

  getNavigationKey() {
    return 'profile-roles';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    this.context.store.dispatch(identityContractManager.fetchEntities(new SearchParameters(SearchParameters.NAME_AUTOCOMPLETE).setFilter('identity', entityId).setFilter('validNowOrInFuture', true), `${uiKeyContracts}-${entityId}`));
  }

  showProcessDetail(entity) {
    this.context.router.push('workflow/history/processes/' + entity.id);
  }

  /**
   * Compute background color row (added, removed, changed)
   */
  _rowClass({rowIndex, data}) {
    if (data[rowIndex].processVariables.operationType === 'add') {
      return 'bg-success';
    }
    if (data[rowIndex].processVariables.operationType === 'remove') {
      return 'bg-danger';
    }
    if (data[rowIndex].processVariables.operationType === 'change') {
      return 'bg-warning';
    }
    return null;
  }

  _onDeleteAddRoleProcessInstance(entity, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs['confirm-delete'].show(
      this.i18n('content.identity.roles.changeRoleProcesses.deleteConfirm', {'processId': entity.id}),
      this.i18n(`action.delete.header`, { count: 1 })
    ).then(() => {
      this.context.store.dispatch(workflowProcessInstanceManager.deleteEntity(entity, null, (deletedEntity, error) => {
        if (!error) {
          this.addMessage({ message: this.i18n('content.identity.roles.changeRoleProcesses.deleteSuccess', {'processId': entity.id})});
        } else {
          this.addError(error);
        }
        this.refs.tableProcesses.getWrappedInstance().reload();
        this.refs.tablePermissionProcesses.getWrappedInstance().reload();
      }));
    }, () => {
      // Rejected
    });
  }

  _roleNameCell({ rowIndex, data }) {
    const roleId = data[rowIndex].processVariables.conceptRole ? data[rowIndex].processVariables.conceptRole.role : data[rowIndex].processVariables.entityEvent.content.role;
    return (
      <Advanced.RoleInfo
      entityIdentifier={ roleId}
      face="popover" />
    );
  }

  _getWfTaskCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.id) {
      return '';
    }
    entity.taskName = entity.name;
    entity.taskDescription = entity.description;
    entity.definition = {id: entity.activityId};
    return (
      workflowTaskInstanceManager.localize(entity, 'name')
    );
  }

  /**
   * Can change identity permission
   *
   * @return {[type]} [description]
   */
  _canChangePermissions() {
    const { _permissions } = this.props;
    return Utils.Permission.hasPermission(_permissions, 'CHANGEPERMISSION');
  }

  /**
   * Redirects to tab with identity contracts
   *
   * @param  {string} identityId
   */
  showContracts(identityId) {
    this.context.router.push(`/identity/${identityId}/contracts`);
  }

  _getWfProcessCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.id) {
      return '';
    }
    return (
      <Advanced.WorkflowProcessInfo entityIdentifier={entity.id}/>
    );
  }

  _getCandidatesCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.candicateUsers) {
      return '';
    }
    return (
      <Advanced.IdentitiesInfo identities={entity.candicateUsers} maxEntry={2} />
    );
  }

  _onChangeSelectTabs(activeKey) {
    this.setState({
      activeKey
    });
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoadingContracts, _contracts, _permissions, _requestUi } = this.props;
    const { activeKey } = this.state;
    //
    let force = new SearchParameters();
    force = force.setFilter('identity', entityId);
    force = force.setFilter('category', 'eu.bcvsolutions.role.approve');
    let roleRequestsForceSearch = new SearchParameters();
    roleRequestsForceSearch = roleRequestsForceSearch.setFilter('applicant', entityId);
    roleRequestsForceSearch = roleRequestsForceSearch.setFilter('states', ['IN_PROGRESS', 'DUPLICATED', 'EXCEPTION']);
    let conceptsForceSearch = new SearchParameters();
    conceptsForceSearch = conceptsForceSearch.setFilter('applicant', entityId);
    conceptsForceSearch = conceptsForceSearch.setFilter('state', 'CONCEPT');
    let hasRoleConcepts = true;
    const uiKeyConceptTable = `table-applicant-concepts-${entityId}`;

    if (this.context.store.getState().data.ui[uiKeyConceptTable]
      && !(this.context.store.getState().data.ui[uiKeyConceptTable].total > 0)) {
      hasRoleConcepts = false;
    }
    //
    return (
      <div style={{ paddingTop: 15 }}>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Helmet title={ this.i18n('title') } />

        {
          (_contracts.length > 0) || !this._canChangePermissions()
          ||
          <Basic.Alert
            text={this.i18n('contracts.empty.message')}
            rendered={ !_showLoadingContracts }
            className="no-margin"
            buttons={[
              <Basic.Button
                level="info"
                rendered={ SecurityManager.hasAuthority('APP_ADMIN') }
                onClick={ this.showContracts.bind(this, entityId) }>
                { this.i18n('contracts.empty.button') }
              </Basic.Button>
            ]}/>
        }

        <Basic.Tabs activeKey={ activeKey } onSelect={ this._onChangeSelectTabs.bind(this) }>
          <Basic.Tab eventKey={ 1 } title={ this.i18n('header') } className="bordered">

            <Basic.ContentHeader icon="fa:universal-access" text={ this.i18n('directRoles.header') } style={{ marginBottom: 0, paddingRight: 15, paddingLeft: 15, paddingTop: 15 }} />

            <IdentityRoleTableComponent
              uiKey={ `${uiKey}-${entityId}` }
              forceSearchParameters={ new SearchParameters().setFilter('identityId', entityId).setFilter('directRole', true) }
              showAddButton={ _contracts.length > 0 }
              params={ this.props.params }
              columns={ _.difference(IdentityRoleTable.defaultProps.columns, ['directRole']) }
              _permissions={ _permissions }/>

            <Basic.ContentHeader icon="arrow-down" text={ this.i18n('subRoles.header') } style={{ marginBottom: 0, paddingRight: 15, paddingLeft: 15 }}/>

            <IdentityRoleTableComponent
              uiKey={ `${uiKey}-sub-${entityId}` }
              forceSearchParameters={ new SearchParameters().setFilter('identityId', entityId).setFilter('directRole', false) }
              showAddButton={ false }
              params={ this.props.params }
              columns={ _.difference(IdentityRoleTable.defaultProps.columns, ['automaticRole']) }/>
          </Basic.Tab>

          <Basic.Tab
            eventKey={ 2 }
            title={
              <span>
                { this.i18n('changePermissionRequests.label') }
                <Basic.Badge
                  level="warning"
                  style={{ marginLeft: 5 }}
                  text={ _requestUi ? _requestUi.total : null }
                  rendered={ _requestUi && _requestUi.total > 0 }
                  title={ this.i18n('changePermissionRequests.header') }/>
              </span>
            }
            className="bordered">
            {
              !SecurityManager.hasAuthority('ROLEREQUEST_READ')
              ||
              <div>
                <Basic.ContentHeader
                  icon="fa:sitemap"
                  text={ this.i18n('conceptPermissionRequests.header') }
                  style={{ marginBottom: 0, paddingTop: 15, paddingRight: 15, paddingLeft: 15 }}
                  rendered={ activeKey === 2 && hasRoleConcepts }/>
                <RoleRequestTable
                  ref="conceptTable"
                  uiKey={ uiKeyConceptTable }
                  showFilter={ false }
                  forceSearchParameters={ conceptsForceSearch }
                  columns={ ['state', 'created', 'modified', 'detail'] }
                  manager={ roleRequestManager }
                  rendered={ activeKey === 2 }
                  className={ hasRoleConcepts ? '' : 'hidden'}/>

                <Basic.ContentHeader
                  icon="fa:sitemap"
                  text={ this.i18n('changePermissionRequests.header') }
                  style={{ marginBottom: 0, paddingRight: 15, paddingLeft: 15, paddingTop: hasRoleConcepts ? 10 : 15 }}/>
                <RoleRequestTable
                  ref="requestTable"
                  uiKey={ 'table-applicant-requests' }
                  showFilter={ false }
                  forceSearchParameters={ roleRequestsForceSearch }
                  columns={ ['state', 'created', 'modified', 'wf', 'detail'] }
                  manager={ roleRequestManager }/>
              </div>
            }

            <Basic.ContentHeader
              icon="fa:sitemap"
              text={ this.i18n('changeRoleProcesses.header') }
              style={{ marginBottom: 0, paddingRight: 15, paddingLeft: 15, paddingTop: SecurityManager.hasAuthority('ROLEREQUEST_READ') ? 10 : 15 }}
              rendered={ activeKey === 2 }/>
            <Advanced.Table
              ref="tableProcesses"
              uiKey="table-processes"
              rowClass={ this._rowClass }
              forceSearchParameters={ force }
              manager={ workflowProcessInstanceManager }
              pagination={ false }>
              <Advanced.Column
                property="detail"
                cell={
                  ({ rowIndex, data }) => {
                    return (
                      <Advanced.DetailButton
                        title={this.i18n('button.detail')}
                        onClick={this.showProcessDetail.bind(this, data[rowIndex])}/>
                    );
                  }
                }
                header={' '}
                sort={false}
                face="text"/>
              <Advanced.Column
                property="currentActivityName"
                header={this.i18n('content.roles.processRoleChange.currentActivity')}
                cell={this._getWfTaskCell}
                sort={false}/>
              <Advanced.Column
                property="processVariables.conceptRole.role"
                cell={this._roleNameCell.bind(this)}
                header={this.i18n('content.roles.processRoleChange.roleName')}
                sort={false}
                face="text"/>
              <Advanced.Column
                property="candicateUsers"
                header={this.i18n('content.roles.processRoleChange.candicateUsers')}
                face="text"
                cell={this._getCandidatesCell}
                />
              <Advanced.Column
                property="processVariables.conceptRole.validFrom"
                header={this.i18n('content.roles.processRoleChange.roleValidFrom')}
                sort={false}
                face="date"/>
              <Advanced.Column
                property="processVariables.conceptRole.validTill"
                header={this.i18n('content.roles.processRoleChange.roleValidTill')}
                sort={false}
                face="date"/>
              <Advanced.Column
                property="id"
                cell={this._getWfProcessCell}
                header={this.i18n('content.roles.processRoleChange.wfProcessId')}
                sort={false}
                face="text"/>
            </Advanced.Table>
          </Basic.Tab>
        </Basic.Tabs>
      </div>
    );
  }
}

IdentityRoles.propTypes = {
  identity: PropTypes.object,
  _showLoading: PropTypes.bool,
  _showLoadingContracts: PropTypes.bool,
  _entities: PropTypes.arrayOf(PropTypes.object),
  _contracts: PropTypes.arrayOf(PropTypes.object),
  userContext: PropTypes.object,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ])
};
IdentityRoles.defaultProps = {
  _showLoading: true,
  _showLoadingContracts: true,
  _contracts: [],
  userContext: null,
  _permissions: null,
};

function select(state, component) {
  let addRoleProcessIds;
  if (state.data.ui['table-processes'] && state.data.ui['table-processes'].items) {
    addRoleProcessIds = state.data.ui['table-processes'].items;
  }
  const entityId = component.params.entityId;

  return {
    identity: identityManager.getEntity(state, entityId),
    _showLoading: identityRoleManager.isShowLoading(state, `${uiKey}-${entityId}`),
    _showLoadingContracts: identityContractManager.isShowLoading(state, `${uiKeyContracts}-${entityId}`),
    _contracts: identityContractManager.getEntities(state, `${uiKeyContracts}-${entityId}`),
    _addRoleProcessIds: addRoleProcessIds,
    userContext: state.security.userContext,
    _permissions: identityManager.getPermissions(state, null, entityId),
    _searchParameters: Utils.Ui.getSearchParameters(state, `${uiKey}-${entityId}`),
    _requestUi: Utils.Ui.getUiState(state, 'table-applicant-requests')
  };
}

export default connect(select)(IdentityRoles);
