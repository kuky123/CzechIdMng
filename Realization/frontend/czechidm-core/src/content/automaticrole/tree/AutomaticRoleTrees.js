import React from 'react';
import Helmet from 'react-helmet';
//
import * as Basic from '../../../components/basic';
import { RoleTreeNodeManager } from '../../../redux';
import RoleTreeNodeTable from '../../role/RoleTreeNodeTable';

/**
 * List of automatic roles by tree node
 *
 * @author Ondrej Kopr
 */
export default class AutomaticRoleTrees extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.manager = new RoleTreeNodeManager();
  }

  getContentKey() {
    return 'content.automaticRoles.tree';
  }

  componentDidMount() {
    this.selectNavigationItems(['roles-menu', 'automatic-roles', 'automatic-role-tree']);
  }

  render() {
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <RoleTreeNodeTable uiKey="automatic-role-tree-table" manager={ this.manager }/>
      </div>
    );
  }
}

AutomaticRoleTrees.propTypes = {
};
AutomaticRoleTrees.defaultProps = {
};
