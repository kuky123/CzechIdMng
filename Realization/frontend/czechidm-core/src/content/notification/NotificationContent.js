import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import * as Basic from '../../components/basic';
import { NotificationManager } from '../../redux';
import NotificationDetail from './NotificationDetail';

const notificationManager = new NotificationManager();

/**
 * Notification detail content
 *
 * @author Radek Tomiška
 */
class NotificationContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.notification';
  }

  _getIsNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  componentDidMount() {
    this.selectNavigationItem('notification-notifications');
    const { entityId } = this.props.params;
    const isNew = this._getIsNew();
    if (isNew) {
      this.context.store.dispatch(notificationManager.receiveEntity(entityId, { }));
    } else {
      this.getLogger().debug(`[NotificationContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(notificationManager.fetchEntity(entityId));
    }
  }

  render() {
    const { notification, showLoading } = this.props;
    const isNew = this._getIsNew();
    return (
      <div>
        <Helmet title={
            isNew
            ?
            this.i18n('titleNew')
            :
            this.i18n('title')
          } />

        <Basic.PageHeader>
          <Basic.Icon value="fa:envelope"/>
          {' '}
          {
            isNew
            ?
            this.i18n('headerNew')
            :
            this.i18n('header')
          }
        </Basic.PageHeader>

        <Basic.Panel showLoading={showLoading} >
          {
            !notification
            ||
            <NotificationDetail notification={notification} isNew={isNew ? true : false} />
          }
        </Basic.Panel>

      </div>
    );
  }
}

NotificationContent.propTypes = {
  notification: PropTypes.object,
  showLoading: PropTypes.bool
};
NotificationContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    notification: notificationManager.getEntity(state, entityId),
    showLoading: notificationManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(NotificationContent);
