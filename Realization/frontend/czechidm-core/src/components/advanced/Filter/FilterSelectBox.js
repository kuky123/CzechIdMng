import PropTypes from 'prop-types';
import * as Basic from '../../basic';

/**
 * Select box used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterSelectBox extends Basic.SelectBox {

}

FilterSelectBox.propTypes = {
  ...Basic.SelectBox.propTypes,
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
const { labelSpan, componentSpan, ...otherDefaultProps } = Basic.SelectBox.defaultProps; // labelSpan etc. override
FilterSelectBox.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ'
};
