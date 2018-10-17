import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Utils from '../../../utils';
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import { SecurityManager, AutomaticRoleRequestManager, RoleManager } from '../../../redux';
import AutomaticRoleRequestTableComponent, { AutomaticRoleRequestTable } from '../../automaticrolerequest/AutomaticRoleRequestTable';
import SearchParameters from '../../../domain/SearchParameters';


const automaticRoleRequestManager = new AutomaticRoleRequestManager();
const roleManager = new RoleManager();
/**
 * Table with automatic roles
 *
 * @author Ondřej Kopr
 */
export class AutomaticRoleAttributeTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return this.props.manager;
  }

  getContentKey() {
    return 'content.automaticRoles.attribute';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  /**
   * Recive new form for create new type else show detail for existing automatic role.
   */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    const { forceSearchParameters } = this.props;

    let roleId = null;
    const entityId = entity.id;
    if (forceSearchParameters) {
      if (forceSearchParameters.getFilters().has('roleId')) {
        roleId = forceSearchParameters.getFilters().get('roleId');
      }
    }
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      if (roleId) {
        this.context.router.push(`/role/${roleId}/automatic-roles/attributes/${entityId}/new?new=1`);
      } else {
        this.context.router.push(`/automatic-role/attributes/${uuidId}/new?new=1`);
      }
    } else {
      if (roleId) {
        this.context.router.push(`/role/${roleId}/automatic-roles/attributes/${entityId}/detail`);
      } else {
        this.context.router.push('/automatic-role/attributes/' + entity.id);
      }
    }
  }

  /**
   * Bulk delete operation
   */
  _onDeleteViaRequest(bulkActionValue, selectedRows) {
    const selectedEntities = this.getManager().getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: this.getManager().getNiceLabel(selectedEntities[0]), records: this.getManager().getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: this.getManager().getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(this.getManager().deleteAutomaticRolesViaRequest(selectedEntities, this.getUiKey(), (entity, error) => {
        if (entity && error) {
          if (error.statusCode !== 202) {
            this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: this.getManager().getNiceLabel(entity) }) }, error);
          } else {
            this.addError(error);
          }
        } else {
          this.afterDelete();
        }
      }));
    }, () => {
      // nothing
    });
  }

  afterDelete() {
    super.afterDelete();
    this.refs['automatic-role-requests-table'].getWrappedInstance().reload();
  }

  _createNewRequest(event, roleId) {
    if (event) {
      event.preventDefault();
    }
    const uuidId = uuid.v1();
    this.context.router.push(`/automatic-role-requests/${uuidId}/new?new=1&roleId=${roleId}`);
  }

  _showCreateDetail(event) {
    const { forceSearchParameters } = this.props;

    let roleId = null;
    if (forceSearchParameters) {
      if (forceSearchParameters.getFilters().has('roleId')) {
        roleId = forceSearchParameters.getFilters().get('roleId');
      }
    }
    if (roleId) {
      this._createNewRequest(event, roleId);
    } else {
      this.setState({
        detail: {
          ... this.state.detail,
          show: true
        }
      });
    }
  }

  _createNewRequestFromModal(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.modalForm.isFormValid()) {
      return;
    }
    const roleId = this.refs.role.getValue();
    const uuidId = uuid.v1();
    this.context.router.push(`/automatic-role-requests/${uuidId}/new?new=1&roleId=${roleId}`);
  }

  /**
   * Close modal dialog
   */
  _closeDetail() {
    this.setState({
      detail: {
        ... this.state.detail,
        show: false
      }
    });
  }

  render() {
    const { uiKey, manager, columns, forceSearchParameters, _showLoading} = this.props;
    const { showLoading, detail } = this.state;
    const innerShowLoading = _showLoading || showLoading;

    let roleId = null;
    if (forceSearchParameters) {
      if (forceSearchParameters.getFilters().has('roleId')) {
        roleId = forceSearchParameters.getFilters().get('roleId');
      }
    }
    let requestForceSearch = new SearchParameters();
    requestForceSearch = requestForceSearch.setFilter('roleId', roleId);
    requestForceSearch = requestForceSearch.setFilter('requestType', 'ATTRIBUTE');
    requestForceSearch = requestForceSearch.setFilter('states', ['IN_PROGRESS', 'CONCEPT', 'EXCEPTION']);
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          showRowSelection={SecurityManager.hasAuthority('AUTOMATIC_ROLE_DELETE')}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row className="last">
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text')}
                      help={ Advanced.Filter.getTextHelp() }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this._onDeleteViaRequest.bind(this), disabled: false }
            ]
          }
          buttons={
            [
              <Basic.Button level="success" key="add_button" className="btn-xs"
                      onClick={this._showCreateDetail.bind(this)}
                      rendered={SecurityManager.hasAuthority('AUTOMATIC_ROLE_CREATE')}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          forceSearchParameters={forceSearchParameters}
          _searchParameters={ this.getSearchParameters() }
          >
          <Advanced.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.showDetail.bind(this, data[rowIndex])}/>
                );
              }
            }
            sort={false}/>
          <Advanced.Column
            property="name"
            width="20%"
            rendered={_.includes(columns, 'name')}
            header={this.i18n('entity.AutomaticRole.name.label')}
            sort/>
          <Advanced.Column
            property="_embedded.role.name"
            header={this.i18n('entity.AutomaticRole.role.label')}
            width="25%"
            rendered={_.includes(columns, 'role')}
            sort
            sortProperty="role.name"
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.EntityInfo
                    entityType="role"
                    entityIdentifier={ entity.role }
                    entity={ entity._embedded.role }
                    face="popover"/>
                );
              }
            }/>
          <Advanced.Column
            property="concept"
            sort
            header={this.i18n('entity.AutomaticRole.attribute.concept.label')}
            cell={
              ({ rowIndex, data }) => {
                if (data && data[rowIndex].concept === true) {
                  return (
                    <Basic.Tooltip value={this.i18n('entity.AutomaticRole.attribute.concept.help')}>
                      <Basic.Label level="warning" text={this.i18n('entity.AutomaticRole.attribute.concept.info')}/>
                    </Basic.Tooltip>
                  );
                }
              }
            }/>
        </Advanced.Table>

        {
          !SecurityManager.hasAuthority('AUTOMATICROLEREQUEST_READ')
          ||
          <div className="tab-pane-table-body">
            <Basic.ContentHeader style={{ marginBottom: 0 }} text={this.i18n('content.automaticRoles.request.header')}/>
            <AutomaticRoleRequestTableComponent
              ref="automatic-role-requests-table"
              uiKey="role-automatic-role-requests-table"
              forceSearchParameters={requestForceSearch}
              columns={ _.difference(AutomaticRoleRequestTable.defaultProps.columns,
                 roleId ? ['role', 'executeImmediately', 'startRequest', 'createNew']
                        : ['executeImmediately', 'startRequest', 'createNew', 'wf_name', 'modified']
              ) }
              showFilter={false}
              manager={automaticRoleRequestManager}/>
          </div>
        }

        <Basic.Modal
          bsSize="default"
          show={detail.show}
          onHide={this._closeDetail.bind(this)}
          backdrop="static"
          keyboard={!innerShowLoading}>

          <form onSubmit={this._createNewRequestFromModal.bind(this)}>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('content.automaticRoleRequests.create.header')}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="modalForm" showLoading={_showLoading}>
                <Basic.SelectBox
                  ref="role"
                  manager={ roleManager }
                  label={ this.i18n('content.automaticRoleRequests.role') }
                  required/>

              </Basic.AbstractForm>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this._closeDetail.bind(this)}
                showLoading={_showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={_showLoading}
                showLoadingIcon>
                {this.i18n('content.automaticRoleRequests.button.createRequest')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

AutomaticRoleAttributeTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired
};

AutomaticRoleAttributeTable.defaultProps = {
  columns: ['name', 'role']
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(AutomaticRoleAttributeTable);
