import React from 'react';
import PropTypes from 'prop-types';
//
import { AbstractComponent } from '../../basic';
import AdvancedColumn from './Column';

/**
 * Component that defines the attributes of table column.
 *
 * @author Radek Tomiška
 */
class AdvancedColumnLink extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered } = this.props;
    if (!rendered) {
      return null;
    }
    return (
      <span>Advanced column never render himself</span>
    );
  }
}

AdvancedColumnLink.propTypes = {
  ...AdvancedColumn.propTypes,
  to: PropTypes.string.isRequired
};
AdvancedColumnLink.defaultProps = {
};
AdvancedColumnLink.__AdvancedColumnLink__ = true;

export default AdvancedColumnLink;
