import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import {RequestItemManager, SecurityManager, WorkflowTaskInstanceManager } from '../../redux';
import ConceptRoleRequestOperationEnum from '../../enums/ConceptRoleRequestOperationEnum';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';
import RequestItemChangesTable from './RequestItemChangesTable';
import _ from 'lodash';

const uiKey = 'universal-request';
const requestItemManager = new RequestItemManager();
const uiKeyRequestItems = 'request-items';
const workflowTaskInstanceManager = new WorkflowTaskInstanceManager();

/**
 * Table for request item changes
 *
 * @author Vít Švanda
 */
class RequestItemTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return requestItemManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.requestDetail';
  }

  /**
   * Create value (highlights changes) cell for attributes table
   */
  _getWishValueCell( old = false, showChanges = true, { rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || (!entity.value && !entity.values)) {
      return '';
    }
    if (entity.multivalue) {
      const listResult = [];
      if (!entity.values) {
        return '';
      }
      for (const item of entity.values) {
        const value = old ? item.oldValue : item.value;
        if (!old && item.change && showChanges) {
          listResult.push(<Basic.Label
            key={value}
            level={ConceptRoleRequestOperationEnum.getLevel(item.change)}
            title={item.change ? this.i18n(`attribute.diff.${item.change}`) : null}
            style={item.change === 'REMOVE' ? {textDecoration: 'line-through'} : null}
            text={value}/>);
        } else {
          listResult.push(value ? (item.value + ' ') : '');
        }
        listResult.push(' ');
      }
      return listResult;
    }

    if (!entity.value) {
      return '';
    }
    const value = old ? entity.value.oldValue : entity.value.value;
    if (!old && entity.value.change && showChanges) {
      return (<Basic.Label
        title={entity.value.change ? this.i18n(`attribute.diff.${entity.value.change}`) : null}
        level={'warning'}
        text={value !== null ? value + '' : '' }/>);
    }
    return value !== null ? value + '' : '';
  }

  _getNameOfDTO(ownerType) {
    const types = ownerType.split('.');
    return types[types.length - 1];
  }

  _getRowClass(updated, deleted, added, { rowIndex, data}) {
    const value = data[rowIndex];
    if (value.changed && updated) {
      return 'warning';
    }
    if (value.changed && deleted) {
      return 'danger';
    }
    if (value.changed && added) {
      return 'success';
    }
    return null;
  }

  _getCurrentActivitiCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    const task = {taskName: entity._embedded.wfProcessId.currentActivityName,
                  processDefinitionKey: entity._embedded.wfProcessId.processDefinitionKey,
                  definition: {id: entity._embedded.wfProcessId.activityId}
                };
    return (
      workflowTaskInstanceManager.localize(task, 'name')
    );
  }

  _getWfProcessCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    return (
      <Advanced.WorkflowProcessInfo entity={entity._embedded.wfProcessId}/>
    );
  }

  _renderDetailCell({ rowIndex, data }) {
    return (
      <Basic.Button
        type="button"
        level="info"
        title={ this.i18n('button.showItemChanges.tooltip')}
        titlePlacement="bottom"
        onClick={this.showItemChanges.bind(this, data[rowIndex])}
        className="btn-xs">
        <Basic.Icon type="fa" icon="eye"/>
      </Basic.Button>
    );
  }

  _renderOwnerCell({ rowIndex, data }) {
    const entity = data[rowIndex];
    if (!entity && entity._embedded) {
      return '';
    }
    const entityType = this._getNameOfDTO(entity.ownerType);
    const owner = entity._embedded.ownerId;
    return (
      <Advanced.EntityInfo
        entityType={ entityType }
        entity={ owner }
        face="popover"/>
    );
  }

  showItemChanges(entity) {
    this.getManager().getService().getChanges(entity.id)
    .then(json => {
      this.setState({itemDetail: {changes: json, show: true, item: entity}});
    })
    .catch(error => {
      this.addError(error);
    });
  }

  _getCandidatesCell({ rowIndex, data, property}) {
    const entity = data[rowIndex];
    if (!entity || !entity._embedded || !entity._embedded.wfProcessId) {
      return '';
    }
    return (
      <Advanced.IdentitiesInfo identities={entity._embedded.wfProcessId[property]} maxEntry={5} />
    );
  }

  closeDetail() {
    this.setState({itemDetail: {show: false}});
  }

  render() {
    const {forceSearchParameters, isEditable, showLoading, columns} = this.props;
    const {itemDetail} = this.state;

    let operation = 'update';
    if (itemDetail && itemDetail.changes && itemDetail.changes.requestItem) {
      operation = itemDetail.changes.requestItem.operation.toLowerCase();
    }
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKeyRequestItems}
          manager={requestItemManager}
          forceSearchParameters={forceSearchParameters}
          showRowSelection={isEditable && SecurityManager.hasAuthority('REQUEST_UPDATE')}
          actions={
            [{ value: 'delete', niceLabel: this.i18n('action.delete.action'),
               action: this.onDelete.bind(this), disabled: false }]
          }
          >
          <Advanced.Column
            property=""
            header=""
            className="detail-button"
            cell={this._renderDetailCell.bind(this)}/>
          <Advanced.Column
            header={this.i18n('entity.RequestItem.state')}
            property="state"
            sort
            face="enum"
            rendered={_.includes(columns, 'state')}
            enumClass={RoleRequestStateEnum}/>
          <Advanced.Column
            property="operation"
            face="enum"
            rendered={_.includes(columns, 'operation')}
            enumClass={ConceptRoleRequestOperationEnum}
            header={this.i18n('entity.RequestItem.operation')}
            sort/>
          <Advanced.Column
            property="ownerId"
            header={ this.i18n('entity.RequestItem.ownerId') }
            rendered={_.includes(columns, 'owner')}
            face="text"
            cell={this._renderOwnerCell.bind(this)}/>
          <Advanced.Column
            property="candicateUsers"
            face="text"
            rendered={_.includes(columns, 'wf')}
            cell={this._getCandidatesCell}
            />
          <Advanced.Column
            property="currentActivity"
            face="text"
            rendered={_.includes(columns, 'wf')}
            cell={this._getCurrentActivitiCell}
            />
          <Advanced.Column
            property="wfProcessId"
            cell={this._getWfProcessCell}
            sort
            rendered={_.includes(columns, 'wf')}
            face="text"/>
          <Advanced.Column
            property="result"
            header={this.i18n('entity.RequestItem.result')}
            face="text"
            rendered={_.includes(columns, 'state')}
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.OperationResult value={ entity.result }/>
                );
              }
            }/>
          <Advanced.Column
            property="created"
            rendered={_.includes(columns, 'created')}
            header={this.i18n('entity.created')}
            sort
            face="datetime"/>
        </Advanced.Table>
        <Basic.Modal
          show={itemDetail && itemDetail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static"
          keyboard={!showLoading}>
            <Basic.Modal.Header closeButton={ !showLoading } text={this.i18n(`itemDetail.title.${operation}`)}/>
            <Basic.Modal.Body>
              <RequestItemChangesTable
                itemData={itemDetail ? itemDetail.changes : null}/>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={ this.closeDetail.bind(this) }
                showLoading={ showLoading }>
                { this.i18n('button.close') }
              </Basic.Button>
            </Basic.Modal.Footer>
        </Basic.Modal>
      </div>
    );
  }
}

RequestItemTable.propTypes = {
  itemData: PropTypes.object,
  isOperationUpdate: PropTypes.bool,
  columns: PropTypes.arrayOf(PropTypes.string),
};

RequestItemTable.defaultProps = {
  columns: ['state', 'created', 'wf', 'operation', 'owner']
};
export default RequestItemTable;
