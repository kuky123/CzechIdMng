import React from 'react';
import PropTypes from 'prop-types';
import EnumSelectBox from '../EnumSelectBox/EnumSelectBox';

/**
 * Select boolean value
 *
 * Warning: string representation is needed (false value not work as selected value for react-select clearable functionality)
 *
 * @author Radek Tomiška
 */
export default class BooleanSelectBox extends EnumSelectBox {

  /**
   * Default options from localization
   *
   * @return {arrayOf(object)}
   */
  getDefaultOptions() {
    return [
      // Warning: string representation is needed (false value not work as selected value for react-select clearable functionality)
      { value: 'true', niceLabel: this.i18n('label.yes') },
      { value: 'false', niceLabel: this.i18n('label.no') }
    ];
  }

  getOptions() {
    const options = this.props.options || this.getDefaultOptions();
    const results = [];
    for (const item in options) {
      if (!options.hasOwnProperty(item)) {
        continue;
      }
      results.push(this.itemRenderer(options[item]));
    }
    return results;
  }

  _findNiceLabel(value) {
    if (!value) {
      return null;
    }
    const options = this.props.options || this.getDefaultOptions();
    for (const item in options) {
      if (options[item].value === value) {
        return options[item].niceLabel;
      }
    }
    return null;
  }
}

BooleanSelectBox.propTypes = {
  ...EnumSelectBox.propTypes,
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.string])
};
BooleanSelectBox.defaultProps = {
  ...EnumSelectBox.defaultProps
};
