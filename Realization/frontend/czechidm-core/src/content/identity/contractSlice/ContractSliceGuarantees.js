import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { ContractSliceGuaranteeManager, SecurityManager, IdentityManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';

const uiKey = 'contract-slice-guarantees-table';
const manager = new ContractSliceGuaranteeManager();
const identityManager = new IdentityManager();

/**
 * Identity contract's managers and guarantees
 *
 * @author Radek Tomiška
 */
class ContractSliceGuarantees extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    this.selectSidebarItem('contract-slice-guarantees');
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.contract-slice.guarantees';
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
    const { entityId } = this.props.params;
    const { _showLoading, _permissions } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new SearchParameters().setFilter('contractSliceId', entityId);
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Panel className="no-border last">
          <Basic.PanelHeader text={ this.i18n('guarantees.direct', { escape: false }) }/>
          <Advanced.Table
            ref="table"
            uiKey={ uiKey }
            manager={ manager }
            forceSearchParameters={ forceSearchParameters }
            showRowSelection={ SecurityManager.hasAnyAuthority(['CONTRACTSLICEGUARANTEE_DELETE']) }
            actions={
              SecurityManager.hasAnyAuthority(['CONTRACTSLICEGUARANTEE_DELETE'])
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
                  onClick={this.showDetail.bind(this, { contractSlice: entityId })}
                  rendered={ SecurityManager.hasAnyAuthority(['CONTRACTSLICEGUARANTEE_CREATE']) }>
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
                readOnly={ !manager.canSave(detail.entity, _permissions) }>
                <Basic.SelectBox
                  ref="guarantee"
                  manager={ identityManager }
                  label={this.i18n('entity.ContractSliceGuarantee.guarantee.label')}
                  helpBlock={this.i18n('entity.ContractSliceGuarantee.guarantee.help')}
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

ContractSliceGuarantees.propTypes = {
  _showLoading: PropTypes.bool
};
ContractSliceGuarantees.defaultProps = {
  _showLoading: false,
};

function select(state) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _permissions: Utils.Permission.getPermissions(state, `${uiKey}-detail`)
  };
}

export default connect(select)(ContractSliceGuarantees);
