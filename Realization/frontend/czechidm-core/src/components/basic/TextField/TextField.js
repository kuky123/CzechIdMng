import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import Joi from 'joi';
//
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import Tooltip from '../Tooltip/Tooltip';
import Icon from '../Icon/Icon';
import Button from '../Button/Button';

const CONFIDENTIAL_VALUE = '*****';

class TextField extends AbstractFormComponent {

  constructor(props) {
    super(props);
    this.state = {
      ...this.state,
      confidentialState: {
        showInput: false
      }
    };
  }

  getComponentKey() {
    return 'component.basic.TextField';
  }

  getValidationDefinition(required) {
    const { min, max } = this.props;
    let validation = super.getValidationDefinition(min ? true : required);

    if (min && max) {
      validation = validation.concat(Joi.string().min(min).max(max));
    } else if (min) {
      validation = validation.concat(Joi.string().min(min));
    } else if (max) {
      if (!required) {
        // if set only max is necessary to set allow null and empty string
        validation = validation.concat(Joi.string().max(max).allow(null).allow(''));
      } else {
        // if set prop required it must not be set allow null or empty string
        validation = validation.concat(Joi.string().max(max));
      }
    }

    return validation;
  }

  getRequiredValidationSchema(validation = null) {
    const { type } = this.props;
    // join validation was given
    if (type === 'number' || (validation && validation._type === 'number')) {
      return Joi.number().required();
    }
    return Joi.string().required();
  }

  /**
   * Focus input field
   */
  focus() {
    this.refs.input.focus();
  }

  onChange(event) {
    super.onChange(event);
    if (this.refs.popover) {
      this.refs.popover.show();
    }
    this.setState( {
      confidentialState: {
        showInput: true
      }
    });
  }

  /**
   * Show / hide confidential. Call after save form.
   *
   * @param  {bool} showInput
   */
  openConfidential(showInput) {
    this.setState({
      value: CONFIDENTIAL_VALUE,
      confidentialState: {
        showInput
      }
    });
  }

  /**
   * Show / hide input istead confidential wrapper
   *
   * @param  {bool} showInput
   */
  toogleConfidentialState(showInput, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this._showConfidentialWrapper()) {
      return;
    }
    //
    this.setState({
      value: null,
      confidentialState: {
        showInput
      }
    }, () => {
      this.focus();
    });
  }

  /**
   * Returns filled value. Depends on confidential property
   *
   * @return {string} filled value or undefined, if confidential value is not edited
   */
  getValue() {
    const { confidential } = this.props;
    const { confidentialState } = this.state;
    //
    if (confidential) {
      // preserve previous value
      if (!confidentialState.showInput) {
        return undefined;
      }
      return super.getValue() || ''; // we need to know, when clear confidential value in BE
    }
    // return filled value
    return super.getValue();
  }

  /**
   * Clears filled values
   */
  clearValue() {
    this.setState({ value: null }, () => { this.validate(); });
  }

  /**
   * Return true, when confidential wrapper should be shown
   *
   * @return {bool}
   */
  _showConfidentialWrapper() {
    const { required, confidential } = this.props;
    const { value, confidentialState, disabled, readOnly } = this.state;
    return confidential && !confidentialState.showInput && (!required || value) && !disabled && !readOnly;
  }

  getBody(feedback) {
    const { type, labelSpan, label, componentSpan, placeholder, style, required } = this.props;
    const { value, disabled, readOnly } = this.state;
    //
    const className = classNames(
      'form-control',
      { 'confidential': this._showConfidentialWrapper() }
    );
    const labelClassName = classNames(labelSpan, 'control-label');
    let showAsterix = false;
    if (required && !feedback && !this._showConfidentialWrapper()) {
      showAsterix = true;
    }
    //
    // value and readonly properties depends on confidential wrapper
    let _value = '';
    if (value !== undefined && value !== null) {
      _value = value;
    }
    let _readOnly = readOnly;
    if (this._showConfidentialWrapper()) {
      if (value) {
        _value = CONFIDENTIAL_VALUE; // asterix will be shown, when value is filled
      } else {
        _value = '';
      }
      _readOnly = true;
    }
    // input component
    const component = (
      <input
        ref="input"
        type={type}
        className={className}
        disabled={disabled}
        placeholder={placeholder}
        onChange={this.onChange.bind(this)}
        onClick={this.toogleConfidentialState.bind(this, true)}
        value={_value}
        style={style}
        readOnly={_readOnly}/>
    );
    //
    // show confidential wrapper, when confidential value could be changed
    let confidentialWrapper = component;
    if (this._showConfidentialWrapper()) {
      confidentialWrapper = (
        <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={this.i18n('confidential.edit')}>
          <div className="input-group">
            { component }
            <span className="input-group-btn">
              <Button
                type="button"
                level="default"
                className="btn-sm"
                style={{ marginTop: '0px', height: '34px' }}
                onClick={this.toogleConfidentialState.bind(this, true)}>
                <Icon icon="fa:edit"/>
              </Button>
            </span>
          </div>
        </Tooltip>
      );
    }

    return (
      <div className={ showAsterix ? 'has-feedback' : '' }>
        {
          !label
          ||
          <label
            className={ labelClassName }>
            { label }
            { this.renderHelpIcon() }
          </label>
        }
        <div className={componentSpan} style={{ whiteSpace: 'nowrap' }}>
          <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={ this.getTitle() }>
            <span>
              {confidentialWrapper}
              {
                (feedback || !showAsterix)
                ||
                <span className="form-control-feedback" style={{color: 'red', zIndex: 0}}>*</span>
              }
            </span>
          </Tooltip>
          { !label ? this.renderHelpIcon() : null }
          { this.renderHelpBlock() }
        </div>
      </div>
    );
  }
}

TextField.propTypes = {
  ...AbstractFormComponent.propTypes,
  type: PropTypes.string,
  placeholder: PropTypes.string,
  help: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.object
  ]),
  min: PropTypes.number,
  max: PropTypes.number,
  /**
   * Confidential text field - if it is filled, then shows asterix only and supports to add new value
   */
  confidential: PropTypes.bool
};

TextField.defaultProps = {
  ...AbstractFormComponent.defaultProps,
  type: 'text',
  confidential: false
};

export default TextField;
