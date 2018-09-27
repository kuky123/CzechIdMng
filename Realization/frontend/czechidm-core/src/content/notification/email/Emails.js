import React from 'react';
import Helmet from 'react-helmet';
import * as Basic from '../../../components/basic';
import { EmailManager } from '../../../redux';
import EmailTable from './EmailTable';

/**
 * List of email in audit log
 *
 * @author Radek Tomiška
 */
export default class Emails extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.emailManager = new EmailManager();
  }

  getContentKey() {
    return 'content.emails';
  }

  getNavigationKey() {
    return 'notification-emails';
  }

  render() {
    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader>
          <Basic.Icon value="fa:envelope-o"/>
          {' '}
          {this.i18n('header')}
        </Basic.PageHeader>

        <Basic.Panel>
          <EmailTable uiKey="email_table" emailManager={this.emailManager} filterOpened/>
        </Basic.Panel>

      </div>
    );
  }
}

Emails.propTypes = {
};
Emails.defaultProps = {
};
