import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { ContractPositionManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';

const uiKey = 'identity-contract-positions-table';
const manager = new ContractPositionManager();

/**
 * Identity contract's other positions
 *
 * @author Radek Tomiška
 */
class IdentityContractPositions extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state
    };
  }

  componentDidMount() {
    // Lookot: getNavigationKey cannot be used -> profile vs users main tab
    this.selectSidebarItem('identity-contract-positions');
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.identity-contract.positions';
  }

  getNavigationKey() {
    // used for title rendering only
    return 'identity-contract-positions';
  }

  showDetail(entity) {
    if (!Utils.Entity.isNew(entity)) {
      this.context.store.dispatch(this.getManager().fetchPermissions(entity.id, `${this.getUiKey()}-detail`));
    }
    const treeType = entity._embedded && entity._embedded.workPosition && entity._embedded.workPosition._embedded ? entity._embedded.workPosition._embedded.treeType : null;
    //
    super.showDetail(entity, () => {
      if (treeType) {
        this.refs.workPosition.setTreeType(treeType);
      }
      this.refs.position.focus();
    });
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      this.addMessage({ message: this.i18n('action.save.success', { count: 1, record: manager.getNiceLabel(entity) }) });
    }
    //
    super.afterSave(entity, error);
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoading, _permissions } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new SearchParameters().setFilter('identityContractId', entityId);
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        { this.renderContentHeader({ style: { marginBottom: 0 }}) }

        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          manager={ manager }
          forceSearchParameters={ forceSearchParameters }
          showRowSelection={ manager.canDelete() }
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]._embedded.guarantee); }}
          className="no-margin"
          actions={
            [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={ this.showDetail.bind(this, { identityContract: entityId }) }
                rendered={ manager.canSave() }>
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
            property="position"
            header={this.i18n('entity.IdentityContract.position')}
            sort/>
          <Basic.Column
            property="workPosition"
            header={this.i18n('entity.IdentityContract.workPosition')}
            cell={
              ({ rowIndex, data }) => {
                return (
                  <span>
                    {
                      data[rowIndex]._embedded && data[rowIndex]._embedded.workPosition
                      ?
                      <Advanced.EntityInfo
                        entity={ data[rowIndex]._embedded.workPosition }
                        entityType="treeNode"
                        entityIdentifier={ data[rowIndex].workPosition }
                        face="popover" />
                      :
                      null
                    }
                  </span>
                );
              }
            }
          />
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
                showLoading={ _showLoading }
                readOnly={ !manager.canSave(detail.entity, _permissions) }>

                <Basic.TextField
                  ref="position"
                  label={ this.i18n('entity.IdentityContract.position') }
                  max={ 255 }/>

                <Advanced.TreeNodeSelect
                  ref="workPosition"
                  header={ this.i18n('entity.IdentityContract.workPosition') }
                  label={ this.i18n('entity.IdentityContract.workPosition') }/>
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

IdentityContractPositions.propTypes = {
  _showLoading: PropTypes.bool
};
IdentityContractPositions.defaultProps = {
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

export default connect(select)(IdentityContractPositions);
