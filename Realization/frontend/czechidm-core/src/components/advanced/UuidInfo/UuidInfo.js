import React, { PropTypes } from 'react';
import classnames from 'classnames';
//
import * as Basic from '../../basic';

const MAX_UUID_LENGTH = 7;

/**
 * Shows uuid - shorten by default - full view in popover for copy
 *
 * @author Radek Tomiška
 */
export default class UuidInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const { rendered, showLoading, value, style, className } = this.props;
    //
    if (!rendered) {
      return null;
    }
    if (showLoading) {
      return (
        <Basic.Icon value="refresh" showLoading/>
      );
    }
    // we don't have anything to render
    if (!value) {
      return null;
    }
    const classNames = classnames(
      'uuid-info',
      className
    );
    //
    return (
      <Basic.Popover
        trigger="click"
        value={<span className="uuid-info-popover-value">{value}</span>}>
        {
          <span
            className={ classNames }
            style={ style }>
            <a href="#" onClick={ (e) => e.preventDefault() }>{ value.substring(0, MAX_UUID_LENGTH) }</a>
          </span>
        }
      </Basic.Popover>
    );
  }
}

UuidInfo.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * uuid, entity identifier
   */
  value: PropTypes.string.isRequired,
};
UuidInfo.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps
};