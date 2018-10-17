import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

/**
 * Simple date formatter
 */
class DateValue extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, value, format, title } = this.props;
    if (!rendered || !value) {
      return null;
    }

    let _formattedValue = moment(value);
    if (format) {
      _formattedValue = _formattedValue.format(format);
    } else {
      _formattedValue = _formattedValue.format();
    }

    return (
      <span className="basic-date-value" title={ title }>
        {_formattedValue}
      </span>
    );
  }
}

DateValue.propTypes = {
  /**
   * If component is rendered on page
   */
  rendered: PropTypes.bool,
  /**
   * Date value in iso-8601 format
   */
  value: PropTypes.string,
  /**
   * Date time format
   */
  format: PropTypes.string
};

DateValue.defaultProps = {
  rendered: true
};


export default DateValue;
