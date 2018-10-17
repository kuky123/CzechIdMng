import React from 'react';
import PropTypes from 'prop-types';
import invariant from 'invariant';
import Immutable from 'immutable';
import _ from 'lodash';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Loading from '../Loading/Loading';
import Alert from '../Alert/Alert';
import Row from './Row';
import DefaultCell from './DefaultCell';
import classNames from 'classnames';

const HEADER = 'header';
// const FOOTER = 'footer';
const CELL = 'cell';

/**
 * Table component with header and columns.
 *
 * @author Radek Tomiška
 */
class Table extends AbstractComponent {

  constructor(props) {
    super(props);
    this.state = {
      selectedRows: this.props.selectedRows ? new Immutable.Set(this.props.selectedRows) : new Immutable.Set()
    };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({
      selectedRows: nextProps.selectedRows ? new Immutable.Set(nextProps.selectedRows) : new Immutable.Set()
    });
  }

  _resolveColumns() {
    const children = [];
    //
    if (this.props.children) {
      React.Children.forEach(this.props.children, (child) => {
        if (child == null) {
          return;
        }
        invariant(
          // child.type.__TableColumnGroup__ ||
          !child.type || child.type.__TableColumn__,
          'child type should be <TableColumn /> or ' +
          '<TableColumnGroup />'
        );
        // rendered columns only
        if (child.props.rendered) {
          children.push(child);
        }
      });
      return children;
    }
    //
    // if columns aren't specified, then resolve columns from given data object
    if (children.length === 0) {
      let properties = [];
      const { data } = this.props;
      if (data && data.length !== 0) {
        // we can not use just first object, because first row could not contain all properties filled
        data.map(row => {
          for (const property in row) {
            if (!row.hasOwnProperty(property)) {
              continue;
            }
            properties = this._appendProperty(properties, property, row[property], '');
          }
        });
      }
      properties.map(property => {
        children.push(
          <DefaultCell property={ property } data={ data }/>
        );
      });
    }
    return children;
  }

  /**
   * Appends property to given properties array - supports nested properties
   *
   * @param  {array[string]} properties
   * @param  {string property
   * @param  {object} propertyValue
   * @param  {string} propertyPrefix nested property prefix
   * @return {array} new properties
   */
  _appendProperty(properties, property, propertyValue, propertyPrefix) {
    if (properties.indexOf(propertyPrefix + property) === -1) {
      if (propertyValue && _.isObject(propertyValue)) { // nested property
        // remove nested property prefix - is nested object
        if (propertyPrefix) {
          properties = properties.filter(p => { return p !== propertyPrefix; });
        }
        // recursion
        for (const nestedProperty in propertyValue) {
          if (!propertyValue.hasOwnProperty(nestedProperty)) {
            continue;
          }
          properties = this._appendProperty(properties, nestedProperty, propertyValue[nestedProperty], propertyPrefix + property + '.');
        }
      } else {
        properties.push(propertyPrefix + property);
      }
    }
    return properties;
  }

  _selectColumnElement(columns, type) {
    const newColumns = [];
    for (let i = 0; i < columns.length; ++i) {
      const column = columns[i];
      newColumns.push(React.cloneElement(
        column,
        {
          cell: type ? column.props[type] : column.props[CELL]
        }
      ));
    }
    return newColumns;
  }

  _showRowSelection({ rowIndex, data, showRowSelection }) {
    if (typeof showRowSelection === 'function') {
      return showRowSelection({
        rowIndex,
        data
      });
    }
    return showRowSelection;
  }

  selectRow(rowIndex, selected) {
    const { selectRowCb } = this.props;
    let newSelectedRows;
    if (selectRowCb != null) {
      newSelectedRows = selectRowCb(rowIndex, selected);
    } else {
      if (rowIndex !== undefined && rowIndex !== null && rowIndex > -1) {
        const recordId = this.getIdentifier(rowIndex);
        newSelectedRows = (selected ? this.state.selectedRows.add(recordId) : this.state.selectedRows.remove(recordId));
      } else { // de/select all
        newSelectedRows = this.state.selectedRows;
        const { data } = this.props;
        //
        for (let i = 0; i < data.length; i++) {
          if (this._showRowSelection({ ...this.props, rowIndex: i })) {
            if (selected) {
              newSelectedRows = newSelectedRows.add(this.getIdentifier(i));
            } else {
              newSelectedRows = newSelectedRows.remove(this.getIdentifier(i));
            }
          }
        }
      }
    }
    this.setState({
      selectedRows: newSelectedRows
    }, this._onRowSelect(rowIndex, selected, newSelectedRows.toArray()));
  }

  /**
   * Clears row selection
   */
  clearSelectedRows() {
    this.setState({
      selectedRows: this.state.selectedRows.clear()
    });
  }

  _onRowSelect(rowIndex, selected, selection) {
    const { onRowSelect } = this.props;
    if (!onRowSelect) {
      return;
    }
    onRowSelect(rowIndex, selected, selection);
  }

  getSelectedRows() {
    return this.state.selectedRows.toArray();
  }

  _isAllRowsSelected() {
    const { data, isAllRowsSelectedCb } = this.props;
    const { selectedRows } = this.state;
    if (isAllRowsSelectedCb) {
      return isAllRowsSelectedCb();
    }
    if (!data || data.length === 0) {
      return false;
    }
    let enabledRowsCount = 0;
    for (let i = 0; i < data.length; i++) {
      if (this._showRowSelection({ ...this.props, rowIndex: i })) {
        if (!selectedRows.has(this.getIdentifier(i))) {
          return false;
        }
        enabledRowsCount++;
      }
    }
    return enabledRowsCount > 0;
  }

