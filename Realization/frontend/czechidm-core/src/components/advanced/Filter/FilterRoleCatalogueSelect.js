import { PropTypes } from 'react';
import RoleCatalogueSelect from '../RoleCatalogueSelect/RoleCatalogueSelect';

/**
 * Role catalogue select used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterRoleCatalogueSelect extends RoleCatalogueSelect {

}

FilterRoleCatalogueSelect.propTypes = {
  ...RoleCatalogueSelect.propTypes,
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
const { labelSpan, componentSpan, ...otherDefaultProps } = RoleCatalogueSelect.defaultProps; // labelSpan etc. override
FilterRoleCatalogueSelect.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ',
  uiKey: 'filter-role-catalogue-tree'
};
