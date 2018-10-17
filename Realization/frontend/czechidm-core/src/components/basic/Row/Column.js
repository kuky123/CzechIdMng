import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

/**
 * Bootstrap column
 *
 * TODO: push, offset
 *
 * @author Radek Tomiška
 */
export default class Column extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, children, className, style, lg, sm, md, xs } = this.props;
    if (!rendered) {
      return null;
    }

    const classNames = classnames(
      { [`col-lg-${lg}`]: (lg > 0) },
      { [`col-sm-${sm}`]: (sm > 0) },
      { [`col-md-${md}`]: (md > 0) },
      { [`col-xs-${xs}`]: (xs > 0) },
      className
    );
    return (
      <div className={classNames} style={style}>
        {children}
      </div>
    );
  }
}

Column.propTypes = {
  /**
   * If component is rendered on page
   */
  rendered: PropTypes.bool,
  /**
   * Column widths
   */
  lg: PropTypes.number,
  md: PropTypes.number,
  sm: PropTypes.number,
  xs: PropTypes.number
};

Column.defaultProps = {
  rendered: true
};
