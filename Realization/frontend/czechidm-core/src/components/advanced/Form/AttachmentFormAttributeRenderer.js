import React from 'react';
import _ from 'lodash';
import classNames from 'classnames';
//
import * as Basic from '../../basic';
import AbstractFormAttributeRenderer from './AbstractFormAttributeRenderer';
import UuidFormAttributeRenderer from './UuidFormAttributeRenderer';
import Dropzone from '../Dropzone/Dropzone';
import { AttachmentService } from '../../../services';

const attachmentService = new AttachmentService();

/**
 * Attachment form value component
 * - supports single file only for now
 * - TODO: support multiple files
 * - TODO: download
 * - TODO: validation support (now is validation on input)
 * - TODO: clearable
 *
 * @author Radek Tomiška
 */
export default class AttachmentFormAttributeRenderer extends UuidFormAttributeRenderer {

  constructor(props) {
    super(props);
    this.state = {
      ...this.state,
      isLoading: false,
      showValidationError: false
    };
  }

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

  isValid() {
    if (this.state.isLoading === true) {
      // value still loading ... component is invalid
      return false;
    }
    const isValid = super.isValid();
    this.setState({
      showValidationError: !isValid
    });
    //
    return isValid;
  }

  /**
   * Fill form value field by persistent type from input value - uuid + string text, which will be used as "saved" placeholder.
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
   * Fill form value field by persistent type from input value
   *
   * @param  {FormValue} formValue - form value
   * @param  {object} formComponent value
   * @return {FormValue}
   */
  fillFormValue(formValue, rawValue) {
    formValue.uuidValue = rawValue;
    // common value can be used without persistent type knowlege (e.g. conversion to properties object)
    formValue.value = formValue.uuidValue;
    if (formValue.value) {
      // fill string value - will be used as "saved" placeholder
      formValue.shortTextValue = this.refs.dropzone.getFile() !== null ? this.refs.dropzone.getFile().name : null;
    } else {
      formValue.shortTextValue = null;
    }
    // TODO: validations for uuid
    return formValue;
  }

  /**
   * Upload temp file to server - returned attachment uuid will be used as value
   *
   * @return {[type]} [description]
   */
  _onDrop() {
    this.setState({
      isLoading: true,
      showValidationError: false
    }, () => {
      const formData = new FormData();
      const file = this.refs.dropzone.getFile();
      formData.append( 'fileName', file.name);
      // TODO: mimetype
      formData.append( 'data', file );
      attachmentService
        .upload(formData)
        .then((uploadedAttachment) => {
          this.setState({
            isLoading: false
          }, () => {
            this.addMessage({
              message: this.i18n('message.success.upload', { record: file.name })
            });
            this.refs[AbstractFormAttributeRenderer.INPUT].setValue(uploadedAttachment.id);
          });
        })
        .catch(uploadError => {
          this.setState({
            isLoading: false
          });
          this.addError(uploadError);
        });
    });
  }

  getContent() {
    const { values } = this.props;
    const { showValidationError } = this.state;
    //
    let formValue = null;
    if (values && _.isArray(values)) {
      if (values.length > 0) {
        formValue = values[0];
      }
    } else {
      formValue = values;
    }
    const result = [];
    if (formValue === null) {
      const value = super.getPlaceholder();
      if (value) {
        result.push(<span>{ value }</span>);
      }
    } else {
      result.push(<span>{ formValue.shortTextValue }</span>);
    }
    if (this.isReadOnly()) {
      result.push(<div>{ `(${ this.i18n('label.readOnly') })` }</div>);
    } else if (result.length === 0) {
      result.push(<span>{ this.i18n('component.basic.Dropzone.infoText') }</span>);
    }
    if (showValidationError) {
      // TODO: tooltip with validation message
      result.unshift(<Basic.Icon icon="warning-sign" className="form-control-feedback" style={{ zIndex: 0 }} />);
    } else if (this.isRequired()) {
      result.push(<span className="form-control-feedback" style={{ color: 'red', zIndex: 0 }}>*</span>);
    }
    //
    return result;
  }

  renderSingleInput() {
    const { values } = this.props;
    const { isLoading, showValidationError } = this.state;
    //
    const style = {};
    if (showValidationError) {
      style.borderColor = '#a94442'; // FIXME: move to less, use variable
    }
    const className = classNames({
      'has-feedback': showValidationError,
      'has-error': showValidationError
    });
    //
    return (
      <Basic.LabelWrapper label={ this.getLabel() } className={ className }>
        <Dropzone
          ref="dropzone"
          multiple={ false }
          readOnly={ this.isReadOnly() }
          onDrop={ this._onDrop.bind(this) }
          showLoading={ isLoading }
          required={ this.isRequired() }
          style={ style }>
          { this.getContent() }
        </Dropzone>

        {/* Attachment uuid is stored in hiden input */}
        <Basic.TextField
          ref={ AbstractFormAttributeRenderer.INPUT }
          value={ this.toInputValue(values) }
          readOnly={ this.isReadOnly() }
          validation={ this.getInputValidation() }
          required={ this.isRequired() }
          hidden/>
      </Basic.LabelWrapper>
    );
  }
}
