import React from 'react';
//
import * as Basic from '../../basic';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';

/**
 * Boolean form value component
 *
 * @author Radek Tomiška
 */
export default class BooleanFormAttributeRenderer extends AbstractFormAttributeRenderer {

  /**
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {[type]} formComponent
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    if (!rawValue || (typeof rawValue === 'string') && rawValue.toLowerCase() !== 'true') { // false
      const { attribute } = this.props;
      //
      if (this.isRequired() || (attribute.defaultValue != null && attribute.defaultValue.toLowerCase() === 'true')) {
        formValue.booleanValue = rawValue;
      } else {
        // not required and false filled - don't need to be saved at all
        formValue.booleanValue = null;
      }
    } else {
      formValue.booleanValue = rawValue;
    }
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.booleanValue;
    //
    return formValue;
  }

  /**
   * Returns value to input from given (persisted) form value
   *
   * @param  {FormValue} formValue
   * @return {object} value by persistent type
   */
  getInputValue(formValue) {
    return formValue.booleanValue ? formValue.booleanValue : formValue.value;
  }

  renderSingleInput() {
    const { values } = this.props;
    //
    return (
      <Basic.Checkbox
        ref={ AbstractFormAttributeRenderer.INPUT }
        label={ this.getLabel() }
        value={ this.toInputValue(values) }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }
        required={ this.isRequired() }/>
    );
  }

}
