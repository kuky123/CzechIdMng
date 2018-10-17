import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { LongRunningTaskItemManager, LongRunningTaskManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';
import OperationStateEnum from '../../enums/OperationStateEnum';
import { SecurityManager } from '../../redux';

const UIKEY = 'long-running-task-queue-table';
const manager = new LongRunningTaskItemManager();
const longRunningTaskManager = new LongRunningTaskManager();

/**
 * Shows all items which were already added into the queue. Supports delete also.
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
class LongRunningTaskQueue extends Advanced.AbstractTableContent {

  constructor(props) {
    super(props);
    //
    this.state = {
      ...this.state,
      filterOpened: false
    };
  }

  getManager() {
    return manager;
  }

  getNavigationKey() {
    return 'long-running-task-queue';
  }

  getContentKey() {
    return 'content.scheduler.all-tasks';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    this.context.store.dispatch(longRunningTaskManager.fetchEntity(entityId));
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

  render() {
    const { entity, showLoading } = this.props;
    const { filterOpened, detail } = this.state;
    //
    if (showLoading) {
      return (
        <Basic.Loading isStatic show />
      );
    }
    //
    if (!entity) {
      return null;
    }
    //
    return (
      <Basic.Panel className="no-border last">
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        {
          !entity.scheduledTask
          ?
          <Basic.PanelBody style={{ padding: '15px 0' }}>
            <Basic.Alert level="info" text={ this.i18n('detail.scheduledTask.empty') } className="no-margin" />
          </Basic.PanelBody>
          :
          <Advanced.Table
            ref="table"
            showRowSelection={ SecurityManager.hasAnyAuthority(['SCHEDULER_EXECUTE']) }
            uiKey={ UIKEY }
            manager={ manager }
            forceSearchParameters={ new SearchParameters().setFilter('scheduledTaskId', entity.scheduledTask) }
            rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.EnumSelectBox
                        ref="operationState"
                        placeholder={this.i18n('entity.LongRunningTaskItem.result.state')}
                        enum={OperationStateEnum}/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.TextField
                        ref="referencedEntityId"
                        placeholder={this.i18n('entity.LongRunningTaskItem.referencedEntityId.placeholder')}/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 } className="text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </Basic.Col>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            filterOpened={ filterOpened }
            actions={
              SecurityManager.hasAnyAuthority(['SCHEDULER_EXECUTE'])
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }
            _searchParameters={ this.getSearchParameters() }>
            <Advanced.Column
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
            <Advanced.Column
              property="operationResult.state"
              header={this.i18n('entity.LongRunningTaskItem.result.state')}
              width={75}
              sort
              cell={
                ({ data, rowIndex }) => {
                  return (
                    <Advanced.OperationResult value={ data[rowIndex].operationResult } detailLink={ () => this.showDetail(data[rowIndex]) }/>
                  );
                }
              }
            />
            <Advanced.Column
              property="referencedEntityId"
              header={this.i18n('entity.LongRunningTaskItem.referencedEntityId')}
              cell={
                ({ rowIndex, data, property }) => {
                  if (!data[rowIndex]._embedded || !data[rowIndex]._embedded[property]) {
                    return (
                      <Advanced.UuidInfo value={ data[rowIndex][property] } />
                    );
                  }
                  //
                  return (
                    <Advanced.EntityInfo
                      entityType={ Utils.Ui.getSimpleJavaType(data[rowIndex].referencedDtoType) }
                      entityIdentifier={ data[rowIndex][property] }
                      entity={ data[rowIndex]._embedded[property] }
                      face="popover"
                      showEntityType={ false }/>
                  );
                }
              }
              sort
              face="text"
            />
            <Advanced.Column
              property="referencedDtoType"
              header={this.i18n('entity.LongRunningTaskItem.referencedDtoType')}
              width={75}
              sort
              face="text"
              cell={
                ({ rowIndex, data, property }) => {
                  const javaType = data[rowIndex][property];
                  return (
                    <span title={ javaType }>{ Utils.Ui.getSimpleJavaType(javaType) }</span>
                  );
                }
              }
            />
            <Advanced.Column
              property="created"
              header={this.i18n('entity.created')}
              width={75}
              sort
              face="datetime"
            />
          </Advanced.Table>
        }

        <Basic.Modal
          show={ detail.show }
          onHide={ this.closeDetail.bind(this) }
          backdrop="static">

          <Basic.Modal.Header text={ this.i18n('detail.header') }/>
          <Basic.Modal.Body>
            <Advanced.OperationResult value={ detail.entity ? detail.entity.operationResult : null } face="full"/>
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={this.closeDetail.bind(this)}>
              {this.i18n('button.close')}
            </Basic.Button>
          </Basic.Modal.Footer>

        </Basic.Modal>
      </Basic.Panel>
    );
  }
}

LongRunningTaskQueue.propTypes = {
  /**
   * Loaded LRT entity
   */
  entity: PropTypes.object,
  /**
   * Entity is currently loaded from BE
   */
  showLoading: PropTypes.bool
};

LongRunningTaskQueue.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: longRunningTaskManager.getEntity(state, entityId),
    showLoading: longRunningTaskManager.isShowLoading(state, null, entityId),
    _searchParameters: Utils.Ui.getSearchParameters(state, UIKEY)
  };
}

export default connect(select)(LongRunningTaskQueue);
