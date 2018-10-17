import React from 'react';
import PropTypes from 'prop-types';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import _ from 'lodash';
import moment from 'moment';
import { LocalizationService } from '../../services';

/**
 * Table with long running task items and detail of LRT
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
export default class LongRunningTaskDetail extends Basic.AbstractContent {

  constructor(props) {
    super(props);
  }

  getContentKey() {
    return 'content.scheduler.all-tasks';
  }

  render() {
    const { entity } = this.props;
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.Panel className="no-border last">
          <Basic.PanelHeader text={ this.i18n('tabs.basic') } />

          <Basic.PanelBody style={{ padding: 0 }}>

            <Basic.AbstractForm data={entity} readOnly>
              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={this.i18n('entity.created')}>
                    <Advanced.DateValue value={entity.created} showTime/>
                  </Basic.LabelWrapper>
                  {
                    !entity.taskStarted
                    ||
                    <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.started')}>
                      <Advanced.DateValue value={entity.taskStarted} showTime/>
                    </Basic.LabelWrapper>
                  }
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.instanceId.label')}>
                    {entity.instanceId}
                    <span className="help-block">{this.i18n('entity.LongRunningTask.instanceId.help')}</span>
                  </Basic.LabelWrapper>
                </Basic.Col>
              </Basic.Row>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.taskType')}>
                    { Utils.Ui.getSimpleJavaType(entity.taskType) }
                  </Basic.LabelWrapper>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.taskProperties.label')}>
                    {
                      _.keys(entity.taskProperties).map(propertyName => {
                        if (Utils.Ui.isEmpty(entity.taskProperties[propertyName])) {
                          return null;
                        }
                        return (
                          <div>{ propertyName }: { Utils.Ui.toStringValue(entity.taskProperties[propertyName]) }</div>
                        );
                      })
                    }
                  </Basic.LabelWrapper>
                </Basic.Col>
              </Basic.Row>

              <Basic.TextArea
                label={this.i18n('entity.LongRunningTask.taskDescription')}
                disabled
                value={entity.taskDescription}/>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  {
                    !entity.taskStarted
                    &&
                    <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.notstarted')}/>
                    ||
                    <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.counter')}>
                      <Advanced.LongRunningTask entity={ entity } face="count"/>
                    </Basic.LabelWrapper>
                  }
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  {
                    !entity.taskStarted
                    ||
                    <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.duration')}>
                      <Basic.Tooltip
                        ref="popover"
                        placement="bottom"
                        value={ moment.utc(moment.duration(moment(entity.modified).diff(moment(entity.taskStarted))).asMilliseconds()).format(this.i18n('format.times'))}>
                        <span>
                          { moment.duration(moment(entity.taskStarted).diff(moment(entity.modified))).locale(LocalizationService.getCurrentLanguage()).humanize() }
                        </span>
                      </Basic.Tooltip>
                    </Basic.LabelWrapper>
                  }
                </Basic.Col>
              </Basic.Row>

              <Advanced.OperationResult value={ entity.result } face="full" />

            </Basic.AbstractForm>
          </Basic.PanelBody>

          <Basic.PanelFooter>
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>
              {this.i18n('button.back')}
            </Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
      </div>
    );
  }
}

LongRunningTaskDetail.propTypes = {
  entity: PropTypes.object,
};
