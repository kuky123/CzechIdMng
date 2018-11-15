import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
//
import { Basic, Domain, Utils } from 'czechidm-core';
import { AccountManager } from '../../redux';
import PasswordChangeForm from 'czechidm-core/src/content/identity/PasswordChangeForm';
//
const IDM_NAME = Utils.Config.getConfig('app.name', 'CzechIdM');
const RESOURCE_IDM = `0:${IDM_NAME}`;
//
const accountManager = new AccountManager();

/**
 * In this component include password change and send props with account options
 *
 * @author Ondřej Kopr
 */
class PasswordChangeAccounts extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    const { entityId } = this.props.params;
    const defaultSearchParameters = accountManager.getDefaultSearchParameters()
                                      .setName(Domain.SearchParameters.NAME_AUTOCOMPLETE)
                                      .setFilter('ownership', true)
                                      .setFilter('supportChangePassword', true)
                                      .setFilter('identity', entityId);

    this.selectSidebarItem('profile-password');
    this.context.store.dispatch(accountManager.fetchEntities(defaultSearchParameters, `${entityId}-accounts`));
  }

  _getOptions() {
    const { entityId } = this.props.params;
    const { accounts, showLoading } = this.props;

    if (showLoading) {
      return null;
    }

    const options = [ ];
    options.push({ value: RESOURCE_IDM, niceLabel: `${IDM_NAME} (${entityId})`});

    accounts.forEach(acc => {
      // Skip account in protection
      if (acc.inProtection) {
        return;
      }
      const niceLabel = acc._embedded.system.name + ' (' + acc.uid + ')';
      options.push({
        value: acc.id,
        niceLabel
      });
    });

    return options;
  }

  render() {
    const { passwordChangeType, userContext, requireOldPassword, showLoading } = this.props;
    const { entityId } = this.props.params;
    const options = this._getOptions();
    return (
      <div>
        {
          showLoading
          ||
          <PasswordChangeForm
            userContext={userContext}
            entityId={entityId}
            passwordChangeType={passwordChangeType}
            requireOldPassword={requireOldPassword}
            accountOptions={options}/>
        }
    </div>
    );
  }

}

PasswordChangeAccounts.propTypes = {
  showLoading: PropTypes.bool,
  userContext: PropTypes.object,
  accounts: PropTypes.object
};
PasswordChangeAccounts.defaultProps = {
  userContext: null,
  showLoading: true,
  accounts: null
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    userContext: state.security.userContext,
    accounts: accountManager.getEntities(state, `${entityId}-accounts`),
    showLoading: accountManager.isShowLoading(state, `${entityId}-accounts`)
  };
}
export default connect(select)(PasswordChangeAccounts);
