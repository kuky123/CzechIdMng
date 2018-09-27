import React, { PropTypes } from 'react';
import moment from 'moment';
//
import * as Basic from '../../basic';
import FilterDateTimePicker from './FilterDateTimePicker';
import FilterEnumSelectBox from './FilterEnumSelectBox';
import DateFaceEnum from '../../../enums/DateFaceEnum';

/**
 * Advanced date filter for dates form - till.
 *
 * Components saves complex object with from and till nested properties into result filter.
 *
 * TODO: validations (+required ...)
 * TODO: flex layout
 *
 * @author Radek Tomiška
 */
export default class FilterDate extends Basic.AbstractFormComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      face: null
    };
  }

  getComponentKey() {
    return 'component.advanced.Filter.FilterDate';
  }

  /**
   * Set value from object, which should contains
   *
   * @param {object}
   */
  setValue(value) {
    const { faceProperty, fromProperty, tillProperty } = this.props;
    //
    let dateFrom = null;
    let dateTill = null;
    let face = null;
    if (value) {
      dateFrom = value[fromProperty];
      dateTill = value[tillProperty];
      face = value[faceProperty];
    }
    this.setState({
      face
    }, () => {
      if (this.refs.face) {
        this.refs.face.setValue(face);
      }
      if (this.refs.from) {
        this.refs.from.setValue(dateFrom);
      }
      if (this.refs.till) {
        this.refs.till.setValue(dateTill);
      }
    });
  }

  /**
   * Return complex value as object with valid from, till and face as nested properties.
   *
   * @return {object}
   */
  getValue() {
    const { mode, faceProperty, fromProperty, tillProperty } = this.props;
    const { face } = this.state;
    const resultValue = {
      [faceProperty]: face
    };
    //
    switch (face) { // string representation
      case DateFaceEnum.findKeyBySymbol(DateFaceEnum.TODAY): {
        resultValue[fromProperty] = this._getValue(mode, moment().startOf('day'));
        resultValue[tillProperty] = this._getValue(mode, moment().endOf('day'));
        break;
      }
      case DateFaceEnum.findKeyBySymbol(DateFaceEnum.THIS_MONTH): {
        resultValue[fromProperty] = this._getValue(mode, moment().startOf('month'));
        resultValue[tillProperty] = this._getValue(mode, moment().endOf('month'));
        break;
      }
      case DateFaceEnum.findKeyBySymbol(DateFaceEnum.BETWEEN): {
        // mode is solved by dateTimePicker components itself
        resultValue[fromProperty] = this.refs.from.getValue();
        resultValue[tillProperty] = this.refs.till.getValue();
        break;
      }
      default: { // face select box only as default => return null
        return null;
      }
    }
    //
    return resultValue;
  }

  /**
   * Moment value as iso 8601 string by mode.
   *
   * @param  {string} mode
   * @param  {moment} value moment object
   * @return {string}  iso 8601
   */
  _getValue(mode = 'datetime', value) {
    if (!value) {
      return null;
    }
    //
    switch (mode) {
      case 'time': {
        return value.format('HH:mm');
      }
      case 'date': {
        return value.format('YYYY-MM-DD');
      }
      default: {
        return value.toISOString();
      }
    }
  }

  onChangeFace(option) {
    this.setState({
      face: option ? option.value : null
    });
  }

  /**
   * Property placeholder
   *
   * @param  {[type]} propertyName 'face', 'from', 'till'
   * @param  {[type]} defaultPlaceholder from props
   * @return {string}
   */
  _getPlaceholder(propertyName, defaultPlaceholder) {
    if (defaultPlaceholder !== null && defaultPlaceholder !== undefined) {
      return defaultPlaceholder;
    }
    // default placeholder
    return this.i18n(`${propertyName}.placeholder`, { defaultValue: 'Date' });
  }

  _renderFaceSelect() {
    const { facePlaceholder, required } = this.props;
    //
    return (
      <FilterEnumSelectBox
        ref="face"
        placeholder={ this._getPlaceholder('face', facePlaceholder) }
        enum={ DateFaceEnum }
        useSymbol={ false }
        useObject={ false }
        clearable
        value={ this.state.face }
        onChange={ this.onChangeFace.bind(this) }
        required={ required }/>
    );
  }

  _renderBetween() {
    const { mode, fromPlaceholder, tillPlaceholder } = this.props;
    //
    return (
      <Basic.Row>
        <Basic.Col lg={ 4 }>
          { this._renderFaceSelect() }
        </Basic.Col>
        <Basic.Col lg={ 4 }>
          <FilterDateTimePicker
            mode={ mode }
            ref="from"
            placeholder={ this._getPlaceholder('from', fromPlaceholder) }/>
        </Basic.Col>
        <Basic.Col lg={ 4 }>
          <FilterDateTimePicker
            mode={ mode }
            ref="till"
            placeholder={ this._getPlaceholder('till', tillPlaceholder) }/>
        </Basic.Col>
      </Basic.Row>
    );
  }

  render() {
    const { face } = this.state;
    //
    switch (face) { // string representation
      case DateFaceEnum.findKeyBySymbol(DateFaceEnum.BETWEEN): {
        return this._renderBetween();
      }
      default: { // face select box only as default
        return (
          <Basic.Row>
            <Basic.Col lg={ 4 }>
              { this._renderFaceSelect() }
            </Basic.Col>
            <Basic.Col lg={ 8 }>
              {/* empty */}
            </Basic.Col>
          </Basic.Row>
        );
      }
    }
  }
}

FilterDate.propTypes = {
  /**
   * Defined mode of component see @DateTimePicker. Use 'datetime' for DateTime columns, timezone is ignored for LocalDate columns.
   */
  mode: FilterDateTimePicker.propTypes.mode,
  /**
   * Property face - face - will be used for get/set value (configurable - more date filters can be used on the same content)
   */
  faceProperty: PropTypes.string,
  /**
   * Property name - from - will be used for get/set value
   */
  fromProperty: PropTypes.string,
  /**
   * Property name - till - will be used for get/set value
   */
  tillProperty: PropTypes.string,
  /**
   * Face select box placeholder - default i18n('face.placeholder') from locale
   */
  facePlaceholder: PropTypes.string,
  /**
   * Face select box placeholder - default i18n('from.placeholder') from locale
   */
  fromPlaceholder: PropTypes.string,
  /**
   * Face select box placeholder - default i18n('till.placeholder') from locale
   */
  tillPlaceholder: PropTypes.string
};
FilterDate.defaultProps = {
  mode: FilterDateTimePicker.defaultProps.mode,
  required: false,
  faceProperty: 'face',
  fromProperty: 'from',
  tillProperty: 'till'
};
