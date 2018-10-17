import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../basic';

/**
 * "<hr>" in navigation
 *
 * @author Radek Tomiška
 */
export default class NavigationSeperator extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const { text, ...others } = this.props;
    return (
      <li className="nav-separator">
        { text }
      </li>
    );
  }
}

NavigationSeperator.propTypes = {
  text: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.arrayOf(PropTypes.oneOf([ PropTypes.node, PropTypes.object ]))
  ])
};

NavigationSeperator.defaultProps = {
  text: null
};
