import React from 'react';
import PropTypes from 'prop-types';
import { OverlayTrigger, Popover } from 'react-bootstrap';
import _ from 'lodash';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';
import Tooltip from '../Tooltip/Tooltip';

/**
 * Overlay with popover
 *
 * @author Radek Tomiška
 */
export default class BasicPopover extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  /**
   * Close popover
   */
  close() {
    if (this.refs.popover) {
      this.refs.popover.hide();
    }
  }

  render() {
    const { id, rendered, children, value, text, title, placement, showLoading, trigger, delayShow, level, className, icon, rootClose, ...others } = this.props;
    if (!rendered || (!children)) {
      return null;
    }
    let _id = id;
    if (!_id) {
      _id = _.uniqueId('tooltip_');
    }
    // text - value alias
    const _value = value || text;
    //
    const classNames = classnames(
      'basic-popover',
      'popover-' + level,
      className
    );

    return (
      <OverlayTrigger
        ref="popover"
        trigger={ trigger }
        rootClose={ rootClose }
        placement={ placement }
        overlay={
          <Popover
            id={_id}
            className={classNames}

            title={
              !title
              ?
              null
              :
              <div>
                {
                  !icon
                  ||
                  <div className="basic-popover-icon"><Icon icon={icon}/></div>
                }
                <div className={icon ? 'basic-popover-title' : ''}>
                  { title }
                </div>
              </div>
            }>
            {
              showLoading
              ?
              <Icon value="Refresh" showLoading/>
              :
              _value

            }
          </Popover>
        }
        delayShow={ delayShow }
        { ...others }>
        { children }
      </OverlayTrigger>
    );
  }
}

BasicPopover.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Popover identifier
   */
  id: PropTypes.string,
  /**
   * Popover level / css / class
   */
  level: PropTypes.oneOf(['default', 'warning']),
  /**
   * Popover position
   */
  placement: PropTypes.oneOf(['top', 'bottom', 'right', 'left']),
  /**
   * Popover value / text
   */
  title: PropTypes.string,
  /**
   * Popover value / text
   */
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  /**
   * Popover value / text - alias to value
   */
  text: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  /**
   * Specify which action or actions trigger popover visibility
   */
  trigger: PropTypes.arrayOf(PropTypes.oneOf(['click', 'hover', 'focus'])),
  /**
   * A millisecond delay amount before showing the Popover once triggered.
   */
  delayShow: PropTypes.number,
  /**
   * Specify whether the overlay should trigger onHide when the user clicks outside the overlay
   */
  rootClose: PropTypes.bool
};

BasicPopover.defaultProps = {
  ...AbstractComponent.defaultProps,
  level: 'default',
  placement: 'bottom',
  trigger: ['hover', 'focus', 'click'],
  delayShow: Tooltip.defaultProps.delayShow,
  rootClose: true
};
