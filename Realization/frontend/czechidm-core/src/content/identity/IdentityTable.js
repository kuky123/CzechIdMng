import React, { PropTypes } from 'react';
import uuid from 'uuid';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { SearchParameters } from '../../domain';
import { DataManager, TreeNodeManager, SecurityManager, ConfigurationManager, RoleManager } from '../../redux';
import IdentityStateEnum from '../../enums/IdentityStateEnum';

/**
 * Table of users
 *
 * @author Radek Tomiška
 */
export class IdentityTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened
    };
    this.dataManager = new DataManager();
    this.treeNodeManager = new TreeNodeManager();
    this.roleManager = new RoleManager();
  }

  componentDidMount() {
    super.componentDidMount();
    //
    if (this.refs.text) {
      this.refs.text.focus();
    }
  }

  getContentKey() {
    return 'content.identities';
  }

  getManager() {
    return this.props.identityManager;
  }

  getUiKey() {
    return this.props.uiKey;
  }

  setTreeNodeId(treeNodeId, cb) {
    this.refs.treeNodeId.setValue(treeNodeId, cb);
  }

  /**
   * Filter identities by given tree node id
   *
   * @param  {string} treeNodeId
   */
  filterByTreeNodeId(treeNodeId) {
    this.setTreeNodeId(treeNodeId, () => {
      this.useFilter();
    });
  }

  /**
  * Redirect to user form
  */
  showDetail(entity) {
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/identity/new?id=${uuidId}`);
    } else {
      this.context.router.push(`/identity/${encodeURIComponent(entity.username)}/profile`);
    }
  }

  getDefaultSearchParameters() {
    return this.getManager().getDefaultSearchParameters().setFilter('disabled', 'false').setFilter('recursively', 'true');
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

  onActivate(bulkActionValue, usernames) {
    const { identityManager } = this.props;
    const selectedEntities = this.getManager().getEntitiesByIds(this.context.store.getState(), usernames);
    //
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, username: this.getManager().getNiceLabel(selectedEntities[0]) }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length})
    ).then(() => {
      this.context.store.dispatch(identityManager.setUsersActivity(selectedEntities, bulkActionValue));
    }, () => {
      // nothing
    });
  }

  render() {
    const {
      uiKey,
      identityManager,
      columns,
      forceSearchParameters,
      showAddButton,
      showDetailButton,
      showFilter,
      deleteEnabled,
      showRowSelection,
      rendered,
      treeType
    } = this.props;
    const { filterOpened } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    let _forceSearchParameters = forceSearchParameters || new SearchParameters();
    let forceTreeNodeSearchParams = new SearchParameters();
    if (!treeType) {
      forceTreeNodeSearchParams = forceTreeNodeSearchParams.setFilter('defaultTreeType', true);
    } else {
      forceTreeNodeSearchParams = forceTreeNodeSearchParams.setFilter('treeTypeId', treeType.id);
      _forceSearchParameters = _forceSearchParameters.setFilter('treeTypeId', treeType.id);
    }
    //
    const roleDisabled = _forceSearchParameters.getFilters().has('role');
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-deactivate" level="danger"/>
        <Basic.Confirm ref="confirm-activate"/>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={identityManager}
          showRowSelection={showRowSelection && (SecurityManager.hasAuthority('IDENTITY_UPDATE') || SecurityManager.hasAuthority('IDENTITY_DELETE'))}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.name.placeholder')}
                      help={ Advanced.Filter.getTextHelp() }/>
                  </Basic.Col>
                  <Basic.Col lg={ 3 }>
                    <Advanced.Filter.SelectBox
                      ref="role"
                      placeholder={ this.i18n('filter.role.placeholder') }
                      manager={ this.roleManager }
                      rendered={ !roleDisabled }/>
                  </Basic.Col>
                  <Basic.Col lg={ 3 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.SelectBox
                      ref="treeNodeId"
                      placeholder={ this.i18n('filter.organization.placeholder') }
                      forceSearchParameters={ forceTreeNodeSearchParams }
                      manager={ this.treeNodeManager }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.BooleanSelectBox
                      ref="recursively"
                      placeholder={ this.i18n('filter.recursively.placeholder') }
                      options={ [
                        { value: 'true', niceLabel: this.i18n('filter.recursively.yes') },
                        { value: 'false', niceLabel: this.i18n('filter.recursively.no') }
                      ]}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.BooleanSelectBox
                      ref="disabled"
                      placeholder={ this.i18n('filter.disabled.placeholder') }
                      options={ [
                        { value: 'true', niceLabel: this.i18n('label.disabled') },
                        { value: 'false', niceLabel: this.i18n('label.enabled') }
                      ]}/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="state"
                      placeholder={ this.i18n('entity.Identity.state.help') }
                      enum={ IdentityStateEnum }/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={ filterOpened }
          showFilter={ showFilter }
          forceSearchParameters={_forceSearchParameters}
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), rendered: SecurityManager.hasAuthority('IDENTITY_DELETE') || !deleteEnabled },
              { value: 'activate', niceLabel: this.i18n('action.activate.action'), action: this.onActivate.bind(this), rendered: SecurityManager.hasAuthority('IDENTITY_ADMIN') },
              { value: 'deactivate', niceLabel: this.i18n('action.deactivate.action'), action: this.onActivate.bind(this), rendered: SecurityManager.hasAuthority('IDENTITY_ADMIN') }
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                type="submit"
                className="btn-xs"
                onClick={this.showDetail.bind(this, {})}
                rendered={showAddButton && SecurityManager.hasAuthority('IDENTITY_CREATE')}
                icon="fa:user-plus">
                {this.i18n('content.identity.create.button.add')}
              </Basic.Button>
            ]
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
            header={ this.i18n('entity.Identity._type') }
            cell={
              ({ rowIndex, data }) => {
                // TODO: generalize to advanced table - column position?
                return (
                  <Advanced.IdentityInfo entityId={ data[rowIndex].id } entity={ data[rowIndex] } face="popover"/>
                );
              }
            }
            rendered={ _.includes(columns, 'entityInfo') }/>
          <Advanced.Column property="_links.self.href" face="text" rendered={ false }/>
          <Advanced.ColumnLink to="identity/:username/profile" property="username" width="20%" sort face="text" rendered={ _.includes(columns, 'username') }/>
          <Advanced.Column property="lastName" sort face="text" rendered={ _.includes(columns, 'lastName') }/>
          <Advanced.Column property="firstName" width="10%" face="text" rendered={ _.includes(columns, 'firstName') }/>
          <Advanced.Column property="externalCode" width="10%" face="text" rendered={ _.includes(columns, 'externalCode') }/>
          <Advanced.Column property="email" width="15%" face="text" sort rendered={_ .includes(columns, 'email') }/>
          <Advanced.Column property="disabled" face="bool" sort width="100px" rendered={ _.includes(columns, 'disabled') }/>
          <Advanced.Column property="state" face="enum" enumClass={ IdentityStateEnum } sort width="100px" rendered={ _.includes(columns, 'state') }/>
          <Advanced.Column property="description" face="text" rendered={ _.includes(columns, 'description') } maxLength={ 30 }/>
        </Advanced.Table>
      </div>
    );
  }
}

IdentityTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  identityManager: PropTypes.object.isRequired,
  /**
   * Rendered columns - see table columns above
   *
   * TODO: move to advanced table and add column sorting
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
   * Filter tree type structure - given id ur default - false
   * @deprecated Remove after better tree type - node filter component
   */
  treeType: PropTypes.oneOfType([PropTypes.bool, PropTypes.string]),
};

IdentityTable.defaultProps = {
  columns: ['username', 'lastName', 'firstName', 'externalCode', 'email', 'state', 'description'],
  filterOpened: false,
  showAddButton: true,
  showDetailButton: true,
  showFilter: true,
  deleteEnabled: false,
  showRowSelection: false,
  forceSearchParameters: null,
  rendered: true,
  treeType: false
};

function select(state, component) {
  return {
    _searchParameters: state.data.ui[component.uiKey] ? state.data.ui[component.uiKey].searchParameters : null,
    deleteEnabled: ConfigurationManager.getPublicValueAsBoolean(state, 'idm.pub.core.identity.delete')
  };
}

export default connect(select, null, null, { withRef: true })(IdentityTable);
