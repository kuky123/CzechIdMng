import React from 'react';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import NotificationStateEnum from '../../enums/NotificationStateEnum';
import { IdentityManager } from '../../redux';

const identityManager = new IdentityManager();

/**
* Common notification filter
*
* @author Radek Tomiška
*/
export default class NotificationFilter extends Advanced.Filter {

  focus() {
    this.refs.text.focus();
  }

  render() {
    const { onSubmit, onCancel } = this.props;
    //
    return (
      <Advanced.Filter onSubmit={ onSubmit }>
        <Basic.AbstractForm ref="filterForm">
          <Basic.Row>
            <Basic.Col lg={ 8 }>
              <Advanced.Filter.FilterDate
                ref="fromTill"
                fromPlaceholder={ this.i18n('content.notifications.filter.dateFrom.placeholder') }
                tillPlaceholder={ this.i18n('content.notifications.filter.dateTill.placeholder') }/>
            </Basic.Col>
            <Basic.Col lg={ 4 } className="text-right">
              <Advanced.Filter.FilterButtons cancelFilter={ onCancel }/>
            </Basic.Col>
          </Basic.Row>

          <Basic.Row>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.TextField
                ref="text"
                placeholder={ this.i18n('content.notifications.filter.text.placeholder') }/>
            </Basic.Col>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.SelectBox
                ref="recipient"
                placeholder={ this.i18n('content.notifications.filter.recipient.placeholder') }
                multiSelect={ false }
                manager={ identityManager }
                returnProperty="username"/>
            </Basic.Col>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.SelectBox
                ref="sender"
                placeholder={ this.i18n('content.notifications.filter.sender.placeholder') }
                multiSelect={ false }
                manager={ identityManager }
                returnProperty="username"/>
            </Basic.Col>
          </Basic.Row>

          <Basic.Row className="last">
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.EnumSelectBox
                ref="state"
                placeholder={ this.i18n('content.notifications.filter.sent.placeholder') }
                enum={ NotificationStateEnum }/>
            </Basic.Col>
            <Basic.Col lg={ 8 } >
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
      );
  }
}
