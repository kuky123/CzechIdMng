import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Domain, Managers, Utils } from 'czechidm-core';
import { TreeAccountManager, AccountManager } from '../../redux';
import AccountTypeEnum from '../../domain/AccountTypeEnum';

const uiKey = 'tree-accounts-table';
const manager = new TreeAccountManager();
const accountManager = new AccountManager();
const treeManager = new Managers.TreeNodeManager();

/**
 * Tree accounts
 *
 * @author Kučera
 */
class TreeAccounts extends Advanced.AbstractTableContent {

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
    return 'acc:content.role.accounts';
  }

  getNavigationKey() {
    return 'tree-accounts';
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
      const treeNode = Utils.Entity.getEntity(state, treeManager.getEntityType(), formEntity.identity);
      formEntity.treeNode = treeNode.id;
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

  render() {
    const { entityId } = this.props.params;
    const { _showLoading, _permissions } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('tree-nodes', entityId);

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <Basic.Panel className="no-border last">
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={this.getManager()}
            forceSearchParameters={forceSearchParameters}
            showRowSelection={Managers.SecurityManager.hasAnyAuthority(['TREEACCOUNT_DELETE'])}
            rowClass={({rowIndex, data}) => { return (data[rowIndex]._embedded.account.inProtection) ? 'disabled' : ''; }}
            actions={
              Managers.SecurityManager.hasAnyAuthority(['TREEACCOUNT_DELETE'])
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
                  rendered={Managers.SecurityManager.hasAnyAuthority(['TREEACCOUNT_CREATE'])}>
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
                      rendered={Managers.SecurityManager.hasAnyAuthority(['TREEACCOUNT_READ'])}
                      onClick={this.showDetail.bind(this, data[rowIndex])}/>
                  );
                }
              }/>
            <Advanced.Column rendered={false} property="_embedded.account.accountType" width="75px" header={this.i18n('acc:entity.Account.accountType')} sort face="enum" enumClass={AccountTypeEnum} />
            <Advanced.Column property="_embedded.account.uid" header={this.i18n('acc:entity.Account.uid')} sort face="text" />
            <Advanced.ColumnLink
              to="/system/:_target/detail"
              target="_embedded.account._embedded.system.id"
              access={{ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SYSTEM_READ']}}
              property="_embedded.account._embedded.system.name"
              header={this.i18n('acc:entity.System.name')} />
            <Advanced.Column property="_embedded.identityRole._embedded.role.name" header={this.i18n('acc:entity.IdentityAccount.role')} face="text" />
            <Advanced.Column property="ownership" width="75px" header={this.i18n('acc:entity.IdentityAccount.ownership')} sort face="bool" />
            <Advanced.Column
              property="_embedded.account.inProtection"
              header={this.i18n('acc:entity.Account.inProtection')}
              face="boolean" />
            <Advanced.Column
              property="_embedded.account.endOfProtection"
              header={this.i18n('acc:entity.Account.endOfProtection')}
              face="datetime" />
          </Advanced.Table>
        </Basic.Panel>

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
                  manager={accountManager}
                  label={this.i18n('acc:entity.Account._type')}
                  readOnly={!Utils.Entity.isNew(detail.entity)}
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
                showLoading={ _showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ manager.canSave(detail.entity, _permissions) }>
                { this.i18n('button.save') }
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

TreeAccounts.propTypes = {
  _showLoading: PropTypes.bool,
};
TreeAccounts.defaultProps = {
  _showLoading: false,
};

function select(state) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _permissions: Utils.Permission.getPermissions(state, `${uiKey}-detail`)
  };
}

export default connect(select)(TreeAccounts);