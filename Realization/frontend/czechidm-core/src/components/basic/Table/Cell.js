import React from 'react';
import PropTypes from 'prop-types';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import DefaultCell from './DefaultCell';

/**
 * Component that renders the cell for <Table />.
 * This component should not be used directly by developer. Instead,
 * only <Row /> should use the component internally.
 */
class Cell extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { property, data, showLoading, width, className, ...props } = this.props;
    const cellProps = {
      showLoading
    };
    const innerStyle = {
      width
    };

    if (props.rowIndex >= 0) {
      cellProps.rowIndex = props.rowIndex;
    }
    // default property from owner component
    if (props.cell && props.cell.props && !props.cell.props.property) {
      cellProps.property = property;
    }
    // default data from owner component
    if (props.cell && props.cell.props && !props.cell.props.data) {
      cellProps.data = data;
    }

    let content;
    if (!props.cell) {
      let text = null;
      if (props.rowIndex === -1) { // header
        text = property;
      } else if (data && property && data[cellProps.rowIndex]) { // body
        text = DefaultCell.getPropertyValue(data[cellProps.rowIndex], property);
      }
      content = (
        <DefaultCell
          {...cellProps}>
          {text}
        </DefaultCell>
      );
    } else if (React.isValidElement(props.cell)) {
      content = React.cloneElement(props.cell, cellProps);
    } else if (typeof props.cell === 'function') {
      content = props.cell(this.props);
    } else {
      content = (
        <DefaultCell
          {...cellProps}>
          {props.cell}
        </DefaultCell>
      );
    }

    return (
      <td style={innerStyle} className={className}>
        {content}
      </td>
    );
  }
}

// Properties will be passed to `cellRenderer` to render.
Cell.propTypes = {
  /**
   * The row index that will be passed to `cellRenderer` to render.
   */
  rowIndex: PropTypes.number,
  /**
   * Property from data object - optional. Can be defined in header (cell or footer) element. Nested properties can be used e.g. `identityManager.name`.
   */
  property: PropTypes.string,
  /**
   * input data as array of json objects
   */
  data: PropTypes.array
};

Cell.defaultProps = {
};

export default Cell;
