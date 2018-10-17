import React from 'react';
import PropTypes from 'prop-types';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import _ from 'lodash';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

/**
 * Overlay with tooltip
 *
 * @author Radek Tomiška
 */
export default class BasicTooltip extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  /**
   * Shows tooltip
   */
  show() {
    const { rendered, children } = this.props;
    if (rendered && children) {
      this.refs.popover.show();
    }
  }

  render() {
    const { id, rendered, children, value, delayShow, placement, trigger, showLoading, ...others } = this.props;
    if (!rendered || !children) {
      return null;
    }
    let _id = id;
    if (!_id) {
      _id = _.uniqueId('tooltip_');
    }

    return (
      <OverlayTrigger
        ref="popover"
        trigger={trigger}
        placement={placement}
        overlay={
          // tooltip without value is not rendered, but we need to prepare empty tooltip for rerendering (i.e. when Texfield validation changes)
          <Tooltip id={_id} style={{ display: value ? '' : 'none' }}>
            {
              showLoading
              ?
              <Icon value="Refresh" showLoading/>
              :
              value
            }
          </Tooltip>
        }
        delayShow={delayShow}>
        { children }
      </OverlayTrigger>
    );
  }
}

BasicTooltip.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Tooltip identifier
   */
  id: PropTypes.string,
  /**
   * Specify which action or actions trigger tooltip visibility
   */
  trigger: PropTypes.arrayOf(PropTypes.oneOf(['click', 'hover', 'focus'])),
  /**
   * tooltip value / text
   */
  value: PropTypes.string,
  /**
   * Tooltip position
   */
  placement: PropTypes.oneOf(['top', 'bottom', 'right', 'left']),
  /**
   * A millisecond delay amount before showing the Tooltip once triggered.
   */
  delayShow: PropTypes.number
};

BasicTooltip.defaultProps = {
  ...AbstractComponent.defaultProps,
  placement: 'bottom',
  trigger: ['hover', 'focus'],
  delayShow: 1000
};
