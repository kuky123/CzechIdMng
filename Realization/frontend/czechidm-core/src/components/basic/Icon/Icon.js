import React, { PropTypes } from 'react';
import classnames from 'classnames';
import _ from 'lodash';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

export const TYPE_GLYPHICON = 'glyph';
export const TYPE_FONT_AWESOME = 'fa'; // https://fortawesome.github.io/Font-Awesome/examples/

/**
 * Icon
 *
 * @author Radek Tomiška
 */
class Icon extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  /**
   * Returns resolved type and icon from given parameters
   *
   * @param  {string} type  icon type
   * @param  {string} icon  requested icon - could contain type definition e.g. `fa:group`
   * @param  {string} value parameter icon alias
   * @return {{_type, _icon}}  object represents resolved type and icon
   */
  resolveParams(type, icon, value) {
    // value could contain type definition
    //
    const _iconValue = icon || value;
    if (!_iconValue) {
      return {};
    }
    const _iconValues = _iconValue.split(':');
    let _type = type;
    let _icon = _iconValue;
    if (_iconValues.length === 2) {
      _type = _iconValues[0];
      _icon = _iconValues[1];
    }
    return {
      _type,
      _icon
    };
  }

  render() {
    const { type, icon, value, className, rendered, showLoading, color, style, disabled, title } = this.props;
    if (!rendered) {
      return null;
    }
    // value could contain type definition
    const { _type, _icon } = this.resolveParams(type, icon, value);
    // without icon defined returns null
    if (!_icon) {
      return null;
    }
    //
    let classNames;
    if (showLoading) {
      classNames = classnames(
        'fa',
        'fa-refresh',
        'fa-spin',
        className
      );
    } else {
      classNames = classnames(
        { 'glyphicon': _type === TYPE_GLYPHICON},
        { ['glyphicon-' + _icon]: _type === TYPE_GLYPHICON},
        { 'fa': _type === TYPE_FONT_AWESOME},
        { ['fa-' + _icon]: _type === TYPE_FONT_AWESOME},
        { 'disabled': disabled === true },
        className,
      );
    }
    const _style = _.merge({}, style);
    if (color) {
      _style.color = color;
    }
    return (
      <span title={title} className={classNames} aria-hidden="true" style={_style}></span>
    );
  }
}

Icon.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * glyphicon or font-awesome, default glyph
   */
  type: PropTypes.oneOf([TYPE_GLYPHICON, TYPE_FONT_AWESOME]),
  /**
   * glyphicon or font-awesome (by type) suffix name
   */
  icon: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.bool // false => no icon
  ]),
  /**
   * glyphicon or font-awesome (by type) suffix name - alias to icon property, has lower priority
   */
  value: PropTypes.string,
  /**
   * css only
   */
  disabled: PropTypes.bool
};

Icon.defaultProps = {
  ...AbstractComponent.defaultProps,
  type: TYPE_GLYPHICON,
  dibaled: false
};

export default Icon;
