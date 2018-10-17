import React from 'react';
import PropTypes from 'prop-types';
import { ProgressBar } from 'react-bootstrap';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

/**
 * Wrapped bootstrap progress bar
 *
 * @author Radek Tomiška
 */
export default class BasicProgressBar extends AbstractComponent {

  /**
   * Resolves label className - when progress is small, then label needs to be inversed - will be visible
   *
   * @return {string}
   */
  _resolveLabel() {
    const { label, now, min } = this.props;
    const max = this._resolveMax();
    //
    let percent = 0;
    if ((max - min) > 0) {
      percent = (now / (max - min)) * 100;
    }
    //
    const classNames = classnames(
      { 'label-inverse': (percent <= 12) } // less than 12%
    );
    return (
      <span className={classNames}>{ label }</span>
    );
  }

  /**
   * Automatic max resolving
   *
   * @return {number}
   */
  _resolveMax() {
    const { now, max } = this.props;
    //
    let _max = max;
    if (_max === null) {
      if (now > 0) {
        _max = now * 2;
      }
    }
    return _max;
  }

  render() {
    const { rendered, showLoading, min, now, active, style, className, bsStyle, children, isChild } = this.props;
    if (!rendered) {
      return null;
    }
    if (showLoading) {
      return (
        <span><Icon value="fa:refresh" showLoading/></span>
      );
    }
    // add component className
    const classNames = classnames(
      'basic-progress-bar',
      className
    );
    //
    if (children) {
      return (
        <ProgressBar
          min={min}
          max={this._resolveMax()}
          now={now}
          label={this._resolveLabel()}
          active={active}
          style={style}
          bsStyle={bsStyle}>
          { children }
        </ProgressBar>
      );
    }
    //
    return (
      <span className={classNames}>
        <ProgressBar
          min={min}
          max={this._resolveMax()}
          now={now}
          label={this._resolveLabel()}
          active={active}
          style={style}
          bsStyle={bsStyle}
          isChild={isChild}/>
      </span>
    );
  }
}

BasicProgressBar.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Start count
   */
  min: PropTypes.number,
  /**
   * End count
   */
  max: PropTypes.number,
  /**
   * Actual counter
   */
  now: PropTypes.number,
  /**
   * Label
   */
  label: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node
  ]),
  /**
   * Adds animation -  the stripes right to left. Not available in IE9 and below.
   */
  active: PropTypes.bool,
  bsStyle: PropTypes.string
};

BasicProgressBar.defaultProps = {
  ...AbstractComponent.defaultProps,
  min: 0,
  now: 0,
  active: true
};
