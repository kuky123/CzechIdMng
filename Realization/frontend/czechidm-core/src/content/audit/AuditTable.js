import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Utils from '../../utils';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { AuditManager } from '../../redux';
import AuditModificationEnum from '../../enums/AuditModificationEnum';

const auditManager = new AuditManager();

/**
* Table of Audit for entities
*
* @author Ondřej Kopr
*/
export class AuditTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showLoading: true
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    // TODO: use redux and load it just one time
    this.context.store.dispatch(auditManager.fetchEntities(auditManager.getAuditedEntitiesNames(), null, (entities) => {
      if (entities !== null) {
        const auditedEntities = entities._embedded.strings.map(item => { return {value: item.content, niceLabel: item.content }; });
        this.setState({
          auditedEntities,
          showLoading: false
        });
      } else {
        this.setState({
          showLoading: false
        });
      }
    }));
  }

  getContentKey() {
    return 'content.audit';
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
    if (this.refs.table !== undefined) {
      this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
    }
  }

  /**
  * Method get last string of split string by dot.
  * Used for get niceLabel for type entity.
  */
  _getType(name) {
    return Utils.Ui.getSimpleJavaType(name);
  }

  /**
  * Method get last string of arrays split string by dot.
  * Used method _getType
  */
  _getTypeArray(arrayOfName) {
    for (const index in arrayOfName) {
      if (arrayOfName.hasOwnProperty(index)) {
        arrayOfName[index] = this._getType(arrayOfName[index]);
      }
    }
    return _.join(arrayOfName, ', ');
  }

  _getAdvancedFilter(auditedEntities, showLoading, columns) {
    return (
      <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
        <Basic.AbstractForm ref="filterForm" showLoading={showLoading}>
          <Basic.Row>
            <Basic.Col lg={ 8 } rendered={ _.includes(columns, 'revisionDate') }>
              <Advanced.Filter.FilterDate ref="fromTill"/>
            </Basic.Col>
            <Basic.Col lg={ 4 } className="text-right">
              <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
            </Basic.Col>
          </Basic.Row>
          <Basic.Row>
            <Basic.Col lg={ 4 } rendered={ _.includes(columns, 'type') }>
              <Advanced.Filter.EnumSelectBox
                ref="type"
                searchable
                placeholder={this.i18n('entity.Audit.type')}
                options={ auditedEntities }/>
            </Basic.Col>
            <Basic.Col lg={ 4 } rendered={ _.includes(columns, 'modification') }>
              <Advanced.Filter.EnumSelectBox
                ref="modification"
                placeholder={this.i18n('entity.Audit.modification')}
                enum={AuditModificationEnum}/>
            </Basic.Col>
            <Basic.Col lg={ 4 } rendered={ _.includes(columns, 'modifier') }>
              <Advanced.Filter.TextField
                className="pull-right"
                ref="modifier"
                placeholder={this.i18n('entity.Audit.modifier')}
                returnProperty="username"/>
            </Basic.Col>
          </Basic.Row>
          <Basic.Row className="last">
            <Basic.Col lg={ 4 } rendered={ _.includes(columns, 'entityId') }>
              <Advanced.Filter.TextField
                ref="entityId"
                placeholder={this.i18n('entity.Audit.entityId')}/>
            </Basic.Col>
            <Basic.Col lg={ 4 } rendered={ _.includes(columns, 'changedAttributes') }>
              <Advanced.Filter.TextField
                ref="changedAttributes"
                placeholder={this.i18n('entity.Audit.changedAttributes')}/>
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
    );
  }

  /**
   * Method for show detail of revision, redirect to detail
   *
   * @param entityId id of revision
   */
  showDetail(entityId) {
    this.context.router.push(`/audit/entities/${entityId}/diff/`);
  }

  _getForceSearchParameters() {
    const { entityId, entityClass } = this.props;

    if (entityId !== undefined || entityClass !== undefined) {
      return auditManager.getDefaultSearchParameters().setFilter('type', entityClass).setFilter('entityId', entityId);
    }
    return null;
  }

  render() {
    const { columns, uiKey } = this.props;
    const { showLoading, auditedEntities } = this.state;
    return (
      <div>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          filterOpened
          manager={auditManager}
          forceSearchParameters={this._getForceSearchParameters()}
          rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          showId
          filter={ this._getAdvancedFilter(auditedEntities, showLoading, columns) }
          _searchParameters={ this.getSearchParameters() }>
          <Advanced.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.showDetail.bind(this, data[rowIndex].id)}/>
                );
              }
            }
            sort={false}/>
          <Advanced.Column
            property="type"
            rendered={_.includes(columns, 'type')}
            width={ 200 }
            cell={
              ({ rowIndex, data, property }) => {
                return (
                  <span title={data[rowIndex][property]}>
                    { this._getType(data[rowIndex][property]) }
                  </span>
                );
              }}
            />
          <Advanced.Column
            property="entityId"
            header={ this.i18n('entity.Audit.entity') }
            rendered={_.includes(columns, 'entityId')}
            cell={
              /* eslint-disable react/no-multi-comp */
              ({ rowIndex, data, property }) => {
                const value = data[rowIndex][property];
                //
                if (!data[rowIndex]._embedded || !data[rowIndex]._embedded[property]) {
                  return (
                    <Advanced.UuidInfo value={ value } />
                  );
                }
                return (
                  <Advanced.EntityInfo
                    entityType={ this._getType(data[rowIndex].type) }
                    entityIdentifier={ value }
                    entity={ data[rowIndex]._embedded[property] }
                    face="popover"
                    showEntityType={ false }/>
                );
              }
            }/>
          <Advanced.Column
            property="modification"
            width={ 100 }
            sort
            rendered={_.includes(columns, 'modification')}
            cell={
              ({ rowIndex, data, property }) => {
                return <Basic.Label level={AuditModificationEnum.getLevel(data[rowIndex][property])} text={AuditModificationEnum.getNiceLabel(data[rowIndex][property])}/>;
              }}/>
          <Advanced.Column property="modifier" sort face="text" rendered={_.includes(columns, 'modifier')}/>
          <Advanced.Column property="timestamp" header={this.i18n('entity.Audit.revisionDate')} sort face="datetime" rendered={_.includes(columns, 'revisionDate')}/>
          <Advanced.Column hidden
            property="changedAttributes"
            rendered={_.includes(columns, 'changedAttributes')}
            cell={
              ({ rowIndex, data, property }) => {
                return _.replace(data[rowIndex][property], ',', ', ');
              }
            }
          />
          <Advanced.Column hidden
              property="modifiedEntityNames" sort
              rendered={_.includes(columns, 'modifiedEntityNames')}
              cell={
                ({ rowIndex, data, property }) => {
                  return this._getTypeArray(data[rowIndex][property]);
                }
              }
            />
        </Advanced.Table>
      </div>
    );
  }
}

AuditTable.propTypes = {
  // table uiKey
  uiKey: PropTypes.string.isRequired,
  // columns for display, check default props.
  columns: PropTypes.arrayOf(PropTypes.string),
  // simple name of entity class, if this paramater isn't defined show all revisions
  entityClass: PropTypes.string,
  // id of entity
  entityId: PropTypes.number,
  // flag for detail
  isDetail: PropTypes.bool
};

AuditTable.defaultProps = {
  columns: ['id', 'type', 'modification', 'modifier', 'revisionDate', 'entityId', 'changedAttributes'],
  isDetail: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(AuditTable);
