import _ from 'lodash';
import { i18n } from '../services/LocalizationService';

/**
 * workaround for enumeration in javascript
 *
 * @author Vít Švanda
 */
export default class AbstractEnum {

  /**
   * Returns localized message
   * TODO: access to LocalizationService in this layer is maybe inappropriate - move localization outside of this class
   */
  static getNiceLabel(key) {
    return i18n(key);
  }

  /**
   * Returns level decorator (Label)
   */
  static getLevel(/* key*/) {
    return null;
  }

  /**
   * Returns icon
   */
  static getIcon(/* key*/) {
    return null;
  }

  /**
   * finds key to given symbol
   */
  static findKeyBySymbol(enumeration, sym) {
    if (sym) {
      for (const enumItem in enumeration) {
        if (enumeration[enumItem] === sym) {
          return enumItem;
        }
      }
    }
  }

  /**
   * find symbol by key
   */
  static findSymbolByKey(enumeration, key) {
    if (key) {
      for (const enumItem in enumeration) {
        if (enumItem === key) {
          return enumeration[enumItem];
        }
      }
    }
  }

  /**
   * Returns true, when key is disabled
   *
   * @return {Boolean}
   */
  static isDisabled(/* key*/) {
    // enabled by default
    return false;
  }

  /**
   * Enumeration values
   *
   * @param  {AbstractEnum} enumeration
   * @return {arrayOf(Symbol)} enumeration values (symbols)
   */
  static values(enumeration) {
    const enumValues = [];
    for (const enumItem in enumeration) {
      if (_.isSymbol(enumeration[enumItem])) {
        enumValues.push(enumeration[enumItem]);
      }
    }
    //
    return enumValues;
  }
}
