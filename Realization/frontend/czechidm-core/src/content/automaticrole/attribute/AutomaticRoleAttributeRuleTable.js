import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid';
import { connect } from 'react-redux';
//
import Helmet from 'react-helmet';
import * as Utils from '../../../utils';
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import { SecurityManager } from '../../../redux';
import AutomaticRoleAttributeRuleTypeEnum from '../../../enums/AutomaticRoleAttributeRuleTypeEnum';
import AutomaticRoleAttributeRuleComparisonEnum from '../../../enums/AutomaticRoleAttributeRuleComparisonEnum';
import IdentityAttributeEnum from '../../../enums/IdentityAttributeEnum';
import ContractAttributeEnum from '../../../enums/ContractAttributeEnum';
import { AutomaticRoleAttributeManager } from '../../../redux';

const automaticRoleAttributeManager = new AutomaticRoleAttributeManager();

/**
 * Table with rules for automatic role by attribute
 *
 * @author Ondřej Kopr
 */
export class AutomaticRoleAttributeRuleTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    // default filter status
    // true - open
    // false - close
    this.state = {
      filterOpened: false
    };
  }

  getManager() {
    return this.props.manager;
  }

  getContentKey() {
    return 'content.automaticRoles.attribute.rule';
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

  /**
   * Recive new form for create new type else show detail for existing automatic role.
   */
  showDetail(entity, event) {
    const { attributeId } = this.props;
    if (event) {
      event.preventDefault();
    }
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/automatic-role/attributes/${attributeId}/rule/${uuidId}?new=1`);
    } else {
      this.context.router.push(`/automatic-role/attributes/${attributeId}/rule/${entity.id}`);
    }
  }

  /**
   * Return name of attribute for evaluating
   *
   * @param  {[String]} automaticRole
   * @return {[String]}
   */
  _getAttributeName(automaticRole) {
    if (automaticRole) {
      if (automaticRole.type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY)) {
        return IdentityAttributeEnum.getNiceLabel(automaticRole.attributeName.toString().toUpperCase());
      } else if (automaticRole.type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.CONTRACT)) {
        return ContractAttributeEnum.getNiceLabel(automaticRole.attributeName.toString().toUpperCase());
      } else if (automaticRole._embedded && automaticRole._embedded.formAttribute) {
        return automaticRole._embedded.formAttribute.name;
      }
    }
  }

  afterDelete() {
    super.afterDelete();
    //
    this.context.store.dispatch(automaticRoleAttributeManager.fetchEntity(this.props.attributeId));
  }

  render() {
    const { uiKey, manager, rendered, attributeId } = this.props;
    const { filterOpened } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    let forceSearchParameters = manager.getDefaultSearchParameters();
    if (attributeId) {
      forceSearchParameters = forceSearchParameters.setFilter('automaticRoleAttributeId', attributeId);
    }
    //
    return (
      <div>
        <Helmet title={this.i18n('content.automaticRoles.attribute.edit.title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          forceSearchParameters={forceSearchParameters}
          showRowSelection={SecurityManager.hasAuthority('AUTOMATICROLERULE_DELETE')}
          noData={this.i18n('content.automaticRoles.emptyRules')}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <div className="col-lg-6">
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text')}/>
                  </div>
                  <div className="col-lg-6 text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </div>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={filterOpened}
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
          <Advanced.Column
            property="type"
            face="enum"
            enumClass={AutomaticRoleAttributeRuleTypeEnum}
            header={this.i18n('entity.AutomaticRole.attribute.type.label')}/>
          <Advanced.Column
            property="attributeName"
            header={this.i18n('entity.AutomaticRole.attribute.attributeName')}
            cell={({ rowIndex, data }) => {
              return this._getAttributeName(data[rowIndex]);
            }}/>
          <Advanced.Column
            property="comparison"
            face="enum"
            enumClass={AutomaticRoleAttributeRuleComparisonEnum}
            header={this.i18n('entity.AutomaticRole.attribute.comparison')}/>
          <Advanced.Column
            property="value"
            header={this.i18n('entity.AutomaticRole.attribute.value.label')}/>
        </Advanced.Table>
      </div>
    );
  }
}

AutomaticRoleAttributeRuleTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  rendered: PropTypes.bool.isRequired
};

AutomaticRoleAttributeRuleTable.defaultProps = {
  rendered: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(AutomaticRoleAttributeRuleTable);
