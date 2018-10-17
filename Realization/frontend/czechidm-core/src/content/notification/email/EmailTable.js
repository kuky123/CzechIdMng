import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { IdentityManager } from '../../../redux';
import NotificationFilter from '../NotificationFilter';
import NotificationRecipientsCell from '../NotificationRecipientsCell';
import NotificationSentState from '../NotificationSentState';
import NotificationLevelEnum from '../../../enums/NotificationLevelEnum';

/**
* Table of audit log for emails
*
* @author Radek Tomiška
*/
export class EmailTable extends Advanced.AbstractTableContent {

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
    return 'content.emails';
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
    this.context.router.push('/notification/emails/' + entity.id);
  }

  _getStatus(data, rowIndex) {
    return (
      <NotificationSentState notification={data[rowIndex]}/>
    );
  }

  render() {
    const { uiKey, emailManager } = this.props;
    const { filterOpened } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={emailManager}
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          filterOpened={filterOpened}
          filter={
            <NotificationFilter
              ref="filterForm"
              onSubmit={ this.useFilter.bind(this) }
              onCancel={ this.cancelFilter.bind(this) }/>
          }
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
          <Advanced.Column property="created" sort face="datetime"/>
          <Advanced.Column property="message.level" sort face="enum" enumClass={NotificationLevelEnum}/>
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
              ({ data, rowIndex}) => {
                return this._getStatus(data, rowIndex);
              }
            }
            />
          <Advanced.Column property="sentLog" sort face="text" width="20%"/>
        </Advanced.Table>
      </div>
    );
  }
}

EmailTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  emailManager: PropTypes.object.isRequired,
  filterOpened: PropTypes.bool
};

EmailTable.defaultProps = {
  filterOpened: false,
  _showLoading: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    _showLoading: component.emailManager.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { withRef: true })(EmailTable);
