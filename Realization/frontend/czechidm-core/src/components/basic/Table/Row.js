import React from 'react';
import PropTypes from 'prop-types';
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Cell from './Cell';

/**
 * Component that renders the row for <Table />.
 * This component should not be used directly by developer. Instead,
 * only <Table /> should use the component internally.
 *
 * @author Radek Tomiška
 */
class Row extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  _onClick(event) {
    this.props.onClick(event, this.props.rowIndex, this.props.data);
  }

  _onDoubleClick(event) {
    this.props.onDoubleClick(event, this.props.rowIndex, this.props.data);
  }

  _onSelect(event) {
    const { onRowSelect, rowIndex } = this.props;
    if (!onRowSelect) {
      return;
    }
    onRowSelect(rowIndex, event.currentTarget.checked);
  }

  _getTitle({ selected, rowIndex }) {
    // TODO: localization - move to props adn fill in advanced table
    if (selected) {
      return 'Zrušit výběr';
    }
    if (rowIndex !== undefined && rowIndex !== null && rowIndex > -1) {
      return 'Vybrat záznam';
    }
    return 'Vybrat záznamy';
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

  render() {
    const { rowIndex, columns, selected, rowClass, onRowSelect, data } = this.props;
    const cells = new Array(columns.length);
    for (let i = 0, j = columns.length; i < j; i++) {
      const columnProps = columns[i].props;
      cells[i] = (
        <Cell
          key={`cell_${i}`}
          rowIndex={rowIndex}
          cell={columnProps.cell}
          property={columnProps.property}
          data={data}
          showLoading={this.props.showLoading}
          width={columnProps.width}
          className={columnProps.className}
        />
      );
    }
    let _rowClass;
    if (!rowClass) {
      _rowClass = '';
    } else if (typeof rowClass === 'function') {
      _rowClass = rowClass({
        rowIndex,
        data
      });
    } else {
      _rowClass = rowClass;
    }

    return (
      <tr
        onClick={this.props.onClick ? this._onClick.bind(this) : null}
        onDoubleClick={this.props.onDoubleClick ? this._onDoubleClick.bind(this) : null}
        className={_rowClass}>
        {
          !onRowSelect
          ||
          <td width="16px" className="bulk-selection">
            <input
              type="checkbox"
              checked={selected}
              onChange={this._onSelect.bind(this)}
              title={this._getTitle(this.props)}
              disabled={!this._showRowSelection(this.props)}/>
          </td>
        }
        {cells}
      </tr>
    );
  }
}

Row.propTypes = {
  rowIndex: PropTypes.number.isRequired,
  columns: PropTypes.array.isRequired,
  /**
   * loadinig indicator
   */
  showLoading: PropTypes.bool,
  /**
   * input data as array of json objects
   */
  data: PropTypes.array,
  /**
   * Callback that is called when a row is clicked.
   */
  onClick: PropTypes.func,

  /**
   * Callback that is called when a row is double clicked.
   */
  onDoubleClick: PropTypes.func,
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
  /**
   * Row selection
   */
  onRowSelect: PropTypes.func,
  /**
   * If row is selected
   */
  selected: PropTypes.bool,
  /**
   * css added to row
   */
  rowClass: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func
  ])
};
Row.defaultProps = {
  showLoading: false,
  selected: false
};

export default Row;
