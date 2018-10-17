import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

/**
 * Badge box
 *
 * @author Radek Tomiška
 */
class Badge extends AbstractComponent {

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
      'badge',
      'badge-' + (level === 'error' ? 'danger' : level),
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

Badge.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Badge level / css / class
   */
  level: PropTypes.oneOf(['default', 'success', 'warning', 'info', 'danger', 'error']),
  /**
   * Badge text content
   */
  text: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.number
  ]),
  /**
   * Badge text content (text alias - text has higher priority)
   */
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.number
  ])
};

Badge.defaultProps = {
  ...AbstractComponent.defaultProps,
  level: 'default'
};

export default Badge;
