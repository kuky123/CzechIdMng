import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import _ from 'lodash';
import * as Basic from '../../basic';

/**
 * Creatable component
 *
 * @author Ondrej Kopr
 */
class CreatableSelectBox extends Basic.EnumSelectBox {

  constructor(props) {
    super(props);
    this.getOptions = this.getOptions.bind(this);
    this.state = {
      ...this.state,
      options: [],
      error: null,
      actualPage: 0,
      multi: true
    };
  }

  getValue() {
    const { options } = this.state;
    const values = [];
    options.forEach( option => {
      values.push(option.value);
    });
    return values;
  }

  setValue(values) {
    const options = [];
    if (values && values !== null && values !== undefined) {
      values.forEach(value => {
        value = _.trim(value);
        if (value.length > 0) {
          options.push({
            label: value,
            value,
            niceLabel: value
          });
        }
        this._onChange(options);
      });
    } else {
      this.setState({
        options
      });
    }
  }

  focus() {
    this.refs.selectComponent.focus();
  }

  getComponentKey() {
    return 'component.advanced.CreatableSelectBox';
  }

  _onChange(values) {
    const { useCheck } = this.props;
    const { options } = this.state;
    //
    if (values.length < options.length || !useCheck) {
      // remove value from options, or useCheck is disabled. It's not necessary made check
      this.setState({
        options: values
      });
      return;
    }
    //
    const lastInsertedValue = _.last(values);
    const lastInsertedIndex = values.length - 1;
    //
    values[lastInsertedIndex].className = 'is-loading';
    values[lastInsertedIndex].isLoading = true;
    //
    this.setState({
      options: values
    });
    this._exists(lastInsertedValue.value, lastInsertedIndex);
  }

  _updateOptions(value, index, newClass) {
    const { options } = this.state;
    options[index].className = newClass;
    options[index].isLoading = false;
    //
    this.setState({
      options
    });
  }

  _exists(identifier, index) {
    const { manager, filterColumnName, existentClass, nonExistentClass } = this.props;
    const searchParameters = manager.getDefaultSearchParameters().setFilter(filterColumnName, identifier);
    this.context.store.dispatch(manager.fetchEntitiesCount(searchParameters, null, count => {
      if (count && count > 0) {
        this._updateOptions(identifier, index, existentClass);
      } else {
        this._updateOptions(identifier, index, nonExistentClass);
      }
    }));
  }

  _onInputChange(inputValue) {
    const { separator } = this.props;
    const { options } = this.state;
    //
    if (inputValue.includes(separator)) {
      // TODO: made seperator configurable via ConfigurationManager
      const valuesArray = _.split(inputValue, separator);
      valuesArray.forEach(value => {
        value = _.trim(value);
        if (value.length > 0) {
          options.push({
            label: value,
            value,
            niceLabel: value
          });
        }
        this._onChange(options);
      });
      // we must reset inputValue
      return '';
    }
    return inputValue;
  }

  _evaluateShowLoading() {
    const { options } = this.state;
    if (options) {
      options.forEach(option => {
        if (option.isLoading) {
          return true;
        }
      });
    }
    return false;
  }

  _promptTextCreator(value) {
    return this.i18n('textCreator', { value });
  }

  getSelectComponent() {
    const { options } = this.state;
    const { placeholder, fieldLabel } = this.props;
    const showLoading = this._evaluateShowLoading();
    return (
      <Select.Creatable
        ref="selectComponent"
        multi
        value={options}
        onChange={this._onChange.bind(this)}
        onInputChange={this._onInputChange.bind(this)}
        isLoading={showLoading}
        removeSelected
        placeholder={this.getPlaceholder(placeholder)}
        labelKey={fieldLabel}
        noResultsText={this.i18n('noResultsText')}
        searchPromptText={this.i18n('searchPromptText')}
        promptTextCreator={this._promptTextCreator.bind(this)}/>
    );
  }
}

CreatableSelectBox.propTypes = {
  ...Basic.EnumSelectBox.propTypes,
  /**
   * Seperator for separate values
   */
  separator: PropTypes.string,
  /**
   * Managers is required when you want use check for values
   */
  manager: PropTypes.object,
  /**
   * Culumn name for made count
   */
  filterColumnName: PropTypes.string,
  /**
   * Css class for existent values
   */
  existentClass: PropTypes.string,
  /**
   * Css class for non existent class
   */
  nonExistentClass: PropTypes.string,
  /**
   * Use the component without check feature
   */
  useCheck: PropTypes.bool,
  value: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.object),
    PropTypes.object,
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.string
  ])
};

CreatableSelectBox.defaultProps = {
  ...Basic.EnumSelectBox.defaultProps,
  separator: ',',
  manager: null,
  filterColumnName: 'identifiers',
  existentClass: 'existent',
  nonExistentClass: 'non-existent',
  useCheck: false
};

export default CreatableSelectBox;
