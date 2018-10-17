import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { SecurityManager, FormAttributeManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';
import PersistentTypeEnum from '../../enums/PersistentTypeEnum';

const attributeManager = new FormAttributeManager();

/**
* Table of forms attributes
*
* @author Ondřej Kopr
* @author Radek Tomiška
*/
class FormAttributeTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened,
      showLoading: true
    };
  }

  getContentKey() {
    return 'content.formAttributes';
  }

  getManager() {
    return attributeManager;
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

  showDetail(entity) {
    const { definitionId } = this.props;
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/forms/attribute/${uuidId}/detail?new=1&formDefinition=${definitionId}`);
    } else {
      this.context.router.push('/forms/attribute/' + entity.id + '/detail');
    }
  }

  render() {
    const { uiKey, definitionId } = this.props;
    const { filterOpened } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          showRowSelection={ SecurityManager.hasAuthority('FORMATTRIBUTE_DELETE') }
          manager={ attributeManager }
          forceSearchParameters={ new SearchParameters().setFilter('definitionId', definitionId) }
          rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <div className="col-lg-6">
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text.placeholder')}/>
                  </div>
                  <div className="col-lg-6 text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </div>
                </Basic.Row>
                <Basic.Row>
                  <div className="col-lg-6">

                  </div>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={this.showDetail.bind(this, { })}
                rendered={SecurityManager.hasAuthority('FORMATTRIBUTE_CREATE')}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          filterOpened={!filterOpened}>
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
            sort={false}
            _searchParameters={ this.getSearchParameters() }/>
          <Advanced.Column property="code" header={ this.i18n('entity.FormAttribute.code.label') } sort/>
          <Advanced.Column property="name" header={ this.i18n('entity.FormAttribute.name.label') } sort/>
          <Advanced.Column property="persistentType" sort face="enum" enumClass={ PersistentTypeEnum } />
          <Advanced.Column
            property="faceType"
            cell={
              ({ data, rowIndex, property }) => {
                const faceType = data[rowIndex][property] || data[rowIndex].persistentType;
                const formComponent = attributeManager.getFormComponent(data[rowIndex]);
                //
                if (!formComponent) {
                  return (
                    <Basic.Label
                      level="warning"
                      value={ faceType }
                      title={ this.i18n('component.advanced.EavForm.persistentType.unsupported.title', { name: data[rowIndex].persistentType, face: faceType }) } />
                  );
                }
                return (
                  <span>{ formComponent.labelKey ? this.i18n(formComponent.labelKey) : faceType }</span>
                );
              }
            }/>
          <Advanced.Column property="unmodifiable" header={this.i18n('entity.FormAttribute.unmodifiable.label')} face="bool" sort />
          <Advanced.Column property="seq" header={ this.i18n('entity.FormAttribute.seq.label') } sort width="5%"/>
        </Advanced.Table>
      </div>
      );
  }
}

FormAttributeTable.propTypes = {
  filterOpened: PropTypes.bool,
  uiKey: PropTypes.string.isRequired,
  definitionId: PropTypes.string.isRequired
};

FormAttributeTable.defaultProps = {
  filterOpened: true,
};

function select(state, component) {
  return {
    _searchParameters: state.data.ui[component.uiKey] ? state.data.ui[component.uiKey].searchParameters : null
  };
}

export default connect(select)(FormAttributeTable);
