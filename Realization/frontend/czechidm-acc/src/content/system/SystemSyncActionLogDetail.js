import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Domain, Utils, Advanced } from 'czechidm-core';
import { SyncItemLogManager, SyncActionLogManager} from '../../redux';
import SynchronizationActionTypeEnum from '../../domain/SynchronizationActionTypeEnum';
import OperationResultTypeEnum from '../../domain/OperationResultTypeEnum';

const uiKey = 'system-synchronization-action-log';
const uiKeyLogs = 'system-synchronization-item-logs';
const syncItemLogManager = new SyncItemLogManager();
const syncActionLogManager = new SyncActionLogManager();

class SystemSyncActionLogDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state
    };
  }

  getManager() {
    return syncActionLogManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.systemSyncActionLogDetail';
  }

  showDetail(entity) {
    const {entityId} = this.props.params;
    this.context.router.push(`/system/${entityId}/synchronization-item-logs/${entity.id}/detail`);
  }

  componentWillReceiveProps(nextProps) {
    const { logActionId} = nextProps.params;
    if (logActionId && logActionId !== this.props.params.logActionId) {
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
    const {logActionId} = props.params;
    this.context.store.dispatch(syncActionLogManager.fetchEntity(logActionId));
    this.selectNavigationItems(['sys-systems', 'system-synchronization-configs']);
  }

  render() {
    const { _showLoading, _syncActionLog} = this.props;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('syncActionLogId', _syncActionLog ? _syncActionLog.id : Domain.SearchParameters.BLANK_UUID);
    const syncActionLog = _syncActionLog;

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm readOnly ref="form" data={syncActionLog} showLoading={_showLoading}>
              <Basic.EnumSelectBox
                ref="syncAction"
                enum={SynchronizationActionTypeEnum}
                label={this.i18n('acc:entity.SyncActionLog.syncAction')}/>
              <Basic.EnumSelectBox
                ref="operationResult"
                enum={OperationResultTypeEnum}
                label={this.i18n('acc:entity.SyncActionLog.operationResult')}/>
              <Basic.TextField
                ref="operationCount"
                label={this.i18n('acc:entity.SyncActionLog.operationCount')}/>
            </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={_showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        <Basic.ContentHeader rendered={syncActionLog} style={{ marginBottom: 0 }}>
          <Basic.Icon value="transfer"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('syncItemLogsHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={syncActionLog} className="no-border">
          <Advanced.Table
            ref="table"
            uiKey={uiKeyLogs}
            manager={syncItemLogManager}
            forceSearchParameters={forceSearchParameters}
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="displayName"
                        placeholder={this.i18n('filter.displayName.placeholder')}/>
                    </div>
                    <div className="col-lg-2"/>
                    <div className="col-lg-4 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            >
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
               property="displayName"
               header={this.i18n('acc:entity.SyncItemLog.displayName')}
               sort/>
            <Advanced.Column
               property="message"
               header={this.i18n('acc:entity.SyncItemLog.message')}
               sort/>
            <Advanced.Column
               property="type"
               header={this.i18n('acc:entity.SyncItemLog.type')}
               sort/>
            <Advanced.Column
               property="identification"
               header={this.i18n('acc:entity.SyncItemLog.identification')}
               sort/>
            </Advanced.Table>
          </Basic.Panel>
        </div>
    );
  }
}


SystemSyncActionLogDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemSyncActionLogDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, syncActionLogManager.getEntityType(), component.params.logActionId);
  return {
    _syncActionLog: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemSyncActionLogDetail);
