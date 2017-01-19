import { Enums } from 'czechidm-core';

/**
 * SynchronizationActionType for synchronization.
 */
export default class SynchronizationActionTypeEnum extends Enums.AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`acc:enums.SynchronizationActionTypeEnum.${key}`);
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
      case this.CREATE_ENTITY: {
        return 'success';
      }
      case this.UPDATE_ENTITY: {
        return 'success';
      }
      case this.UPDATE_ACCOUNT: {
        return 'success';
      }
      case this.LINK_AND_UPDATE_ACCOUNT: {
        return 'success';
      }
      case this.LINK: {
        return 'success';
      }
      case this.UNLINK: {
        return 'warning';
      }
      case this.UNLINK_AND_REMOVE_ROLE: {
        return 'danger';
      }
      case this.CREATE_ACCOUNT: {
        return 'success';
      }
      case this.DELETE_ENTITY: {
        return 'danger';
      }
      case this.IGNORE: {
        return 'primary';
      }
      default: {
        return 'default';
      }
    }
  }
}

SynchronizationActionTypeEnum.CREATE_ENTITY = Symbol('CREATE_ENTITY');
SynchronizationActionTypeEnum.UPDATE_ENTITY = Symbol('UPDATE_ENTITY');
SynchronizationActionTypeEnum.DELETE_ENTITY = Symbol('DELETE_ENTITY');
SynchronizationActionTypeEnum.LINK_AND_UPDATE_ACCOUNT = Symbol('LINK_AND_UPDATE_ACCOUNT');
SynchronizationActionTypeEnum.LINK = Symbol('LINK');
SynchronizationActionTypeEnum.UNLINK_AND_REMOVE_ROLE = Symbol('UNLINK_AND_REMOVE_ROLE');
SynchronizationActionTypeEnum.UNLINK = Symbol('UNLINK');
SynchronizationActionTypeEnum.CREATE_ACCOUNT = Symbol('CREATE_ACCOUNT');
SynchronizationActionTypeEnum.UPDATE_ACCOUNT = Symbol('UPDATE_ACCOUNT');
SynchronizationActionTypeEnum.IGNORE = Symbol('IGNORE');