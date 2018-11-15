import React from 'react';
//
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';
import SelectBoxFormAttributeRenderer from './SelectBoxFormAttributeRenderer';
import RoleCatalogueSelect from '../RoleCatalogueSelect/RoleCatalogueSelect';

/**
 * Tree select component
 * - TODO: supports multiple attributes
 * - TODO: validation
 *
 * @author Radek Tomiška
 */
export default class RoleCatalogueSelectFormAttributeRenderer extends SelectBoxFormAttributeRenderer {

  constructor(props) {
    super(props);
    this.state = {
      ...this.state
    };
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
   * Returns true, when confidential mode is supported
   *
   * @return {boolean}
   */
  supportsConfidential() {
    return false;
  }

  renderSingleInput() {
    const { attribute, values, uiKey } = this.props;
    //
    return (
      <RoleCatalogueSelect
        ref={ AbstractFormAttributeRenderer.INPUT }
        uiKey={ uiKey || `form-attribute-${attribute.code}` }
        manager={ this.getManager() }
        header={ this.getLabel() }
        label={ this.getLabel() }
        placeholder={ this.getPlaceholder() }
        helpBlock={ this.getHelpBlock() }
        value={ !attribute.multiple ? this.toInputValue(values) : this.toInputValues(values) }
        readOnly={ this.isReadOnly() }
        required={ this.isRequired() }
        multiSelect={ attribute.multiple }/>
    );
  }
}
