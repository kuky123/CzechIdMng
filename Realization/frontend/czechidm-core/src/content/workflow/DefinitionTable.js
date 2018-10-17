import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Utils from '../../utils';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Managers from '../../redux/data';

const manager = new Managers.WorkflowProcessDefinitionManager();

/**
 * Table of workflow definitions
 *
 * @author Roman Kučera
 * @author Radek Tomiška
 */
export class DefinitionTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: false
    };
  }

  getManager() {
    return manager;
  }

  getContentKey() {
    return 'content.workflow.definitions';
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
    this.context.router.push(`/workflow/definitions/${entity.key}`);
  }

  reload() {
    this.refs.table.getWrappedInstance().reload();
  }

  render() {
    const { showLoading, forceSearchParameters, uiKey } = this.props;
    const { filterOpened } = this.state;
    return (
      <Advanced.Table
        ref="table"
        uiKey={uiKey}
        manager={ this.getManager() }
        showLoading={showLoading}
        rowClass={({rowIndex, data}) => { return Utils.Ui.getDisabledRowClass(data[rowIndex]); }}
        noData={this.i18n('component.basic.Table.noData')}
        forceSearchParameters={forceSearchParameters}
        filter={
          <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
            <Basic.AbstractForm ref="filterForm">
              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Advanced.Filter.TextField
                    ref="processDefinitionKey"
                    placeholder={this.i18n('key')}/>
                </Basic.Col>
                <Basic.Col lg={ 6 } className="text-right">
                  <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                </Basic.Col>
              </Basic.Row>
              <Basic.Row className="last">
                <Basic.Col lg={ 6 }>
                  <Advanced.Filter.TextField
                    ref="name"
                    placeholder={this.i18n('name')}/>
                </Basic.Col>
              </Basic.Row>
            </Basic.AbstractForm>
          </Advanced.Filter>
        }
        filterOpened={!filterOpened}
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
        <Advanced.Column property="key" header={this.i18n('key')} width="25%"
          cell={<Basic.LinkCell property="key" to="workflow/definitions/:key"/>} sort />
        <Advanced.Column property="name" header={this.i18n('name')} width="20%" sort />
        <Advanced.Column property="resourceName" header={this.i18n('resourceName')} width="15%" />
        <Advanced.Column property="description" header={this.i18n('description')} width="25%" />
        <Advanced.Column property="version" header={this.i18n('version')} width="5%" />
      </Advanced.Table>
    );
  }
}

DefinitionTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  forceSearchParameters: PropTypes.object,
  showLoading: PropTypes.bool
};

DefinitionTable.defaultProps = {};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true})(DefinitionTable);
