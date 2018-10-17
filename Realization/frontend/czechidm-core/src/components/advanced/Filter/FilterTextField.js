import PropTypes from 'prop-types';
import * as Basic from '../../basic';

/**
 * Text field used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterTextField extends Basic.TextField {

}

FilterTextField.propTypes = {
  ...Basic.TextField.propTypes,
  /**
   * Field name - if is not set, then ref is used
   * @type {[type]}
   */
  field: PropTypes.string,
  /**
   * Filter relation
   * TODO: enumeration
   */
  relation: PropTypes.oneOf(['EQ', 'NEQ', 'LT', 'LE', 'GT', 'GE', 'LIKE', 'NOTLIKE', 'IN', 'IS_NULL', 'IS_NOT_NULL', 'IS_EMPTY', 'IS_NOT_EMPTY'])
};
const { labelSpan, componentSpan, ...otherDefaultProps } = Basic.TextField.defaultProps; // labelSpan etc. override
FilterTextField.defaultProps = {
  ...otherDefaultProps,
  relation: 'LIKE'
};
