import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import {FormValueManager, SecurityManager} from '../../redux';
import * as Utils from '../../utils';
import PersistentTypeEnum from '../../enums/PersistentTypeEnum';

const manager = new FormValueManager();

/**
 * Table of abstract form values
 *
 * @author Roman Kučera
 * @author Radek Tomiška
 */
export class FormValueTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened,
      showLoading: true
    };
  }

  getContentKey() {
    return 'content.form-values';
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return this.props.uiKey;
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

  render() {
    const { uiKey, forceSearchParameters, showFilter, columns } = this.props;
    const { filterOpened } = this.state;
    //
    return (
        <div>
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Advanced.Table
              ref="table"
              uiKey={uiKey}
              manager={manager}
              _searchParameters={this.getSearchParameters()}
              forceSearchParameters={forceSearchParameters}
              showFilter={showFilter}
              showRowSelection={ SecurityManager.hasAuthority('FORMVALUE_DELETE') }
              // actions={
              //   [
              //     { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
              //   ]
              // }
              filter={
                <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                  <Basic.AbstractForm ref="filterForm">
                    <Basic.Row>
                      <Basic.Col lg={6}>
                        <Advanced.Filter.TextField
                            ref="text"
                            placeholder={this.i18n('filter.text.placeholder')}/>
                      </Basic.Col>
                      <Basic.Col lg={6} className="text-right">
                        <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                      </Basic.Col>
                    </Basic.Row>
                    <Basic.Row>
                      <Basic.Col lg={6}>
                        <Basic.EnumSelectBox
                            ref="persistentType"
                            placeholder={this.i18n('filter.type.placeholder')}
                            multiSelect={false}
                            enum={PersistentTypeEnum}/>
                      </Basic.Col>
                    </Basic.Row>
                  </Basic.AbstractForm>
                </Advanced.Filter>
              }
              filterOpened={filterOpened}>

            <Advanced.Column
                property="ownerId"
                cell={
                  ({rowIndex, data}) => {
                    return (
                        <Advanced.EntityInfo
                            entityType={Utils.Ui.getSimpleJavaType((data[rowIndex].ownerType))}
                            entityIdentifier={data[rowIndex].ownerId}
                            showIcon
                            face="popover"
                            showEntityType/>
                    );
                  }
                }
                rendered={_.includes(columns, 'owner')}/>
            <Advanced.Column
                property="_embedded.formAttribute.code"
                header={this.i18n('entity.FormAttribute.code.label')}
                sort
                sortProperty="formAttribute.code"
                rendered={_.includes(columns, 'code')}/>
            <Advanced.Column
                property="_embedded.formAttribute.name"
                header={this.i18n('entity.FormAttribute.name.label')}
                sort
                sortProperty="formAttribute.name"
                rendered={_.includes(columns, 'name')}/>
            <Advanced.Column
                property="value"
                rendered={_.includes(columns, 'value')}/>
            <Advanced.Column
                property="persistentType"
                face="enum"
                enumClass={PersistentTypeEnum}
                sort
                rendered={_.includes(columns, 'persistentType')}/>
            <Advanced.Column
                property="_embedded.formAttribute.defaultValue"
                header={this.i18n('entity.FormAttribute.defaultValue')}
                sort
                sortProperty="formAttribute.defaultValue"
                rendered={_.includes(columns, 'defaultValue')}/>
            <Advanced.Column
                property="_embedded.formAttribute.faceType"
                header={this.i18n('entity.FormAttribute.faceType.label')}
                sort
                sortProperty="formAttribute.faceType"
                rendered={_.includes(columns, 'faceType')}/>
          </Advanced.Table>
        </div>
    );
  }
}

FormValueTable.propTypes = {
  filterOpened: PropTypes.bool,
  showFilter: PropTypes.bool,
  uiKey: PropTypes.string.isRequired,
  forceSearchParameters: PropTypes.object,
  columns: PropTypes.arrayOf(PropTypes.string),
};

FormValueTable.defaultProps = {
  filterOpened: true,
  showFilter: true,
  forceSearchParameters: null,
  columns: ['owner', 'code', 'name', 'value', 'persistentType', 'defaultValue', 'faceType'],
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(FormValueTable);
