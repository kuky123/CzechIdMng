import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import classnames from 'classnames';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import RoleTypeEnum from '../../enums/RoleTypeEnum';
//
import { RoleManager, RequestManager, SecurityManager, RoleCatalogueManager, ConfigurationManager } from '../../redux';

// Table uiKey
const requestManager = new RequestManager();

/**
* Table of roles
*
* @author Radek Tomiška
*/
class RoleTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened
    };
    this.roleCatalogueManager = new RoleCatalogueManager();
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.refs.text.focus();
  }

  getContentKey() {
    return 'content.roles';
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

  _validateCreateRequestDialog(result) {
    if (result === 'reject') {
      return true;
    }
    if (result === 'confirm' && this.refs['new-request-form'].isFormValid()) {
      return true;
    }
    return false;
  }

  _focusOnRequestDialog() {
    this.refs['role-name'].focus();
  }

  createRequest(event) {
    if (event && event.preventDefault) {
      event.preventDefault();
    }

    this.refs[`confirm-new-request`].show(
      null,
      this.i18n(`content.roles.action.createRequest.header`),
      this._validateCreateRequestDialog.bind(this),
      this._focusOnRequestDialog.bind(this)
    ).then(() => {
      const roleName = this.refs[`role-name`].getValue();
      const promise = requestManager.getService().createRequest('roles', {name: roleName, code: roleName});
      promise.then((json) => {
        // Init universal request manager (manually)
        const manager = this.getRequestManager({requestId: json.id}, new RoleManager());
        // Fetch entity - we need init permissions for new manager
        this.context.store.dispatch(manager.fetchEntityIfNeeded(json.ownerId, null, (e, error) => {
          this.handleError(error);
        }));
        // Redirect to new request
        this.context.router.push(`${this.addRequestPrefix('role', {requestId: json.id})}/${json.ownerId}/detail`);
      }).catch(ex => {
        this.setState({
          showLoading: false
        });
        this.handleError(ex);
      });
    });
  }

  showDetail(entity) {
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/role/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`/role/${entity.id}/detail`);
    }
  }

  onDelete(bulkActionValue, selectedRows) {
    const { roleManager, uiKey } = this.props;
    const selectedEntities = roleManager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: roleManager.getNiceLabel(selectedEntities[0]), records: roleManager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: roleManager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(roleManager.deleteEntities(selectedEntities, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          // redirect to role detail with identities table
          if (error.statusEnum === 'ROLE_DELETE_FAILED_IDENTITY_ASSIGNED') {
            this.context.router.push(`/role/${entity.id}/identities`);
            this.addMessage({
              position: 'tc',
              level: 'info',
              title: this.i18n('delete.identityAssigned.title'),
              message: this.i18n('delete.identityAssigned.message', { role: roleManager.getNiceLabel(entity) })
            });
          } else {
            this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: roleManager.getNiceLabel(entity) }) }, error);
          }
        }
        if (!error && successEntities) {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      // nothing
    });
  }

  _useFilterByTree(nodeId, event) {
    if (event) {
      event.preventDefault();
      // Stop propagation is important for prohibition of node tree expand.
      // After click on link node, we want only filtering ... not node expand.
      event.stopPropagation();
    }
    const data = {
      ... this.refs.filterForm.getData(),
      roleCatalogue: nodeId
    };
    this.refs.roleCatalogue.setValue(nodeId);
    this.refs.table.getWrappedInstance().useFilterData(data);
  }

  render() {
    const { uiKey, roleManager, columns, showCatalogue, forceSearchParameters, _requestsEnabled, className } = this.props;
    const { filterOpened, showLoading } = this.state;
    const _showTree = showCatalogue && SecurityManager.hasAuthority('ROLECATALOGUE_AUTOCOMPLETE');
    //
    return (
      <Basic.Row>
        <Basic.Confirm ref="confirm-new-request" level="success">
          <Basic.AbstractForm ref="new-request-form" uiKey="confirm-new-request" >
            <Basic.TextField
              label={this.i18n('content.roles.action.createRequest.name')}
              ref="role-name"
              placeholder={this.i18n('content.roles.action.createRequest.message')}
              required/>
          </Basic.AbstractForm>
        </Basic.Confirm>

        {/* FIXME: resposive design - wrong wrapping on mobile */}
        <Basic.Col
          lg={ 3 }
          style={{ paddingRight: 0, marginLeft: 0, marginRight: -15 }}
          rendered={ _showTree }>
          <Advanced.Tree
            ref="roleCatalogueTree"
            uiKey="role-catalogue-tree"
            manager={ this.roleCatalogueManager }
            onSelect={ this._useFilterByTree.bind(this) }
            header={ this.i18n('content.roles.roleCataloguePick') }
            rendered={ _showTree }/>
        </Basic.Col>

        <Basic.Col lg={ !_showTree ? 12 : 9 }>
          <Basic.Confirm ref="confirm-delete" level="danger"/>

          <Advanced.Table
            ref="table"
            uiKey={ uiKey }
            manager={ roleManager }
            rowClass={ ({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); } }
            filterOpened={ filterOpened }
            forceSearchParameters={ forceSearchParameters }
            showRowSelection={ SecurityManager.hasAuthority('ROLE_DELETE') }
            style={ !_showTree ? {} : { borderLeft: '1px solid #ddd' } }
            showLoading={ showLoading }
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className={ _showTree ? '' : 'last'}>
                    <Basic.Col lg={ 8 }>
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('content.roles.filter.text.placeholder')}
                        help={ Advanced.Filter.getTextHelp() }/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 } rendered={ false }>
                      <Advanced.Filter.EnumSelectBox
                        ref="roleType"
                        placeholder={this.i18n('entity.Role.roleType')}
                        enum={RoleTypeEnum}/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 } className="text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </Basic.Col>
                  </Basic.Row>
                  <Basic.Row className={ classnames('last', { 'hidden': !_showTree })}>
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.RoleCatalogueSelect
                        ref="roleCatalogue"
                        label={ null }
                        placeholder={ this.i18n('entity.Role.roleCatalogue.name') }
                        header={ this.i18n('entity.Role.roleCatalogue.name') }/>
                    </Basic.Col>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            buttons={[
              <span>
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { })}
                  rendered={!_requestsEnabled && SecurityManager.hasAuthority('ROLE_CREATE')}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
                <Basic.Button
                  level="success"
                  key="add_request"
                  className="btn-xs"
                  onClick={this.createRequest.bind(this)}
                  rendered={_requestsEnabled && SecurityManager.hasAuthority('ROLE_CREATE')}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              </span>
            ]}
            _searchParameters={ this.getSearchParameters() }
            className={ className }>

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
            <Advanced.ColumnLink to="role/:id/detail" property="name" width="15%" sort face="text" rendered={_.includes(columns, 'name')}/>
            <Advanced.Column property="roleType" width="75px" sort face="enum" enumClass={RoleTypeEnum} rendered={false && _.includes(columns, 'roleType')}/>
            <Advanced.Column property="roleCatalogue.name" width="75px" face="text" rendered={_.includes(columns, 'roleCatalogue')}/>
            <Advanced.Column property="description" sort face="text" rendered={_.includes(columns, 'description')}/>
            <Advanced.Column property="disabled" sort face="bool" width="75px" rendered={_.includes(columns, 'disabled')}/>
          </Advanced.Table>
        </Basic.Col>
      </Basic.Row>
    );
  }
}

RoleTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  roleManager: PropTypes.object.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  /**
   * If role catalogue is shown
   */
  showCatalogue: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Css
   */
  className: PropTypes.string
};

RoleTable.defaultProps = {
  columns: ['name', 'roleType', 'disabled', 'approvable', 'description'],
  filterOpened: true,
  showCatalogue: true,
  forceSearchParameters: null
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _requestsEnabled: ConfigurationManager.getPublicValueAsBoolean(state, component.roleManager.getEnabledPropertyKey())
  };
}

export default connect(select, null, null, { withRef: true })(RoleTable);
