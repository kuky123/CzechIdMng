import AbstractEnum from '../enums/AbstractEnum';

/**
 * Persistent type enum for EAVs
 *
 * @author Radek Tomiška
 */
export default class PersistentTypeEnum extends AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`core:enums.PersistentTypeEnum.${key}`);
  }

  static findKeyBySymbol(sym) {
    return super.findKeyBySymbol(this, sym);
  }

  static findSymbolByKey(key) {
    return super.findSymbolByKey(this, key);
  }
}

PersistentTypeEnum.CHAR = Symbol('CHAR');
PersistentTypeEnum.TEXT = Symbol('TEXT');
PersistentTypeEnum.SHORTTEXT = Symbol('SHORTTEXT');
PersistentTypeEnum.INT = Symbol('INT');
PersistentTypeEnum.LONG = Symbol('LONG');
PersistentTypeEnum.DOUBLE = Symbol('DOUBLE');
PersistentTypeEnum.BOOLEAN = Symbol('BOOLEAN');
PersistentTypeEnum.DATE = Symbol('DATE');
PersistentTypeEnum.DATETIME = Symbol('DATETIME');
PersistentTypeEnum.BYTEARRAY = Symbol('BYTEARRAY');
PersistentTypeEnum.UUID = Symbol('UUID');
PersistentTypeEnum.ATTACHMENT = Symbol('ATTACHMENT');
