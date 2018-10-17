import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import * as Basic from '../../../components/basic';
import { LoggingEventManager } from '../../../redux';
import LogTypeEnum from '../../../enums/LogTypeEnum';
import LoggingEventExceptionDetail from './LoggingEventExceptionDetail';

/**
* Basic detail for template detail,
* this detail is also used for create entity.
*
* @author Ondřej Kopr
*/

const manager = new LoggingEventManager();

class LoggingEventDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.audit.logging-event';
  }

  getNavigationKey() {
    return 'audit-logging-events';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    this.context.store.dispatch(manager.fetchEntity(entityId));
  }

  render() {
    const { uiKey, entity, showLoading } = this.props;
    //
    return (
      <Basic.Row>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader>
          <Basic.Icon value="eye-open"/>
          {' '}
          {this.i18n('header-detail')}
          {' '}
          <small>{this.i18n('detail')}</small>
        </Basic.PageHeader>

        <Basic.Panel>
          <Basic.AbstractForm
            data={entity} readOnly
            ref="form"
            uiKey={uiKey}
            style={{ padding: '15px 15px 0 15px' }}
            showLoading={ showLoading }>
            <Basic.Row>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="id"
                  label={this.i18n('entity.LoggingEvent.id')}/>
              </div>
              <div className="col-lg-9">
                <Basic.TextField
                  ref="loggerName"
                  label={this.i18n('entity.LoggingEvent.loggerName')}/>
              </div>
            </Basic.Row>
            <Basic.Row>
              <div className="col-lg-3">
                <Basic.DateTimePicker
                  ref="timestmp"
                  label={this.i18n('entity.LoggingEvent.timestmp')}
                  timeFormat={ this.i18n('format.times') }/>
              </div>
              <div className="col-lg-4">
                <Basic.EnumSelectBox
                  ref="levelString" enum={LogTypeEnum}
                  label={this.i18n('entity.LoggingEvent.levelString')}/>
              </div>
              <div className="col-lg-5">
                <Basic.TextField
                  ref="threadName"
                  label={this.i18n('entity.LoggingEvent.threadName')}/>
              </div>
            </Basic.Row>
            <Basic.TextField
              ref="callerFilename"
              label={this.i18n('entity.LoggingEvent.callerFilename')}/>
            <Basic.TextField
              ref="callerClass"
              label={this.i18n('entity.LoggingEvent.callerClass')}/>
            <Basic.Row>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="callerLine"
                  label={this.i18n('entity.LoggingEvent.callerLine')}/>
              </div>
              <div className="col-lg-9">
                <Basic.TextField
                  ref="callerMethod"
                  label={this.i18n('entity.LoggingEvent.callerMethod')}/>
              </div>
            </Basic.Row>
            <Basic.Row>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="arg0"
                  hidden={entity && entity.arg0 === null}
                  label={this.i18n('entity.LoggingEvent.arg0')}/>
              </div>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="arg1" hidden={entity && entity.arg1 === null}
                  label={this.i18n('entity.LoggingEvent.arg1')}/>
              </div>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="arg2" hidden={entity && entity.arg2 === null}
                  label={this.i18n('entity.LoggingEvent.arg2')}/>
              </div>
              <div className="col-lg-3">
                <Basic.TextField
                  ref="arg3" hidden={entity && entity.arg3 === null}
                  label={this.i18n('entity.LoggingEvent.arg3')}/>
              </div>
            </Basic.Row>

            <Basic.TextArea
              ref="formattedMessage"
              label={this.i18n('entity.LoggingEvent.formattedMessage')}/>

            <Basic.ContentHeader style={{ marginBottom: 0 }} className="marginable">
              <Basic.Icon value="warning-sign"/>
              {' '}
              { this.i18n('exceptions', { escape: false }) }
            </Basic.ContentHeader>
          </Basic.AbstractForm>

          {
            entity
            ?
            <LoggingEventExceptionDetail eventId={entity.id} />
            :
            <Basic.Loading show />
          }

          <Basic.PanelFooter>
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
      </Basic.Row>
    );
  }
}

LoggingEventDetail.propTypes = {
  uiKey: PropTypes.string.isRequired,
  isNew: PropTypes.bool
};
LoggingEventDetail.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;

  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(LoggingEventDetail);
