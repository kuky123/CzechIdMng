import React, { PropTypes } from 'react';
//
import * as Basic from '../../basic';
import { FormAttributeManager } from '../../../redux';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';
//
const attributeManager = new FormAttributeManager();

/**
 * Component renders form by given form attributes without instance of definition
 * and filled values
 *
 * @author Radek Tomiška
 * @author Ondrej Kopr
 */
export default class EavAttributeForm extends Basic.AbstractContextComponent {

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return 'component.advanced.EavAttributeForm';
  }

  /**
   * Returns true, when form is valid, otherwise false
   *
   * @return {Boolean} [description]
   */
  isValid() {
    const { formAttributes } = this.props;
    let isAllValid = true;
    formAttributes.forEach(attribute => {
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
   * Returns object with filled form values (code => value)
   *
   * @return {Object(attributeCode: attributeValue)}
   */
  getValues() {
    const { formAttributes } = this.props;
    const filledFormValues = {};
    //
    formAttributes.forEach(attribute => {
      const formComponent = this.refs[attribute.code];
      if (!formComponent) {
        // unsupported persistentType
        return true;
      }
      filledFormValues[attribute.code] = formComponent.refs[AbstractFormAttributeRenderer.INPUT].getValue();
    });
    return filledFormValues;
  }

  render() {
    const {
      formAttributes,
      rendered,
      showLoading,
      readOnly,
      localizationKey,
      localizationModule,
      localizationType,
      useDefaultValue
    } = this.props;
    //
    if (!rendered || !formAttributes) {
      return null;
    }
    //
    if (showLoading) {
      return (
        <Basic.Loading isStatic showLoading/>
      );
    }
    if (formAttributes.size === 0) {
      return (
        <Basic.Alert level="info" text={ this.i18n('attributes.empty') } className="no-margin"/>
      );
    }
    //
    // values are not initialized
    return (
      <span>
        {
          formAttributes.map(attribute => {
            const component = attributeManager.getFormComponent(attribute);
            if (localizationKey) {
              // if localizationKey is defined we must fake formDefinition.code
              attribute._embedded.formDefinition = {
                ...attribute._embedded.formDefinition,
                code: localizationKey
              };
            }
            attribute._embedded.formDefinition = {
              ...attribute._embedded.formDefinition,
              module: localizationModule,
              type: localizationType
            };
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
            let value = {};
            if (useDefaultValue && attribute.defaultValue) {
              value = { value: attribute.defaultValue};
            }
            return (
              <FormValueComponent
                ref={ attribute.code }
                uiKey={ `form-attribute-${attribute.code}` }
                attribute={ attribute }
                values={ value }
                readOnly={ readOnly }
                manager={ ManagerType ? new ManagerType() : null }/>
            );
          })
        }
      </span>
    );
  }
}

EavAttributeForm.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  formAttributes: PropTypes.object,
  readOnly: PropTypes.bool,
  localizationKey: PropTypes.string,
  localizationModule: PropTypes.string,
  localizationType: PropTypes.string,
  /**
   * Use default value as filled value
   */
  useDefaultValue: PropTypes.bool
};
EavAttributeForm.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  formAttributes: null,
  readOnly: false,
  localizationKey: null,
  localizationModule: 'core',
  localizationType: null,
  useDefaultValue: true
};
