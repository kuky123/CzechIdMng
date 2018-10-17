import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import SearchParameters from '../../domain/SearchParameters';
import { IdentityRoleManager, IdentityManager, RoleTreeNodeManager, RoleManager, IdentityContractManager } from '../../redux';

const manager = new IdentityRoleManager();
const identityManager = new IdentityManager();
const roleManager = new RoleManager();
const roleTreeNodeManager = new RoleTreeNodeManager();
const identityContractManager = new IdentityContractManager();

const TEST_ADD_ROLE_DIRECTLY = false;

/**
 * Table of assigned roles ~ identity roles
 * - table suppors add permission
 *
 * @author Radek Tomiška
 */
export class IdentityRoleTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.identity.roles';
  }

  getManager() {
    return manager;
  }

  /**
   * Can change identity permission
   *
   * @return {[type]} [description]
   */
  _canChangePermissions() {
    const { _permissions } = this.props;
    //
    return Utils.Permission.hasPermission(_permissions, 'CHANGEPERMISSION');
  }

  _changePermissions() {
    const { entityId } = this.props.params;
    const identity = identityManager.getEntity(this.context.store.getState(), entityId);
    //
    const uuidId = uuid.v1();
    this.context.router.push(`/role-requests/${uuidId}/new?new=1&applicantId=${identity.id}`);
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  showDetail(entity) {
    super.showDetail(entity, () => {
      this.refs.role.focus();
    });
  }

  render() {
    const { forceSearchParameters, showAddButton, showDetailButton, _showLoading, columns, className, rendered } = this.props;
    const { detail } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    return (
      <div>
        <Advanced.Table
          uiKey={ this.getUiKey() }
          manager={ manager }
          forceSearchParameters={ forceSearchParameters }
          showRefreshButton={ false }
          className={ className }
          showToolbar={ showAddButton }
          buttons={[
            <Basic.Button level="success" className="btn-xs" onClick={ this.showDetail.bind(this, {}) } rendered={ showAddButton && TEST_ADD_ROLE_DIRECTLY }>
              <Basic.Icon value="fa:plus"/>
              {' '}
              {this.i18n('button.add')}
            </Basic.Button>,
            <Basic.Button
              level="warning"
              onClick={ this._changePermissions.bind(this) }
              rendered={ showAddButton }
              disabled={ !this._canChangePermissions() }
              title={ this._canChangePermissions() ? null : this.i18n('security.access.denied') }
              titlePlacement="bottom">
              <Basic.Icon type="fa" icon="key"/>
              {' '}
              { this.i18n('changePermissions') }
            </Basic.Button>
          ]}
          _searchParameters={ this.getSearchParameters() }>
          <Advanced.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={ this.i18n('button.detail') }
                    onClick={ this.showDetail.bind(this, data[rowIndex]) }/>
                );
              }
            }
            rendered={ showDetailButton }/>
          <Advanced.Column
            header={this.i18n('entity.IdentityRole.role')}
            sort
            sortProperty="role.name"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data }) => {
                return (
                  <Advanced.EntityInfo
                    entityType="role"
                    entityIdentifier={ data[rowIndex].role }
                    entity={ data[rowIndex]._embedded.role }
                    face="popover" />
                );
              }
            }
            rendered={ _.includes(columns, 'role') }/>
          <Advanced.Column
            header={this.i18n('entity.IdentityRole.identityContract.title')}
            property="identityContract"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data, property }) => {
                return (
                  <Advanced.EntityInfo
                    entityType="identityContract"
                    entityIdentifier={ data[rowIndex][property] }
                    entity={ data[rowIndex]._embedded[property] }
                    showIdentity={ false }
                    face="popover" />
                );
              }
            }
            rendered={ _.includes(columns, 'identityContract') }/>
          <Advanced.Column
            header={this.i18n('entity.IdentityRole.contractPosition.label')}
            property="contractPosition"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data, property }) => {
                if (!data[rowIndex][property]) {
                  return null;
                }
                return (
                  <Advanced.EntityInfo
                    entityType="contractPosition"
                    entityIdentifier={ data[rowIndex][property] }
                    entity={ data[rowIndex]._embedded[property] }
                    showIdentity={ false }
                    face="popover" />
                );
              }
            }
            rendered={ _.includes(columns, 'contractPosition') }/>
          <Advanced.Column
            property="validFrom"
            header={this.i18n('label.validFrom')}
            face="date"
            sort
            rendered={ _.includes(columns, 'validFrom') }/>
          <Advanced.Column
            property="validTill"
            header={this.i18n('label.validTill')}
            face="date"
            sort
            rendered={ _.includes(columns, 'validTill') }/>
          <Advanced.Column
            property="directRole"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data, property }) => {
                if (!data[rowIndex][property]) {
                  return null;
                }
                return (
                  <Advanced.EntityInfo
                    entityType="identityRole"
                    entityIdentifier={ data[rowIndex][property] }
                    entity={ data[rowIndex]._embedded[property] }
                    showIdentity={ false }
                    face="popover" />
                );
              }
            }
            width={ 150 }
            rendered={ _.includes(columns, 'directRole') }/>

          <Advanced.Column
            property="automaticRole"
            face="bool"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data }) => {
                return (
                  <Basic.BooleanCell propertyValue={ data[rowIndex].automaticRole !== null } className="column-face-bool"/>
                );
              }
            }
            width={ 150 }
            rendered={ _.includes(columns, 'automaticRole') }/>
        </Advanced.Table>

        <Basic.Modal
          bsSize="large"
          show={ detail.show }
          onHide={ this.closeDetail.bind(this) }
          backdrop="static"
          keyboard={ !_showLoading }
          rendered={ showDetailButton }>

          <form onSubmit={ this.save.bind(this) }>
            <Basic.Modal.Header closeButton={ !_showLoading } text={ this.i18n('create.header') } rendered={ Utils.Entity.isNew(detail.entity) }/>
            <Basic.Modal.Header
              closeButton={ !_showLoading }
              text={this.i18n('edit.header', { role: detail.entity._embedded ? roleManager.getNiceLabel(detail.entity._embedded.role) : null })}
              rendered={ !Utils.Entity.isNew(detail.entity) }/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="form" showLoading={ _showLoading } readOnly={ !TEST_ADD_ROLE_DIRECTLY }>
                <Basic.SelectBox
                  ref="role"
                  manager={ roleManager }
                  label={ this.i18n('entity.IdentityRole.role') }
                  required/>
                <Basic.SelectBox
                  ref="identityContract"
                  manager={ identityContractManager }
                  label={ this.i18n('entity.IdentityRole.identityContract.label') }
                  helpBlock={ this.i18n('entity.IdentityRole.identityContract.help') }
                  readOnly={ !TEST_ADD_ROLE_DIRECTLY }
                  required/>
                <Basic.LabelWrapper
                  label={ this.i18n('entity.IdentityRole.automaticRole.label') }
                  helpBlock={ this.i18n('entity.IdentityRole.automaticRole.help') }
                  rendered={ detail.entity.automaticRole }>
                  { detail.entity.automaticRole ? roleTreeNodeManager.getNiceLabel(detail.entity._embedded.automaticRole) : null }
                </Basic.LabelWrapper>
                <Basic.Row>
                  <Basic.Col md={ 6 }>
                    <Basic.DateTimePicker
                      mode="date"
                      ref="validFrom"
                      label={this.i18n('label.validFrom')}/>
                  </Basic.Col>
                  <Basic.Col md={ 6 }>
                    <Basic.DateTimePicker
                      mode="date"
                      ref="validTill"
                      label={this.i18n('label.validTill')}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>

              {
                detail.entity.directRole !== null
                ||
                <div>
                  <Basic.ContentHeader style={{ marginBottom: 0 }} className="marginable">
                    <Basic.Icon value="arrow-down"/>
                    {' '}
                    { this.i18n('detail.directRole.subRoles.header') }
                  </Basic.ContentHeader>
                  <IdentityRoleTable
                    uiKey={ `${this.getUiKey()}-all-sub-roles` }
                    showAddButton={ false }
                    forceSearchParameters={ new SearchParameters().setFilter('directRoleId', detail.entity.id) }
                    showDetailButton={ false }
                    params={ this.props.params }
                    columns={ _.difference(IdentityRoleTable.defaultProps.columns, ['identityContract', 'directRole', 'automaticRole']) }
                    className="marginable"/>
                </div>
              }
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={ this.closeDetail.bind(this) }
                showLoading={ _showLoading }>
                { this.i18n('button.close') }
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}
                rendered={ TEST_ADD_ROLE_DIRECTLY }>
                { this.i18n('button.save') }
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

IdentityRoleTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  /**
   * Rendered columns - see table columns above
   *
   * TODO: move to advanced table and add column sorting
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Button for create user will be shown
   */
  showAddButton: PropTypes.bool,
  /**
   * Button for show entity detail
   */
  showDetailButton: PropTypes.bool,
  /**
   * Css
   */
  className: PropTypes.string,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ])
};

IdentityRoleTable.defaultProps = {
  rendered: true,
  columns: ['role', 'identityContract', 'contractPosition', 'validFrom', 'validTill', 'directRole', 'automaticRole'],
  forceSearchParameters: null,
  showAddButton: true,
  showDetailButton: true,
  _permissions: null
};

function select(state, component) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, component.uiKey),
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(IdentityRoleTable);
