import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import { SecurityManager } from '../../../redux';
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';

/**
 * Table with definitions of password policies
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
export class TemplateTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: true
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.refs.text.focus();
  }

  getContentKey() {
    return 'content.notificationTemplate';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }

    this.refs.table.getWrappedInstance().useFilterData(this.refs.filterForm.getData());
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  onRedeployOrBackup(bulkActionValue, selectedRows) {
    const { manager, uiKey } = this.props;
    const selectedEntities = manager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    // show confirm message for notification operation redeploy/backup
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: manager.getNiceLabel(selectedEntities[0]), records: manager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: manager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(manager.notificationBulkOperationForEntities(selectedEntities, bulkActionValue, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.${bulkActionValue}.error`, { record: manager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          // refresh data in table
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      // nothing
    });
  }

  onDelete(bulkActionValue, selectedRows) {
    const { uiKey, manager } = this.props;
    const selectedEntities = manager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    // show confirm message for deleting entity or entities
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: manager.getNiceLabel(selectedEntities[0]), records: manager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: manager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      // try delete
      this.context.store.dispatch(manager.deleteEntities(selectedEntities, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: manager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          // refresh data in table
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      //
    });
  }

  /**
   * Recive new form for create new type else show detail for existing notification templates
   */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/notification/templates/${uuidId}?new=1`);
    } else {
      this.context.router.push('/notification/templates/' + entity.id);
    }
  }

  render() {
    const { uiKey, manager } = this.props;
    const { filterOpened } = this.state;

    return (
      <Basic.Row>
        <div className="col-lg-12">
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Basic.Confirm ref="confirm-backup" level="danger"/>
          <Basic.Confirm ref="confirm-redeploy" level="danger"/>
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={manager}
            showRowSelection={SecurityManager.hasAuthority('NOTIFICATIONTEMPLATE_DELETE')}
            rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row>
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.text')}/>
                    </div>
                    <div className="col-lg-6 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            filterOpened={filterOpened}
            actions={
              [
                { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false },
                { value: 'redeploy', niceLabel: this.i18n('action.redeploy.action'), action: this.onRedeployOrBackup.bind(this), disabled: false },
                { value: 'backup', niceLabel: this.i18n('action.backup.action'), action: this.onRedeployOrBackup.bind(this), disabled: false }
              ]
            }
            buttons={
              [
                <Basic.Button level="success" key="add_button" className="btn-xs"
                        onClick={this.showDetail.bind(this, {})}
                        rendered={SecurityManager.hasAuthority('NOTIFICATIONTEMPLATE_CREATE')}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
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
              }
              sort={false}/>
            <Advanced.Column property="code" width="125px" sort/>
            <Advanced.Column property="name" sort
              cell={
              ({ rowIndex, data }) => {
                if (data[rowIndex].module) {
                  return (data[rowIndex].name + ' ' + '(' + data[rowIndex].module + ')');
                }
                return (data[rowIndex].name);
              }
            }/>
            <Advanced.Column property="subject" sort/>
            <Advanced.Column
              property="unmodifiable"
              header={this.i18n('entity.NotificationTemplate.unmodifiable.name')}
              face="bool"
              sort />
          </Advanced.Table>
        </div>
      </Basic.Row>
    );
  }
}

TemplateTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  passwordPolicyManager: PropTypes.object.isRequired
};

TemplateTable.defaultProps = {
  _showLoading: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(TemplateTable);