  getIdentifierProperty() {
    // TODO: support for custom property?
    return 'id';
  }

  getIdentifier(rowIndex) {
    const { data } = this.props;
    return data[rowIndex][this.getIdentifierProperty()];
  }

  renderHeader(columns) {
    const { showLoading, showRowSelection, noHeader, data } = this.props;
    if (noHeader) {
      return null;
    }
    //
    const headerColumns = this._selectColumnElement(columns, HEADER);
    return (
      <thead key="basic-table-header">
        <Row
          key="row-header"
          columns={headerColumns}
          rowIndex={-1}
          showLoading={showLoading}
          showRowSelection={showRowSelection}
          onRowSelect={showRowSelection ? this.selectRow.bind(this) : null}
          selected={this._isAllRowsSelected()}
          data={ data }/>
      </thead>
    );
  }

  renderBody(columns) {
    const { data } = this.props;
    if (!data || data.length === 0) {
      return null;
    }
    const rows = [];
    for (let i = 0; i < data.length; i++) {
      rows.push(this.renderRow(columns, i));
    }
    return (
      <tbody key="basic-table-body">
        { rows }
      </tbody>
    );
  }

  renderRow(columns, rowIndex) {
    const { onRowClick, onRowDoubleClick, showRowSelection, rowClass, isRowSelectedCb } = this.props;
    const key = 'row-' + rowIndex;
    return (
       <Row
         key={key}
         data={this.props.data}
         columns={columns}
         rowIndex={rowIndex}
         showRowSelection={showRowSelection}
         onRowSelect={showRowSelection ? this.selectRow.bind(this) : null}
         selected={isRowSelectedCb === null ? this.state.selectedRows.has(this.getIdentifier(rowIndex)) : isRowSelectedCb(this.getIdentifier(rowIndex))}
         onClick={onRowClick}
         onDoubleClick={onRowDoubleClick}
         rowClass={rowClass}/>
    );
  }

  renderFooter() {
    return null;
  }

  render() {
    const {
      data,
      noData,
      rendered,
      showLoading,
      hover,
      className,
      condensed,
      header,
      noHeader
    } = this.props;
    //
    if (!rendered) {
      return null;
    }

    const columns = this._resolveColumns();
    const columnsHeaders = this.renderHeader(columns);
    const body = this.renderBody(columns);
    const footer = this.renderFooter();
    const classNamesTable = classNames(
      { 'table': true },
      { 'table-hover': hover},
      { 'table-condensed': condensed },
      { 'table-no-header': noHeader }
    );
    //
    const content = [];
    if (!data || data.length === 0) {
      if (showLoading) {
        content.push(
          <tr key={ `row-show-loading` }>
            <td colSpan={ columns.length }>
              <Loading showLoading className="static"/>
            </td>
          </tr>
        );
      } else {
        content.push(
          <tr key={ `row-no-data` }>
            <td colSpan={ columns.length }>
              <Alert text={ noData } className="no-data"/>
            </td>
          </tr>
        );
      }
    } else {
      content.push(columnsHeaders);
      content.push(body);
      content.push(footer);
    }
    //
    return (
      <div className={classNames(className, 'basic-table')}>
        <Loading showLoading={ showLoading && data && data.length > 0 }>
          <table className={ classNamesTable }>
            {
              !header || noHeader
              ||
              <thead>
                <tr className="basic-table-header">
                  <th colSpan={ columns.length }>
                    { header }
                  </th>
                </tr>
              </thead>
            }
            { content }
          </table>
        </Loading>
      </div>
    );
  }
}

Table.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * input data as array of json objects
   */
  data: PropTypes.array,
  /**
   * Table Header
   */
  header: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  /**
   * Callback that is called when a row is clicked.
   */
  onRowClick: PropTypes.func,
  /**
   * Callback that is called when a row is double clicked.
   */
  onRowDoubleClick: PropTypes.func,
  /**
   * Callback that is called when a row is selected.
   */
  onRowSelect: PropTypes.func,
  /**
   * selected row indexes as immutable set
   */
  selectedRows: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.number])),
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
  /**
   * css added to row
   */
  rowClass: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func
  ]),
  /**
   * If table data is empty, then this text will be shown
   */
  noData: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  /**
   * Show column headers
   */
  noHeader: PropTypes.bool,
  /**
   * Enable condensed table class, make tables more compact by cutting cell padding in half.
   */
  condensed: PropTypes.bool,
  /**
   * Enable hover table class
   */
  hover: PropTypes.bool,
  /**
   * Function that is called after de/select row/s
   */
  selectRowCb: PropTypes.func,
  /**
   * Function that is called for check if row is selcted
   */
  isRowSelectedCb: PropTypes.func,
  /**
   * Function that is called for check if all row are selected
   */
  isAllRowsSelectedCb: PropTypes.func
};
Table.defaultProps = {
  ...AbstractComponent.defaultProps,
  data: [],
  selectedRows: [],
  showRowSelection: false,
  noData: 'No record found',
  hover: true,
  condensed: false,
  noHeader: false,
  selectRowCb: null,
  isRowSelectedCb: null,
  isAllRowsSelectedCb: null
};

Table.SELECT_ALL = 'select-all-rows';

export default Table;
