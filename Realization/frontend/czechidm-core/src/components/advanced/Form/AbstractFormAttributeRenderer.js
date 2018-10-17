import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import { FormAttributeManager } from '../../../redux';

const formAttributeManager = new FormAttributeManager();

/**
 * Abstract form value component
 * - supper class for all face type renderers
 * - provide basic implementation for single value Input
 * - multi value input has to be implemented in descendant
 *
 * @author Radek Tomiška
 */
export default class AbstractFormAttributeRenderer extends Basic.AbstractContextComponent {

  /**
   * Returns true, when confidential mode is supported
   *
   * @return {boolean}
   */
  supportsConfidential() {
    return false;
  }

  /**
   * Returns true, when multi value mode is supported
   *
   * @return {boolean}
   */
  supportsMultiple() {
    return false;
  }

  /**
   * Returns array of filled form values (form value object).
   * Single value attribute - array with one form velue
   * Multiple value attribute - array with form values
   *
   * @return {arrayOf(formValue)}
   */
  getValues() {
    const { attribute } = this.props;
    //
    if (attribute.multiple) {
      return this.toFormValues();
    }
    const filledFormValues = [];
    const formValue = this.toFormValue();
    if (formValue) {
      filledFormValues.push(formValue);
    }
    return filledFormValues;
  }

  getValue() {
    const filledFormValues = this.getValues();
    //
    // event if multiple output values, return only one
    return filledFormValues.pop();
  }


  /**
   * Input value is valid
   *
   * @return {Boolean} is valid
   */
  isValid() {
    const formComponent = this.refs[AbstractFormAttributeRenderer.INPUT];
    if (!formComponent) {
      // not supported compoenents
      return true;
    }
    if (!formComponent.isValid()) {
      formComponent.setState({
        showValidationError: true
      });
      return false;
    }
    return true;
  }

  /**
   * Creates form value object. Preserve form value id attribute.
   *
   * @param  {[type]} formValue previously saved form valu
   * @param  {[type]} seq       value order (for multiple attrivbutes)
   * @return {FormValue}
   */
  prepareFormValue(formValue, seq = 0) {
    const { attribute } = this.props;
    //
    if (formValue === null) {
      formValue = {
        formAttribute: attribute.id,
        _embedded: {
          formAttribute: attribute
        }
      };
    }
    formValue.seq = seq;
    return formValue;
  }

  /**
   * Returns joi validator by persistent type
   *
   * @param  {FormAttribute} attribute
   * @return {Joi}
   */
  getInputValidation() {
    return null;
  }

  /**
   * Filled form value for single input
   *
   * @return {FormValue}
   */
  toFormValue() {
    const { values } = this.props;
    const formComponent = this.refs[AbstractFormAttributeRenderer.INPUT];
    //
    if (!formComponent) {
      // not supported compoenents
      return null;
    }
    const componentValue = formComponent.getValue();
    // undefined values are not sent (confidential properties  etc.)
    if (componentValue === undefined) {
      return null;
    }
    return this.fillFormValue(this.prepareFormValue(values ? values[0] : null), componentValue);
  }

  /**
   * Form values from multi input
   *
   * @return {arrayOf(FormValue)}
   */
  toFormValues() {
    throw new TypeError('Must override method toFormValues()');
  }

  /**
   * Fill form value field by persistent type from input value.
   * Don't forget to set both values: by persistent type (e.g. 'shortTextValue') and common 'value'.
   *
   * @param  {FormValue} formValue - form value
   * @param  {object} formComponent value
   * @return {FormValue}
   */
  fillFormValue(/* formValue, rawValue */) {
    throw new TypeError('Must override method fillFormValue()');
  }

  /**
   * Return value by attribute persistent type
   *
   * @param  {arrayOf(FormValue)} formValue form values array or single form value
   * @return {object} value by persistent type
   */
  toInputValue(formValues) {
    const { attribute, useDefaultValue } = this.props;
    //
    let formValue = null;
    if (formValues && _.isArray(formValues)) {
      if (formValues.length > 0) {
        formValue = formValues[0];
      }
    } else {
      formValue = formValues;
    }
    if (formValue === null) {
      if (useDefaultValue) {
        return attribute.defaultValue;
      }
      return null;
    }
    return this.getInputValue(formValue);
  }

  toInputValues(/* formValues*/) {
    throw new TypeError('Must override method toInputValues()');
  }

  /**
   * Returns value, which has to be set to input from given (persisted) form value
   *
   * @param  {FormValue} formValue
   * @return {object} value by persistent type
   */
  getInputValue(/* formValue*/) {
    throw new TypeError('Must override method getInputValue(formValue)');
  }

