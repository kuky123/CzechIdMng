import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../basic';

/**
 * Filter buttons (use, clear) mainly for advanced table
 *
 * @author Radek Tomiška
 */
export default class FilterButtons extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  _cancelFilter(event) {
    const { cancelFilter } = this.props;
    if (event) {
      event.preventDefault();
    }
    if (cancelFilter) {
      cancelFilter(event);
    }
  }

  render() {
    const { rendered, showLoading, style, className } = this.props;
    if (!rendered || showLoading) {
      return null;
    }
    //
    return (
      <span style={ style } className={ className }>
        <Basic.Button onClick={this._cancelFilter.bind(this)} style={{ marginRight: 5 }}>
          {this.i18n('button.filter.cancel')}
        </Basic.Button>
        <Basic.Button level="primary" type="submit">
          {this.i18n('button.filter.use')}
        </Basic.Button>
      </span>
    );
  }
}

FilterButtons.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Callback, when filter is canceled
   * @type {function} function(event)
   */
  cancelFilter: PropTypes.func
};
FilterButtons.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps
};
