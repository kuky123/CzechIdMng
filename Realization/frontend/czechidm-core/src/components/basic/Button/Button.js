import React, { PropTypes } from 'react';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';
import Tooltip from '../Tooltip/Tooltip';

/**
 * TODO: Improvent:
 * - add button size (className is abused now)
 *
 * @author Radek Tomiška
 */
class Button extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  focus() {
    this.refs.button.focus();
  }

  render() {
    const {
      level,
      text,
      className,
      children,
      showLoading,
      showLoadingIcon,
      showLoadingText,
      disabled,
      hidden,
      type,
      rendered,
      title,
      titlePlacement,
      titleDelayShow,
      style,
      onClick,
      onDoubleClick,
      icon
    } = this.props;
    //
    if (!rendered) {
      return null;
    }

    const classNames = classnames(
      'btn',
      'btn-' + level,
      { hidden },
      className
    );
    let _showLoadingText = children;
    if (showLoadingText !== null) {
      _showLoadingText = showLoadingText;
    }
    //
    return (
      <Tooltip placement={titlePlacement} value={title} delayShow={titleDelayShow}>
        <span>
          <button
            ref="button"
            type={ type ? type : 'button' }
            disabled={ disabled || showLoading }
            className={ classNames }
            style={ style }
            onClick={ onClick }
            onDoubleClick={ onDoubleClick }>
            {
              showLoading
              ?
              <span>
                {
                  showLoadingIcon
                  ?
                  <Icon type="fa" icon="refresh" showLoading/>
                  :
                  <Icon value={ icon } className="icon-left"/>
                }
                {
                  showLoadingIcon && _showLoadingText
                  ?
                  '\u00a0'
                  :
                  null
                }
                {_showLoadingText}
              </span>
              :
              <span>
                <Icon value={ icon } className="icon-left" style={ (text || (children && children.length > 0)) ? { marginRight: 5 } : {} }/>
                { text }
                { children }
              </span>
            }
          </button>
        </span>
     </Tooltip>
    );
  }
}

Button.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Button level / css / class
   */
  level: PropTypes.oneOf(['default', 'success', 'warning', 'info', 'danger', 'link', 'primary']),
  /**
   * When showLoading is true, then showLoadingIcon is shown
   */
  showLoadingIcon: PropTypes.bool,
  /**
   *  When showLoading is true, this text will be shown
   */
  showLoadingText: PropTypes.string,
  /**
   * Title position
   */
  titlePlacement: PropTypes.oneOf(['top', 'bottom', 'right', 'left']),
  /**
   * Title show delay
   */
  titleDelayShow: PropTypes.number,
  /**
   * Button icon
   */
  icon: PropTypes.string,
  /**
   * On click node callback
   */
  onClick: PropTypes.func,
  /**
   * On double click node callback
   */
  onDoubleClick: PropTypes.func
};
Button.defaultProps = {
  ...AbstractComponent.defaultProps,
  type: 'button',
  level: 'default',
  hidden: false,
  showLoadingIcon: false,
  showLoadingText: null,
  titlePlacement: 'right',
  icon: null
};

export default Button;
