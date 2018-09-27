import React from 'react';
import _ from 'lodash';
import Joi from 'joi';
//
import * as Basic from '../../basic';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';

/**
 * Text form value component
 * - supports multiple and confidential attributes
 *
 * @author Radek Tomiška
 */
export default class TextFormAttributeRenderer extends AbstractFormAttributeRenderer {

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
   * Form values from multi input
   *
   * @return {arrayOf(FormValue)}
   */
  toFormValues() {
    const { values } = this.props;
    const formComponent = this.refs[AbstractFormAttributeRenderer.INPUT];
    const filledFormValues = [];
    //
    if (!formComponent) {
      // not supported compoenents
      return filledFormValues;
    }
    const componentValue = formComponent.getValue();
    // undefined values are not sent (confidential properties  etc.)
    if (componentValue === undefined) {
      return filledFormValues;
    }
    if (componentValue) {
      const textValues = componentValue.split('\n');
      for (let i = 0; i < textValues.length; i++) {
        let formValue = null;
        if (values && i < values.length) {
          formValue = values[i];
        }
        filledFormValues.push(this.fillFormValue(this.prepareFormValue(formValue, i), textValues[i]));
      }
    }
    return filledFormValues;
  }

  /**
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {[type]} formComponent
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    formValue.stringValue = rawValue;
    if (formValue.stringValue === '') {
      // empty string is sent as null => value will not be saved on BE
      formValue.stringValue = null;
    }
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.stringValue;
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
    return formValue.stringValue ? formValue.stringValue : formValue.value;
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
      return formValues.stringValue; // returns proxied guarded string everytime
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
    let validation = Joi.string();
    if (!this.isRequired()) {
      validation = validation.concat(Joi.string().allow(null).allow(''));
    }
    return validation;
  }

  renderSingleInput() {
    const { attribute, values } = this.props;
    //
    return (
      <Basic.TextField
        ref={ AbstractFormAttributeRenderer.INPUT }
        type={ attribute.confidential ? 'password' : 'text' }
        label={ this.getLabel() }
        placeholder={ this.getPlaceholder() }
        value={ this.toInputValue(values) }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }
        validation={ this.getInputValidation() }
        required={ this.isRequired() }
        confidential={ attribute.confidential }/>
    );
  }

  renderMultipleInput() {
    const { attribute, values } = this.props;
    //
    return (
      <Basic.TextArea
        ref={ AbstractFormAttributeRenderer.INPUT }
        type={ attribute.confidential ? 'password' : 'text' }
        required={ this.isRequired() }
        label={
          <span>
            { this.getLabel() }
            {' '}
            <Basic.Tooltip placement="bottom" value={ this.i18n('component.advanced.EavForm.multiple.title') }>
              {
                <small>({this.i18n('component.advanced.EavForm.multiple.label')})</small>
              }
            </Basic.Tooltip>
          </span>
        }
        value={ this.toInputValues(values) }
        helpBlock={ this.getHelpBlock(this.i18n('multiple.title')) }
        readOnly={ this.isReadOnly() }
        placeholder={ this.getPlaceholder() }/>
    );
  }
}
