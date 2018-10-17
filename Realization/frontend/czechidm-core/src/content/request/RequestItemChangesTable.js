import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import {RequestItemManager } from '../../redux';
import ConceptRoleRequestOperationEnum from '../../enums/ConceptRoleRequestOperationEnum';
import _ from 'lodash';

const uiKey = 'universal-request';
const requestItemManager = new RequestItemManager();

/**
 * Table for request item changes
 *
 * @author Vít Švanda
 */
class RequestItemChangesTable extends Advanced.AbstractTableContent {

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

  /**
   * Return data (attributes) for table of changes
   */
  _getDataWithChanges(onlyChangedAttributes) {
    const {itemData} = this.props;
    if (itemData && itemData.attributes) {
      const attributes = itemData.attributes
        .filter(attribute => onlyChangedAttributes ? attribute.changed : true);
      // sort by name
      return _(attributes).sortBy('name').value();
    }
    return null;
  }

  render() {
    const {itemData} = this.props;
    if (!itemData || !itemData.requestItem) {
      return (<Basic.Alert
        level="info"
        title={this.i18n('itemDetail.nochanges.title')}
        text={this.i18n('itemDetail.nochanges.text')}
      />);
    }
    const isOperationUpdate = itemData && itemData.requestItem.operation === 'UPDATE';
    const isOperationRemove = itemData && itemData.requestItem.operation === 'REMOVE';
    const isOperationAdd = itemData && itemData.requestItem.operation === 'ADD';

    const entityType = this._getNameOfDTO(itemData.requestItem.ownerType);
    const sortedItemData = this._getDataWithChanges(isOperationUpdate);
    if (!sortedItemData || sortedItemData.length === 0) {
      return (<Basic.Alert
        level="info"
        title={this.i18n('itemDetail.nochanges.title')}
        text={this.i18n('itemDetail.nochanges.text')}
      />);
    }
    return (
      <div>
        <Advanced.EntityInfo
          entityType={entityType}
          entityIdentifier={ itemData.requestItem.ownerId }
          face="full"/>
        <Basic.Table
          data={sortedItemData}
          noData={this.i18n('component.basic.Table.noData')}
          rowClass={this._getRowClass.bind(this, isOperationUpdate, isOperationRemove, isOperationAdd)}
          className="table-bordered">
          <Basic.Column
            property="name"
            header={this.i18n('itemDetail.changes.property')}/>
          <Basic.Column
            property="oldValue"
            rendered={isOperationUpdate}
            header={this.i18n('itemDetail.changes.oldValue')}
            cell={this._getWishValueCell.bind(this, true, isOperationUpdate)}/>
          <Basic.Column
            property="value"
            header={this.i18n('itemDetail.changes.newValue')}
            cell={this._getWishValueCell.bind(this, false, isOperationUpdate)}/>
        </Basic.Table>
      </div>
    );
  }
}

RequestItemChangesTable.propTypes = {
  itemData: PropTypes.object,
  isOperationUpdate: PropTypes.bool
};

export default RequestItemChangesTable;
