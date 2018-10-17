import React from 'react';
import PropTypes from 'prop-types';
//
import NotificationRecipient from './NotificationRecipient';
import { Basic } from 'czechidm-core';

/**
 * Renders cell with notification recipient content
 *
 * @author Peter Šourek
 * @author Radek Tomiška
 */
export default class NotificationRecipientsCell extends Basic.AbstractContent {

  constructor(props) {
    super(props);
  }

  render() {
    const { identityOnly, notification } = this.props;

    if (!notification) {
      return null;
    }

    return (
      <span>
        {
          notification.recipients.map(recipient => {
            return (
              <NotificationRecipient recipient={recipient} identityOnly={identityOnly} />
            );
          })
        }
      </span>
  );
  }
}

NotificationRecipientsCell.propTypes = {
  identityOnly: PropTypes.bool,
  notification: PropTypes.string.required
};

NotificationRecipientsCell.defaultProps = {
  identityOnly: true
};
