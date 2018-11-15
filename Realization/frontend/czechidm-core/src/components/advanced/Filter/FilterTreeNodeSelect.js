import { PropTypes } from 'react';
import TreeNodeSelect from '../TreeNodeSelect/TreeNodeSelect';

/**
 * Tree node select used in filters
 *
 * @author Radek Tomiška
 */
export default class FilterTreeNodeSelect extends TreeNodeSelect {

}

FilterTreeNodeSelect.propTypes = {
  ...TreeNodeSelect.propTypes,
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
const { labelSpan, componentSpan, ...otherDefaultProps } = TreeNodeSelect.defaultProps; // labelSpan etc. override
FilterTreeNodeSelect.defaultProps = {
  ...otherDefaultProps,
  relation: 'EQ',
  uiKey: 'filter-tree-node-tree'
};
