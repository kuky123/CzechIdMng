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
    const { attribute, readOnly, values } = this.props;
    //
    return (
      <Basic.BooleanSelectBox
        ref={ AbstractFormAttributeRenderer.INPUT }
        label={ attribute.name }
        value={ this.toInputValue(values) }
        helpBlock={ attribute.description }
        readOnly={ readOnly || attribute.readonly }
        required={ attribute.required }
        placeholder={ attribute.placeholder }/>
    );
  }

}
