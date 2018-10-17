import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { IdentityManager, SecurityManager } from '../../redux';
import NotificationFilter from './NotificationFilter';
import NotificationLevelEnum from '../../enums/NotificationLevelEnum';
import NotificationRecipientsCell from './NotificationRecipientsCell';
import NotificationSentState from './NotificationSentState';
import SearchParameters from '../../domain/SearchParameters';

/**
* Audit for sent notifications
*
* @author Radek Tomiška
*/
export class NotificationTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
      detail: {
        show: false,
        entity: {}
      }
    };
    this.identityManager = new IdentityManager();
  }

  getContentKey() {
    return 'content.notifications';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }

    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/notification/notification/${uuidId}?new=1`);
    } else {
      this.context.router.push('/notification/notification/' + entity.id);// notification/configurations
    }
  }

  _getStatus(data, rowIndex) {
    return (
      <NotificationSentState notification={data[rowIndex]}/>
    );
  }

  render() {
    const { uiKey, notificationManager } = this.props;
    const { filterOpened } = this.state;
    const forceSearchParameters = new SearchParameters().setFilter('notificationType', 'notification');
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={notificationManager}
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          filterOpened={filterOpened}
          filter={
            <NotificationFilter
              ref="filterForm"
              onSubmit={ this.useFilter.bind(this) }
              onCancel={ this.cancelFilter.bind(this) }/>
          }
          buttons={
            [
              <Basic.Button level="success" key="add_button" className="btn-xs" onClick={this.showDetail.bind(this, {})} rendered={SecurityManager.hasAuthority('NOTIFICATION_CREATE')}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.send')}
              </Basic.Button>
            ]
          }
          forceSearchParameters={ forceSearchParameters }
          _searchParameters={ this.getSearchParameters() }>

          <Advanced.Column
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
          <Advanced.Column property="type" header={this.i18n('entity.Notification.type')} rendered={ false }/>
          <Advanced.Column property="created" sort face="datetime"/>
          <Advanced.Column property="topic" sort face="text"/>
          <Advanced.Column property="message.level" sort face="enum" enumClass={ NotificationLevelEnum }/>
          <Advanced.Column property="message.subject" sort face="text"/>
          <Advanced.Column
            property="recipients"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <NotificationRecipientsCell notification={ data[rowIndex] } identityOnly />
                );
              }
            }/>
          <Advanced.Column
            property="sender"
            cell={
              ({ rowIndex, data }) => {
                return !data[rowIndex]._embedded ? null : this.identityManager.getNiceLabel(data[rowIndex]._embedded.identitySender);
              }
            }/>
          <Advanced.Column
            property="sent"
            cell={
              ({ rowIndex, data }) => {
                return this._getStatus(data, rowIndex);
              }
            }/>
          <Advanced.Column property="sentLog" sort face="text" rendered={false} width="300px"/>
        </Advanced.Table>
      </div>
    );
  }
}

NotificationTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  notificationManager: PropTypes.object.isRequired,
  filterOpened: PropTypes.bool
};

NotificationTable.defaultProps = {
  filterOpened: false,
  _showLoading: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _showLoading: component.notificationManager.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { withRef: true })(NotificationTable);
