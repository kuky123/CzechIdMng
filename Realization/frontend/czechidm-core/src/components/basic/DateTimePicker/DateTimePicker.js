import React, { PropTypes } from 'react';
import classNames from 'classnames';
import Joi from 'joi';
import moment from 'moment';
//
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import Datetime from 'react-datetime';
import LocalizationService from '../../../services/LocalizationService';
import Button from '../Button/Button';
import Icon from '../Icon/Icon';
import Tooltip from '../Tooltip/Tooltip';

const INVALID_DATE = 'Invalid date';

/**
 * Wrapped react-datetime component
 *
 * @author Vít Švanda
 * @author Radek Tomiška
 */
class DateTimePicker extends AbstractFormComponent {

  constructor(props) {
    super(props);
  }

  getRequiredValidationSchema() {
    return Joi.any().required();
  }

  componentDidMount() {
    super.componentDidMount();
    this.setValue(this.props.value);
  }

  componentWillReceiveProps(nextProps) {
    super.componentWillReceiveProps(nextProps);
    if (nextProps.value && this.props.value && nextProps.value !== this.props.value) {
      this.setValue(nextProps.value);
    }
  }

  getFormat() {
    const { mode, dateFormat, timeFormat } = this.props;
    //
    if (!mode || mode === 'datetime') {
      return this._getDateFormat(dateFormat) + ' ' + this._getTimeFormat(timeFormat);
    }
    if (mode === 'date') {
      return this._getDateFormat(dateFormat);
    }
    if (mode === 'time') {
      return this._getTimeFormat(timeFormat);
    }
  }

  _getDateFormat(dateFormat) {
    return dateFormat || this.i18n('format.date', { defaultValue: 'DD.MM.YYYY'});
  }

  _getTimeFormat(timeFormat) {
    return timeFormat || this.i18n('format.time', { defaultValue: 'HH:mm' });
  }

  setValue(value) {
    const dateTime = this._format(value);
    if (this.refs.input && !dateTime) {
      this.refs.input.setState({ inputValue: '' }); // we need to set empty string, null does not work
    }
    this.setState({value: dateTime}, () => { this.validate(false); });
  }

  validate(showValidationError) {
    const {required, isValidDate} = this.props;
    const {value} = this.state;

    const showValidations = showValidationError != null ? showValidationError : true;
    if (this.state.validation) {
      let result = true;
      let key;
      const params = {};
      if (required) {
        result = value !== null;
        key = 'string.base';
      }
      if (result && value) {
        const iso8601Value = moment(value, this.getFormat(), true);
        if (iso8601Value && !iso8601Value.isValid()) {
          result = false;
          key = 'date.unvalid';
        } else if (isValidDate && !isValidDate(value)) {
          result = false;
          key = 'date.unvalid';
        }
      }
      if (!result) {
        const message = this._localizationValidation(key, params);
        this.setState({
          validationResult: {
            status: 'error',
            class: 'has-error has-feedback',
            isValid: false,
            message
          },
          showValidationError: showValidations
        });
      } else {
        this.setState({
          validationResult: {
            status: null,
            class: '',
            isValid: true,
            message: null,
            showValidationError: true
          },
          showValidationError: showValidations
        });
      }
      // show validation error on UI
      return false;
    }
  }

  onChange(value) {
    if (value && value._isAMomentObject) {
      value = moment(value, this.getFormat());
    }
    let result = true;
    if (this.props.onChange) {
      result = this.props.onChange(value);
    }
    // if onChange listener returns false, then we can end
    if (result === false) {
      return;
    }
    this.setState({
      value
    }, () => {
      this.validate();
    });
  }

  getValue() {
    const { mode } = this.props;
    const { value } = this.state;
    //
    if (value === INVALID_DATE || !value) {
      return null;
    }
    if (!mode || mode === 'datetime') {
      return moment(value, this.getFormat()).toISOString(); // iso 8601
    }
    if (mode === 'date') {
      return moment(value, this.getFormat()).format('YYYY-MM-DD'); // iso 8601
    }
    // time
    return moment(value, this.getFormat()).format('HH:mm'); // iso 8601
  }

