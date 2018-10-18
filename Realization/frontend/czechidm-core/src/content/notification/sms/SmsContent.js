import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../../../components/basic';
import { SmsManager } from '../../../redux';
import NotificationDetail from '../NotificationDetail';

const manager = new SmsManager();

/**
 * Sms audit log detail content
 *
 * @author Peter Sourek
 */
class SmsContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.sms';
  }

  componentDidMount() {
    this.selectNavigationItem('notification-sms');
    const { entityId } = this.props.params;
    //
    this.getLogger().debug(`[SmsContent] loading entity detail [id:${entityId}]`);
    this.context.store.dispatch(manager.fetchEntity(entityId));
  }

  render() {
    const { entity, showLoading } = this.props;
    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader>
          <Basic.Icon value="fa:envelope-o"/>
          {' '}
          {this.i18n('header')}
        </Basic.PageHeader>

        <Basic.Panel>
          <Basic.Loading isStatic showLoading={showLoading} />
          {
            !entity
            ||
            <NotificationDetail notification={ entity } showTopic={ false }/>
          }
        </Basic.Panel>

      </div>
    );
  }
}

SmsContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
SmsContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(SmsContent);
