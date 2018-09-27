import React from 'react';
//
import * as Basic from '../../basic';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';

/**
 * DateTime form value component
 * - based on DateTimePicker - mode (date / datetime) is supported
 *
 * @author Radek Tomiška
 */
export default class DateTimeFormAttributeRenderer extends AbstractFormAttributeRenderer {

  /**
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {[type]} formComponent
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    formValue.dateValue = rawValue;
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.dateValue;
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
    return formValue.dateValue ? formValue.dateValue : formValue.value;
  }

  renderSingleInput() {
    const { attribute, values } = this.props;
    //
    return (
      <Basic.DateTimePicker
        ref={ AbstractFormAttributeRenderer.INPUT }
        mode={ attribute.persistentType.toLowerCase() }
        required={ this.isRequired() }
        label={ this.getLabel() }
        placeholder={ this.getPlaceholder() }
        value={ this.toInputValue(values) }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }/>
    );
  }

}
