import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { IdentityManager } from '../../redux';
/**
* Table of tasks
* Without given props search parameter show this table only tasks for logged identity.
*
* FIXME: remember filled filter (generalize AbstractTableContent ... exception now)
*
* @author Vít Švanda
*/
export class TaskInstanceTable extends Basic.AbstractContent {

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
    return 'content.task.instances';
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

  showDetail(entity) {
    this.context.router.push('task/' + entity.id);
  }

  closeDetail() {
    this.setState({
      detail: {
        show: false,
        entity: {}
      }
    });
  }

  _getWfTaskCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.id) {
      return '';
    }
    return (
      <Advanced.WorkflowTaskInfo entity={entity}/>
    );
  }

  render() {
    const { uiKey, taskInstanceManager, columns, searchParameters, showFilter, showToolbar, username } = this.props;
    const { filterOpened} = this.state;
    let _searchParameters = null;
    if (searchParameters == null) {
      _searchParameters = taskInstanceManager.getDefaultSearchParameters().setFilter('candidateOrAssigned', username);
    } else {
      _searchParameters = searchParameters;
    }
    return (
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={taskInstanceManager}
          forceSearchParameters={_searchParameters}
          showRowSelection={false}
          filterOpened={filterOpened}
          showFilter={showFilter}
          showToolbar={showToolbar}
          filter={
           <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
             <Basic.AbstractForm ref="filterForm">
               <Basic.Row>
                 <Basic.Col lg={ 6 }>
                   <Advanced.Filter.SelectBox
                     ref="candidateOrAssigned"
                     rendered={_.includes(columns, 'taskAssignee')}
                     placeholder={this.i18n('entity.WorkflowTaskInstance.taskAssignee')}
                     manager={this.identityManager}/>
                 </Basic.Col>
                 <Basic.Col lg={ 6 } className="text-right">
                   <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                 </Basic.Col>
               </Basic.Row>
               <Basic.Row>
                 <Basic.Col lg={ 12 } >
                   <Advanced.Filter.TextField
                     ref="description"
                     rendered={_.includes(columns, 'description')}
                     placeholder={this.i18n('entity.WorkflowTaskInstance.taskDescription')}/>
                 </Basic.Col>
               </Basic.Row>
               <Basic.Row className="last">
                 <Basic.Col lg={ 8 }>
                   <Advanced.Filter.FilterDate
                     ref="fromTill"
                     facePlaceholder={ this.i18n('entity.WorkflowTaskInstance.filter.created') }
                     fromProperty="createdAfter"
                     fromPlaceholder={ this.i18n('entity.WorkflowTaskInstance.filter.createdAfter') }
                     tillProperty="createdBefore"
                     tillPlaceholder={ this.i18n('entity.WorkflowTaskInstance.filter.createdBefore') }/>
                 </Basic.Col>
                 <Basic.Col lg={ 4 }>
                 </Basic.Col>
               </Basic.Row>
             </Basic.AbstractForm>
           </Advanced.Filter>
         }>
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
          <Advanced.Column
            header=""
            property="taskDescription"
            cell={this._getWfTaskCell}
            sort={false}
            rendered={_.includes(columns, 'description')}/>
          <Advanced.Column property="taskCreated" sort face="datetime" rendered={_.includes(columns, 'created')}/>
          <Advanced.Column property="taskAssignee" sort={false} face="text" rendered={_.includes(columns, 'taskAssignee')}
            cell={({rowIndex, data}) => {
              const identityIds = [];
              for (const index in data[rowIndex].identityLinks) {
                if (data[rowIndex].identityLinks.hasOwnProperty(index)) {
                  identityIds.push(data[rowIndex].identityLinks[index].userId);
                }
              }
              return (
                 <Advanced.IdentitiesInfo identities={identityIds} maxEntry={5} />
              );
            }
          }/>
      </Advanced.Table>
    );
  }
}

TaskInstanceTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  taskInstanceManager: PropTypes.object.isRequired,
  columns: PropTypes.arrayOf(PropTypes.string),
  filterOpened: PropTypes.bool,
  searchParameters: PropTypes.object,
  showFilter: PropTypes.bool,
  showToolbar: PropTypes.bool
};

TaskInstanceTable.defaultProps = {
  columns: ['created', 'description', 'id'],
  filterOpened: false,
  _showLoading: false,
  searchParameters: null,
  showFilter: false,
  showToolbar: false
};

function select(state, component) {
  return {
    _searchParameters: state.data.ui[component.uiKey] ? state.data.ui[component.uiKey].searchParameters : {},
    _showLoading: component.taskInstanceManager.isShowLoading(state, `${component.uiKey}-detail`),
    username: state.security.userContext.username
  };
}

export default connect(select, null, null, { withRef: true })(TaskInstanceTable);
