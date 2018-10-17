import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
//
import { Domain, Advanced, Basic } from 'czechidm-core';
import AccountTableComponent, { AccountTable } from '../account/AccountTable';

/**
 * Linked accounts on target system
 *
 * @author Radek Tomiška
 * @author Vít Švanda
 */
class SystemAccountsContent extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      systemEntity: null
    };
  }

  getContentKey() {
    return 'acc:content.system.accounts';
  }

  getNavigationKey() {
    return 'system-accounts';
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoading } = this.props;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('systemId', entityId);
    const forceSystemEntitySearchParameters = new Domain.SearchParameters().setFilter('systemId', entityId);

    return (
      <div>
        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <Basic.Panel className="no-border last">
          <AccountTableComponent
            uiKey="system-accounts-table"
            showLoading={ _showLoading }
            forceSearchParameters={ forceSearchParameters }
            forceSystemEntitySearchParameters={ forceSystemEntitySearchParameters }
            columns={ _.difference(AccountTable.defaultProps.columns, ['system']) }/>
        </Basic.Panel>
      </div>
    );
  }
}

SystemAccountsContent.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemAccountsContent.defaultProps = {
  _showLoading: false,
};


export default SystemAccountsContent;
