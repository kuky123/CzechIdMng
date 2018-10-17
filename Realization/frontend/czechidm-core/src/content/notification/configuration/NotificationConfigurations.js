import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { NotificationConfigurationManager, SecurityManager, DataManager, NotificationTemplateManager } from '../../../redux';
import NotificationLevelEnum from '../../../enums/NotificationLevelEnum';

const uiKey = 'notification-configurations-table';
const manager = new NotificationConfigurationManager();
const notificationTemplateManager = new NotificationTemplateManager();

/**
 * Notification configuration
 *
 * @author Radek Tomiška
 */
export default class NotificationConfigurations extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      detail: {
        show: false,
        entity: {}
      },
      filterOpened: true
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.context.store.dispatch(manager.fetchSupportedNotificationTypes());
    this.refs.filterForm.focus();
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.notificationConfigurations';
  }

  getNavigationKey() {
    return 'notification-configurations';
  }

  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      detail: {
        show: true,
        entity
      }
    });
  }

  closeDetail(event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      detail: {
        show: false,
        entity: {}
      }
    });
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    // transform template id
    const savedEntity = {
      ...formEntity
    };
    //
    super.save(savedEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      this.refs.table.getWrappedInstance().reload();
      this.addMessage({ message: this.i18n('save.success', { name: this.getManager().getNiceLabel(entity) }) });
    }
    //
    super.afterSave(entity, error);
  }

  render() {
    const { _showLoading, _supportedNotificationTypesLoading, _supportedNotificationTypes } = this.props;
    const { detail, filterOpened } = this.state;

    return (
      <div>
        { this.renderPageHeader() }
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.Panel className="last">
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            filterOpened={filterOpened}
            manager={this.getManager()}
            showRowSelection={SecurityManager.hasAnyAuthority(['NOTIFICATIONCONFIGURATION_UPDATE'])}
            actions={
              SecurityManager.hasAnyAuthority(['NOTIFICATIONCONFIGURATION_DELETE'])
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { level: NotificationLevelEnum.findKeyBySymbol(NotificationLevelEnum.SUCCESS) })}
                  rendered={SecurityManager.hasAnyAuthority(['NOTIFICATIONCONFIGURATION_CREATE'])}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }
            filter={
              <Filter
                ref="filterForm"
                onSubmit={ this.useFilter.bind(this) }
                onCancel={ this.cancelFilter.bind(this) }
                supportedNotificationTypes={_supportedNotificationTypes}/>
            }
            _searchParameters={ this.getSearchParameters() }>
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex])}/>
                  );
                }
              }/>
            <Advanced.Column property="topic" width={ 200 } header={this.i18n('entity.NotificationConfiguration.topic')} sort face="text" />
            <Advanced.Column property="level" width={ 75 } header={this.i18n('entity.NotificationConfiguration.level')} sort face="enum" enumClass={NotificationLevelEnum} />
            <Advanced.Column property="notificationType" width="75px" header={this.i18n('entity.NotificationConfiguration.notificationType')} sort face="text" />
            <Advanced.Column property="template" header={this.i18n('entity.NotificationConfiguration.template')} sort
              cell={
                ({ rowIndex, data }) => {
                  const templId = data[rowIndex].template;
                  if (!templId) {
                    return null;
                  }
                  //
                  return (
                    <Advanced.EntityInfo
                      entityType="notificationTemplate"
                      entityIdentifier={ templId }
                      entity={ data[rowIndex]._embedded.template }
                      face="popover" />
                  );
                }
                }
              />
            <Advanced.Column property="recipients" sort face="text" />
            <Advanced.Column property="redirect" width={ 75 } sort face="bool" />
            <Advanced.Column property="disabled" width={ 75 } sort face="bool" />
          </Advanced.Table>
        </Basic.Panel>

        <Basic.Modal
          bsSize="large"
          show={detail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static"
          keyboard={!_showLoading}
          showLoading={_supportedNotificationTypesLoading}>

          <form onSubmit={this.save.bind(this, {})}>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('create.header')} rendered={Utils.Entity.isNew(detail.entity)}/>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('edit.header', { name: detail.entity.topic })} rendered={!Utils.Entity.isNew(detail.entity)}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="form" data={detail.entity} showLoading={_showLoading}>
                <Basic.TextField
                  ref="topic"
                  label={this.i18n('entity.NotificationConfiguration.topic')}
                  required/>
                <Basic.EnumSelectBox
                  ref="level"
                  enum={NotificationLevelEnum}
                  useSymbol={false}
                  label={this.i18n('entity.NotificationConfiguration.level')}/>
                <Basic.EnumSelectBox
                  ref="notificationType"
                  label={this.i18n('entity.NotificationConfiguration.notificationType')}
                  options={!_supportedNotificationTypes ? null : _supportedNotificationTypes.map(type => { return { value: type, niceLabel: type }; })}
                  required/>
                <Basic.SelectBox
                  ref="template"
                  label={this.i18n('entity.NotificationConfiguration.template')}
                  manager={notificationTemplateManager}/>
                <Basic.TextArea
                  ref="description"
                  label={this.i18n('entity.NotificationConfiguration.description')}/>
                <Basic.TextField
                  ref="recipients"
                  label={this.i18n('entity.NotificationConfiguration.recipients.label')}
                  helpBlock={ this.i18n('entity.NotificationConfiguration.recipients.help') }/>
                <Basic.Checkbox
                  ref="redirect"
                  label={ this.i18n('entity.NotificationConfiguration.redirect.label') }
                  helpBlock={ this.i18n('entity.NotificationConfiguration.redirect.help') }/>
                <Basic.Checkbox
                  ref="disabled"
                  label={ this.i18n('entity.NotificationConfiguration.disabled.label') }
                  helpBlock={ this.i18n('entity.NotificationConfiguration.disabled.help') }/>
              </Basic.AbstractForm>
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.closeDetail.bind(this)}
                showLoading={_showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

NotificationConfigurations.propTypes = {
  _showLoading: PropTypes.bool,
  _supportedNotificationTypesLoading: PropTypes.bool
};
NotificationConfigurations.defaultProps = {
  _showLoading: false,
  _supportedNotificationTypesLoading: true
};

function select(state) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _supportedNotificationTypesLoading: Utils.Ui.isShowLoading(state, NotificationConfigurationManager.SUPPORTED_NOTIFICATION_TYPES),
    _supportedNotificationTypes: DataManager.getData(state, NotificationConfigurationManager.SUPPORTED_NOTIFICATION_TYPES),
    _searchParameters: Utils.Ui.getSearchParameters(state, uiKey)
  };
}

