import _ from 'lodash';
import Joi from 'joi';
//
import TextFormAttributeRenderer from './TextFormAttributeRenderer';

/**
 * Short text form value component
 * - supports multiple and confidential attributes
 *
 * @author Radek Tomiška
 */
export default class ShortTextFormAttributeRenderer extends TextFormAttributeRenderer {

  /**
   * Returns true, when confidential mode is supported
   *
   * @return {boolean}
   */
  supportsConfidential() {
    return true;
  }

  /**
   * Returns true, when multi value mode is supported
   *
   * @return {boolean}
   */
  supportsMultiple() {
    return true;
  }

  /**
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {[type]} formComponent
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    formValue.shortTextValue = rawValue;
    if (formValue.shortTextValue === '') {
      // empty string is sent as null => value will not be saved on BE
      formValue.shortTextValue = null;
    }
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.shortTextValue;
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
    return formValue.shortTextValue ? formValue.shortTextValue : formValue.value;
  }

  /**
   * Return value by attribute persistent type
   *
   * @param  {FormValue} formValue form value
   * @return {oneOf([string, boolean, long])}
   */
  toInputValues(formValues) {
    const { attribute, useDefaultValue } = this.props;
    //
    if (formValues === null) {
      if (useDefaultValue) {
        return attribute.defaultValue;
      }
      return null;
    }
    if (_.isArray(formValues)) {
      // multi values are transformed to multi lines
      let result = null;
      formValues.forEach(singleValue => {
        if (result !== null) {
          if (!attribute.multiple) {
            return false;
          }
          result += '\n';
        }
        const inputValue = this.toInputValues(singleValue);
        if (inputValue) {
          if (result === null) { // single values should not be concated
            result = inputValue;
          } else {
            result += inputValue;
          }
        }
      });
      return result;
    }
    //
    if (attribute.confidential) {
      return formValues.shortTextValue; // returns proxied guarded string everytime
    }
    return this.getInputValue(formValues);
  }

  /**
   * Returns joi validator by persistent type
   *
   * @param  {FormAttribute} attribute
   * @return {Joi}
   */
  getInputValidation() {
    let validation = Joi.string().max(2000);
    if (!this.isRequired()) {
      validation = validation.concat(Joi.string().allow(null).allow(''));
    }
    return validation;
  }
}
