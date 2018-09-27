import React from 'react';
//
import * as Basic from '../../basic';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';
import BooleanFormAttributeRenderer from './BooleanFormAttributeRenderer';

/**
 * Boolean selectbox form value component
 *
 * @author Radek Tomiška
 */
export default class BooleanSelectFormAttributeRenderer extends BooleanFormAttributeRenderer {

  /**
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {[type]} formComponent
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    formValue.booleanValue = rawValue;
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.booleanValue;
    //
    return formValue;
  }

  /**
   * Returns value to ipnut from given (persisted) form value
   *
   * @param  {FormValue} formValue
   * @return {object} value by persistent type
   */
  getInputValue(formValue) {
    // Warning: string representation is needed (false value not work as selected value for react-select clearable functionality)
    if (formValue.booleanValue === null || formValue.booleanValue === undefined
      || formValue.value === null || formValue.value === undefined) {
      return null;
    }
    return formValue.booleanValue ? formValue.booleanValue.toString() : formValue.value.toString();
  }

  renderSingleInput() {
    const { values } = this.props;
    //
    return (
      <Basic.BooleanSelectBox
        ref={ AbstractFormAttributeRenderer.INPUT }
        label={ this.getLabel() }
        value={ this.toInputValue(values) }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }
        required={ this.isRequired() }
        placeholder={ this.getPlaceholder() }/>
    );
  }

}
