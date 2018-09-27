import React, { PropTypes } from 'react';
//
import * as Basic from '../../basic';
import { FormAttributeManager } from '../../../redux';
import FormInstance from '../../../domain/FormInstance';
//
const attributeManager = new FormAttributeManager();

/**
 * Content of eav form by given form instance (= form definition + form values)
 *
 * @author Radek Tomiška
 */
export default class EavForm extends Basic.AbstractContextComponent {

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return 'component.advanced.EavForm';
  }

  /**
   * Returns true, when form is valid, otherwise false
   *
   * @return {Boolean} [description]
   */
  isValid() {
    const { formInstance } = this.props;
    let isAllValid = true;
    formInstance.getAttributes().forEach(attribute => {
      const formComponent = this.refs[attribute.code];
      if (!formComponent) {
        // unsupported persistentType
        return true;
      }
      // we need to call validate method on all component (break is not needed)
      if (!formComponent.isValid()) {
        isAllValid = false;
      }
    });
    //
    return isAllValid;
  }

  /**
   * Return form definition used for form instance
   *
   * @return {formDefinition}
   */
  getFormDefinition() {
    const { formInstance } = this.props;
    //
    if (!formInstance) {
      return null;
    }
    return formInstance.getDefinition();
  }

  /**
   * Returns array of filled form values (form value object)
   *
   * @return {arrayOf(formValue)}
   */
  getValues() {
    const { formInstance } = this.props;
    let filledFormValues = [];
    //
    formInstance.getAttributes().forEach(attribute => {
      if (attribute.readonly) {
        // readOnly (~ disabled from our point of view) attributes are not sent to BE
        return true;
      }
      const formComponent = this.refs[attribute.code];
      if (!formComponent) {
        // unsupported persistentType
        return true;
      }
      filledFormValues = filledFormValues.concat(formComponent.getValues());
    });
    return filledFormValues;
  }

  /**
   * Returns filled values as properties object (ConfigurationMap on BE is preferred)
   *
   * TODO: multiple properties
   *
   * @return {object} [description]
   */
  getProperties() {
    const formInstance = new FormInstance(this.props.formInstance.getDefinition(), this.getValues());
    //
    return formInstance.getProperties();
  }

  render() {
    const { formInstance, rendered, showLoading, readOnly, useDefaultValue } = this.props;
    //
    if (!rendered || !formInstance) {
      return null;
    }
    //
    if (showLoading) {
      return (
        <Basic.Loading isStatic showLoading/>
      );
    }
    if (formInstance.getAttributes().size === 0) {
      return (
        <Basic.Alert level="info" text={ this.i18n('attributes.empty') } className="no-margin"/>
      );
    }

    return (
      <span>
        {
          formInstance.getAttributes().map(attribute => {
            const component = attributeManager.getFormComponent(attribute);
            if (!component) {
              return (
                <Basic.LabelWrapper label={attribute.name}>
                  <Basic.Alert
                    level="warning"
                    text={ this.i18n('persistentType.unsupported.title', { name: attribute.persistentType, face: attribute.faceType }) }
                    className="no-margin"/>
                </Basic.LabelWrapper>
              );
            }
            //
            const FormValueComponent = component.component;
            const ManagerType = component.manager;
            return (
              <FormValueComponent
                ref={ attribute.code }
                formDefinition={ this.getFormDefinition() }
                attribute={ attribute }
                values={ formInstance.getValues(attribute.code) }
                readOnly={ readOnly }
                useDefaultValue={ useDefaultValue }
                manager={ ManagerType ? new ManagerType() : null }/>
            );
          })
        }
      </span>
    );
  }
}

EavForm.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * FormInstance (definition + values)
   */
  formInstance: PropTypes.object,
  /**
   * ReadOnly form
   */
  readOnly: PropTypes.bool,
  /**
   * Use default value as filled value
   */
  useDefaultValue: PropTypes.bool
};
EavForm.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  formInstance: null,
  readOnly: false,
  useDefaultValue: false
};
