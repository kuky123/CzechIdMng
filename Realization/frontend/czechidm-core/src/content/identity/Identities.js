import React from 'react';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { IdentityManager } from '../../redux';
import IdentityTable from './IdentityTable';

/**
 * List of identities
 *
 * @author Radek Tomiška
 */
class Identities extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
  }

  getContentKey() {
    return 'content.identities';
  }

  getNavigationKey() {
    return 'identities';
  }

  render() {
    return (
      <div>
        { this.renderPageHeader() }

        <Basic.Panel>
          <IdentityTable
            uiKey="identity-table"
            identityManager={ this.identityManager }
            filterOpened
            showRowSelection />
        </Basic.Panel>

      </div>
    );
  }
}

Identities.propTypes = {
};
Identities.defaultProps = {
};

function select() {
  return {
  };
}

export default connect(select)(Identities);