  /**
   * Unsupported info
   */
  _unsupportedMode(mode = 'single') {
    const { attribute, formDefinition } = this.props;
    const _formDefinition = formDefinition || attribute._embedded.formDefinition;
    //
    return (
      <Basic.LabelWrapper label={ attribute.code } >
        <Basic.Alert level="warning" className="no-margin">
          <div>{ this.i18n(`component.advanced.EavForm.${mode}.unsupported.title`, { name: attribute.persistentType, face: attribute.faceType }) }</div>
          <div>{ this.i18n(`component.advanced.EavForm.${mode}.unsupported.formDefinition.title`) }:</div>
          <div style={{ wordWrap: 'break-word' }}>{ this.i18n(`component.advanced.EavForm.${mode}.unsupported.formDefinition.type`) }: { _formDefinition.type }</div>
          <div style={{ wordWrap: 'break-word' }}>{ this.i18n(`component.advanced.EavForm.${mode}.unsupported.formDefinition.code`) }: { _formDefinition.code }</div>
        </Basic.Alert>
      </Basic.LabelWrapper>
    );
  }

  _getLocalization(property, defaultValue = null) {
    const { attribute, formDefinition } = this.props;
    //
    return formAttributeManager.getLocalization(formDefinition, attribute, property, defaultValue);
  }

  /**
   * Return localized label for current attribute. As key is used
   * form definition code and attribute code.
   * If key in localization and form name is not defined, it will be used default value.
   */
  getLabel(defaultValue = null) {
    const { attribute } = this.props;
    //
    return this._getLocalization('label', attribute.name || attribute.code || defaultValue);
  }

  /**
   * Return localized help block for current attribute. As key is used
   * form definition code and attribute code.
   * If key in localization and form name is defined, it will be used default value.
   */
  getHelpBlock(defaultValue = null) {
    const { attribute } = this.props;
    //
    return this._getLocalization('help', attribute.description || defaultValue);
  }

  /**
   * Return localized help block for current attribute. As key is used
   * form definition code and attribute code.
   * If key in localization and form name is defined, it will be used default value.
   */
  getPlaceholder(defaultValue = null) {
    const { attribute } = this.props;
    //
    return this._getLocalization('placeholder', attribute.placeholder || defaultValue);
  }

  /**
   * Returns true, is attribute is required. ReadOnly (+ hidden, disabled) attributes are not required.
   *
   * @return {Boolean} [description]
   */
  isRequired() {
    const { attribute } = this.props;
    //
    if (this.isReadOnly()) {
      // read only attribute cannot be required
      return false;
    }
    return attribute.required;
  }

  isReadOnly() {
    const { attribute, readOnly } = this.props;
    //
    return readOnly || attribute.readonly;
  }

  /**
   * Input as single fields
   */
  renderSingleInput() {
    return this._unsupportedMode();
  }

  /**
   * Input for multiple values
   */
  renderMultipleInput() {
    return this._unsupportedMode('multiple');
  }

  render() {
    const { attribute } = this.props;
    // check confidential support
    if (attribute.confidential && !this.supportsConfidential()) {
      return (
        <Basic.LabelWrapper label={attribute.name}>
          <Basic.Alert level="warning" text={ this.i18n('component.advanced.EavForm.persistentType.unsupported.confidential', { name: attribute.persistentType}) } className="no-margin"/>
        </Basic.LabelWrapper>
      );
    }
    // multiple module is disabled, even when renderMultipleInput is overriden
    if (!this.supportsMultiple() && attribute.multiple) {
      return this._unsupportedMode('multiple');
    }
    return (attribute.multiple) ? this.renderMultipleInput() : this.renderSingleInput();
  }
}

AbstractFormAttributeRenderer.propTypes = {
  /**
   * Form attribute
   */
  attribute: PropTypes.object.isRequired,
  /**
   * Form definition. Attribute embedded form definition will be used otherwise.
   */
  formDefinition: PropTypes.object,
  /**
   * Filled form values
   */
  values: PropTypes.arrayOf(PropTypes.object),
  /**
   * ReadOnly form field
   */
  readOnly: PropTypes.bool,
  /**
   * Use configured attribute default value as filled.
   */
  useDefaultValue: PropTypes.bool
};
AbstractFormAttributeRenderer.defaultProps = {
  readOnly: false,
  useDefaultValue: false
};

AbstractFormAttributeRenderer.INPUT = 'input'; // input ref - is used internally for common operations
