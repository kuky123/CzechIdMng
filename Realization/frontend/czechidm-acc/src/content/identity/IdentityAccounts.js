import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Domain, Managers, Utils } from 'czechidm-core';
import { IdentityAccountManager, AccountManager } from '../../redux';
import AccountTypeEnum from '../../domain/AccountTypeEnum';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
import AccountTableComponent, { AccountTable } from '../account/AccountTable';

const uiKey = 'identity-accounts-table';
const manager = new IdentityAccountManager();
const accountManager = new AccountManager();
const identityManager = new Managers.IdentityManager();

/**
 * System accounts
 *
 * @author Radek Tomiška
 */
class IdentityAccountsContent extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.identity.accounts';
  }

  getNavigationKey() {
    return 'identity-accounts';
  }

  showDetail(entity) {
    if (!Utils.Entity.isNew(entity)) {
      this.context.store.dispatch(this.getManager().fetchPermissions(entity.id, `${this.getUiKey()}-detail`));
    }
    //
    const entityFormData = _.merge({}, entity, {
      identity: entity._embedded && entity._embedded.identity ? entity._embedded.identity.id : this.props.params.entityId,
      account: entity.account ? entity.account.id : null
    });
    //
    super.showDetail(entityFormData, () => {
      this.refs.account.focus();
    });
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    const state = this.context.store.getState();
    if (Utils.Entity.isNew(formEntity)) {
      const identity = Utils.Entity.getEntity(state, identityManager.getEntityType(), formEntity.identity);
      formEntity.identity = identity.id;
      formEntity.account = formEntity.account;
    }
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      this.addMessage({ message: this.i18n('save.success', { name: entity.account.uid }) });
    }
    //
    super.afterSave(entity, error);
  }

  _onChangeSelectTabs(activeTab) {
    if (activeTab === 2) {
      this.refs.table.getWrappedInstance().reload();
    } else if (activeTab === 1) {
      this.refs.accountTable.getWrappedInstance().reload();
    }
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoading, _permissions } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('identity', entityId);
    const accountSearchParameters = new Domain.SearchParameters().setFilter('entityType', SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY));
    const forceAccountSearchParameters = new Domain.SearchParameters().setFilter('identity', entityId).setFilter('entityType', SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY));
    const forceSystemEntitySearchParameters = new Domain.SearchParameters().setFilter('entityType', SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY));
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.Tabs style={{ paddingTop: 15 }} onSelect={this._onChangeSelectTabs.bind(this)}>
          <Basic.Tab eventKey={1} title={this.i18n('header')}>
            <AccountTableComponent
              ref="accountTable"
              uiKey="accounts-table"
              showLoading={_showLoading}
              forceSearchParameters={forceAccountSearchParameters}
              forceSystemEntitySearchParameters={forceSystemEntitySearchParameters}
              columns={ _.difference(AccountTable.defaultProps.columns, ['entityType', 'systemEntity']) }
              showAddButton={ false }/>
          </Basic.Tab>
          <Basic.Tab eventKey={2} title={this.i18n('identity-accounts')}>
            <Advanced.Table
              ref="table"
              uiKey={uiKey}
              manager={this.getManager()}
              forceSearchParameters={forceSearchParameters}
              showRowSelection={Managers.SecurityManager.hasAnyAuthority(['IDENTITYACCOUNT_DELETE'])}
              rowClass={({rowIndex, data}) => { return (data[rowIndex]._embedded.account.inProtection) ? 'disabled' : ''; }}
              actions={
                Managers.SecurityManager.hasAnyAuthority(['IDENTITYACCOUNT_DELETE'])
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
                    onClick={this.showDetail.bind(this, { type: AccountTypeEnum.findKeyBySymbol(AccountTypeEnum.PERSONAL) })}
                    rendered={Managers.SecurityManager.hasAnyAuthority(['IDENTITYACCOUNT_CREATE'])}>
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
                rendered={ Managers.SecurityManager.hasAuthority('ACCOUNT_READ') }
                cell={
                  ({ rowIndex, data }) => {
                    return (
                      <Advanced.DetailButton
                        title={this.i18n('button.detail')}
                        rendered={Managers.SecurityManager.hasAnyAuthority(['IDENTITYACCOUNT_READ'])}
                        onClick={this.showDetail.bind(this, data[rowIndex])}/>
                    );
                  }
                }/>
              <Advanced.Column property="_embedded.account.uid" header={this.i18n('acc:entity.Account.uid')} sort face="text" />
              <Advanced.Column
                header={this.i18n('acc:entity.System.name')}
                cell={
                  /* eslint-disable react/no-multi-comp */
                  ({rowIndex, data}) => {
                    return (
                      <Advanced.EntityInfo
                        entityType="system"
                        entityIdentifier={ data[rowIndex]._embedded.account.system }
                        entity={ data[rowIndex]._embedded.account._embedded ? data[rowIndex]._embedded.account._embedded.system : null }
                        face="popover" />
                    );
                  }
                }/>
              <Advanced.Column
                header={this.i18n('acc:entity.IdentityAccount.role')}
                cell={
                  /* eslint-disable react/no-multi-comp */
                  ({rowIndex, data}) => {
                    if (!data[rowIndex]._embedded.identityRole) {
                      return null;
                    }
                    return (
                      <Advanced.EntityInfo
                        entityType="role"
                        entityIdentifier={ data[rowIndex]._embedded.identityRole.role }
                        entity={ data[rowIndex]._embedded.identityRole._embedded.role}
                        face="popover" />
                    );
                  }
                } />
              <Advanced.Column property="ownership" width="75px" header={this.i18n('acc:entity.IdentityAccount.ownership')} sort face="bool" />
            </Advanced.Table>

            <Basic.Modal
              bsSize="large"
              show={detail.show}
              onHide={this.closeDetail.bind(this)}
              backdrop="static"
              keyboard={!_showLoading}>

              <form onSubmit={this.save.bind(this, {})}>
                <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('create.header')} rendered={Utils.Entity.isNew(detail.entity)}/>
                <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('edit.header', { name: detail.entity.name })} rendered={!Utils.Entity.isNew(detail.entity)}/>
                <Basic.Modal.Body>
                  <Basic.AbstractForm
                    ref="form"
                    showLoading={_showLoading}
                    readOnly={ !manager.canSave(detail.entity, _permissions) }>
                    <Basic.SelectBox
                      ref="account"
                      manager={ accountManager }
                      label={ this.i18n('acc:entity.Account._type') }
                      readOnly={ !Utils.Entity.isNew(detail.entity) }
                      forceSearchParameters={ accountSearchParameters }
                      required/>
                    <Basic.Checkbox
                      ref="ownership"
                      label={this.i18n('acc:entity.IdentityAccount.ownership')}/>
                  </Basic.AbstractForm>
                </Basic.Modal.Body>

                <Basic.Modal.Footer>
                  <Basic.Button
                    level="link"
                    onClick={this.closeDetail.bind(this)}
                    showLoading={_showLoading}>
                    {this.i18n('button.close')}
                  </Basic.Button>
                  <Basic.Button
                    type="submit"
                    level="success"
                    showLoading={_showLoading}
                    showLoadingIcon
                    showLoadingText={this.i18n('button.saving')}
                    rendered={ manager.canSave(detail.entity, _permissions) }>
                    {this.i18n('button.save')}
                  </Basic.Button>
                </Basic.Modal.Footer>
              </form>
            </Basic.Modal>
          </Basic.Tab>
        </Basic.Tabs>
      </div>
    );
  }
}

IdentityAccountsContent.propTypes = {
  _showLoading: PropTypes.bool,
};
IdentityAccountsContent.defaultProps = {
  _showLoading: false,
};

function select(state) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _permissions: Utils.Permission.getPermissions(state, `${uiKey}-detail`)
  };
}

export default connect(select)(IdentityAccountsContent);
