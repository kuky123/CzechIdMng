import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import NotificationSystem from 'react-notification-system';
//
import AbstractContextComponent from '../AbstractContextComponent/AbstractContextComponent';

export class FlashMessages extends AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  componentWillReceiveProps(nextProps) {
    const unprocessedMessages = this._getUnproccessedMessages(nextProps.messages, this.props.messages);
    unprocessedMessages.added.map(message => {
      this._showMessage(message);
    });
    unprocessedMessages.hidden.map(message => {
      this._hideMessage(message);
    });
  }

  /**
  * Returns newly {added, hidden} mesaages
  */
  _getUnproccessedMessages(newMessages, oldMessages) {
    let added = [];
    const hidden = [];
    if (!oldMessages || !oldMessages.length) {
      added = newMessages;
    } else {
      // show or hide new
      newMessages.map(message => {
        if (!message.hidden) {
          added[message.id] = message;
        } else {
          hidden[message.id] = message;
        }
      });
    }
    return {
      added,
      hidden
    };
  }

  _hideMessage(message) {
    if (message === null) {
      return;
    }
    this.refs.messages.removeNotification(message.id);
  }

  _onRemove(notification) {
    this.context.store.dispatch(this.getFlashManager().hideMessage(notification.uid));
  }

  _getAutoDismiss(message) {
    let autoDismiss = 10;
    if (message.level === 'error') {
      autoDismiss = 20;
    } else if (message.level === 'success') {
      autoDismiss = 5;
    }
    return autoDismiss;
  }

  /**
  * Adds message to UI
  */
  _showMessage(options) {
    if (!options) {
      return;
    }
    const message = this.getFlashManager().createMessage(options);
    //
    if (message.hidden) {
      // skip hidden messages
      return;
    }
    // show max 3 messages
    let messageCounter = 0;
    this.props.messages.map(m => {
      if (!m.hidden) {
        messageCounter++;
        if (messageCounter > this.props.maxShown) {
          this._hideMessage(m);
        }
      }
    });
    //
    this.refs.messages.addNotification({
      uid: message.id,
      title: message.title,
      message: message.message,
      level: message.level,
      position: message.position || 'tr',
      onRemove: (n) => this._onRemove(n),
      dismissible: message.dismissible,
      autoDismiss: this._getAutoDismiss(message),
      action: message.action,
      children: message.children
    });
  }

  _getNotificationSystemStyles() {
    const styles = {
      Containers: {
        DefaultStyle: {
          top: '15px'
        },
        tr: {
          top: '50px',
          bottom: 'auto',
          left: 'auto',
          right: '0px'
        },
        tc: {
          width: '600px',
          margin: '0px auto 0px -300px'
        }
      },
      NotificationItem: { // Override the notification item
        DefaultStyle: { // Applied to every notification, regardless of the notification level
          margin: '10px 100px 2px 0px'
        },

        success: { // Applied only to the success notification item
          // color: 'red'
        }
      },
      ActionWrapper: {
        DefaultStyle: {
          textAlign: 'right'
        }
      }
    };
    return styles;
  }

  render() {
    const styles = this. _getNotificationSystemStyles();
    return (
      <div id="flash-messages">
        <NotificationSystem ref="messages" style={styles}/>
      </div>
    );
  }
}

FlashMessages.propTypes = {
  messages: PropTypes.array,
  maxShown: PropTypes.number
};
FlashMessages.defaultProps = {
  maxShown: 4
};
FlashMessages.contextTypes = {
  store: PropTypes.object.isRequired
};

// Which props do we want to inject, given the global state?
// Note: use https://github.com/faassen/reselect for better performance.
function select(state) {
  return {
    messages: state.messages.messages.toArray()
  };
}

// Wrap the component to inject dispatch and state into it
// this.refs.form.getWrappedInstance().submit() - could call connected instance
export default connect(select)(FlashMessages);
