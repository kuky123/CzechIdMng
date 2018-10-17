import React from 'react';
import PropTypes from 'prop-types';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Label from '../Label/Label';
import Icon from '../Icon/Icon';

/**
 * Simple enum formatter
 *
 * @author Radek Tomiška
 */
class EnumValue extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, value, style, label, level } = this.props;
    const enumClass = this.props.enum;
    //
    if (!rendered || !value) {
      return null;
    }

    let content = (
      <span>{ label || value }</span>
    );
    if (value && enumClass) {
      content = (
        <span>{ label || enumClass.getNiceLabel(value) }</span>
      );
      //
      const icon = enumClass.getIcon(value);
      if (icon) {
        content = (
          <span>
            <Icon value={icon} style={{ marginRight: 3 }}/>
            { content }
          </span>
        );
      }
      const _level = level || enumClass.getLevel(value);
      if (_level) {
        content = (
          <Label style={ style } level={ _level } text={ content }/>
        );
      }
    }
    return content;
  }
}

EnumValue.propTypes = {
  /**
   * If component is rendered on page
   */
  rendered: PropTypes.bool,
  /**
   * enumarition - see domain or enums package
   */
  enum: PropTypes.func.isRequired,
  /**
   * enum value
   */
  value: PropTypes.string,
  /**
   * Custom label - label will be used by enum value, but label will be this one.
   * If no label is given, then localized label by enum value will be used.
   */
  label: PropTypes.string,
  /**
   * Custom level. If no level is given, then level by enum value will be used.
   */
  level: PropTypes.oneOf(['default', 'success', 'warning', 'info', 'danger', 'link', 'primary', 'error']),
};

EnumValue.defaultProps = {
  rendered: true,
  label: null
};


export default EnumValue;
