import Table from './Table/Table';
import Column from './Table/Column';
import ColumnLink from './Table/ColumnLink';
import RefreshButton from './Table/RefreshButton';
import IdentityInfo from './IdentityInfo/IdentityInfo';
import Navigation from './Navigation/Navigation';
import TabPanel from './TabPanel/TabPanel';
import Filter from './Filter/Filter';
import DateValue from './DateValue/DateValue';
import Tree from './Tree/Tree';
import TreeNodeSelect from './TreeNodeSelect/TreeNodeSelect';
import DetailButton from './Table/DetailButton';
import ModalProgressBar from './ModalProgressBar/ModalProgressBar';
import EavForm from './Form/EavForm';
import EavContent from './Form/EavContent';
import AbstractFormAttributeRenderer from './Form/AbstractFormAttributeRenderer';
import SelectBoxFormAttributeRenderer from './Form/SelectBoxFormAttributeRenderer';
import PasswordField from './PasswordField/PasswordField';
import ProgressBar from './ProgressBar/ProgressBar';
import RichTextArea from './RichTextArea/RichTextArea';
import AbstractTableContent from './Content/AbstractTableContent';
import EntityInfo from './EntityInfo/EntityInfo';
import AbstractEntityInfo from './EntityInfo/AbstractEntityInfo';
import UuidInfo from './UuidInfo/UuidInfo';
import RoleInfo from './RoleInfo/RoleInfo';
import RoleCatalogueInfo from './RoleCatalogueInfo/RoleCatalogueInfo';
import RoleCatalogueSelect from './RoleCatalogueSelect/RoleCatalogueSelect';
import IdentityContractInfo from './IdentityContractInfo/IdentityContractInfo';
import IdentityRoleInfo from './IdentityRoleInfo/IdentityRoleInfo';
import WorkflowProcessInfo from './WorkflowProcessInfo/WorkflowProcessInfo';
import WorkflowTaskInfo from './WorkflowTaskInfo/WorkflowTaskInfo';
import NotificationTemplateInfo from './NotificationTemplateInfo/NotificationTemplateInfo';
import ScriptArea from './ScriptArea/ScriptArea';
import RoleSelect from './RoleSelect/RoleSelect';
import Recaptcha from './Recaptcha/Recaptcha';
import IdentitiesInfo from './IdentitiesInfo/IdentitiesInfo';
import SchedulerTaskInfo from './SchedulerTaskInfo/SchedulerTaskInfo';
import EntitySelectBox from './EntitySelectBox/EntitySelectBox';
import Dropzone from './Dropzone/Dropzone';
import PasswordChangeComponent from './PasswordChangeComponent/PasswordChangeComponent';
import ValidationMessage from './ValidationMessage/ValidationMessage';
import DynamicTaskDetail from '../../content/task/DynamicTaskDetail';
import DecisionButtons from '../../content/task/DecisionButtons';
import OperationResult from './OperationResult/OperationResult';
import ImageDropzone from './ImageDropzone/ImageDropzone';
import TreeTypeInfo from './TreeTypeInfo/TreeTypeInfo';
import TreeNodeInfo from './TreeNodeInfo/TreeNodeInfo';
import ImageCropper from './ImageCropper/ImageCropper';
import LongRunningTask from './LongRunningTask/LongRunningTask';
import CreatableSelectBox from './CreatableSelectBox/CreatableSelectBox';

const Components = {
  Table,
  Column,
  ColumnLink,
  RefreshButton,
  IdentityInfo,
  Navigation,
  TabPanel,
  Filter,
  _ToogleButton: Filter.ToogleButton,
  _FilterButtons: Filter.FilterButtons,
  DateValue,
  ProgressBar,
  ModalProgressBar,
  Tree,
  TreeNodeSelect,
  DetailButton,
  EavForm,
  EavContent,
  AbstractFormAttributeRenderer,
  SelectBoxFormAttributeRenderer,
  PasswordField,
  RichTextArea,
  AbstractTableContent,
  EntityInfo,
  AbstractEntityInfo,
  UuidInfo,
  RoleInfo,
  RoleCatalogueInfo,
  RoleCatalogueSelect,
  IdentityContractInfo,
  IdentityRoleInfo,
  WorkflowProcessInfo,
  NotificationTemplateInfo,
  ScriptArea,
  RoleSelect,
  Recaptcha,
  IdentitiesInfo,
  SchedulerTaskInfo,
  EntitySelectBox,
  Dropzone,
  PasswordChangeComponent,
  ValidationMessage,
  DecisionButtons,
  DynamicTaskDetail,
  OperationResult,
  ImageDropzone,
  WorkflowTaskInfo,
  TreeTypeInfo,
  TreeNodeInfo,
  ImageCropper,
  LongRunningTask,
  CreatableSelectBox
};

Components.version = '0.0.1';
module.exports = Components;
