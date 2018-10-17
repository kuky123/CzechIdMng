import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import _ from 'lodash';
//
import { Basic, Advanced, Utils } from 'czechidm-core';
import { SystemInfo } from '../../components/SystemInfo/SystemInfo.js';
import SearchParameters from 'czechidm-core/src/domain/SearchParameters';
import { DataManager, ConfigurationManager, RoleManager } from 'czechidm-core/src/redux';
import { SystemMappingManager, SystemManager, RoleSystemManager } from '../../redux';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';

const originalManager = new RoleSystemManager();
let manager = null;
let roleManager = null;
const systemManager = new SystemManager();
/**
 * Table component to display roles, assigned to system
 *
 * @author Petr Hanák
 * @author Radek Tomiška
 */
export class RoleSystemTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened
    };
    this.dataManager = new DataManager();
    this.systemMappingManager = new SystemMappingManager();
    this.configurationManager = new ConfigurationManager();
  }

  getContentKey() {
    return 'acc:content.role.systems';
  }

  getManager() {
    // Init manager - evaluates if we want to use standard (original) manager or
    // universal request manager (depends on existing of 'requestId' param)
    manager = this.getRequestManager(this.props.params, originalManager);
    roleManager = this.getRequestManager(this.props.params, new RoleManager());
    return manager;
  }

  getUiKey() {
    return this.props.uiKey;
  }

  /**
  * Redirect to role system detail
  */
  showDetail(entity) {
    const { menu } = this.props;
    //
    if (menu === 'system') {
      this.context.router.push(`/system/${entity.system}/roles/${entity.id}/detail`);
    } else {
      this.context.router.push(`${this.addRequestPrefix('role', this.props.params)}/${entity.role}/systems/${entity.id}/detail`);
    }
  }

  addRoleSystemConnection() {
    const { entityId } = this.props.params;
    const uuidId = uuid.v1();
    const { menu } = this.props;
    //
    if (menu === 'system') {
      this.context.router.push(`/system/${entityId}/roles/${uuidId}/new?new=1`);
    } else { // role detail as default
      this.context.router.push(`${this.addRequestPrefix('role', this.props.params)}/${entityId}/systems/${uuidId}/new?new=1`);
    }
  }

  getDefaultSearchParameters() {
    // TODO make this work!!!
    return this.getManager().getDefaultSearchParameters();
  }

  _getSystemMappingLink(roleSystem) {
    return (
      <Link to={`/system/${roleSystem._embedded.system.id}/mappings/${roleSystem._embedded.systemMapping.id}/detail`} >
        { this.systemMappingManager.getNiceLabel(roleSystem._embedded.systemMapping) }
      </Link>
    );
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
    this.setState({
      text: null,
      treeNodeId: null
    }, () => {
      this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
    });
  }

  systemInfoCard({ rowIndex, data }) {
    return (
      <SystemInfo
        entityIdentifier={ data[rowIndex]._embedded.system.id }
        entity={ data[rowIndex]._embedded.system }
        face="popover" />
    );
  }

  render() {
    const {
      uiKey,
      columns,
      forceSearchParameters,
      showAddButton,
      showDetailButton,
      rendered,
      filterOpened,
      showFilter,
      filterColumns,
      className
    } = this.props;
    //
    if (!rendered) {
      return null;
    }
    if (!manager) {
      return null;
    }
    //
    const _forceSearchParameters = forceSearchParameters || new SearchParameters();
    //
    return (
      <div>
      <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={ this.getManager() }
          showRowSelection={roleManager.canSave()}
          forceSearchParameters={_forceSearchParameters}
          filterOpened={ filterOpened }
          showFilter={ showFilter }
          filterColumns={ filterColumns }
          className={ className }
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
                type="submit"
                className="btn-xs"
                onClick={this.addRoleSystemConnection.bind(this, {})}
                rendered={showAddButton && roleManager.canSave()}
                icon="fa:plus">
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          filter={
            <Filter
              ref="filterForm"
              onSubmit={ this.useFilter.bind(this) }
              onCancel={ this.cancelFilter.bind(this) }
              filterColumns={ filterColumns }
              forceSearchParameters={ forceSearchParameters } />
          }
          _searchParameters={ this.getSearchParameters() }>
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
            sort={ false }
            rendered={ showDetailButton }/>
            <Advanced.Column
              property="_embedded.systemMapping.entityType"
              header={this.i18n('acc:entity.SystemEntity.entityType')}
              rendered={_.includes(columns, 'entityType')}
              face="enum"
              enumClass={SystemEntityTypeEnum}
              sort
              sortProperty="systemMapping.entityType" />
            <Advanced.Column
              property="_embedded.role.name"
              header={this.i18n('core:entity.Role._type')}
              rendered={_.includes(columns, 'role')}
              sort
              sortProperty="role.name"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.RoleInfo
                      entityIdentifier={ data[rowIndex]._embedded.role.id }
                      entity={ data[rowIndex]._embedded.role }
                      face="popover" />
                  );
                }
              }/>
            <Advanced.Column
              header={this.i18n('acc:entity.RoleSystem.system')}
              rendered={_.includes(columns, 'system')}
              sort
              sortProperty="system.name"
              cell={
                this.systemInfoCard.bind(this)
              } />
            <Advanced.Column
              property="systemMapping"
              header={this.i18n('acc:entity.RoleSystem.systemMapping')}
              rendered={_.includes(columns, 'mapping')}
              cell={
                ({ rowIndex, data }) => {
                  return this._getSystemMappingLink(data[rowIndex]);
                }
              }/>
        </Advanced.Table>
      </div>
    );
  }
}

RoleSystemTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  /**
   * Rendered columns - see table columns above
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Button for create user will be shown
   */
  showAddButton: PropTypes.bool,
  /**
   * Detail button will be shown
   */
  showDetailButton: PropTypes.bool,
  /**
   * Show filter
   */
  showFilter: PropTypes.bool,
  /**
   * Table supports delete identities
   */
  deleteEnabled: PropTypes.bool,
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.bool,
  /**
   * Rendered
   */
  rendered: PropTypes.bool,
  /**
   * Parent menu agenda - we can go from roles or systems ... role as defaultProps
   *
   * @type {[type]}
   */
  menu: PropTypes.string
};

RoleSystemTable.defaultProps = {
  columns: ['entityType', 'role', 'system', 'mapping'],
  filterOpened: false,
  showAddButton: true,
  showDetailButton: true,
  showFilter: true,
  showRowSelection: false,
  forceSearchParameters: null,
  rendered: true,
  menu: 'role'
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
  };
}

export default connect(select, null, null, { withRef: true })(RoleSystemTable);

/**
 * Table filter component
 *
 * @author Radek Tomiška
 */
class Filter extends Advanced.Filter {

  getComponentKey() {
    return 'acc:content.role.systems';
  }

  render() {
    const {
      onSubmit,
      onCancel,
      filterOpened,
      forceSearchParameters
    } = this.props;
    //
    return (
      <Advanced.Filter
        onSubmit={ onSubmit }
        filterOpened={ filterOpened }>
        <Basic.AbstractForm ref="filterForm">
          <Basic.Row className="last">
            <Basic.Col lg={ 4 } rendered={ !forceSearchParameters.getFilters().has('roleId') }>
              <Advanced.Filter.SelectBox
                ref="roleId"
                manager={ roleManager }
                placeholder={ this.i18n('filter.role.placeholder') }/>
            </Basic.Col>
            <Basic.Col lg={ 4 } rendered={ !forceSearchParameters.getFilters().has('systemId') }>
              <Advanced.Filter.SelectBox
                ref="systemId"
                manager={ systemManager }
                placeholder={ this.i18n('filter.system.placeholder') }/>
            </Basic.Col>
            <Basic.Col lg={ 8 } className="text-right">
              <Advanced.Filter.FilterButtons cancelFilter={ onCancel }/>
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
    );
  }
}
