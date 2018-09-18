import React from 'react';
import Helmet from 'react-helmet';
//
import * as Basic from '../../components/basic';
import SearchParameters from '../../domain/SearchParameters';
import AuthorizationPolicyTable from './AuthorizationPolicyTable';

/**
 * Automatic roles - tab on role detail
 *
 * @author Radek Tomiška
 */
export default class AuthorizationPolicies extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.role.authorization-policies';
  }

  getNavigationKey() {
    return this.getRequestNavigationKey('role-authorization-policies', this.props.params);
  }

  render() {
    const forceSearchParameters = new SearchParameters().setFilter('roleId', this.props.params.entityId);
    //
    return (
      <div>
        <Helmet title={ this.i18n('title') } />

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          {this.i18n('header')}
        </Basic.ContentHeader>

        <Basic.Panel className="no-border last">
          <AuthorizationPolicyTable
            uiKey="role-authorization-policies-table"
            forceSearchParameters={ forceSearchParameters }
            params={ this.props.params }
            className="no-margin"/>
        </Basic.Panel>

      </div>
    );
  }
}
