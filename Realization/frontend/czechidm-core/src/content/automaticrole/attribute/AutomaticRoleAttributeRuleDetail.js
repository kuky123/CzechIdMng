import React, { PropTypes } from 'react';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import { AutomaticRoleAttributeRuleManager, FormAttributeManager } from '../../../redux';
import AutomaticRoleAttributeRuleTypeEnum from '../../../enums/AutomaticRoleAttributeRuleTypeEnum';
import AutomaticRoleAttributeRuleComparisonEnum from '../../../enums/AutomaticRoleAttributeRuleComparisonEnum';
import IdentityAttributeEnum from '../../../enums/IdentityAttributeEnum';
import AbstractEnum from '../../../enums/AbstractEnum';
/**
 * Constant for get eav attribute for identity contract
 * @type {String}
 */
const CONTRACT_EAV_TYPE = 'eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract';
/**
 * Constatn for get eav attribute for identity
 * @type {String}
 */
const IDENTITY_EAV_TYPE = 'eu.bcvsolutions.idm.core.model.entity.IdmIdentity';

const DEFINITION_TYPE_FILTER = 'definitionType';

/**
 * Modified ContractAttributeEnum - singular properties
 *
 * TODO: DRY, but how to generalize enum + static methods ...
 */
class ContractAttributeEnum extends AbstractEnum {

