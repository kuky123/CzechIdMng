import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import classnames from 'classnames';
//
import { Basic, Advanced, Enums, Utils, Managers } from 'czechidm-core';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
import ProvisioningOperationTypeEnum from '../../domain/ProvisioningOperationTypeEnum';
import { SystemManager } from '../../redux';

const systemManager = new SystemManager();

/**
 * Provisioning operation and archive table
 *
 * @author Radk Tomiška
 */
export class ProvisioningOperationTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
    };
  }

  getContentKey() {
    return 'acc:content.provisioningOperations';
  }

  clearSelectedRows() {
    this.refs.table.getWrappedInstance().clearSelectedRows();
  }

  reload() {
    this.refs.table.getWrappedInstance().reload();
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

  _cancelAll() {
    const { uiKey, manager, isArchive } = this.props;
    if (isArchive) {
      // not supported for archive
      return;
    }
    //
    this.useFilter();
    //
    this.refs['confirm-cancelAll'].show(
      this.i18n(`action.cancelAll.message`),
      this.i18n(`action.cancelAll.header`)
    ).then(() => {
      // get filled filter
      const searchParameters = this.refs.table.getWrappedInstance().getSearchParameters(this.refs.filterForm).setName(null); // prevent to use search url
      //
      this.context.store.dispatch(manager.cancelAll(searchParameters, uiKey, (lrt, error) => {
        if (!error || error.statusCode === 202) {
          this.addMessage({ level: 'info', message: this.i18n('action.cancelAll.accepted')});
        } else {
          this.addError(error);
        }
      }));
    }, () => {
      // nothing
    });
  }

  _deleteAll() {
    const { uiKey, manager, isArchive } = this.props;
    if (isArchive) {
      // not supported for archive
      return;
    }
    //
    this.refs['confirm-deleteAll'].show(
      this.i18n(`action.deleteAll.message`),
      this.i18n(`action.deleteAll.header`),
      (result) => {
        if (result === 'reject') {
          return true;
        }
        if (result === 'confirm' && this.refs['delete-form'].isFormValid()) {
          return true;
        }
        return false;
      }
    ).then(() => {
      const systemField = this.refs['delete-system'];
      if (systemField.isValid()) {
        //
        this.context.store.dispatch(manager.deleteAll(systemField.getValue(), uiKey, (entity, error) => {
          if (!error) {
            this.addMessage({ level: 'success', message: this.i18n('action.deleteAll.success')});
          } else {
            this.addError(error);
          }
          this.reload();
        }));
      }
    }, () => {
      // nothing
    });
  }

  render() {
    const { uiKey, manager, showRowSelection, actions, showDetail, forceSearchParameters, columns, isArchive } = this.props;
    const { filterOpened } = this.state;
    let systemId = null;
    if (forceSearchParameters && forceSearchParameters.getFilters().has('systemId')) {
      systemId = forceSearchParameters.getFilters().get('systemId');
    }
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-cancelAll" level="warning"/>
        <Basic.Confirm ref="confirm-deleteAll" level="danger">
          <div style={{ marginTop: 20 }}>
            <Basic.AbstractForm ref="delete-form" uiKey="confirm-deleteAll" >
              <Basic.SelectBox
                ref="delete-system"
                label={ this.i18n('action.deleteAll.system.label') }
                placeholder={ this.i18n('action.deleteAll.system.placeholder') }
                manager={ systemManager }
                value={ systemId }
                required
                readOnly={ systemId !== null }/>
            </Basic.AbstractForm>
          </div>
        </Basic.Confirm>

        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={ manager }
          showRowSelection={ showRowSelection }
          actions={ actions }
          forceSearchParameters={ forceSearchParameters }
          filterOpened={ filterOpened }
          filter={
            <Advanced.Filter onSubmit={ this.useFilter.bind(this) }>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate ref="fromTill"/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={ this.cancelFilter.bind(this) }/>
                  </Basic.Col>
                </Basic.Row>

                <Basic.Row className={ classnames({ last: !_.includes(columns, 'entityIdentifier') })}>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="resultState"
                      placeholder={this.i18n('acc:entity.ProvisioningOperation.resultState')}
                      enum={ Enums.OperationStateEnum }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="operationType"
                      placeholder={this.i18n('acc:entity.ProvisioningOperation.operationType')}
                      enum={ ProvisioningOperationTypeEnum }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    {
                      !_.includes(columns, 'system')
                      ||
                      <Advanced.Filter.SelectBox
                        ref="systemId"
                        placeholder={this.i18n('acc:entity.System._type')}
                        multiSelect={false}
                        manager={systemManager}/>
                    }
                  </Basic.Col>
                </Basic.Row>

                <Basic.Row className="last" rendered={ _.includes(columns, 'entityIdentifier') }>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="entityType"
                      placeholder={this.i18n('acc:entity.SystemEntity.entityType')}
                      enum={SystemEntityTypeEnum}/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="entityIdentifier"
                      placeholder={this.i18n('acc:entity.ProvisioningOperation.entityIdentifier')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="systemEntityUid"
                      placeholder={this.i18n('acc:entity.SystemEntity.uid')}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          buttons={
            [
              <Basic.Button
                level="danger"
                key="delete-all-button"
                className="btn-xs"
                onClick={ this._deleteAll.bind(this) }
                rendered={ Managers.SecurityManager.hasAnyAuthority(['APP_ADMIN']) && !isArchive }
                title={ this.i18n('action.deleteAll.button.title') }
                titlePlacement="bottom"
                icon="fa:trash">
                { this.i18n('action.deleteAll.button.label') }
              </Basic.Button>,
              <Basic.Button
                level="warning"
                key="cancel-all-button"
                className="btn-xs"
                onClick={ this._cancelAll.bind(this) }
                rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_ADMIN']) && !isArchive }
                title={ this.i18n('action.cancelAll.button.title') }
                titlePlacement="bottom"
                icon="fa:ban"
                style={{ marginLeft: 3 }}>
                { this.i18n('action.cancelAll.button.label') }
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }>
          {
            !showDetail
            ||
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={() => showDetail(data[rowIndex], isArchive)}/>
                  );
                }
              }/>
          }
          <Advanced.Column
            property="resultState"
            width={75}
            header={this.i18n('acc:entity.ProvisioningOperation.resultState')}
            face="text"
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.OperationResult value={ entity.result } detailLink={ !showDetail ? null : () => showDetail(data[rowIndex], isArchive) }/>
                );
              }
            }
            rendered={_.includes(columns, 'resultState')}/>
          <Advanced.Column property="created" width="125px" header={this.i18n('entity.created')} sort face="datetime" rendered={_.includes(columns, 'created')}/>
          <Advanced.Column
            property="operationType"
            width={75}
            header={this.i18n('acc:entity.ProvisioningOperation.operationType')}
            sort
            face="enum"
            enumClass={ProvisioningOperationTypeEnum}
            rendered={_.includes(columns, 'operationType')}/>
          <Advanced.Column
            property="entityType"
            width={ 75 }
            header={ this.i18n('acc:entity.SystemEntity.entityType') }
            sort
            face="enum"
            enumClass={ SystemEntityTypeEnum }
            rendered={ _.includes(columns, 'entityType') } />
          <Advanced.Column
            property="entityIdentifier"
            header={ this.i18n('acc:entity.ProvisioningOperation.entity') }
            face="text"
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                //
                if (!data[rowIndex]._embedded || !data[rowIndex]._embedded.entity) {
                  return (
                    <Advanced.UuidInfo value={ entity.entityIdentifier } />
                  );
                }
                //
                return (
                  <Advanced.EntityInfo
                    entityType={ entity.entityType }
                    entityIdentifier={ entity.entityIdentifier }
                    entity={ entity._embedded.entity }
                    face="popover"/>
                );
              }
            }
            rendered={ _.includes(columns, 'entityIdentifier') }/>
          <Advanced.Column
            property="system.name"
            header={ this.i18n('acc:entity.System.name') }
            sort
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.EntityInfo
                    entityType="system"
                    entityIdentifier={ entity.system }
                    entity={ entity._embedded.system }
                    face="popover"/>
                );
              }
            }
            rendered={_.includes(columns, 'system')} />
          <Advanced.Column
            property="systemEntityUid"
            header={this.i18n('acc:entity.SystemEntity.uid')}
            sort
            sortProperty="systemEntityUid"
            face="text"
            rendered={isArchive && _.includes(columns, 'systemEntityUid')}/>
          <Advanced.Column
            property="_embedded.systemEntity.uid"
            header={this.i18n('acc:entity.SystemEntity.uid')}
            sort
            sortProperty="systemEntity"
            face="text"
            rendered={!isArchive && _.includes(columns, 'systemEntityUid')}/>
        </Advanced.Table>
      </div>
    );
  }
}

ProvisioningOperationTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
  /**
   * Bulk actions e.g. { value: 'activate', niceLabel: this.i18n('content.identities.action.activate.action'), action: this.onActivate.bind(this) }
   */
  actions: PropTypes.arrayOf(PropTypes.object),
  /**
   * Detail  callback
   */
  showDetail: PropTypes.func,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Rendered columns
   */
  columns: PropTypes.arrayOf(PropTypes.string),
};
ProvisioningOperationTable.defaultProps = {
  showRowSelection: false,
  actions: [],
  forceSearchParameters: null,
  columns: ['resultState', 'created', 'operationType', 'entityType', 'entityIdentifier', 'system', 'systemEntityUid']
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(ProvisioningOperationTable);
