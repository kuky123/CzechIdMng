import React from 'react';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { LongRunningTaskItemManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';
import OperationStateEnum from '../../enums/OperationStateEnum';
//
const UIKEY = 'long-running-task-item-table';
const manager = new LongRunningTaskItemManager();

/**
 * Queue(list) of processed items
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
class LongRunningTaskItems extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
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
    return 'long-running-task-items';
  }

  getContentKey() {
    return 'content.scheduler.all-tasks';
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
    const { entityId } = this.props.params;
    const { filterOpened, detail } = this.state;
    const forceSearchParameters = new SearchParameters().setFilter('longRunningTaskId', entityId);
    //
    return (
      <Basic.Panel className="no-border last">
        <Advanced.Table
          ref="table"
          uiKey={ UIKEY }
          manager={ manager }
          forceSearchParameters={ forceSearchParameters }
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          className="no-margin"
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
          filterOpened={filterOpened}
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
                const entity = data[rowIndex];
                return (
                  <Advanced.OperationResult value={ entity.operationResult } detailLink={ () => this.showDetail(data[rowIndex]) }/>
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

function select(state) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, UIKEY)
  };
}

export default connect(select)(LongRunningTaskItems);
