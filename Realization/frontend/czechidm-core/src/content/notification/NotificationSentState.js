import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import NotificationStateEnum from '../../enums/NotificationStateEnum';

/**
 * Notification sent state
 *
 * @author Radek Tomiška
 */
export default class NotificationSentState extends Basic.AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, notification, ...others } = this.props;
    //
    if (!rendered || !notification) {
      return null;
    }
    if (notification.state) {
      return (
        <Basic.EnumValue value={ notification.state } enum={ NotificationStateEnum }/>
      );
    }
    //
    const sentCount = !notification.relatedNotifications ? 0 : notification.relatedNotifications.reduce((result, _notification) => { return result + (_notification.sent ? 1 : 0); }, 0);
    return (
      <div {...others}>
        {
          !notification.relatedNotifications || notification.relatedNotifications.length === 0
          ?
          <span>
            {
              notification.sent !== null
              ?
              <Basic.Label level="success" text={<Advanced.DateValue value={notification.sent} showTime/>}/>
              :
              <Basic.Label level="danger" text={NotificationStateEnum.getNiceLabelBySymbol(NotificationStateEnum.NOT)}/>
            }
          </span>
          :
          <span>
            {
              sentCount === notification.relatedNotifications.length
              ?
              <Basic.Label level="success" text={<Advanced.DateValue value={notification.sent} showTime/>}/>
              :
              <span>
                {
                  sentCount === 0
                  ?
                  <Basic.Label level="danger" text={NotificationStateEnum.getNiceLabelBySymbol(NotificationStateEnum.NOT)}/>
                  :
                  <Basic.Label level="warning" text={NotificationStateEnum.getNiceLabelBySymbol(NotificationStateEnum.PARTLY)}/>
                }
              </span>
            }
          </span>
        }
      </div>
    );
  }
}

NotificationSentState.propTypes = {
  ...Basic.AbstractComponent.propTypes,
  notification: PropTypes.object
};
NotificationSentState.defaultProps = {
  ...Basic.AbstractComponent.defaultProps
};
