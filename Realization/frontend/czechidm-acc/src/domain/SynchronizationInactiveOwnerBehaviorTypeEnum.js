import { Enums } from 'czechidm-core';

/**
 * SynchronizationInactiveOwnerBehaviorTypeEnum for synchronization.
 */
export default class SynchronizationInactiveOwnerBehaviorTypeEnum extends Enums.AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`acc:enums.SynchronizationInactiveOwnerBehaviorTypeEnum.${key}`);
  }

  static findKeyBySymbol(sym) {
    return super.findKeyBySymbol(this, sym);
  }

  static findSymbolByKey(key) {
    return super.findSymbolByKey(this, key);
  }

  static getLevel(key) {
    if (!key) {
      return null;
    }

    const sym = super.findSymbolByKey(this, key);

    switch (sym) {
      case this.DO_NOT_LINK: {
        return 'success';
      }
      case this.LINK_PROTECTED: {
        return 'success';
      }
      case this.LINK: {
        return 'success';
      }
      default: {
        return 'default';
      }
    }
  }
}

SynchronizationInactiveOwnerBehaviorTypeEnum.DO_NOT_LINK = Symbol('DO_NOT_LINK');
SynchronizationInactiveOwnerBehaviorTypeEnum.LINK_PROTECTED = Symbol('LINK_PROTECTED');
SynchronizationInactiveOwnerBehaviorTypeEnum.LINK = Symbol('LINK');
