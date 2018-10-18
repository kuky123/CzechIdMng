import AbstractEnum from '../enums/AbstractEnum';

/**
 * Password policy generate type enum,
 * - normal generating
 * - passphrase
 */
export default class PasswordPolicyGenerateTypeEnum extends AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`core:enums.PasswordPolicyGenerateTypeEnum.${key}`);
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
    // TODO: use some diff level?
    switch (sym) {
      case this.RANDOM: {
        return 'default';
      }
      case this.PASSPHRASE: {
        return 'info';
      }
      default: {
        return 'default';
      }
    }
  }
}

PasswordPolicyGenerateTypeEnum.RANDOM = Symbol('RANDOM');
PasswordPolicyGenerateTypeEnum.PASSPHRASE = Symbol('PASSPHRASE');
