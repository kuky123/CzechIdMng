import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';
import Button from '../Button/Button';

/**
 * Alert box
 *
 * @author Radek Tomiška
 */
class Alert extends AbstractComponent {

  constructor(props) {
    super(props);
    this.state = {
      closed: false
    };
  }

  _onClose(event) {
    const { onClose } = this.props;
    this.setState({
      closed: true
    }, () => {
      if (onClose) {
        onClose(event);
      }
    });
  }

  render() {
    const { level, title, text, className, icon, onClose, rendered, showLoading, children, style, buttons, showHtmlText } = this.props;
    const { closed } = this.state;
    if (!rendered || closed || (!text && !title && !children)) {
      return null;
    }
    const classNames = classnames(
      'alert',
      'alert-' + (level === 'error' ? 'danger' : level),
      { 'alert-dismissible': (onClose !== null) },
      { 'text-center': showLoading },
      className
    );
    if (showLoading) {
      return (
        <div className={classNames} style={style}>
          <Icon type="fa" icon="refresh" showLoading/>
        </div>
      );
    }

    return (
      <div className={classNames} style={style}>
        {
          !onClose
          ||
          <Button ref="close" type="button" className="close" aria-label="Close" onClick={this._onClose.bind(this)}><span aria-hidden="true">&times;</span></Button>
        }
        {
          !icon
          ||
          <div className="alert-icon"><Icon icon={icon}/></div>
        }
        <div className={icon ? 'alert-desc' : ''}>
          {
            !title
            ||
            <div className="alert-title">{title}</div>
          }
          {showHtmlText ? <span dangerouslySetInnerHTML={{ __html: text}}/> : text}
          {children}
          {
            (!buttons || buttons.length === 0)
            ||
            <div className="buttons">
              {buttons}
            </div>
          }
        </div>
      </div>
    );
  }
}

Alert.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Alert level / css / class
   */
  level: PropTypes.oneOf(['success', 'warning', 'info', 'danger', 'error']),
  /**
   * Alert strong title content
   */
  title: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node
  ]),
  /**
   * Alert text content
   */
  text: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node
  ]),
  /**
   * glyphicon suffix name
   */
  icon: PropTypes.string,
  /**
   * Close function - if it's set, then close icon is shown and this method is called on icon click
   */
  onClose: PropTypes.func,
  /**
   * Alert action buttons
   */
  buttons: PropTypes.arrayOf(PropTypes.node),
  /**
   * Show text as html (dangerouslySetInnerHTML)
   */
  showHtmlText: PropTypes.bool
};

Alert.defaultProps = {
  ...AbstractComponent.defaultProps,
  level: 'info',
  onClose: null,
  buttons: [],
  showHtmlText: false
};

export default Alert;
