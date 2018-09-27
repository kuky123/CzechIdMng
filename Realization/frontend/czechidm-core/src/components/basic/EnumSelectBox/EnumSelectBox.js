import React, { PropTypes } from 'react';
import _ from 'lodash';
import Select from 'react-select';
//
import SelectBox from '../SelectBox/SelectBox';
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';

/**
 * Select box with enumetation or static options
 *
 * @author Vít Švanda
 * @author Radek Tomiška
 */
class EnumSelectBox extends SelectBox {

  constructor(props) {
    super(props);
    this.useSymbol = props.useSymbol;
  }


  // Did mount only call initComponent method
  componentDidMount() {
    super.componentDidMount();
  }

  setValue(value) {
    if (this.props.useObject) {
      const options = this.getOptions();
      let option = null;
      if (options) {
        option = this.getOptions().find(o => { return o.value === value; });
      }
      super.setValue(option ? option : this.normalizeValue(value));
    } else {
      super.setValue(value);
    }
  }

  getOptions() {
    if (this.props.enum) {
      const enumeration = this.props.enum;
      const results = [];
      if (enumeration) {
        for (const enumItem in enumeration) {
          if (_.isSymbol(enumeration[enumItem])) {
            const item = this.itemRenderer(enumeration[enumItem], enumItem);
            results.push(item);
          }
        }
        return results;
      }
    }
    if (this.props.options) {
      const options = this.props.options;
      const results = [];
      for (const item in options) {
        if (!options.hasOwnProperty(item)) {
          continue;
        }
        results.push(this.itemRenderer(options[item]));
      }
      return results;
    }
  }

  itemRenderer(enumItem, key) {
    let item;
    if (enumItem && enumItem.value && !enumItem[SelectBox.ITEM_FULL_KEY]) {
      item = _.merge({}, enumItem, {
        [SelectBox.NICE_LABEL]: enumItem.niceLabel ? enumItem.niceLabel : this._findNiceLabel(enumItem.value),
        [SelectBox.ITEM_FULL_KEY]: enumItem.value,
        disabled: this._isDisabled(enumItem.value)
      });
    } else { // item is rendered already
      item = _.merge({}, enumItem);
      let niceLabel;
      if (this.props.enum) {
        if (enumItem.niceLabel) {
          // enum item has nice label, then use this niceLabel
          niceLabel = enumItem.niceLabel;
        } else {
          // niceLabel dont exist, then get new by key
          niceLabel = this.props.enum.getNiceLabel(key);
        }
      } else if (typeof enumItem === 'string') {
        niceLabel = enumItem;
        key = key || enumItem;
      }
      const itemFullKey = niceLabel;
      _.merge(item, {
        [SelectBox.NICE_LABEL]: niceLabel,
        [SelectBox.ITEM_FULL_KEY]: itemFullKey,
        value: key,
        disabled: this._isDisabled(key)
      });
    }
    return item;
  }

  _findKeyBySymbol(sym) {
    const enumeration = this.props.enum;
    //
    if (sym) {
      for (const enumItem in enumeration) {
        if (typeof enumeration[enumItem] === 'symbol' && enumeration[enumItem] === sym) {
          return enumItem;
        }
      }
    }
  }

  _findNiceLabel(value) {
    if (!value) {
      return null;
    }
    let rawValue;
    if (_.isSymbol(value)) {
      rawValue = this._findKeyBySymbol(value);
    } else {
      rawValue = value;
    }
    //
    const enumeration = this.props.enum;
    const { options } = this.props;
    if (enumeration) {
      return enumeration.getNiceLabel(rawValue);
    }
    if (options) {
      for (const item in options) {
        if (options[item].value === rawValue) {
          return options[item].niceLabel;
        }
        if (options[item] === rawValue) {
          return options[item];
        }
      }
    }
    return null;
  }

  _isDisabled(value) {
    if (!value) {
      return false;
    }
    let rawValue;
    if (_.isSymbol(value)) {
      rawValue = this._findKeyBySymbol(value);
    } else {
      rawValue = value;
    }
    //
    const enumeration = this.props.enum;
    const { options } = this.props;
    if (enumeration) {
      return enumeration.isDisabled(rawValue);
    }
    if (options) {
      for (const item in options) {
        if (options[item].value === rawValue) {
          return options[item].disabled === true;
        }
        if (options[item] === rawValue) {
          return false;
        }
      }
    }
    return false;
  }

