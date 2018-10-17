import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

/**
 * Bootstrap row
 *
 * @author Radek Tomiška
 */
class Row extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, children, className, ...others } = this.props;
    if (!rendered) {
      return null;
    }

    const classNames = classnames(
      'row',
      className
    );
    return (
      <div className={classNames} {...others}>
        {children}
      </div>
    );
  }
}

Row.propTypes = {
  /**
   * If component is rendered on page
   */
  rendered: PropTypes.bool
};

Row.defaultProps = {
  rendered: true
};


export default Row;