  _format(iso8601Value) {
    if (!iso8601Value) {
      return null;
    }
    if (typeof iso8601Value === 'string') {
      // TODO: deprecated by next moment version
      return moment(iso8601Value).format(this.getFormat());
    }
    return moment(iso8601Value).format(this.getFormat());
  }

  _clear() {
    this.refs.input.setState({ inputValue: null }); // we need to set empty string, null does not work
    this.onChange(null);
  }
  _openDialog() {
    const dateTimePicker = this.refs.input;
    dateTimePicker.setState({ open: true });
  }

  getBody(feedback) {
    const {
      mode,
      labelSpan,
      label,
      componentSpan,
      placeholder,
      style,
      locale,
      dateFormat,
      timeFormat,
      isValidDate,
    } = this.props;

    const { readOnly, disabled, value } = this.state;
    //
    // default prop values - we need initialized LocalizationService
    const _locale = locale || LocalizationService.getCurrentLanguage();
    const _dateFormat = this._getDateFormat(dateFormat);
    const _timeFormat = this._getTimeFormat(timeFormat);
    //
    const labelClassName = classNames(labelSpan, 'control-label');

    // VS: I added value attribute to Datetime component. External set value (in setValue method) now set only empty string if value is null.
    // Without value attribute is in some case (detail of contract) value not show after first render.
    return (
      <div>
        {
          !label
          ||
          <label
            className={labelClassName}>
            {label}
            { this.renderHelpIcon() }
          </label>
        }
        <div className={componentSpan}>
          <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={ this.getTitle() }>
            <div className="btn-group input-group basic-date-time-picker">
              {
                (disabled || readOnly)
                ?
                <input type="text" value={value && value._isAMomentObject ? this._format(value) : value} readOnly className="form-control" style={style}/>
                :
                <Datetime
                  ref="input"
                  value={value}
                  onChange={this.onChange}
                  disabled={disabled}
                  readOnly={readOnly}
                  style={style}
                  closeOnSelect
                  locale={_locale === 'cs' ? 'cs' : 'en'}
                  dateFormat={mode === 'time' ? false : _dateFormat}
                  timeFormat={mode === 'date' ? false : _timeFormat}
                  inputProps={{
                    title: (this.getValidationResult() != null ? this.getValidationResult().message : ''),
                    placeholder,
                    style: {
                      zIndex: 0
                    }
                  }}
                  isValidDate={isValidDate}/>
              }
              <Button type="button"
                level="default"
                className="btn-sm"
                disabled={disabled || readOnly}
                style={{marginTop: 0, height: '34px', borderLeftWidth: 0, borderRadius: 0 }}
                onClick={this._openDialog.bind(this)}>
                <Icon type="fa" icon="calendar"/>
              </Button>
              <Button type="button"
                level="default"
                className="btn-sm"
                disabled={disabled || readOnly}
                style={{marginTop: 0, height: '34px', borderLeftWidth: 0, borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }}
                onClick={this._clear.bind(this)}>
                <Icon type="fa" icon="remove"/>
              </Button>
              {
                !feedback
                ||
                <Icon icon="warning-sign" className="form-control-feedback" style={{ right: -30, top: 0, zIndex: 0 }}/>
              }
            </div>
          </Tooltip>
          { !label ? this.renderHelpIcon() : null }
          { this.renderHelpBlock() }
        </div>
      </div>
    );
  }
}

DateTimePicker.propTypes = {
  ...AbstractFormComponent.propTypes,
  /**
   *  Defined mode of component see @DateTimePicker. Use 'datetime' for DateTime columns, timezone is ignored for LocalDate columns.
   */
  mode: PropTypes.oneOf(['date', 'time', 'datetime']),
  locale: PropTypes.oneOf(['cs', 'en']), // TODO: supports other locales needs import
  dateFormat: PropTypes.string,
  timeFormat: PropTypes.string,
  /**
   * Define the dates that can be selected. The function receives (currentDate, selectedDate) and shall return a true or false whether the currentDate is valid or not.
   */
  isValidDate: PropTypes.func
};

const { componentSpan, ...otherDefaultProps} = AbstractFormComponent.defaultProps; // componentSpan override
DateTimePicker.defaultProps = {
  ...otherDefaultProps,
  mode: 'datetime'
};

export default DateTimePicker;
