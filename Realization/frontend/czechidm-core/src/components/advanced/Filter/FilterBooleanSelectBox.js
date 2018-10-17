import PropTypes from 'prop-types';
//
import * as Basic from '../../basic';

/**
 * Boolean select box used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterBooleanSelectBox extends Basic.BooleanSelectBox {

}

FilterBooleanSelectBox.propTypes = {
  ...Basic.BooleanSelectBox.propTypes,
  /**
   * Field name - if is not set, then ref is used
   * @type {[type]}
   */
  field: PropTypes.string,
  /**
   * Filter relation
   * TODO: enumeration
   */
  relation: PropTypes.oneOf(['EQ', 'NEQ'])
};
const { labelSpan, componentSpan, ...otherDefaultProps } = Basic.BooleanSelectBox.defaultProps; // labelSpan etc. override
FilterBooleanSelectBox.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ'
};
