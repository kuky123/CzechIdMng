import React from 'react';
import PropTypes from 'prop-types';
import { Collapse } from 'react-bootstrap';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Loading from '../Loading/Loading';

/**
 * Colappse panel
 *
 * TODO: can be controlled by external action only. Support default collapse action (e.g. bz button)
 *
 * @author Radek Tomiška
 */
export default class BasicCollapse extends AbstractComponent {

  render() {
    const { rendered, showLoading, children, ...others } = this.props;
    if (!rendered) {
      return null;
    }

    return (
      <Collapse {...others}>
        {
          showLoading
          ?
          <Loading isStatic showLoading/>
          :
          children
        }
      </Collapse>
    );
  }
}

BasicCollapse.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * If collapse children is shown
   */
  in: PropTypes.bool,

  /**
   * ... and other react bootstap collapse props
   */
};

BasicCollapse.defaultProps = {
  ...AbstractComponent.defaultProps,
  in: false
};
