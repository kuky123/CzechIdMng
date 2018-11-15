import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Utils, Managers, Domain } from 'czechidm-core';

/**
* Table of controlled values
*
* @author Vít Švanda
*
*/
export class AttributeControlledValueTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
    };
  }

  getContentKey() {
    return 'acc:content.attributeControlledValue';
  }

  getManager() {
    const { manager } = this.props;
    //
    return manager;
  }


  render() {
    const { uiKey, manager, columns, forceSearchParameters, showRowSelection, rendered } = this.props;

    if (rendered === false) {
      return <div/>;
    }

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          showRowSelection={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') && showRowSelection}
          forceSearchParameters={forceSearchParameters}
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          >
          <Advanced.Column property="value" rendered={_.includes(columns, 'value')} sort face="text" header={this.i18n('acc:entity.AttributeControlledValue.value')}/>
          <Advanced.Column property="created" rendered={_.includes(columns, 'created')} sort face="datetime" header={this.i18n('acc:entity.AttributeControlledValue.created')}/>
          <Advanced.Column property="creator" rendered={_.includes(columns, 'creator')} sort face="text" header={this.i18n('acc:entity.AttributeControlledValue.creator')}/>
        </Advanced.Table>
      </div>
    );
  }
}

AttributeControlledValueTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  forceSearchParameters: PropTypes.object,
  showAddButton: PropTypes.bool,
  showRowSelection: PropTypes.bool
};

AttributeControlledValueTable.defaultProps = {
  columns: ['value', 'created', 'creator'],
  _showLoading: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showRowSelection: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _showLoading: component.manager.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { withRef: true })(AttributeControlledValueTable);
