import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
import uuid from 'uuid';
//
import { Basic, Advanced, Utils, Managers, Domain } from 'czechidm-core';
import { ExampleProductManager } from '../../redux';

const manager = new ExampleProductManager();

/**
* Table of example products
*
* @author Radek Tomiška
*
*/
export class ExampleProductTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened,
    };
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'example:content.example-products';
  }

  /**
   * Base manager for this agenda (used in `AbstractTableContent`)
   */
  getManager() {
    return manager;
  }

  /**
   * Submit filter action
   */
  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  /**
   * Cancel filter action
   */
  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  /**
   * Link to detail / create
   */
  showDetail(entity) {
    if (Utils.Entity.isNew(entity)) {
      const uuidId = uuid.v1();
      this.context.router.push(`/example/product/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`/example/product/${entity.id}/detail`);
    }
  }

  render() {
    const { uiKey, columns, forceSearchParameters, showAddButton, showRowSelection } = this.props;
    const { filterOpened } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          manager={ manager }
          rowClass={ ({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); } }
          filterOpened={filterOpened}
          forceSearchParameters={ forceSearchParameters }
          showRowSelection={ Managers.SecurityManager.hasAuthority('EXAMPLEPRODUCT_DELETE') && showRowSelection }
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row className="last">
                  <Basic.Col lg={ 4 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text.placeholder')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 }>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false },
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={this.showDetail.bind(this, { })}
                rendered={Managers.SecurityManager.hasAuthority('EXAMPLEPRODUCT_CREATE') && showAddButton}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }
          >

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
          <Advanced.ColumnLink
            to="example/product/:id/detail"
            property="code"
            width={ 100 }
            sort
            face="text"
            rendered={_.includes(columns, 'code')}/>
          <Advanced.Column property="name" width="15%" sort face="text" rendered={_.includes(columns, 'name')}/>
          <Advanced.Column property="description" sort face="text" rendered={_.includes(columns, 'description')}/>
          <Advanced.Column property="price" width={ 125 } sort rendered={_.includes(columns, 'price')}/>
          <Advanced.Column property="disabled" width={ 100 } sort face="bool" rendered={_.includes(columns, 'disabled')}/>
        </Advanced.Table>
      </div>
    );
  }
}

ExampleProductTable.propTypes = {
  /**
   * Entities, permissions etc. fro this content are stored in redux under given key
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Rendered columns (all by default)
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  /**
   * Show filter or collapse
   */
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Show add button to create new product
   */
  showAddButton: PropTypes.bool,
  /**
   * Show row selection for bulk actions
   */
  showRowSelection: PropTypes.bool
};

ExampleProductTable.defaultProps = {
  columns: ['code', 'name', 'description', 'price', 'disabled'],
  filterOpened: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showAddButton: true,
  showRowSelection: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey), // persisted filter state in redux
  };
}

export default connect(select, null, null, { withRef: true })(ExampleProductTable);
