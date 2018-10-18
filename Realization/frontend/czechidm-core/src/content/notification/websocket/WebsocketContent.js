import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../../../components/basic';
import { WebsocketManager } from '../../../redux';
import NotificationDetail from '../NotificationDetail';

const manager = new WebsocketManager();

/**
 * Websockert audit log detail content
 *
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
class WebsocketContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.websocket';
  }

  componentDidMount() {
    this.selectNavigationItem('notification-websockets');
    const { entityId } = this.props.params;
    //
    this.getLogger().debug(`[WebsocketContent] loading entity detail [id:${entityId}]`);
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
            <NotificationDetail notification={entity}/>
          }
        </Basic.Panel>

      </div>
    );
  }
}

WebsocketContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
WebsocketContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(WebsocketContent);
