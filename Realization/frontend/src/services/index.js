

import SettingService from './SettingService';
import IdentityAccountService from './IdentityAccountService';
import AttachmentService from './AttachmentService';
import IdentitySubordinateService from './IdentitySubordinateService';
import ApprovalTaskService from './ApprovalTaskService';
import RoleApprovalTaskService from './RoleApprovalTaskService';
import IdentityDelegateService from './IdentityDelegateService';

const ServiceRoot = {
  SettingService,
  IdentityAccountService,
  AttachmentService,
  IdentitySubordinateService,
  ApprovalTaskService,
  RoleApprovalTaskService,
  IdentityDelegateService
};

ServiceRoot.version = '0.0.1';
module.exports = ServiceRoot;
