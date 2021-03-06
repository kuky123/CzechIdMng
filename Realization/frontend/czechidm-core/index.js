/**
 * Module API
 *
 * import { Basic, Advanced, Managers, Utils } from 'czechidm-core';
 *
 * @author Radek Tomiška
 */
import * as Basic from './src/components/basic';
import * as Advanced from './src/components/advanced';
import * as Services from './src/services';
import * as Managers from './src/redux';
import * as ConfigActions from './src/redux/config/actions';
import * as ConfigReducers from './src/redux/config/reducer';
import * as FlashReducers from './src/redux/flash/reducer';
import * as DataReducers from './src/redux/data/reducer';
import * as SecurityReducers from './src/redux/security/reducer';
import * as Utils from './src/utils';
import * as Domain from './src/domain';
// import Routes from './routes';
import ComponentService from './src/services/ComponentService';
//
import AbstractEnum from './src/enums/AbstractEnum';
import OperationStateEnum from './src/enums/OperationStateEnum';
import PasswordPolicyTypeEnum from './src/enums/PasswordPolicyTypeEnum';
import ScriptCategoryEnum from './src/enums/ScriptCategoryEnum';
import ApiOperationTypeEnum from './src/enums/ApiOperationTypeEnum';
import IdentityAttributeEnum from './src/enums/IdentityAttributeEnum';
import ContractAttributeEnum from './src/enums/ContractAttributeEnum';
import ContractSliceAttributeEnum from './src/enums/ContractSliceAttributeEnum';
//
import ValidationMessage from './src/components/advanced/ValidationMessage/ValidationMessage';

const ModuleRoot = {
  Basic,
  Advanced,
  Services,
  Managers,
  ConfigActions,
  Reducers: {
    config: ConfigReducers.config,
    messages: FlashReducers.messages,
    data: DataReducers.data,
    security: SecurityReducers.security
  },
  ComponentService,
  Utils,
  Domain,
  Enums: {
    AbstractEnum,
    OperationStateEnum,
    PasswordPolicyTypeEnum,
    ScriptCategoryEnum,
    ApiOperationTypeEnum,
    IdentityAttributeEnum,
    ContractAttributeEnum,
    ContractSliceAttributeEnum
  },
  Content: {
    ValidationMessage // backward compatibility
  }
};
ModuleRoot.version = '0.0.1';
module.exports = ModuleRoot;