export default connect(select)(NotificationConfigurations);


/**
 * Table filter component
 *
 * @author Radek Tomiška
 * @author Patrik Stloukal
 */
class Filter extends Advanced.Filter {

  focus() {
    this.refs.text.focus();
  }

  render() {
    const { onSubmit, onCancel, supportedNotificationTypes } = this.props;
    //
    return (
      <Advanced.Filter onSubmit={ onSubmit }>
        <Basic.AbstractForm ref="filterForm">
          <Basic.Row>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.TextField
                ref="text"
                placeholder={this.i18n('entity.NotificationConfiguration.topic')}
                help={ Advanced.Filter.getTextHelp() }/>
            </Basic.Col>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.EnumSelectBox
                ref="level"
                placeholder={this.i18n('entity.NotificationConfiguration.level')}
                enum={NotificationLevelEnum}/>
            </Basic.Col>
            <Basic.Col lg={ 4 } className="text-right">
              <Advanced.Filter.FilterButtons cancelFilter={ onCancel }/>
            </Basic.Col>
          </Basic.Row>
          <Basic.Row className="lact">
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.EnumSelectBox
                ref="notificationType"
                placeholder={ this.i18n('entity.NotificationConfiguration.notificationType') }
                options={ !supportedNotificationTypes ? null : supportedNotificationTypes.map(type => { return { value: type, niceLabel: type }; }) }
                useObject={ false }
                useSymbol={ false }/>
            </Basic.Col>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.SelectBox
                ref="template"
                placeholder={this.i18n('entity.NotificationConfiguration.template')}
                multiSelect={false}
                manager={notificationTemplateManager}/>
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
      );
  }
}
