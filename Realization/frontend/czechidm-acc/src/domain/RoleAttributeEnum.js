import { Enums } from 'czechidm-core';

/**
 * Keys of Role fields
 */
export default class RoleAttributeEnum extends Enums.AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`acc:enums.RoleAttributeEnum.${key}`);
  }

  static findKeyBySymbol(sym) {
    return super.findKeyBySymbol(this, sym);
  }

  static findSymbolByKey(key) {
    return super.findSymbolByKey(this, key);
  }

  static getField(key) {
    if (!key) {
      return null;
    }

    const sym = super.findSymbolByKey(this, key);

    switch (sym) {
      case this.NAME: {
        return 'name';
      }
      case this.CODE: {
        return 'code';
      }
      case this.ROLE_TYPE: {
        return 'roleType';
      }
      case this.PRIORITY: {
        return 'priority';
      }
      case this.APPROVE_REMOVE: {
        return 'approveRemove';
      }
      case this.DESCRIPTION: {
        return 'description';
      }
      case this.DISABLED: {
        return 'disabled';
      }
      default: {
        return null;
      }
    }
  }

  static getEnum(field) {
    if (!field) {
      return null;
    }

    switch (field) {
      case 'name': {
        return this.NAME;
      }
      case 'code': {
        return this.CODE;
      }
      case 'roleType': {
        return this.ROLE_TYPE;
      }
      case 'priority': {
        return this.PRIORITY;
      }
      case 'approveRemove': {
        return this.APPROVE_REMOVE;
      }
      case 'description': {
        return this.DESCRIPTION;
      }
      case 'disabled': {
        return this.DISABLED;
      }
      default: {
        return null;
      }
    }
  }

  static getLevel(key) {
    if (!key) {
      return null;
    }

    const sym = super.findSymbolByKey(this, key);

    switch (sym) {
      default: {
        return 'default';
      }
    }
  }
}

RoleAttributeEnum.NAME = Symbol('NAME');
RoleAttributeEnum.CODE = Symbol('CODE');
RoleAttributeEnum.ROLE_TYPE = Symbol('ROLE_TYPE');
RoleAttributeEnum.PRIORITY = Symbol('PRIORITY');
RoleAttributeEnum.APPROVE_REMOVE = Symbol('APPROVE_REMOVE');
RoleAttributeEnum.DESCRIPTION = Symbol('DESCRIPTION');
RoleAttributeEnum.DISABLED = Symbol('DISABLED');
