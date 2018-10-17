import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { ContractGuaranteeManager, SecurityManager, IdentityManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';
import IdentityTable from '../IdentityTable';

const uiKey = 'identity-contract-guarantees-table';
const uiKeyManagers = 'contract-managers-table';
const manager = new ContractGuaranteeManager();
const identityManager = new IdentityManager();

/**
 * Identity contract's managers and guarantees
 *
 * @author Radek Tomiška
 */
class IdentityContractGuarantees extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    // Lookot: getNavigationKey cannot be used -> profile vs users main tab
    this.selectSidebarItem('identity-contract-guarantees');
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.identity-contract.guarantees';
  }

  showDetail(entity) {
    if (!Utils.Entity.isNew(entity)) {
      this.context.store.dispatch(this.getManager().fetchPermissions(entity.id, `${this.getUiKey()}-detail`));
    }

    super.showDetail(entity, () => {
      this.refs.guarantee.focus();
    });
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      const state = this.context.store.getState();
      const identity = identityManager.getEntity(state, entity.guarantee);
      this.addMessage({ message: this.i18n('action.save.success', { count: 1, record: identityManager.getNiceLabel(identity) }) });
    }
    //
    super.afterSave(entity, error);
  }

  render() {
    const { entityId, identityId, controlledBySlices } = this.props.params;
    const { _showLoading, _permissions } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new SearchParameters().setFilter('identityContractId', entityId);
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.Row>
          <Basic.Col lg={ 6 }>
            <Basic.Panel className="no-border last">
              <Basic.PanelHeader text={ this.i18n('guarantees.direct', { escape: false }) }/>
              <Advanced.Table
                ref="table"
                uiKey={ uiKey }
                manager={ manager }
                forceSearchParameters={ forceSearchParameters }
                showRowSelection={ SecurityManager.hasAnyAuthority(['CONTRACTGUARANTEE_DELETE']) }
                rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]._embedded.guarantee); }}
                className="no-margin"
                actions={
                  SecurityManager.hasAnyAuthority(['CONTRACTGUARANTEE_DELETE'])
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
                      onClick={this.showDetail.bind(this, { identityContract: entityId })}
                      rendered={ !controlledBySlices && SecurityManager.hasAnyAuthority(['CONTRACTGUARANTEE_CREATE']) }>
                      <Basic.Icon type="fa" icon="plus"/>
                      {' '}
                      { this.i18n('button.add') }
                    </Basic.Button>
                  ]
                }>
                <Advanced.Column
                  className="detail-button"
                  cell={
                    ({rowIndex, data}) => {
                      return (
                        <Advanced.DetailButton onClick={this.showDetail.bind(this, data[rowIndex])}/>
                      );
                    }
                  }/>
                <Advanced.Column
                  property="guarantee"
                  header={ this.i18n('entity.Identity._type') }
                  cell={
                    ({ rowIndex, data, property }) => {
                      return (
                        <Advanced.IdentityInfo entityId={ data[rowIndex][property] } entity={ data[rowIndex]._embedded[property] } face="popover"/>
                      );
                    }
                  }/>
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
                    showLoading={ _showLoading }
                    readOnly={ controlledBySlices || !manager.canSave(detail.entity, _permissions) }>
                    <Basic.SelectBox
                      ref="guarantee"
                      manager={ identityManager }
                      label={this.i18n('entity.ContractGuarantee.guarantee.label')}
                      helpBlock={this.i18n('entity.ContractGuarantee.guarantee.help')}
                      required/>
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
                    rendered={!controlledBySlices && manager.canSave(detail.entity, _permissions) }>
                    { this.i18n('button.save') }
                  </Basic.Button>
                </Basic.Modal.Footer>
              </form>
            </Basic.Modal>
          </Basic.Col>

          <Basic.Col lg={ 6 }>
            <Basic.ContentHeader text={ this.i18n('guarantees.byTree', { escape: false }) } style={{ marginBottom: 0 }}/>

            <IdentityTable
              ref="managers"
              uiKey={ uiKeyManagers }
              identityManager={ identityManager }
              forceSearchParameters={ new SearchParameters().setFilter('managersFor', identityId).setFilter('managersByContract', entityId).setFilter('includeGuarantees', false) }
              showAddButton={ false }
              showDetailButton={ false }
              showFilter={ false }
              className="no-margin"
              columns={ ['entityInfo'] }/>
          </Basic.Col>
        </Basic.Row>
      </div>
    );
  }
}

IdentityContractGuarantees.propTypes = {
  _showLoading: PropTypes.bool
};
IdentityContractGuarantees.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const { entityId } = component.params;

  return {
    entity: manager.getEntity(state, entityId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _permissions: Utils.Permission.getPermissions(state, `${uiKey}-detail`)
  };
}

export default connect(select)(IdentityContractGuarantees);
