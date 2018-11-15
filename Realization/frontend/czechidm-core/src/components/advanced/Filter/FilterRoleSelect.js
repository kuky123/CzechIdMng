import { PropTypes } from 'react';
import RoleSelect from '../RoleSelect/RoleSelect';

/**
 * Role catalogue select used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterRoleSelect extends RoleSelect {

}

FilterRoleSelect.propTypes = {
  ...RoleSelect.propTypes,
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
const { labelSpan, componentSpan, ...otherDefaultProps } = RoleSelect.defaultProps; // labelSpan etc. override
FilterRoleSelect.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ',
  uiKey: 'filter-role-tree'
};
