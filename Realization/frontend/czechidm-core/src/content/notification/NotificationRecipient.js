import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { IdentityManager } from '../../redux';

/**
 * Notification recipient
 *
 * @author Peter Šourek
 * @author Radek Tomiška
 */
export default class NotificationRecipient extends Basic.AbstractComponent {

  constructor(props) {
    super(props);
    this.identityManager = new IdentityManager();
  }

  renderIdentity(identity) {
    if (!identity) {
      return null;
    }
    //
    return (
      <Advanced.IdentityInfo entity={ identity } entityIdentifier={ identity.id } face="popover"/>
    );
  }

  render() {
    const { rendered, recipient, identityOnly, className } = this.props;
    //
    if (!recipient || !rendered) {
      return null;
    }
    let content = null;
    if (identityOnly && recipient._embedded && recipient._embedded.identityRecipient) {
      content = this.renderIdentity(recipient._embedded.identityRecipient);
    } else if (recipient.realRecipient) {
      content = recipient.realRecipient;
    } else if (recipient._embedded && recipient._embedded.identityRecipient) {
      content = this.renderIdentity(recipient._embedded.identityRecipient);
    }
    //
    return (
      <div className={ className }>
        { content }
      </div>
    );
  }
}

NotificationRecipient.propTypes = {
  ...Basic.AbstractComponent.propTypes,
  recipient: PropTypes.object,
  /**
   * Shows identity only, if its filled. If identity is not fillef, then realRecipient is shown instead.
   */
  identityOnly: PropTypes.bool
};
NotificationRecipient.defaultProps = {
  ...Basic.AbstractComponent.defaultProps,
  identityOnly: false
};
