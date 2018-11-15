import React, { PropTypes } from 'react';
//
import * as Basic from '../../basic';
import { HelpContent } from '../../../domain';
import { i18n } from '../../../services/LocalizationService';
import FilterToogleButton from './FilterToogleButton';
import FilterButtons from './FilterButtons';
import FilterTextField from './FilterTextField';
import FilterBooleanSelectBox from './FilterBooleanSelectBox';
import FilterEnumSelectBox from './FilterEnumSelectBox';
import FilterSelectBox from './FilterSelectBox';
import FilterDateTimePicker from './FilterDateTimePicker';
import FilterCreatableSelectBox from './FilterCreatableSelectBox';
import FilterDate from './FilterDate';
import FilterTreeNodeSelect from './FilterTreeNodeSelect';
import FilterRoleCatalogueSelect from './FilterRoleCatalogueSelect';
import FilterRoleSelect from './FilterRoleSelect';

/**
 * Filter mainly for advanced table
 *
 * @author Radek Tomiška
 */
export default class Filter extends Basic.AbstractContextComponent {

  getComponent(property) {
    return this.refs.filterForm.getComponent(property);
  }

  isFormValid() {
    return this.refs.filterForm.isFormValid();
  }

  getData() {
    return this.refs.filterForm.getData();
  }

  setData(filterData) {
    this.refs.filterForm.setData(filterData);
  }

  useFilter(event) {
    const { onSubmit } = this.props;
    if (onSubmit) {
      onSubmit(event);
    } else if (event) {
      event.preventDefault();
    }
  }

  render() {
    const { rendered, showloading } = this.props;
    if (!rendered || showloading) {
      return false;
    }
    return (
      <form onSubmit={this.useFilter.bind(this)} className="advanced-filter">
        {this.props.children}
      </form>
    );
  }

  /**
   * Return "like" operator help
   *
   * @return {HelpContent} help can be given to input help props or to the help icon itself
   */
  static getTextHelp() {
    let helpContent = new HelpContent();
    helpContent = helpContent.setHeader(
      <span>
        <Basic.Icon value="filter"/> { i18n('filter.text.help.header') }
      </span>
    );
    helpContent = helpContent.setBody((
      (<span dangerouslySetInnerHTML={{__html: i18n('filter.text.help.body') }}/>)
    ));
    //
    return helpContent;
  }
}

Filter.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Submit function
   */
  onSubmit: PropTypes.func.isRequired
};
Filter.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
};

Filter.ToogleButton = FilterToogleButton;
Filter.FilterButtons = FilterButtons;
Filter.TextField = FilterTextField;
Filter.BooleanSelectBox = FilterBooleanSelectBox;
Filter.EnumSelectBox = FilterEnumSelectBox;
Filter.SelectBox = FilterSelectBox;
Filter.DateTimePicker = FilterDateTimePicker;
Filter.CreatableSelectBox = FilterCreatableSelectBox;
Filter.FilterDate = FilterDate;
Filter.TreeNodeSelect = FilterTreeNodeSelect;
Filter.RoleCatalogueSelect = FilterRoleCatalogueSelect;
Filter.RoleSelect = FilterRoleSelect;