  static getNiceLabel(key) {
    return super.getNiceLabel(`core:enums.ContractAttributeEnum.${key}`);
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
      case this.IDENTITY: {
        return 'identity';
      }
      case this.VALID_FROM: {
        return 'validFrom';
      }
      case this.VALID_TILL: {
        return 'validTill';
      }
      case this.WORK_POSITION: {
        return 'workPosition';
      }
      case this.POSITION: {
        return 'position';
      }
      case this.EXTERNE: {
        return 'externe';
      }
      case this.MAIN: {
        return 'main';
      }
      case this.DESCRIPTION: {
        return 'description';
      }
      case this.STATE: {
        return 'state';
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
      case 'identity': {
        return this.IDENTITY;
      }
      case 'validFrom': {
        return this.VALID_FROM;
      }
      case 'validTill': {
        return this.VALID_TILL;
      }
      case 'workPosition': {
        return this.WORK_POSITION;
      }
      case 'position': {
        return this.POSITION;
      }
      case 'externe': {
        return this.EXTERNE;
      }
      case 'main': {
        return this.MAIN;
      }
      case 'description': {
        return this.DESCRIPTION;
      }
      case 'disabled': {
        return this.DISABLED;
      }
      case 'state': {
        return this.STATE;
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

ContractAttributeEnum.IDENTITY = Symbol('IDENTITY');
ContractAttributeEnum.MAIN = Symbol('MAIN');
ContractAttributeEnum.STATE = Symbol('STATE');
ContractAttributeEnum.POSITION = Symbol('POSITION');
ContractAttributeEnum.WORK_POSITION = Symbol('WORK_POSITION');
ContractAttributeEnum.VALID_FROM = Symbol('VALID_FROM');
ContractAttributeEnum.VALID_TILL = Symbol('VALID_TILL');
ContractAttributeEnum.EXTERNE = Symbol('EXTERNE');
ContractAttributeEnum.DESCRIPTION = Symbol('DESCRIPTION');

/**
 * Detail rules of automatic role attribute
 *
 */
export default class AutomaticRoleAttributeRuleDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.manager = new AutomaticRoleAttributeRuleManager();
    this.formAttributeManager = new FormAttributeManager();
    this.state = {
      showLoading: false,
      typeForceSearchParameters: null, // force search parameters for EAV attribute
      type: AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY), // default type, show when create new entity
      valueRequired: true, // flag for required field
      formAttribute: null, // instance of form attribute, is used for computed field input
      attributeName: null // name of identity attribute
    };
  }

  getContentKey() {
    return 'content.automaticRoles.attribute';
  }

  componentDidMount() {
    const { entity } = this.props;
    this._initForm(entity);
  }

  /**
   * Method check if props in this component is'nt different from new props.
   */
  componentWillReceiveProps(nextProps) {
    // check id of old and new entity
    if (nextProps.entity.id !== this.props.entity.id || nextProps.entity.attributeName !== this.props.entity.attributeName) {
      this._initForm(nextProps.entity);
    }
  }

  getForm() {
    return this.refs.form;
  }

  getValue() {
    return this.refs.value;
  }

  getCompiledData() {
    const formData = this.getForm().getData();
    let value = this.getValue().getValue();
    if (_.isObject(value)) {
      // eav form value
      value = this._getValueFromEav(value);
    }
    // attribute in backend is String type, we must explicit cast to string
    formData.value = String(value);
    // we must transform attribute name with case sensitive letters
    if (formData.attributeName) {
      if (formData.type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY)) {
        let attributeName = IdentityAttributeEnum.getField(IdentityAttributeEnum.findKeyBySymbol(formData.attributeName));
        if (!attributeName) {
          attributeName = IdentityAttributeEnum.getField(formData.attributeName);
        }
        formData.attributeName = attributeName;
      } else {
        let attributeName = ContractAttributeEnum.getField(ContractAttributeEnum.findKeyBySymbol(formData.attributeName));
        if (!attributeName) {
          attributeName = ContractAttributeEnum.getField(formData.attributeName);
        }
        formData.attributeName = attributeName;
      }
    }
    if (formData.type !== AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY_EAV)
     && formData.type !== AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.CONTRACT_EAV)) {
      formData.formAttribute = null;
    }
    const formAttribute = formData.formAttribute;
    if (formAttribute) {
      formData.formAttribute = formAttribute.id;
      if (!formData.attributeName) {
        formData.attributeName = formAttribute.code;
      }
    }
    return formData;
  }

  /**
   * Method for basic initial form
   */
  _initForm(entity) {
    let attributeName = null;
    if (entity !== undefined) {
      let formAttribute = null;
      if (!entity.id) {
        entity.type = AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY);
        entity.comparison = AutomaticRoleAttributeRuleComparisonEnum.findKeyBySymbol(AutomaticRoleAttributeRuleComparisonEnum.EQUALS);
        entity.attributeName = IdentityAttributeEnum.USERNAME;
        attributeName = IdentityAttributeEnum.USERNAME;
      } else {
        if (entity.type !== AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY_EAV)
         && entity.type !== AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.CONTRACT_EAV)) {
          if (entity.type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY)) {
            entity.attributeName = IdentityAttributeEnum.getEnum(entity.attributeName);
            attributeName = IdentityAttributeEnum.findKeyBySymbol(entity.attributeName);
          } else {
            entity.attributeName = ContractAttributeEnum.getEnum(entity.attributeName);
            attributeName = ContractAttributeEnum.findKeyBySymbol(entity.attributeName);
          }
        } else {
          // eav is used
          if (entity._embedded && entity._embedded.formAttribute) {
            formAttribute = entity._embedded.formAttribute;
          }
        }
      }
      this.setState({
        typeForceSearchParameters: this._getForceSearchParametersForType(entity.type),
        type: entity.type,
        formAttribute,
        entity,
        attributeName
      });
      this.refs.type.focus();
    }
  }

  _getForceSearchParametersForType(type) {
    let typeForceSearchParameters = this.formAttributeManager.getDefaultSearchParameters();
    if (type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY_EAV)) {
      typeForceSearchParameters = typeForceSearchParameters.setFilter(DEFINITION_TYPE_FILTER, IDENTITY_EAV_TYPE);
    } else if (type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.CONTRACT_EAV)) {
      typeForceSearchParameters = typeForceSearchParameters.setFilter(DEFINITION_TYPE_FILTER, CONTRACT_EAV_TYPE);
    } else {
      typeForceSearchParameters = null;
    }
    return typeForceSearchParameters;
  }

  /**
   * Get field form EAV, saved eav form value contains
   * value, seq and then value by persistent type for example: dateValue, doubleValue, ...
   */
  _getValueFromEav(eav) {
    for (const field in eav) {
      if (field !== 'value' && field.includes('Value')) {
        return eav[field];
      }
    }
  }

  _typeChange(option) {
    let typeForceSearchParameters = null;
    if (option) {
      typeForceSearchParameters = this._getForceSearchParametersForType(option.value);
    }
    //
    // clear values in specific fields
    this.refs.attributeName.setValue(null);
    this.refs.formAttribute.setValue(null);
    //
    const newEntity = _.merge({}, this.state.entity);
    newEntity.type = option ? option.value : null;
    this.setState({
      typeForceSearchParameters,
      type: option ? option.value : null,
      entity: newEntity
    });
  }

  _comparsionChange(option) {
    let valueRequired = false;
    if (option && option.value === AutomaticRoleAttributeRuleComparisonEnum.findKeyBySymbol(AutomaticRoleAttributeRuleComparisonEnum.EQUALS)) {
      valueRequired = true;
    }
    //
    this.setState({
      valueRequired
    });
  }

  _formAttributeChange(option) {
    // just change formAttribute
    this.setState({
      formAttribute: option
    });
  }

  _attributeNameChange(option) {
    // set new attribute name
    this.setState({
      attributeName: option ? option.value : null
    });
  }

  /**
   * Return component that corespond with persisntent type of value type.
   * As default show text field.
   */
  _getValueField(type, valueRequired, formAttribute, attributeName) {
    const { entity } = this.props;
    const value = entity.value;
    let finalComponent = null;
    //
    if (type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY) ||
    type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.CONTRACT)) {
      finalComponent = this._getValueFieldForEntity(entity, type, value, attributeName);
    } else if (formAttribute) {
      finalComponent = this._getValueFieldForEav(formAttribute, value, valueRequired);
    } else {
      // form attribute doesn't exists
      finalComponent = this._getDefaultTextField(value);
    }
    return finalComponent;
  }

  _getValueFieldForEntity(entity, type, value, attributeName) {
    if (attributeName == null) {
      return this._getDefaultTextField(value);
    }
    // identity attributes
    if (type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY)) {
      // disabled is obly attribute that has different face
      if (IdentityAttributeEnum.findSymbolByKey(attributeName) === IdentityAttributeEnum.DISABLED) {
        return this._getDefaultBooleanSelectBox(value);
      }
      return this._getDefaultTextField(value);
    }
    // contracts attributes
    // contract has externe and main as boolean and valid attributes as date
    if (ContractAttributeEnum.findSymbolByKey(attributeName) === ContractAttributeEnum.MAIN || ContractAttributeEnum.findSymbolByKey(attributeName) === ContractAttributeEnum.EXTERNE) {
      return this._getDefaultBooleanSelectBox(value);
    } else if (ContractAttributeEnum.findSymbolByKey(attributeName) === ContractAttributeEnum.VALID_FROM || ContractAttributeEnum.findSymbolByKey(attributeName) === ContractAttributeEnum.VALID_TILL) {
      return this._getDefaultDateTimePicker(value);
    }
    return this._getDefaultTextField(value);
  }

  _getValueFieldForEav(formAttribute, value, valueRequired) {
    const { readOnly } = this.props;
    const component = this.formAttributeManager.getFormComponent(formAttribute);
    if (!component || !component.component) {
      // when component doesn't exists show default field
      return this._getDefaultTextField(value);
    }
    if (formAttribute.persistentType === 'TEXT') {
      return (
        <Basic.LabelWrapper label={ this.i18n('entity.AutomaticRole.attribute.value.label') }>
          <Basic.Alert text={this.i18n('attributeCantBeUsed.persistentTypeText', {name: formAttribute.name})}/>
        </Basic.LabelWrapper>
      );
    }
    if (formAttribute.confidential) {
      return (
        <Basic.LabelWrapper label={ this.i18n('entity.AutomaticRole.attribute.value.label') }>
          <Basic.Alert text={this.i18n('attributeCantBeUsed.confidential', {name: formAttribute.name})}/>
        </Basic.LabelWrapper>
      );
    }
    const FormValueComponent = component.component;
    //
    // override helpBlock, label and placeholder
    formAttribute.description = this.i18n('entity.AutomaticRole.attribute.value.help');
    formAttribute.name = this.i18n('entity.AutomaticRole.attribute.value.label');
    formAttribute.placeholder = '';
    formAttribute.defaultValue = null;
    formAttribute.required = valueRequired;
    formAttribute.readonly = readOnly; // readnOnly from props has prio, default value is false
    //
    // is neccessary transform value to array
    return (
      <FormValueComponent
        ref="value"
        required={valueRequired}
        attribute={formAttribute}
        readOnly={readOnly}
        values={[{value}]}/>
    );
  }

  /**
   * Return simple text field for value input
   */
  _getDefaultTextField(value) {
    const { readOnly } = this.props;
    const { valueRequired } = this.state;
    return (
      <Basic.TextField
        ref="value"
        value={value}
        readOnly={readOnly}
        required={valueRequired}
        label={this.i18n('entity.AutomaticRole.attribute.value.label')}
        helpBlock={this.i18n('entity.AutomaticRole.attribute.value.help')}/>);
  }

  /**
   * Return date time picker
   */
  _getDefaultDateTimePicker(value) {
    const { valueRequired } = this.state;
    const { readOnly } = this.props;

    return (<Basic.DateTimePicker
      ref="value"
      mode="date"
      readOnly={readOnly}
      value={value}
      required={valueRequired}
      label={this.i18n('entity.AutomaticRole.attribute.value.label')}
      helpBlock={this.i18n('entity.AutomaticRole.attribute.value.help')}/>);
  }

  /**
   * Return default boolean select box
   */
  _getDefaultBooleanSelectBox(value) {
    const { valueRequired } = this.state;
    const { readOnly } = this.props;

    return (
      <Basic.BooleanSelectBox
        ref="value"
        value={value}
        readOnly={readOnly}
        required={valueRequired}
        label={this.i18n('entity.AutomaticRole.attribute.value.label')}
        helpBlock={this.i18n('entity.AutomaticRole.attribute.value.help')}/>);
  }

  render() {
    const { uiKey, entity, readOnly} = this.props;
    const {
      typeForceSearchParameters,
      type,
      valueRequired,
      formAttribute,
      attributeName
    } = this.state;

    let data = this.state.entity;
    if (!data) {
      data = entity;
    }
    //
    return (
      <div>
          <Basic.AbstractForm
            ref="form"
            uiKey={uiKey}
            data={data}
            readOnly={ readOnly}>
            <Basic.EnumSelectBox
              ref="type"
              required
              label={this.i18n('entity.AutomaticRole.attribute.type.label')}
              helpBlock={this.i18n('entity.AutomaticRole.attribute.type.help')}
              enum={AutomaticRoleAttributeRuleTypeEnum}
              onChange={this._typeChange.bind(this)}/>
            <Basic.EnumSelectBox
              ref="attributeName"
              clearable={false}
              label={this.i18n('entity.AutomaticRole.attribute.attributeName')}
              enum={
                type === AutomaticRoleAttributeRuleTypeEnum.findKeyBySymbol(AutomaticRoleAttributeRuleTypeEnum.IDENTITY)
                ?
                IdentityAttributeEnum
                :
                ContractAttributeEnum
              }
              hidden={typeForceSearchParameters !== null}
              onChange={this._attributeNameChange.bind(this)}
              required={!(typeForceSearchParameters !== null)}/>
            <Basic.SelectBox
              ref="formAttribute"
              useFirst
              clearable={false}
              returnProperty={null}
              onChange={this._formAttributeChange.bind(this)}
              forceSearchParameters={typeForceSearchParameters}
              label={this.i18n('entity.AutomaticRole.attribute.formAttribute')}
              hidden={typeForceSearchParameters === null}
              required={!(typeForceSearchParameters === null)}
              manager={this.formAttributeManager}/>
            <Basic.Row>
              <div className="col-lg-4">
                <Basic.EnumSelectBox
                  ref="comparison"
                  required
                  useFirst
                  onChange={this._comparsionChange.bind(this)}
                  label={this.i18n('entity.AutomaticRole.attribute.comparison')}
                  enum={AutomaticRoleAttributeRuleComparisonEnum}/>
              </div>
              <div className="col-lg-8">
                { this._getValueField(type, valueRequired, formAttribute, attributeName) }
              </div>
            </Basic.Row>
          </Basic.AbstractForm>
      </div>
    );
  }
}

AutomaticRoleAttributeRuleDetail.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
  attributeId: PropTypes.string,
  readOnly: PropTypes.bool
};
AutomaticRoleAttributeRuleDetail.defaultProps = {
  readOnly: false
};
