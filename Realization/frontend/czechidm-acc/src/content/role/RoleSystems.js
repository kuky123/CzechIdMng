import React from 'react';
import _ from 'lodash';
//
import { Basic, Domain } from 'czechidm-core';
import RoleSystemTableComponent, { RoleSystemTable } from '../role/RoleSystemTable';

const uiKey = 'role-systems-table';

/**
 * Linked target systems to role
 *
 * @author Radek Tomiška
 * @author Petr Hanák
 */
export default class RoleSystems extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.role.systems';
  }

  getNavigationKey() {
    return this.getRequestNavigationKey('role-systems', this.props.params);
  }

  render() {
    const { entityId } = this.props.params;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('roleId', entityId);
    return (
      <div className="tab-pane-table-body">
        { this.renderContentHeader({ style: { marginBottom: 0 }}) }

        <Basic.Panel className="no-border last">
          <RoleSystemTableComponent
            uiKey={ `${this.getUiKey()}-${entityId}` }
            columns={ _.difference(RoleSystemTable.defaultProps.columns, ['role']) }
            forceSearchParameters={ forceSearchParameters }
            params={ this.props.params }
            className="no-margin"/>
        </Basic.Panel>
      </div>
    );
  }
}
