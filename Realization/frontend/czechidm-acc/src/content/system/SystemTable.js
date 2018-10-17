import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Utils, Managers, Domain } from 'czechidm-core';
import ProvisioningOperationTypeEnum from '../../domain/ProvisioningOperationTypeEnum';
//
import uuid from 'uuid';

/**
* Table of target systems
*
* @author Radek Tomiška
*
*/
export class SystemTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
    };
  }

  getContentKey() {
    return 'acc:content.systems';
  }

  getManager() {
    const { manager } = this.props;
    //
    return manager;
  }

  componentDidMount() {
    super.componentDidMount();
    //
    if (this.props.filterOpened) {
      this.refs.text.focus();
    }
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

  showDetail(entity) {
    if (Utils.Entity.isNew(entity)) {
      const uuidId = uuid.v1();
      this.context.router.push(`/system/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`/system/${entity.id}/detail`);
    }
  }

  onDuplicate(bulkActionValue, selectedRows) {
    const { manager, uiKey } = this.props;
    const selectedEntities = manager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    this.refs['confirm-duplicate'].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: manager.getNiceLabel(selectedEntities[0]), records: manager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: manager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(manager.duplicateEntities(selectedEntities, uiKey, (entity, error) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: manager.getNiceLabel(entity) }) }, error);
        } else {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      // nothing
    });
  }

  getTableButtons(showAddButton) {
    return (
      [
        <Basic.Button
          level="success"
          key="add_button"
          className="btn-xs"
          onClick={this.showDetail.bind(this, { })}
          rendered={Managers.SecurityManager.hasAuthority('SYSTEM_CREATE') && showAddButton}>
          <Basic.Icon type="fa" icon="plus"/>
          {' '}
          {this.i18n('button.add')}
        </Basic.Button>
      ]);
  }

  /**
   * Return div with all labels for system with information about blocked operations.
   * There can't be used EnumLabel because every enumlable is on new line
   */
  _getBlockedOperations(system) {
    if (system && system.blockedOperation) {
      // every blocked operation has same level in this table
      const level = 'error';
      //
      const createKey = ProvisioningOperationTypeEnum.findKeyBySymbol(ProvisioningOperationTypeEnum.CREATE);
      const updateKey = ProvisioningOperationTypeEnum.findKeyBySymbol(ProvisioningOperationTypeEnum.UPDATE);
      const deleteKey = ProvisioningOperationTypeEnum.findKeyBySymbol(ProvisioningOperationTypeEnum.DELETE);
      return (
        <div>
        {
          !system.blockedOperation.createOperation
          ||
          <span>
            {' '}
            <Basic.Label
              level={level}
              value={ProvisioningOperationTypeEnum.getNiceLabel(createKey)}/>
          </span>
        }
        {
          !system.blockedOperation.updateOperation
          ||
          <span>
            {' '}
            <Basic.Label
              level={level}
              value={ProvisioningOperationTypeEnum.getNiceLabel(updateKey)}/>
          </span>
        }
        {
          !system.blockedOperation.deleteOperation
          ||
          <span>
            {' '}
            <Basic.Label
              level={level}
              value={ProvisioningOperationTypeEnum.getNiceLabel(deleteKey)}/>
          </span>
        }
        </div>
      );
    }
  }

  render() {
    const { uiKey, manager, columns, forceSearchParameters, showAddButton, showRowSelection } = this.props;
    const { filterOpened } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-duplicate" level="danger"/>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          filterOpened={filterOpened}
          forceSearchParameters={forceSearchParameters}
          showRowSelection={Managers.SecurityManager.hasAuthority('SYSTEM_DELETE') && showRowSelection}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row className="last">
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('acc:entity.System.name')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          buttons = {this.getTableButtons(showAddButton)}
          _searchParameters={ this.getSearchParameters() }
          >

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
          <Advanced.ColumnLink to="system/:id/detail" property="name" width="15%" sort face="text" rendered={_.includes(columns, 'name')}/>
          <Advanced.Column property="description" sort face="text" rendered={_.includes(columns, 'description')}/>
          <Advanced.Column property="queue" sort face="bool" width="75px" rendered={_.includes(columns, 'queue')}/>
          <Advanced.Column property="readonly" header={this.i18n('acc:entity.System.readonly.label')} sort face="bool" width="75px" rendered={_.includes(columns, 'readonly')}/>
          <Advanced.Column property="disabled" sort face="bool" width="75px" rendered={_.includes(columns, 'disabled')}/>
          <Advanced.Column
            property="blockedOperation"
            width="12%"
            cell={({ rowIndex, data }) => {
              return (
                this._getBlockedOperations(data[rowIndex])
              );
            }}
            rendered={_.includes(columns, 'blockedOperation')}/>
        </Advanced.Table>
      </div>
    );
  }
}

SystemTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  forceSearchParameters: PropTypes.object,
  showAddButton: PropTypes.bool,
  showRowSelection: PropTypes.bool
};

SystemTable.defaultProps = {
  columns: ['name', 'description', 'disabled', 'virtual', 'readonly', 'queue', 'blockedOperation'],
  filterOpened: false,
  _showLoading: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showAddButton: true,
  showRowSelection: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _showLoading: component.manager.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { withRef: true })(SystemTable);
