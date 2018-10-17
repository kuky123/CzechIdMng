import PropTypes from 'prop-types';
import * as Basic from '../../basic';

/**
 * Enum select box used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterEnumSelectBox extends Basic.EnumSelectBox {

}

FilterEnumSelectBox.propTypes = {
  ...Basic.EnumSelectBox.propTypes,
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
const { labelSpan, componentSpan, ...otherDefaultProps } = Basic.EnumSelectBox.defaultProps; // labelSpan etc. override
FilterEnumSelectBox.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ'
};
