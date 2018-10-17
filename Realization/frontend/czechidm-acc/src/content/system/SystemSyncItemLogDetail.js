import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Advanced, Utils } from 'czechidm-core';
import { SyncItemLogManager } from '../../redux';

const uiKey = 'system-synchronization-item-log';
const syncItemLogManager = new SyncItemLogManager();

class SystemSyncItemLogDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state
    };
  }

  getManager() {
    return syncItemLogManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.SystemSyncItemLogDetail';
  }

  componentWillReceiveProps(nextProps) {
    const { logItemId} = nextProps.params;
    if (logItemId && logItemId !== this.props.params.logItemId) {
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
    const {logItemId} = props.params;
    this.context.store.dispatch(syncItemLogManager.fetchEntity(logItemId));
    this.selectNavigationItems(['sys-systems', 'system-synchronization-configs']);
  }

  render() {
    const { _showLoading, _syncItemLog} = this.props;
    const syncItemLog = _syncItemLog;

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm readOnly ref="form" data={syncItemLog} showLoading={_showLoading}>
              <Basic.TextField
                ref="displayName"
                label={this.i18n('acc:entity.SyncItemLog.displayName')}/>
              <Basic.TextField
                ref="type"
                label={this.i18n('acc:entity.SyncItemLog.type')}/>
              <Basic.TextField
                ref="identification"
                label={this.i18n('acc:entity.SyncItemLog.identification')}/>
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
        </div>
    );
  }
}


SystemSyncItemLogDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemSyncItemLogDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, syncItemLogManager.getEntityType(), component.params.logItemId);
  return {
    _syncItemLog: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemSyncItemLogDetail);
