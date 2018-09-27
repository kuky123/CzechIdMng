import _ from 'lodash';
import Immutable from 'immutable';
import ConfigLoader from '../utils/ConfigLoader';

/**
 * Immutable search representation - filter, sort, pageable.
 * Every modify operation returns new cloned SearchParameters with new values
 *
 * @author Radek Tomiška
 */
export default class SearchParameters {

  constructor(name = null, page = 0, size = SearchParameters.getDefaultSize()) {
    this.name = name;
    this.page = page;
    this.size = size;
    this.filters = new Immutable.Map({});
    this.sorts = new Immutable.OrderedMap({});
  }

  _clone() {
    return _.clone(this);
  }

  /**
   * Search name
   *
   * @return {string}
   */
  getName() {
    return this.name;
  }

  /**
   * Sets search name
   *
   * @param {[type]} name [description]
   */
  setName(name) {
    const newState = this._clone();
    newState.name = name;
    return newState;
  }

  /**
   * Pageable size
   *
   * @return {number}
   */
  getSize() {
    return this.size;
  }

  /**
   * Pageable size
   *
   * @param {number} size = SearchParameters.DEFAULT_SIZE
   * @return {SearchParameters} new copy
   */
  setSize(size = SearchParameters.getDefaultSize()) {
    const newState = this._clone();
    newState.size = size;
    return newState;
  }

  /**
   * Current Pageable (counted from zero)
   *
   * @return {Number}
   */
  getPage() {
    return this.page;
  }

  /**
   * Sets current page (counted from zero)
   *
   * @param {number} page = 0
   * @return {SearchParameters} new copy
   */
  setPage(page = 0) {
    const newState = this._clone();
    newState.page = page;
    return newState;
  }

  /**
   * Sets sort by property
   *
   * @param {string} property
   * @param {bool} ascending = true
   * @return {SearchParameters} new copy
   */
  setSort(property, ascending = true) {
    if (!property) {
      return this;
    }
    if (typeof ascending === 'string') {
      ascending = ascending.toLowerCase() === 'asc';
    }
    const newState = this._clone();
    newState.sorts = newState.sorts.set(property, ascending);
    return newState;
  }

  /**
   * Clears sort by property. If propety is null, then all sorts ic cleared
   *
   * @param  {string} property
   * @return {SearchParameters} new copy
   */
  clearSort(property = null) {
    const newState = this._clone();
    //
    if (property) {
      if (newState.sorts.has(property)) {
        newState.sorts = newState.sorts.delete(property);
      }
    } else { // clear all
      newState.sorts = newState.sorts.clear();
    }
    return newState;
  }

  /**
   * Returns true, when sort for given property is setted ascending.
   * Returns false, when sort for given property is setted descending.
   * If sort for given property is not setted, then returns null.
   *
   * @param  {string} property
   * @return {bool} true - asc | false - desc | null - not setted
   */
  getSort(property) {
    if (!property || !this.sorts.has(property)) {
      return null;
    }
    return this.sorts.get(property);
  }

  /**
   * Returns setted sorts as immutable ordered map
   *
   * @return {Immutable.OrderedMap<property, bool>} true = asc | false = desc
   */
  getSorts() {
    return this.sorts;
  }

  /**
   * Sets filter for given property
   *
   * @param {string} property
   * @param {string} filter value
   * @return {SearchParameters} new copy
   */
  setFilter(property, filter) {
    if (!property) {
      return this;
    }
    const newState = this._clone();
    newState.filters = newState.filters.set(property, filter);
    return newState;
  }

  /**
   * Sets filters - immutable map is needed
   *
   * @param {Immutable.Map} filters
   */
  setFilters(filters) {
    const newState = this._clone();
    if (!filters) {
      newState.clearFilter();
    } else if (!(filters instanceof Immutable.Map)) {
      // console.log('#setFilters supports immutable maps only');
      newState.clearFilter();
    } else {
      newState.filters = filters;
    }
    return newState;
  }

  /**
   * Returns setted filters as immutable map
   *
   * @return {Immutable.Map<property, filter>}
   */
  getFilters() {
    return this.filters;
  }

  /**
   * Clears filter by property. If propety is null, then all filter is cleared
   *
   * @param  {string} property
   * @return {SearchParameters} new copy
   */
  clearFilter(property = null) {
    const newState = this._clone();
    //
    if (property) {
      if (newState.filters.has(property)) {
        newState.filters = newState.filters.delete(property);
      }
    } else { // clear all
      newState.filters = newState.filters.clear();
    }
    return newState;
  }

  /**
   * Converts from json
   *
   * @param  {object} json
   * @return {SearchParameters}
   */
  static fromJS(json) {
    if (!json) {
      return new SearchParameters();
    }
    // TODO: convert from json
    throw new Error('unsupported operation');
  }

  /**
   * Converts to json
   *
   * @return {object}
   */
  toJs() {
    // TODO: convert to json
    throw new Error('unsupported operation');
  }

