import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Domain, Managers, Utils, Advanced } from 'czechidm-core';
import { SynchronizationLogManager, SyncActionLogManager} from '../../redux';
import SynchronizationActionTypeEnum from '../../domain/SynchronizationActionTypeEnum';
import OperationResultTypeEnum from '../../domain/OperationResultTypeEnum';

const uiKey = 'system-synchronization-log';
const uiKeyLogs = 'system-synchronization-action-logs';
const synchronizationLogManager = new SynchronizationLogManager();
const syncActionLogManager = new SyncActionLogManager();

class SystemSynchronizationLogDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state
    };
  }

  getManager() {
    return synchronizationLogManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.systemSynchronizationLogDetail';
  }

  showDetail(entity) {
    const {entityId} = this.props.params;
    this.context.router.push(`/system/${entityId}/synchronization-action-logs/${entity.id}/detail`);
  }

  componentWillReceiveProps(nextProps) {
    const { logId} = nextProps.params;
    if (logId && logId !== this.props.params.logId) {
      this._initComponent(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const {logId} = props.params;
    this.context.store.dispatch(synchronizationLogManager.fetchEntity(logId));
    this.selectNavigationItems(['sys-systems', 'system-synchronization-configs']);
  }

  render() {
    const { _showLoading, _synchronizationLog} = this.props;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('synchronizationLogId', _synchronizationLog ? _synchronizationLog.id : Domain.SearchParameters.BLANK_UUID);
    const synchronizationLog = _synchronizationLog;

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm readOnly ref="form" data={synchronizationLog} showLoading={_showLoading}>
              <Basic.Checkbox
                ref="running"
                label={this.i18n('acc:entity.SynchronizationLog.running')}/>
              <Basic.Checkbox
                ref="containsError"
                label={this.i18n('acc:entity.SynchronizationLog.containsError')}/>
              <Basic.DateTimePicker
                ref="started"
                label={this.i18n('acc:entity.SynchronizationLog.started')}/>
              <Basic.DateTimePicker
                ref="ended"
                label={this.i18n('acc:entity.SynchronizationLog.ended')}/>
              <Basic.TextField
                ref="token"
                label={this.i18n('acc:entity.SynchronizationLog.token')}/>
              <Basic.TextArea
                ref="log"
                rows="15"
                label={this.i18n('acc:entity.SyncItemLog.log')}/>
            </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={_showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        <Basic.ContentHeader rendered={synchronizationLog} style={{ marginBottom: 0 }}>
          <Basic.Icon value="transfer"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('syncActionLogsHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={synchronizationLog} className="no-border">
          <Advanced.Table
            ref="table"
            uiKey={uiKeyLogs}
            manager={syncActionLogManager}
            forceSearchParameters={forceSearchParameters}
            showRowSelection={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])}
            actions={
              Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }>
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex], false)}/>
                  );
                }
              }/>
            <Advanced.Column
               property="syncAction"
               face="enum"
               enumClass={SynchronizationActionTypeEnum}
               header={this.i18n('acc:entity.SyncActionLog.syncAction')}
               sort/>
            <Advanced.Column
               property="operationResult"
               face="enum"
               enumClass={OperationResultTypeEnum}
               header={this.i18n('acc:entity.SyncActionLog.operationResult')}
               sort/>
             <Advanced.Column
               property="operationCount"
               face="text"
               header={this.i18n('acc:entity.SyncActionLog.operationCount')}
               sort/>
            </Advanced.Table>
          </Basic.Panel>
        </div>
    );
  }
}


SystemSynchronizationLogDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemSynchronizationLogDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, synchronizationLogManager.getEntityType(), component.params.logId);
  return {
    _synchronizationLog: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemSynchronizationLogDetail);
