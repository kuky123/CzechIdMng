import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Utils, Managers, Domain } from 'czechidm-core';

const ROLE_TYPE = 'roleType';
const IDENTITY_TYPE = 'identityType';

/**
* Table of provisioning break recipient
*
* @author Ondrej Kopr
*
*/
export class SystemProvisioningBreakConfigRecipientTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
      detail: {
        show: false,
        entity: {
          type: ROLE_TYPE
        }
      }
    };
    this.identityManager = new Managers.IdentityManager();
    this.roleManager = new Managers.RoleManager();
  }

  getContentKey() {
    return 'acc:content.provisioningBreakConfigRecipient';
  }

  getManager() {
    const { manager } = this.props;
    //
    return manager;
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

  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    const { detail } = this.state;
    detail.show = true;
    if (entity) {
      detail.entity = entity;
      if (entity.role != null) {
        detail.entity.type = ROLE_TYPE;
      } else {
        detail.entity.type = IDENTITY_TYPE;
      }
    } else {
      detail.entity = {
        type: ROLE_TYPE // role type
      };
    }
    this.setState({
      ...detail
    });
  }

  saveDetail(event) {
    if (event) {
      event.preventDefault();
    }
    const { detail } = this.state;
    const { provisioningBreakConfigId, uiKey, manager } = this.props;
    //
    detail.showLoading = true;
    this.setState({
      ...detail
    });
    //
    const formEntity = this.refs.form.getData();
    //
    const savedEntity = {
      ...formEntity,
      breakConfig: provisioningBreakConfigId
    };
    //
    // null unused value
    if (formEntity.type === ROLE_TYPE) {
      formEntity.identity = null;
    } else {
      formEntity.role = null;
    }
    //
    if (savedEntity.id === undefined) {
      this.context.store.dispatch(manager.createEntity(savedEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error, true);
      }));
    } else {
      this.context.store.dispatch(manager.updateEntity(savedEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
    }
  }

  afterSave(entity, error, newEntity) {
    if (!error) {
      if (newEntity) {
        this.addMessage({ message: this.i18n('create.success', { entityType: entity.entityType, operationType: entity.operationType}) });
      } else {
        this.addMessage({ message: this.i18n('save.success', {entityType: entity.entityType, operationType: entity.operationType}) });
      }
    } else {
      this.addError(error);
    }
    const { detail } = this.state;
    //
    detail.show = false;
    detail.showLoading = false;
    this.setState({
      ...detail
    });
    this.refs.table.getWrappedInstance().reload();
    super.afterSave();
  }

  _closeModal(event) {
    if (event) {
      event.preventDefault();
    }
    const { detail } = this.state;
    detail.show = false;
    this.setState({
      ...detail
    });
  }

  changeRecipientType(value, event) {
    if (event) {
      event.preventDefault();
    }
    const { detail } = this.state;
    detail.entity.type = value.value;
    this.setState({
      ...detail
    });
  }

  _getRecipientType(entity) {
    if (entity && entity.role) {
      return (<Basic.Label level="info" text={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.role')}/>);
    } else if (entity && entity.identity) {
      return (<Basic.Label level="success" text={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.identity')}/>);
    }
  }

  _getRecipientName(entity) {
    if (entity && entity.role) {
      if (entity._embedded) {
        return (
          <Advanced.EntityInfo
            entityType="role"
            entityIdentifier={ entity.role }
            entity={ entity._embedded.role }
            face="popover"/>
        );
      }
      return entity.role;
    } else if (entity && entity.identity) {
      if (entity._embedded) {
        return (
          <Advanced.EntityInfo
            entityType="identity"
            entityIdentifier={ entity.identity }
            entity={ entity._embedded.identity }
            face="popover"/>
        );
      }
      return entity.identity;
    }
  }

  getTableButtons(showAddButton) {
    return (
      [
        <Basic.Button
          level="success"
          key="add_button"
          className="btn-xs"
          onClick={this.showDetail.bind(this, null)}
          rendered={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') && showAddButton}>
          <Basic.Icon type="fa" icon="plus"/>
          {' '}
          {this.i18n('button.add')}
        </Basic.Button>
      ]);
  }

  render() {
    const { uiKey, manager, columns, forceSearchParameters, showAddButton, showRowSelection, rendered } = this.props;
    const { filterOpened, detail } = this.state;
    const isNew = detail.entity.id === undefined || detail.entity.id === null;

    if (!rendered) {
      return null;
    }
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Modal show={detail.show} showLoading={detail.showLoading} onHide={this._closeModal.bind(this)}>
          <form onSubmit={this.saveDetail.bind(this)}>
            <Basic.Modal.Header text={this.i18n('acc:content.provisioningBreakConfigRecipient.new')} rendered={isNew} />
            <Basic.Modal.Header text={this.i18n('acc:content.provisioningBreakConfigRecipient.edit')} rendered={!isNew} />
            <Basic.Modal.Body>
                <Basic.AbstractForm ref="form" data={detail.entity}>
                  <Basic.EnumSelectBox
                    ref="type"
                    label={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.label')}
                    value={detail.type}
                    clearable={false}
                    options={[
                      { value: ROLE_TYPE, niceLabel: this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.role') },
                      { value: IDENTITY_TYPE, niceLabel: this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.identity') }
                    ]}
                    onChange={this.changeRecipientType.bind(this)}
                    required/>
                  <Basic.SelectBox
                    ref="identity"
                    label={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.identity')}
                    hidden={detail.entity.type === ROLE_TYPE}
                    manager={this.identityManager}/>
                  <Basic.SelectBox
                    ref="role"
                    label={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.role')}
                    hidden={detail.entity.type === IDENTITY_TYPE}
                    manager={this.roleManager}/>
                </Basic.AbstractForm>
                {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
                <input type="submit" className="hidden"/>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button level="link" onClick={this._closeModal.bind(this)}>{this.i18n('button.cancel')}</Basic.Button>
              <Basic.Button ref="yesButton" level="success" onClick={this.saveDetail.bind(this)} rendered={isNew}>{this.i18n('button.create')}</Basic.Button>
              <Basic.Button ref="yesButton" level="success" onClick={this.saveDetail.bind(this)} rendered={!isNew}>{this.i18n('button.save')}</Basic.Button>
            </Basic.Modal.Footer>
          </form>
      </Basic.Modal>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          filterOpened={filterOpened}
          forceSearchParameters={forceSearchParameters}
          showRowSelection={ Managers.SecurityManager.hasAuthority('SYSTEM_DELETE') && showRowSelection }
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row className="last">
                  <div className="col-lg-4">
                    <Advanced.Filter.SelectBox
                      ref="identityId"
                      manager={this.identityManager}/>
                  </div>
                  <div className="col-lg-4">
                    <Advanced.Filter.SelectBox
                      ref="roleId"
                      manager={this.roleManager}/>
                  </div>
                  <div className="col-lg-4 text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </div>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          buttons = {this.getTableButtons(showAddButton)}
          _searchParameters={ this.getSearchParameters() }
          >
          <Advanced.Column
            header=""
            className="detail-button"
            rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }
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
            property="type"
            rendered={_.includes(columns, 'type')}
            header={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.type.label')}
            width="130px"
            cell={({ rowIndex, data }) => {
              return (
                this._getRecipientType(data[rowIndex])
              );
            }}/>
          <Advanced.Column
            property="name"
            sort
            face="text"
            rendered={_.includes(columns, 'name')}
            header={this.i18n('acc:entity.ProvisioningBreakConfigRecipient.name')}
            cell={({ rowIndex, data }) => {
              return (
                this._getRecipientName(data[rowIndex])
              );
            }}/>
        </Advanced.Table>
      </div>
    );
  }
}

SystemProvisioningBreakConfigRecipientTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  forceSearchParameters: PropTypes.object,
  showAddButton: PropTypes.bool,
  showRowSelection: PropTypes.bool,
  rendered: PropTypes.bool,
  provisioningBreakConfigId: PropTypes.string
};

SystemProvisioningBreakConfigRecipientTable.defaultProps = {
  columns: ['name', 'type'],
  filterOpened: false,
  _showLoading: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showAddButton: true,
  showRowSelection: true,
  rendered: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _showLoading: component.manager.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select)(SystemProvisioningBreakConfigRecipientTable);
