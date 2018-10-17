import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

/**
 * Label box
 *
 * @author Radek Tomiška
 */
class Label extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  _onClose(event) {
    const { onClose } = this.props;
    if (!onClose) {
      return;
    }
    onClose(event);
  }

  render() {
    const { level, title, text, value, className, icon, rendered, showLoading, ...others } = this.props;
    const _text = text || value;
    if (!rendered || !_text) {
      return null;
    }
    const classNames = classnames(
      'label',
      'label-' + (level === 'error' ? 'danger' : level),
      className
    );
    return (
      <span className={classNames} title={title} {...others}>
        {
          showLoading
          ?
          <Icon type="fa" icon="refresh" showLoading/>
          :
          _text
        }
      </span>
    );
  }
}

Label.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Label level / css / class
   */
  level: PropTypes.oneOf(['default', 'success', 'warning', 'info', 'danger', 'error', 'primary']),
  /**
   * Label text content
   */
  text: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.number
  ]),
  /**
   * Label text content (text alias - text has higher priority)
   */
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.number
  ])
};

Label.defaultProps = {
  ...AbstractComponent.defaultProps,
  level: 'default'
};

export default Label;
