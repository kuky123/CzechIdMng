import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

const ASC = 'ASC';
const DESC = 'DESC';

/**
 * Header with sort action
 */
class SortHeaderCell extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  _activeSort() {
    const { searchParameters, sortProperty, property } = this.props;
    //
    if (!searchParameters) {
      return null;
    }
    const sort = searchParameters.getSort(sortProperty || property);
    if (sort === null) {
      return null;
    }
    return sort === true ? ASC : DESC;
  }

  _handleSort(order, event) {
    if (event) {
      event.preventDefault();
    }
    const { sortProperty, property, sortHandler } = this.props;
    if (!sortHandler) {
      // if handleSort is not set, then its nothing to do
      return null;
    }
    sortHandler(sortProperty || property, order);
  }

  render() {
    const { header, title, property, sortHandler, showLoading, className } = this.props;
    const active = this._activeSort();
    const content = header || property;
    const classNames = classnames(
      'sort-header-cell',
      className
    );
    return (
      <div className={ classNames } title={ title }>
        {
          sortHandler
          ?
          <a href="#" onClick={this._handleSort.bind(this, active === 'ASC' ? 'DESC' : 'ASC')}>
            { content }
            <SortIcon active={active} showLoading={showLoading}/>
          </a>
          :
          <span>{ content }</span>
        }
      </div>
    );
  }
}

SortHeaderCell.propTypes = {
  /**
   * Property for header and sorting
   */
  property: PropTypes.string,
  /**
   * Property for sorting - higher priority than property
   */
  sortProperty: PropTypes.string,
  /**
   * Column header text - if isn't set, then property is shown
   */
  header: PropTypes.string,
  /**
   * Current searchparameters - sort
   */
  searchParameters: PropTypes.object,
  /**
   * Callback action for data sorting

   * @param string property
   * @param string order [ASC, DESC]
   */
  sortHandler: PropTypes.func,
  /**
   * loadinig indicator
   */
  showLoading: PropTypes.bool
};
SortHeaderCell.defaultProps = {
  showLoading: false
};

/**
 * Sort icon
 */
class SortIcon extends AbstractComponent {

  render() {
    const { active, showLoading } = this.props;
    const ascClassName = classnames(
      'sort-icon sort-asc',
      { 'active': active === ASC}
    );
    const descClassName = classnames(
      'sort-icon sort-desc',
      { 'active': active === DESC }
    );

    return (
      <span className="sort-icons">
        {(showLoading && active)
          ?
          <Icon type="fa" icon="refresh" showLoading className="sort-icon active"/>
          :
          <span>
            <Icon icon="triangle-top" className={ascClassName}/>
            <Icon icon="triangle-bottom" className={descClassName}/>
          </span>
        }
      </span>
    );
  }
}

SortIcon.propTypes = {
  /**
   * current order for current property
   */
  active: PropTypes.oneOf([ASC, DESC]),
  /**
   * loadinig indicator
   */
  showLoading: PropTypes.bool
};
SortIcon.defaultProps = {
  active: null,
  showLoading: false
};

const SortHeader = ({...props}) => (
  <SortHeaderCell {...props}/>
);

export default SortHeader;
