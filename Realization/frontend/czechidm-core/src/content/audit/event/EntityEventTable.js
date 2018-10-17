import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { EntityEventManager, SecurityManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';
import EntityStateTableComponent, { EntityStateTable } from './EntityStateTable';
import OperationStateEnum from '../../../enums/OperationStateEnum';
import PriorityTypeEnum from '../../../enums/PriorityTypeEnum';

const manager = new EntityEventManager();

/**
 * Table of persisted entity events
 *
 * @author Radek Tomiška
 */
export class EntityEventTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened,
      detail: {
        show: false,
        entity: {},
        message: null
      }
    };
  }

  getContentKey() {
    return 'content.entityEvents';
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return this.props.uiKey;
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

  _refreshDetail() {
    const entity = this.state.detail.entity;
    //
    this.context.store.dispatch(manager.fetchEntity(entity.id, null, (refreshedEntity, error) => {
      if (!error) {
        this.setState({
          detail: {
            show: true,
            entity: refreshedEntity
          }
        });
      } else {
        let message;
        if (error.statusCode === 404) {
          message = {
            level: 'info',
            title: this.i18n('error.EVENT_NOT_FOUND.title'),
            message: this.i18n('error.EVENT_NOT_FOUND.message')
          };
        } else {
          message = this.flashMessagesManager.convertFromError(error);
        }
        this.setState({
          detail: {
            ...this.state.detail,
            message
          }
        }, () => {
          this.addErrorMessage({ hidden: true }, error);
          if (error.statusCode === 404) {
            this.refs.table.getWrappedInstance().reload();
          }
        });
      }
    }));
    this.refs.stateTable.getWrappedInstance().reload();
  }

  _deleteAll() {
    const { uiKey, } = this.props;
    //
    this.refs['confirm-deleteAll'].show(
      this.i18n(`action.deleteAll.message`),
      this.i18n(`action.deleteAll.header`)
    ).then(() => {
      this.context.store.dispatch(manager.deleteAll(uiKey, (entity, error) => {
        if (!error) {
          this.addMessage({ level: 'success', message: this.i18n('action.deleteAll.success')});
          this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
        } else {
          this.addError(error);
        }
      }));
    }, () => {
      // nothing
    });
  }

  render() {
    const {
      columns,
      forceSearchParameters,
      rendered,
      className,
      showDeleteAllButton
    } = this.props;
    const { filterOpened, detail } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    const _forceSearchParameters = forceSearchParameters || new SearchParameters();
    //
    let stateForceSearchParameters = new SearchParameters();
    if (detail.entity) {
      stateForceSearchParameters = stateForceSearchParameters.setFilter('eventId', detail.entity.id);
    }
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-deleteAll" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={ this.getUiKey() }
          manager={ this.getManager() }
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row className={ _.includes(columns, 'ownerType') ? '' : 'last' }>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate
                      ref="fromTill"
                      fromProperty="createdFrom"
                      tillProperty="createdTill"/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className={ _.includes(columns, 'ownerType') ? '' : 'last' }>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="states"
                      placeholder={ this.i18n('entity.EntityEvent.result.label') }
                      enum={ OperationStateEnum }
                      multiSelect
                      useSymbol={ false }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="rootId"
                      placeholder={ this.i18n('entity.EntityEvent.rootId.label') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="parentId"
                      placeholder={ this.i18n('entity.EntityEvent.parent.label') }/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last" rendered={ _.includes(columns, 'ownerType') }>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={ this.i18n('filter.text.placeholder') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="ownerType"
                      placeholder={ this.i18n('filter.ownerType.placeholder') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="ownerId"
                      placeholder={ this.i18n('filter.ownerId.placeholder') }/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={ filterOpened }
          forceSearchParameters={_forceSearchParameters}
          _searchParameters={ this.getSearchParameters() }
          showRowSelection
          className={ className }
          buttons={[
            <Basic.Button
              level="danger"
              key="delete-all-button"
              className="btn-xs"
              onClick={ this._deleteAll.bind(this) }
              rendered={ showDeleteAllButton && SecurityManager.hasAnyAuthority('APP_ADMIN') }
              title={ this.i18n('action.deleteAll.button.title') }
              titlePlacement="bottom"
              icon="fa:trash">
              { this.i18n('action.deleteAll.button.label') }
            </Basic.Button>
          ]}>
          <Advanced.Column
            property=""
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={() => this.showDetail(data[rowIndex])}/>
                );
              }
            }/>
          <Advanced.Column
            property="result"
            width={75}
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                return (
                  <Advanced.OperationResult value={ entity.result } detailLink={ () => this.showDetail(data[rowIndex]) }/>
                );
              }
            }
            rendered={_.includes(columns, 'result')}/>
          <Advanced.Column property="created" sort face="datetime" rendered={ _.includes(columns, 'created') } width={ 175 }/>
          <Advanced.Column
            property="superOwnerId"
            header={ this.i18n('entity.EntityEvent.superOwnerId.label') }
            rendered={_.includes(columns, 'superOwnerId')}
            cell={
              ({ rowIndex, data, property }) => {
                // TODO: add owner type int persistent entity
                return (
                  <Advanced.UuidInfo value={ data[rowIndex][property] } />
                );
              }
            }/>
          <Advanced.Column
            property="ownerType"
            rendered={_.includes(columns, 'ownerType')}
            width={ 200 }
            cell={
              ({ rowIndex, data, property }) => {
                return (
                  <span title={data[rowIndex][property]}>
                    { Utils.Ui.getSimpleJavaType(data[rowIndex][property]) }
                  </span>
                );
              }}
            />
          <Advanced.Column
            property="ownerId"
            header={ this.i18n('entity.EntityEvent.owner.label') }
            rendered={_.includes(columns, 'ownerId')}
            cell={
              ({ rowIndex, data, property }) => {
                //
                if (!data[rowIndex]._embedded || !data[rowIndex]._embedded[property]) {
                  return (
                    <Advanced.UuidInfo value={ data[rowIndex][property] } />
                  );
                }
                //
                return (
                  <Advanced.EntityInfo
                    entityType={ Utils.Ui.getSimpleJavaType(data[rowIndex].ownerType) }
                    entityIdentifier={ data[rowIndex][property] }
                    entity={ data[rowIndex]._embedded[property] }
                    face="popover"
                    showEntityType={ false }/>
                );
              }
            }/>
          <Advanced.Column property="eventType" sort rendered={_.includes(columns, 'eventType')} />
          <Advanced.Column property="priority" face="enum" enumClass={ PriorityTypeEnum } sort width="100px" rendered={_.includes(columns, 'priority')}/>
          <Advanced.Column property="instanceId" width={ 100 } rendered={_.includes(columns, 'instanceId')} />
            <Advanced.Column
              width={ 125 }
              property="rootId"
              rendered={_.includes(columns, 'root')}
              cell={
                ({ rowIndex, data, property }) => {
                  if (data[rowIndex][property]) {
                    // TODO: info card?
                    return (
                      <Advanced.UuidInfo value={ data[rowIndex][property] }/>
                    );
                  }
                  return null;
                }
              }/>
          <Advanced.Column
            width={ 125 }
            property="parent"
            rendered={_.includes(columns, 'parent')}
            cell={
              ({ rowIndex, data, property }) => {
                if (data[rowIndex][property]) {
                  // TODO: info card?
                  return (
                    <Advanced.UuidInfo value={ data[rowIndex][property] }/>
                  );
                }
                return data[rowIndex].parentEventType;
              }
            }/>
        </Advanced.Table>

        <Basic.Modal
          bsSize="large"
          show={ detail.show }
          onHide={ this.closeDetail.bind(this) }
          backdrop="static">
          <Basic.Modal.Header closeButton text={ this.i18n('event.detail.header') }/>
          <Basic.Modal.Body>
            <Basic.FlashMessage message={ detail.message } className="no-margin" />

            <Basic.AbstractForm ref="form" data={ detail.entity } readOnly>
              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.created') }>
                    <Advanced.DateValue value={ detail.entity.created } format={ this.i18n('format.datetimemilis') }/>
                  </Basic.LabelWrapper>
                  <Basic.LabelWrapper label={ this.i18n('entity.modified.label') }>
                    <Advanced.DateValue value={ detail.entity.modified } format={ this.i18n('format.datetimemilis') }/>
                  </Basic.LabelWrapper>
                  <Basic.LabelWrapper
                    label={ this.i18n('entity.EntityEvent.executeDate.label') }
                    helpBlock={ this.i18n('entity.EntityEvent.executeDate.help') }
                    rendered={ detail.entity.executeDate }>
                    <Advanced.DateValue value={ detail.entity.executeDate } showTime/>
                  </Basic.LabelWrapper>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.EntityEvent.instanceId.label') }>
                    { detail.entity.instanceId }
                    <span className="help-block">{ this.i18n('entity.EntityEvent.instanceId.help') }</span>
                  </Basic.LabelWrapper>
                </Basic.Col>
              </Basic.Row>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.EntityEvent.ownerType.label') }>
                    <span title={ detail.entity.ownerType }>
                      { Utils.Ui.getSimpleJavaType(detail.entity.ownerType) }
                    </span>
                  </Basic.LabelWrapper>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.EntityEvent.owner.label') }>
                    {
                      !detail.entity || !detail.entity.ownerType
                      ||
                      <Advanced.EntityInfo
                        entityType={ Utils.Ui.getSimpleJavaType(detail.entity.ownerType) }
                        entityIdentifier={ detail.entity.ownerId }
                        style={{ margin: 0 }}
                        face="popover"
                        showEntityType={ false }/>
                    }
                  </Basic.LabelWrapper>
                </Basic.Col>
              </Basic.Row>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.EntityEvent.eventType.label') }>
                    { detail.entity.eventType }
                  </Basic.LabelWrapper>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.LabelWrapper label={ this.i18n('entity.EntityEvent.priority.label') }>
                    <Basic.EnumValue value={ detail.entity.priority } enum={ PriorityTypeEnum }/>
                  </Basic.LabelWrapper>
                </Basic.Col>
              </Basic.Row>

              <Advanced.OperationResult value={ detail.entity.result } face="full" rendered={ !detail.message }/>
            </Basic.AbstractForm>

            <Basic.ContentHeader text={ this.i18n('state.header') } style={{ marginBottom: 0 }} rendered={ !detail.message } />

            <EntityStateTableComponent
              ref="stateTable"
              uiKey={ `entity-event-state-table-${detail.entity.id}` }
              rendered={ !detail.message && detail.entity.id !== undefined && detail.entity.id !== null }
              showFilter={ false }
              showToolbar
              forceSearchParameters={ stateForceSearchParameters }
              columns={ _.difference(EntityStateTable.defaultProps.columns, ['ownerType', 'ownerId', 'event', 'instanceId']) }
              className="no-margin"/>
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={this.closeDetail.bind(this)}>
              {this.i18n('button.close')}
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </div>
    );
  }
}

EntityEventTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Rendered
   */
  rendered: PropTypes.bool
};

EntityEventTable.defaultProps = {
  columns: ['result', 'created', 'ownerType', 'ownerId', 'eventType', 'priority', 'instanceId', 'root', 'parent'],
  filterOpened: false,
  forceSearchParameters: null,
  rendered: true,
  showDeleteAllButton: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(EntityEventTable);
