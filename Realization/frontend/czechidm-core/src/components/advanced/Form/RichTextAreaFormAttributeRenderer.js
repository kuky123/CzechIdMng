import React from 'react';
//
import RichTextArea from '../RichTextArea/RichTextArea';
import TextFormAttributeRenderer from './TextFormAttributeRenderer';

/**
 * RichTextArea form value component
 *
 * @author Radek Tomiška
 */
export default class RichTextAreaFormAttributeRenderer extends TextFormAttributeRenderer {

  /**
   * Returns true, when multi value mode is supported
   *
   * @return {boolean}
   */
  supportsMultiple() {
    return false;
  }

  /**
   * Returns true, when confidential mode is supported
   *
   * @return {boolean}
   */
  supportsConfidential() {
    return false;
  }

  renderSingleInput() {
    const { values } = this.props;
    //
    return (
      <RichTextArea
        ref={ TextFormAttributeRenderer.INPUT }
        label={ this.getLabel() }
        value={ this.toInputValue(values) }
        placeholder={ this.getPlaceholder() }
        helpBlock={ this.getHelpBlock() }
        readOnly={ this.isReadOnly() }
        required={ this.isRequired() }/>
    );
  }

}
