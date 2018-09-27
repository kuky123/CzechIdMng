import React from 'react';
import Helmet from 'react-helmet';
import * as Basic from '../../../components/basic';
import { WebsocketManager } from '../../../redux';
import WebsocketTable from './WebsocketTable';

/**
 * List of websocket logs
 */
export default class Websockets extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.manager = new WebsocketManager();
  }

  getContentKey() {
    return 'content.websockets';
  }

  getNavigationKey() {
    return 'notification-websockets';
  }

  render() {
    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader>
          {this.i18n('header')}
        </Basic.PageHeader>

        <Basic.Panel>
          <WebsocketTable uiKey="websocket-table" manager={this.manager} filterOpened/>
        </Basic.Panel>

      </div>
    );
  }
}

Websockets.propTypes = {
};
Websockets.defaultProps = {
};
