import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
//
import { IdentityManager, NotificationManager, NotificationTemplateManager } from '../../redux';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import NotificationRecipient from './NotificationRecipient';
import NotificationRecipientsCell from './NotificationRecipientsCell';
import NotificationSentState from '../notification/NotificationSentState';
import NotificationLevelEnum from '../../enums/NotificationLevelEnum';
import SearchParameters from '../../domain/SearchParameters';

/**
 * Notification detail content
 *
 * @author Radek Tomiška
 */
class NotificationDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showLoading: false
    };
    this.identityManager = new IdentityManager();
    this.notificationManager = new NotificationManager();
    this.notificationTemplateManager = new NotificationTemplateManager();
  }

  getContentKey() {
    return 'content.notification';
  }

  componentDidMount() {
    const { isNew } = this.props;
    if (isNew) {
      this.refs.topic.focus();
    }
  }

  save(event) {
    const { uiKey } = this.props;

    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      showLoading: true
    }, this.refs.form.processStarted());

    const entity = this.refs.form.getData();
    // append recipients to recipientsData
    const recipientsData = [];
    if (entity.recipients) {
      entity.recipients.forEach(entityId => {
        recipientsData.push({identityRecipient: entityId});
      });
    }

    const saveEntity = {
      topic: entity.topic,
      identitySender: entity.identitySender,
      recipients: recipientsData,
      message: {
        subject: entity.subject,
        textMessage: entity.textMessage,
        htmlMessage: entity.htmlMessage,
        level: entity.level,
        template: entity.template ? entity.template.id : null
      }
    };

    this.context.store.dispatch(this.notificationManager.createEntity(saveEntity, `${uiKey}-detail`, (createdEntity, error) => {
      this._afterSave(createdEntity, error);
    }));
  }

  _afterSave(entity, error) {
    if (error) {
      this.addError(error);
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      return;
    }
    this.addMessage({ message: this.i18n('sent.success', { name: entity.name }) });
    this.context.router.replace('notification/notifications/');
  }

  _pickTemplate(template) {
    if (template) {
      const data = {
        ...this.refs.form.getData(),
        subject: template.subject,
        textMessage: template.bodyText,
        htmlMessage: template.bodyHtml
      };
      this.refs.form.setData(data);
    }
  }

  render() {
    const { notification, identityOnly, isNew, userContext, showTopic } = this.props;
    const { showLoading } = this.state;
    //
    if (!notification) {
      return null;
    }
    const forceSearchParameters = new SearchParameters().setFilter('parent', notification.id);

    let formData;
    if (isNew) {
      notification.identitySender = userContext.id;
      formData = { ...notification };
    } else {
      let template = null;
      if (notification.message.template) {
        template = notification.message.template;
      } else if (notification.message._embedded && notification.message._embedded.template) {
        template = notification.message._embedded.template;
      }
      formData = {
        ...notification,
        subject: notification.message.subject,
        textMessage: notification.message.textMessage,
        htmlMessage: notification.message.htmlMessage,
        level: notification.message.level,
        template
      };
    }
    //
    return (
      <div>
        <Basic.AbstractForm ref="form" data={formData} style={{ padding: 15 }}>
          <Basic.DateTimePicker ref="created" label={this.i18n('entity.Notification.created')} readOnly hidden={isNew}/>

          <Basic.LabelWrapper hidden={isNew}
            label={this.i18n('entity.Notification.sent')}>
            <NotificationSentState notification={notification} style={{ margin: '7px 0' }}/>
          </Basic.LabelWrapper>

          <Basic.TextField ref="topic" label={ this.i18n('entity.Notification.topic') } readOnly={ !isNew } hidden={ !showTopic }/>

          <Basic.SelectBox
            readOnly={!isNew}
            onChange={ this._pickTemplate.bind(this) }
            ref="template"
            label={this.i18n('entity.Notification.template')}
            manager={this.notificationTemplateManager}/>

          <Basic.TextField ref="subject" required label={this.i18n('entity.Notification.message.subject')} readOnly={!isNew} />

          <Basic.EnumSelectBox
            hidden={!isNew}
            ref="level"
            enum={NotificationLevelEnum}
            useSymbol={false}
            label={this.i18n('entity.Notification.message.level')}
            required/>

          <Basic.LabelWrapper hidden={isNew || !notification.message.level}
            label={this.i18n('entity.Notification.message.level')}>
            <div style={{ margin: '7px 0' }}>
              {
                !notification.message
                ||
                <Basic.EnumValue value={notification.message.level} enum={NotificationLevelEnum} />
              }
            </div>
          </Basic.LabelWrapper>

          <Basic.SelectBox
            hidden={ !isNew }
            ref="identitySender"
            label={ this.i18n('entity.Notification.sender') }
            manager={ this.identityManager }/>

          <Basic.LabelWrapper hidden={isNew || !notification._embedded || !notification._embedded.identitySender}
            label={this.i18n('entity.Notification.sender')}>
            <div style={{ margin: '7px 0' }}>
              {
                !notification._embedded
                ||
                this.identityManager.getNiceLabel(notification._embedded.identitySender)
              }
            </div>
          </Basic.LabelWrapper>

          <Basic.LabelWrapper hidden={isNew}
            label={this.i18n('entity.Notification.recipients')}>
            {
              notification.recipients
              ?
              <div style={{ margin: '7px 0' }}>
                {
                  notification.recipients.map(recipient => {
                    return (
                      <NotificationRecipient recipient={recipient} identityOnly={identityOnly}/>
                    );
                  })
                }
              </div>
              :
              null
            }
          </Basic.LabelWrapper>

          <Basic.SelectBox hidden={!isNew}
            ref="recipients" required
            label={this.i18n('entity.Notification.recipients')}
            manager={this.identityManager}
            multiSelect />

          <Basic.TextArea ref="textMessage" label={this.i18n('entity.Notification.message.textMessage')} readOnly={!isNew} hidden={!isNew && !notification.message.textMessage} />
          <Basic.Tabs>
            <Basic.Tab eventKey={1} title={this.i18n('entity.Notification.message.htmlMessage')} className="bordered">
              <div style={{padding: '10px'}}>
                <Basic.TextArea ref="htmlMessage" rows="10" readOnly={!isNew} hidden={!isNew && !notification.message.htmlMessage} />
              </div>
            </Basic.Tab>
            <Basic.Tab
              eventKey={ 2 }
              title={ this.i18n('entity.Notification.message.renderedHtmlMessage') }
              className="bordered"
              rendered={ !isNew }>
              <div style={{ padding: '10px' }}>
                <span dangerouslySetInnerHTML={{ __html: formData.htmlMessage || '' }}/>
              </div>
            </Basic.Tab>
          </Basic.Tabs>
          <Basic.LabelWrapper hidden={isNew || !notification.message.model}
            label={this.i18n('entity.Notification.message.model')}>
            {
              !notification.message
              ||
              <Basic.FlashMessage level={notification.message.level} message={this.getFlashManager().convertFromResultModel(notification.message.model)} style={{ margin: '7px 0' }}/>
            }
          </Basic.LabelWrapper>

          <Basic.TextArea ref="sentLog" label={this.i18n('entity.Notification.sentLog')} readOnly hidden={isNew || !notification.sentLog} max={2000} />
        </Basic.AbstractForm>
        {
          notification.type !== 'notification'
          ||
          <Advanced.Table
            header={ this.i18n('relatedNotifications') }
            hidden={ isNew }
            manager={ this.notificationManager }
            forceSearchParameters={ forceSearchParameters }>
            <Basic.Column
              property="type"
              header={this.i18n('entity.Notification.type')}/>
            <Basic.Column
              property="created"
              header={this.i18n('entity.Notification.created')}
              cell={<Basic.DateCell format={this.i18n('format.datetime')}/>}
              rendered={false}/>
            <Basic.Column
              property="recipients"
              header={this.i18n('entity.Notification.recipients')}
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <NotificationRecipientsCell notification={ data[rowIndex] } />
                  );
                }
              }/>
              <Basic.Column
                property="sent"
                header={ this.i18n('entity.Notification.sent') }
                cell={
                  /* eslint-disable react/no-multi-comp */
                  ({ rowIndex, data}) => {
                    return (
                      <NotificationSentState notification={ data[rowIndex] }/>
                    );
                  }
                }/>
                <Basic.Column
                  property="sentLog"
                  header={this.i18n('entity.Notification.sentLog')}
                  width="20%"/>
              </Advanced.Table>
            }
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={this.context.router.goBack} showLoading={showLoading}>{this.i18n('button.back')}</Basic.Button>
              <Basic.Button
                hidden={!isNew}
                onClick={this.save.bind(this)}
                level="success"
                showLoadingIcon
                showLoadingText={this.i18n('button.sending')}>
                { this.i18n('button.send') }
              </Basic.Button>
            </Basic.PanelFooter>
      </div>
    );
  }
}

NotificationDetail.propTypes = {
  notification: PropTypes.object,
  identityOnly: PropTypes.bool,
  isNew: PropTypes.bool,
  userContext: PropTypes.object,
  showTopic: PropTypes.bool
};
NotificationDetail.defaultProps = {
  identityOnly: false,
  showLoading: false,
  userContext: null,
  showTopic: true
};

function select(state) {
  return {
    userContext: state.security.userContext
  };
}

export default connect(select)(NotificationDetail);
