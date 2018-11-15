import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Managers, Basic, Domain, Utils, Advanced } from 'czechidm-core';
import { RoleSystemManager, SystemManager, RoleSystemAttributeManager, SystemMappingManager } from '../../redux';
import uuid from 'uuid';
import SystemOperationTypeEnum from '../../domain/SystemOperationTypeEnum';

const uiKey = 'role-system';
const uiKeyAttributes = 'role-system-attributes';
let roleSystemAttributeManager = null;
const systemManager = new SystemManager();
let roleSystemManager = null;
let roleManager = null;
const systemMappingManager = new SystemMappingManager();

/**
 * Role system mapping detail - attribute mapping can be overriden.
 *
 * @author Vít Švanda
 * @author Radek Tomiška
 */
class RoleSystemDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      systemId: this._isSystemMenu() ? props.params.entityId : null, // dependant select box
      roleSystem: {
        role: this._isSystemMenu() ? null : props.params.entityId,
        system: this._isSystemMenu() ? props.params.entityId : null
      }
    };
  }

  getManager() {
    return roleSystemAttributeManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.role.roleSystemDetail';
  }

  getNavigationKey() {
    if (this._isSystemMenu()) {
      return 'system-roles';
    }
    return this.getRequestNavigationKey('role-systems', this.props.params);
  }

  _isMenu(menu = 'role') {
    // TODO: better alghoritm
    return this.props.location.pathname.lastIndexOf(`/${menu}`, 0) === 0;
  }

  _isSystemMenu() {
    return this._isMenu('system');
  }

  showDetail(entity, add) {
    let entityId = null;
    if (!add) {
      entityId = this._isSystemMenu() ? entity._embedded.roleSystem.system : entity._embedded.roleSystem.role;
    } else {
      entityId = this.props.params.entityId;
    }
    const roleSystem = this.props.params.roleSystemId;
    const linkMenu = this._isSystemMenu() ? `system/${entityId}/roles/${roleSystem}/attributes` : `role/${entityId}/systems/${roleSystem}/attributes`;
    //
    if (add) {
      // When we add new object class, then we need id of role as parametr and use "new" url
      const uuidId = uuid.v1();
      this.context.router.push(`${this.addRequestPrefix(linkMenu, this.props.params)}/${uuidId}/new?new=1&mappingId=${entity.systemMapping}`);
    } else {
      this.context.router.push(`${this.addRequestPrefix(linkMenu, this.props.params)}/${entity.id}/detail`);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { roleSystemId } = nextProps.params;
    if (roleSystemId && roleSystemId !== this.props.params.roleSystemId) {
      this._initComponent(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    super.componentDidMount();
    //
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    // Init managers - evaluates if we want to use standard (original) manager or
    // universal request manager (depends on existing of 'requestId' param)
    roleSystemManager = this.getRequestManager(props.params, new RoleSystemManager());
    roleSystemAttributeManager = this.getRequestManager(props.params, new RoleSystemAttributeManager());
    roleManager = this.getRequestManager(props.params, new Managers.RoleManager());

    if (!this._getIsNew(props)) {
      const { roleSystemId } = props.params;
      this.context.store.dispatch(roleSystemManager.fetchEntity(roleSystemId));
    } else {
      if (this._isSystemMenu()) {
        if (this.refs.role) {
          this.refs.role.focus();
        }
      } else {
        if (this.refs.system) {
          this.refs.system.focus();
        }
      }
    }
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

    const formEntity = this.refs.form.getData();
    if (formEntity.id === undefined) {
      this.context.store.dispatch(roleSystemManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
        if (!error && this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    } else {
      this.context.store.dispatch(roleSystemManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
    }
  }

  afterSave(entity, error) {
    if (!error) {
      if (this._getIsNew()) {
        this.addMessage({ message: this.i18n('create.success', { system: entity._embedded.system.name, role: entity._embedded.role.name }) });
        if (this._isSystemMenu()) {
          this.context.router.replace(`/system/${entity.system}/roles/${entity.id}/detail`, { entityId: entity.id });
        } else {
          this.context.router.replace(`${this.addRequestPrefix('role', this.props.params)}/${entity.role}/systems/${entity.id}/detail`, { entityId: entity.id });
        }
      } else {
        this.addMessage({ message: this.i18n('save.success', { system: entity._embedded.system.name, role: entity._embedded.role.name }) });
      }
    } else {
      this.addError(error);
    }
    super.afterSave();
  }

  closeDetail() {
    this.refs.form.processEnded();
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    return (query) ? query.new : null;
  }

  /**
   * Set filter to dependant select box
   *
   * @param  {System} system selected system
   */
  onChangeSystem(system) {
    const systemId = system ? system.id : null;
    this.setState({
      systemId
    }, () => {
      // clear selected systemMapping
      this.refs.systemMapping.setValue(null);
    });
  }

  render() {
    const { _showLoading, _roleSystem } = this.props;
    const { entityId } = this.props.params;
    const { systemId } = this.state;
    //
    if (!roleSystemManager || !roleManager) {
      return null;
    }
    const forceSearchParameters = new Domain.SearchParameters().setFilter('roleSystemId', _roleSystem && _roleSystem.id ? _roleSystem.id : Domain.SearchParameters.BLANK_UUID);
    const isNew = this._getIsNew();
    const roleSystem = isNew ? this.state.roleSystem : _roleSystem;
    const forceSearchMappings = new Domain.SearchParameters()
      .setFilter('operationType', SystemOperationTypeEnum.findKeyBySymbol(SystemOperationTypeEnum.PROVISIONING))
      .setFilter('systemId', systemId || Domain.SearchParameters.BLANK_UUID);
    let linkMenu = this._isSystemMenu() ? `/system/${roleSystem.system}/roles/${roleSystem ? roleSystem.id : ''}/attributes` : `/role/${roleSystem.role}/systems/${roleSystem ? roleSystem.id : ''}/attributes`;
    linkMenu = this.addRequestPrefix(linkMenu, this.props.params);
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <Basic.Icon value="link"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <form onSubmit={this.save.bind(this)}>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm ref="form" data={ roleSystem } readOnly={!roleManager.canSave()} showLoading={ _showLoading } style={{ padding: 0 }}>
              <Advanced.RoleSelect
                ref="role"
                manager={ roleManager }
                label={this.i18n('acc:entity.RoleSystem.role')}
                readOnly={ !isNew || !this._isSystemMenu() }
                required/>
              <Basic.SelectBox
                ref="system"
                manager={ systemManager }
                label={ this.i18n('acc:entity.RoleSystem.system') }
                readOnly={ !isNew || this._isSystemMenu() }
                required
                onChange={ this.onChangeSystem.bind(this) }/>
              <Basic.SelectBox
                ref="systemMapping"
                manager={ systemMappingManager }
                forceSearchParameters={ forceSearchMappings }
                label={ this.i18n('acc:entity.RoleSystem.systemMapping') }
                placeholder={ systemId ? null : this.i18n('systemMapping.systemPlaceholder') }
                readOnly={!isNew || !systemId}
                required
                useFirst/>
              <Basic.Checkbox
                ref="forwardAccountManagemen"
                label={this.i18n('acc:entity.RoleSystem.forwardAccountManagemen.label')}
                helpBlock={this.i18n('acc:entity.RoleSystem.forwardAccountManagemen.help')}/>
            </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={_showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
              <Basic.Button
                onClick={this.save.bind(this)}
                level="success"
                type="submit"
                rendered={roleManager.canSave()}
                showLoading={_showLoading}>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
        <Basic.ContentHeader rendered={roleSystem && !isNew} style={{ marginBottom: 0 }}>
          <Basic.Icon value="list-alt"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('roleSystemAttributesHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={ roleSystem && !isNew } className="no-border last">
          <Advanced.Table
            ref="table"
            uiKey={ `${uiKeyAttributes}-${entityId}` }
            manager={ roleSystemAttributeManager }
            forceSearchParameters={ forceSearchParameters }
            showRowSelection={ roleManager.canSave() }
            className="no-margin"
            actions={
              roleManager.canSave()
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, roleSystem, true)}
                  rendered={roleManager.canSave()}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }>
            <Advanced.Column
              property=""
              header=""
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
              <Advanced.ColumnLink
                to={`${linkMenu}/:id/detail`}
                property="name"
                header={this.i18n('acc:entity.RoleSystemAttribute.name.label')}
                sort />
              <Advanced.Column property="idmPropertyName" header={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyName.label')} sort/>
              <Advanced.Column property="uid" face="boolean" header={this.i18n('acc:entity.SystemAttributeMapping.uid.label')} sort/>
              <Advanced.Column property="entityAttribute" face="boolean" header={this.i18n('acc:entity.SystemAttributeMapping.entityAttribute')} sort/>
              <Advanced.Column property="extendedAttribute" face="boolean" header={this.i18n('acc:entity.SystemAttributeMapping.extendedAttribute.label')} sort/>
              <Advanced.Column property="disabledDefaultAttribute" face="boolean" header={this.i18n('acc:entity.RoleSystemAttribute.disabledDefaultAttribute')} sort/>
              <Advanced.Column property="transformScript" face="boolean" header={this.i18n('acc:entity.RoleSystemAttribute.transformScriptTable')}/>
            </Advanced.Table>
          </Basic.Panel>
        </div>
    );
  }
}

RoleSystemDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
RoleSystemDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  if (!roleSystemManager) {
    return {};
  }
  const entity = Utils.Entity.getEntity(state, roleSystemManager.getEntityType(), component.params.roleSystemId);
  return {
    _roleSystem: entity || {},
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(RoleSystemDetail);
