import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import Joi from 'joi';
//
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import Tooltip from '../Tooltip/Tooltip';

class TextArea extends AbstractFormComponent {

  constructor(props) {
    super(props);
  }

  getValidationDefinition(required) {
    const { min, max } = this.props;
    let validation = super.getValidationDefinition(min ? true : required);

    if (min && max) {
      validation = validation.concat(Joi.string().min(min).max(max).disallow(''));
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

  getRequiredValidationSchema() {
    return Joi.string().required();
  }

  /**
   * Focus input field
   */
  focus() {
    this.refs.input.focus();
  }

  getBody(feedback) {
    const { labelSpan, label, componentSpan, placeholder, style, required } = this.props;
    //
    const className = classNames('form-control');
    const labelClassName = classNames(labelSpan, 'control-label');
    let showAsterix = false;
    if (required && !feedback) {
      showAsterix = true;
    }
    //
    return (
      <div className={ showAsterix ? 'has-feedback' : ''}>
        {
          !label
          ||
          <label
            className={labelClassName}>
            {label}
            { this.renderHelpIcon() }
          </label>
        }

        <div className={componentSpan}>
          <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={ this.getTitle() }>
            <span>
              <textarea
                ref="input"
                className={className}
                title={this.getValidationResult() != null ? this.getValidationResult().message : ''}
                disabled={this.state.disabled}
                placeholder={placeholder}
                rows={this.props.rows}
                style={style}
                readOnly={this.state.readOnly}
                onChange={this.onChange}
                value={this.state.value || ''}/>
              {
                feedback
                ||
                !showAsterix
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

TextArea.propTypes = {
  ...AbstractFormComponent.propTypes,
  placeholder: PropTypes.string,
  rows: PropTypes.number,
  min: PropTypes.number,
  max: PropTypes.number
};

TextArea.defaultProps = {
  ...AbstractFormComponent.defaultProps,
  rows: 3
};

export default TextArea;
