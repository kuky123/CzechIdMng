import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../basic';

/**
 * Component with two TextField and password estimator.
 * TODO: - isValid method is not supported
 *
 * @author Ondřej Kopr
 */
class PasswordField extends Basic.AbstractFormComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      passwordForValidation: null
    };
  }

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return 'component.advanced.PasswordField';
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.newPassword !== nextProps.newPassword) {
      this.refs.newPassword.setValue(nextProps.newPassword);
      this._updatePasswordForValidation(nextProps.newPassword);
      this.refs.newPasswordAgain.setValue(nextProps.newPasswordAgain);
    }
  }

  getValue() {
    return this.refs.newPassword.getValue();
  }

  setValue(value) {
    this.refs.newPassword.setValue(value);
    this.refs.newPasswordAgain.setValue(value);
  }

  /**
   * Focus input field
   */
  focus() {
    this.refs.newPassword.focus();
  }

  _updatePasswordForValidation(value) {
    let passwordForValidation = '';

    // if exist value, set passwordForValidation, other way set empty string
    if (value) {
      passwordForValidation = value;
    }

    this.setState({
      passwordForValidation
    });
  }

  validate(showValidationError) {
    const showValidations = showValidationError != null ? showValidationError : true;
    if (!this.refs.newPassword.validate() || !this.refs.newPasswordAgain.validate()) {
      return false;
    }

    return showValidations;
  }

  _validatePassword(property, onlyValidate, value, result) {
    if (property === 'newPasswordAgain') {
      this._updatePasswordForValidation(this.refs.newPassword.getValue());
    }

    if (onlyValidate) {
      this.refs[property].validate();
      return result;
    }
    if (result.error) {
      return result;
    }
    const opositeValue = this.refs[property].getValue();
    if (opositeValue !== value) {
      return {error: {key: 'passwords_not_same'}};
    }
    return result;
  }

  _newPasswordLabel(label) {
    if (label !== null && label !== undefined) {
      return label;
    }
    // default label
    return this.i18n('newPassword.label');
  }

  _newPasswordAgainLabel(label) {
    if (label !== null && label !== undefined) {
      return label;
    }
    // default label
    return this.i18n('newPasswordAgain.label');
  }

  render() {
    const {
      newPassword,
      newPasswordAgain,
      type,
      required,
      readOnly,
      rendered,
      labelSpan,
      componentSpan,
      newPasswordLabel,
      newPasswordAgainLabel,
      hidden,
      helpBlock,
      disabled
    } = this.props;
    const { passwordForValidation } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    return (
      <div className={ hidden ? 'hidden' : '' }>
        <Basic.TextField
          type={ type }
          ref="newPassword"
          value={ newPassword }
          validate={ this._validatePassword.bind(this, 'newPasswordAgain', true) }
          readOnly={ readOnly }
          label={ this._newPasswordLabel(newPasswordLabel) }
          required={ required }
          labelSpan={ labelSpan }
          componentSpan={ componentSpan }
          disabled={disabled}
          style={{ marginBottom: 0 }}/>
        <div className="form-group" style={{ margin: 0 }}>
          {
            !labelSpan
            ||
            <span className={labelSpan}></span>
          }
          <Basic.PasswordStrength
            max={5}
            initialStrength={0}
            opacity={1}
            value={passwordForValidation}
            isIcon={false}
            spanClassName={componentSpan} />
        </div>
        <Basic.TextField
          type={type}
          ref="newPasswordAgain"
          value={ newPasswordAgain }
          readOnly={ readOnly }
          validate={ this._validatePassword.bind(this, 'newPassword', false) }
          label={ this._newPasswordAgainLabel(newPasswordAgainLabel) }
          required={required}
          labelSpan={ labelSpan }
          componentSpan={ componentSpan }
          disabled={disabled}
          helpBlock={ helpBlock }/>
      </div>
    );
  }
}

PasswordField.propTypes = {
  newPassword: PropTypes.string,
  newPasswordAgain: PropTypes.string,
  readOnly: PropTypes.bool,
  rendered: PropTypes.bool,
  hidden: PropTypes.bool,
  type: PropTypes.string,
  required: PropTypes.bool,
  newPasswordLabel: PropTypes.string,
  newPasswordAgainLabel: PropTypes.string,
  disabled: PropTypes.bool
};

PasswordField.defaultProps = {
  newPassword: '',
  readOnly: false,
  newPasswordAgain: '',
  type: 'password',
  required: true,
  hidden: false,
  rendered: true,
  disabled: false
};


export default PasswordField;