  normalizeValue(value) {
    if (value) {
      // value is array ... enum multiselect
      if (value instanceof Array && this.props.multiSelect === true && _.isSymbol(value[0])) {
        const valueArray = [];
        for (const item of value) {
          if (_.isSymbol(value)) {
            // value is symbol
            // add item to array
            valueArray.push(this.itemRenderer(item, this._findKeyBySymbol(item)));
          }
        }
        return valueArray;
      } else if (_.isSymbol(value)) {
        // value is symbol
        return this.itemRenderer(value, this._findKeyBySymbol(value));
      } else if (typeof value === 'string') {
        // value is string any selectBox
        this.useSymbol = false;
        return this.itemRenderer({ value });
      } else if (value instanceof Array && this.props.multiSelect === true && typeof value[0] === 'string') {
        // value is string array ... any multiselect
        this.useSymbol = false;
        const valueArray = [];
        for (const item of value) {
          if (typeof item === 'string') {
            // value is string
            // add item to array
            valueArray.push(this.itemRenderer({ value: item }));
          }
        }
        return valueArray;
      } else if (value instanceof Array && this.props.multiSelect === true && typeof value[0] === 'object') {
        const valueArray = [];
        for (const item of value) {
          valueArray.push(this.itemRenderer(item, this._findKeyBySymbol(item)));
        }
        return valueArray;
      }
    }
    return value;
  }

  getValue() {
    const { value } = this.state;
    //
    if (!value) {
      return null;
    }
    // value is array ... multiselect
    if (value instanceof Array && this.props.multiSelect === true) {
      const copyValue = [];
      for (const item of value) {
        copyValue.push(this._convertValue(_.merge({}, item)));
      }
      return copyValue;
    }
    // value is not array
    return this._convertValue(_.merge({}, value));
  }

  /**
   * Converts value to / from symbol or object. Is dependent on input and props:
   * - when input is string - results is string. Enum property has to be setted.
   * - when useObject is true, then returns whole object
   *
   * @param  {[type]} value selected value
   * @return {string|symbol}
   */
  _convertValue(item) {
    if (!item) {
      // nothing to convert
      return item;
    }
    if (this.props.useObject) {
      // or useObject - keeping value intouched
      return item;
    }
    if (!this.props.enum) {
      // Enum property has to be setted - when option props is given, then nothing can be done.
      return item.value;
    }
    const value = item.value;
    //
    let convertedValue = value;
    if (this.useSymbol) {
      if (!_.isSymbol(value)) {
        convertedValue = this.props.enum.findSymbolByKey(value);
      }
    } else {
      if (_.isSymbol(value)) {
        convertedValue = this.props.enum.findKeyBySymbol(value);
      }
    }
    this.getLogger().trace('[Basic.EnumSelectBox] converted value:', convertedValue);
    return convertedValue;
  }

  onInputChange(value) {
    this.getOptions(value);
  }

  getPlaceholder(placeholder) {
    if (placeholder !== null && placeholder !== undefined) {
      return placeholder;
    }
    // default placeholder
    return this.i18n('label.select', { defaultValue: 'Select ...' });
  }

  getSelectComponent() {
    const { placeholder, multiSelect, fieldLabel, searchable, clearable } = this.props;
    const { value, readOnly, disabled } = this.state;
    //
    return (
      <span>
        <Select
          ref="selectComponent"
          title={"title"}
          value={value}
          onChange={this.onChange}
          disabled={readOnly || disabled}
          ignoreCase
          clearable={clearable}
          ignoreAccents={false}
          multi={multiSelect}
          onValueClick={this.gotoContributor}
          valueKey={SelectBox.ITEM_VALUE}
          labelKey={fieldLabel}
          noResultsText={this.i18n('component.basic.SelectBox.noResultsText')}
          placeholder={this.getPlaceholder(placeholder)}
          searchingText={this.i18n('component.basic.SelectBox.searchingText')}
          searchPromptText={this.i18n('component.basic.SelectBox.searchPromptText')}
          onInputChange={this.onInputChange.bind(this)}
          options={this.getOptions()}
          searchable={searchable}/>
      </span>
    );
  }
}

EnumSelectBox.propTypes = {
  ...AbstractFormComponent.propTypes,
  placeholder: PropTypes.string,
  fieldLabel: PropTypes.string,
  multiSelect: PropTypes.bool,
  enum: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.func
  ]),
  options: PropTypes.arrayOf(PropTypes.object),
  value: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.func),
    PropTypes.string,
    PropTypes.symbol
  ]),
  searchable: PropTypes.bool,
  useSymbol: PropTypes.bool,
  useObject: PropTypes.bool,
  clearable: PropTypes.bool
};

EnumSelectBox.defaultProps = {
  ...SelectBox.defaultProps,
  searchable: false,
  useSymbol: true,
  useObject: false,
  clearable: true
};

export default EnumSelectBox;
