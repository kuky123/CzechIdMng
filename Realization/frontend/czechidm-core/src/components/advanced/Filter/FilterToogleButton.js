import React from 'react';
import PropTypes from 'prop-types';
//
import AbstractContextComponent from '../../basic/AbstractContextComponent/AbstractContextComponent';
import * as Basic from '../../basic';

/**
 * Button for closable filter mainly for advanced table
 *
 * @author Radek Tomiška
 */
export default class FilterToogleButton extends AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: this.props.filterOpened
    };
  }

  _filterOpen(opened) {
    const { filterOpen } = this.props;
    this.setState({
      filterOpened: opened
    }, () => {
      if (filterOpen) {
        filterOpen(opened);
      }
    });
  }

  /**
   * Returns true, if some search parameters filter is filled excluding force search parameters.
   *
   * TODO: can be improved to ignore null filter values
   *
   * @return {Boolean}
   */
  _isFilterEmpty() {
    const { searchParameters, forceSearchParameters } = this.props;
    if (!searchParameters) {
      // search paramters are empty
      return false;
    }
    if (!forceSearchParameters || forceSearchParameters.getFilters().size === 0) {
      return searchParameters.getFilters().size === 0;
    }
    return searchParameters.getFilters().reduce((result, filter, key) => {
      return result && forceSearchParameters.getFilters().has(key);
    }, true);
  }

  render() {
    const { rendered, showLoading, searchParameters, ...others } = this.props;
    const { filterOpened } = this.state;
    if (!rendered) {
      return null;
    }
    let level = 'default';
    let tooltip = this.i18n('component.advanced.Table.filter.empty');
    if (!this._isFilterEmpty()) {
      level = 'info';
      tooltip = this.i18n('component.advanced.Table.filter.notEmpty');
    }
    //
    return (
      <Basic.Tooltip value={tooltip}>
        <span>
          <Basic.Button
            className="btn-xs"
            level={level}
            onClick={this._filterOpen.bind(this, !filterOpened)}
            showLoading={showLoading}
            icon="filter"
            {...others}>
            { this.i18n('button.filter.toogle') }
            {' '}
            <Basic.Icon icon={!filterOpened ? 'triangle-bottom' : 'triangle-top'} style={{ fontSize: '0.85em'}}/>
          </Basic.Button>
        </span>
      </Basic.Tooltip>
    );
  }
}

FilterToogleButton.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Callback, when filter is opened
   * @type {function} function(bool)
   */
  filterOpen: PropTypes.func,
  /**
   * Filter is opened
   * @type {bool}
   */
  filterOpened: PropTypes.bool,
  /**
   * Used search parameters in redux
   *
   * @type {SearchParameters}
   */
  searchParameters: PropTypes.object,
  /**
   * "Hard filters"
   *
   * @type {SearchParameters}
   */
  forceSearchParameters: PropTypes.object,
};
FilterToogleButton.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps
};
