import React from 'react';
//
import * as Basic from '../../basic';
import TextFormAttributeRenderer from './TextFormAttributeRenderer';

/**
 * TextArea form value component
 *
 * @author Radek Tomiška
 */
export default class TextAreaFormAttributeRenderer extends TextFormAttributeRenderer {

  /**
   * Returns true, when multi value mode is supported
   *
   * @return {boolean}
   */
  supportsMultiple() {
    return false;
  }

  renderSingleInput() {
    const { attribute, values } = this.props;
    //
    if (attribute.confidential) {
      return super.renderSingleInput();
    }
    return (
      <Basic.TextArea
        ref={ TextFormAttributeRenderer.INPUT }
        label={ this.getLabel() }
        value={ this.toInputValue(values) }
        placeholder={ this.getPlaceholder() }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }
        validation={ this.getInputValidation() }
        required={ this.isRequired() }/>
    );
  }

}
