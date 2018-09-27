import AbstractEnum from '../enums/AbstractEnum';

/**
 * Enumeration for dates filter
 *
 * @author Radek Tomiška
 */
export default class DateFaceEnum extends AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`core:enums.DateFaceEnum.${key}`);
  }

  static findKeyBySymbol(sym) {
    return super.findKeyBySymbol(this, sym);
  }

  static findSymbolByKey(key) {
    return super.findSymbolByKey(this, key);
  }
}

DateFaceEnum.TODAY = Symbol('today');
DateFaceEnum.THIS_MONTH = Symbol('thisMonth');
DateFaceEnum.BETWEEN = Symbol('between');
