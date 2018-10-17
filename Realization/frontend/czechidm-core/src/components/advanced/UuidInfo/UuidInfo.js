import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import * as Basic from '../../basic';

const MAX_UUID_LENGTH = 7;

/**
 * Shows uuid - shorten by default - full view in popover for copy
 *
 * TODO: readonly textfiled with selected uuid value - for copy / paste
 *
 * @author Radek Tomiška
 */
export default class UuidInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * Shortens given value, if value is type of string, returns value otherwise.
   *
   * @param  {object} value
   * @return {string|object}
   */
  shorten(value) {
    if (typeof value === 'string') {
      return value.substring(0, MAX_UUID_LENGTH);
    }
    return value;
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
        trigger={['click']}
        value={<span className="uuid-info-popover-value">{value}</span>}>
        {
          <span
            className={ classNames }
            style={ style }>
            <Basic.Button level="link" style={{ padding: 0 }} onClick={ (e) => e.preventDefault() }>{ this.shorten(value) }</Basic.Button>
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
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number
  ]).isRequired
};
UuidInfo.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps
};