  /**
   * Returns search url (search suffix with name, page, size, sorts, filters)
   *
   * @return {string} url part
   */
  toUrl() {
    let url = '';
    if (this.name) { // if search name is setted - else default endpoint is used
      url += `/search/${this.name}`;
    }
    url += `?size=${this.size}&page=${this.page}`;
    // pageable
    this.sorts.forEach((ascending, property) => {
      url += `&sort=${property},` + (ascending ? 'asc' : 'desc');
    });
    // filterable
    this.filters.forEach((filter, property) => {
      if (filter !== null && filter !== undefined) {
        if (_.isArray(filter)) {
          filter.forEach(singleValue => {
            url += `&${property}=${encodeURIComponent(singleValue)}`;
          });
        } else if (_.isObject(filter)) {
          // expand nested properties
          for (const nestedProperty in filter) {
            if (!filter.hasOwnProperty(nestedProperty)) {
              continue;
            }
            if (filter[nestedProperty] !== null && filter[nestedProperty] !== undefined) {
              url += `&${nestedProperty}=${encodeURIComponent(filter[nestedProperty])}`;
            }
          }
        } else {
          url += `&${property}=${encodeURIComponent(filter)}`;
        }
      }
    });
    return url;
  }

  /**
   * Returs true, if searchparameters are equals
   *
   * @param  {SearchParameters} other
   * @return {bool}
   */
  equals(other) {
    const isEquals = this.name === other.getName()
      && this.page === other.getPage()
    && this.size === other.getSize()
    && Immutable.is(this.sorts, other.getSorts());
    //
    if (!isEquals) {
      return false;
    }
    // we need to compare filters manually - they can contains arrays - Immutable.is don't works
    const thisKeys = this.filters.keySeq().toArray();
    const otherKeys = other.getFilters().keySeq().toArray();
    // keys - ignoring order
    if (!_.isEqual(thisKeys.sort(), otherKeys.sort())) {
      return false;
    }
    // for each - check array
    for (let i = 0; i < thisKeys.length; i++) {
      const key = thisKeys[i];
      if (!_.isEqual(this.getFilters().get(key), other.getFilters().get(key))) {
        return false;
      }
    }
    //
    return true;
  }

  /**
   * Returs true, if searchparameters are equals
   *
   * @param  {SearchParameters} one
   * @param  {SearchParameters} two
   * @return {bool}
   */
  static is(one, two) {
    if (!one && !two) {
      return true;
    }
    if ((one && !two) || (!one && two)) {
      return false;
    }
    return one.equals(two);
  }

  /**
   * Returns configured page size or constant DEFAULT_SIZE
   *
   * @return {int} default page size
   */
  static getDefaultSize() {
    return ConfigLoader.getConfig('pagination.size', SearchParameters.DEFAULT_SIZE);
  }

  /**
   * Returns filled filter values from filter filterForm
   *
   * @param  {ref} filterForm reference to filter form
   * @return {object}
   */
  static getFilterData(filterForm) {
    const filters = {};
    const filterValues = filterForm.getData();
    for (const property in filterValues) {
      if (!filterValues.hasOwnProperty(property)) {
        continue;
      }
      const filterComponent = filterForm.getComponent(property);
      if (!filterComponent) {
        // filter is not rendered
        continue;
      }
      const field = filterComponent.props.field || property;
      //
      // if filterComponent uses multiSelect
      if (filterComponent.props.multiSelect === true) {
        // if filterEnumSelectBox uses Symbol
        if (filterComponent.props.enum && filterComponent.props.useSymbol && filterValues[property] !== null) {
          const filledValues = [];
          //
          filterValues[property].forEach(item => {
            filledValues.push(filterComponent.props.enum.findKeyBySymbol(item));
          });
          filters[field] = filledValues;
        } else {
          // if filterComponent does not useSymbol
          let filledValues;
          filledValues = filterValues[property];
          filters[field] = filledValues;
        }
      } else {
        // filterComponent does not use multiSelect
        let filledValue = filterValues[property];
        if (filterComponent.props.enum) { // enumeration
          filledValue = filterComponent.props.enum.findKeyBySymbol(filledValue);
        }
        filters[field] = filledValue;
      }
    }
    return filters;
  }

  /**
   * Returns search parameters filled from given filter form data
   *
   * @param  {object} formData
   * @return {SearchParameters} previosly used search parameters
   */
  static getSearchParameters(formData, searchParameters = null) {
    if (!searchParameters) {
      // TODO: this is a little dangerous - name is not filled
      searchParameters = new SearchParameters();
    }
    //
    searchParameters = searchParameters.setPage(0);
    for (const property in formData) {
      if (!formData.hasOwnProperty(property)) {
        continue;
      }
      if (!formData[property]) {
        searchParameters = searchParameters.clearFilter(property);
      } else {
        searchParameters = searchParameters.setFilter(property, formData[property]);
      }
    }
    return searchParameters;
  }
}

/**
 * Default search name
 * @type {String}
 */
SearchParameters.NAME_QUICK = 'quick';
/**
 * Search name for autocomplete (select box etc.)
 * @type {String}
 */
SearchParameters.NAME_AUTOCOMPLETE = 'autocomplete';
/**
 * Search name for count (return long how many entities was found)
 * @type {String}
 */
SearchParameters.NAME_COUNT = 'count';
/**
 * Default page size
 * @type {Number}
 */
SearchParameters.DEFAULT_SIZE = 10;
/**
 * Maximum page size
 * @type {Number}
 */
SearchParameters.MAX_SIZE = 500;
/**
 * Blank UUID. Can be use for sitiuations when we don't have ID for some entity, but we need use some "default" value.
 * @type {String}
 */
SearchParameters.BLANK_UUID = '00000000-0000-0000-0000-000000000000';
/**
 * Filter property - id
 * @type {String}
 */
SearchParameters.FILTER_PROPERTY_ID = 'id';
/**
 * Filter property - codeable identifier (id or code alias)
 * @type {String}
 */
SearchParameters.FILTER_PROPERTY_CODEABLE_IDENTIFIER = 'codeable';
