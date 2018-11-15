import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Advanced, Utils, Domain, Managers, Enums } from 'czechidm-core';
import { SystemMappingManager, SystemAttributeMappingManager, SchemaAttributeManager, AttributeControlledValueManager} from '../../redux';
import AttributeMappingStrategyTypeEnum from '../../domain/AttributeMappingStrategyTypeEnum';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
import AttributeControlledValueTable from './AttributeControlledValueTable';

const uiKey = 'system-attribute-mapping';
const manager = new SystemAttributeMappingManager();
const controlledValueManager = new AttributeControlledValueManager();
const systemMappingManager = new SystemMappingManager();
const schemaAttributeManager = new SchemaAttributeManager();
const scriptManager = new Managers.ScriptManager();
// password attributes is from 9.3.0 marked as passwordAttribute (boolean)
// const PASSWORD_ATTRIBUTE = '__PASSWORD__';

class SystemAttributeMappingDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      activeKey: 1,
      strategyType: null
    };
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.attributeMappingDetail';
  }

  componentWillReceiveProps(nextProps) {
    const {_attribute} = nextProps;
    const { attributeId} = nextProps.params;
    if (attributeId && attributeId !== this.props.params.attributeId) {
      this._initComponent(nextProps);
    }
    if (_attribute && _attribute !== this.props._attribute) {
      if (_attribute && this.refs.form) {
        this.setState({
          disabledAttribute: _attribute.disabledAttribute,
          entityAttribute: _attribute.entityAttribute,
          extendedAttribute: _attribute.extendedAttribute,
          passwordAttribute: _attribute.passwordAttribute
        }, () => {
          // Workaround - We need set value after enumeration type was changed
          this.refs.idmPropertyEnum.setValue(_attribute.idmPropertyEnum);
        });
      }
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const { attributeId} = props.params;
    if (this._getIsNew(props)) {
      this.setState({
        attribute: {
          systemMapping: props.location.query.mappingId,
          objectClassId: props.location.query.objectClassId,
          cached: true,
          strategyType: AttributeMappingStrategyTypeEnum.findKeyBySymbol(AttributeMappingStrategyTypeEnum.SET)
        }
      });
    //  this.context.store.dispatch(systemMappingManager.fetchEntity(props.location.query.mappingId));
    } else {
      this.context.store.dispatch(this.getManager().fetchEntity(attributeId));
    }
    this.selectNavigationItems(['sys-systems', 'system-mappings']);
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    return (query) ? query.new : null;
  }

  save(event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      if (this._getIsNew()) {
        this.addMessage({ message: this.i18n('create.success', { name: entity.name }) });
      } else {
        this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
      }
      this.context.router.goBack();
    } else {
      this.addError(error);
    }
    super.afterSave();
  }

  closeDetail() {
    this.refs.form.processEnded();
  }

  _checkboxChanged(key, uncheckKey, event) {
    const checked = event.currentTarget.checked;
    this.setState({[key]: checked}, ()=>{
      if (checked && uncheckKey !== null) {
        this.setState({[uncheckKey]: false});
        this.refs[uncheckKey].setState({value: false}, () => {
          this.forceUpdate();
        });
      }
      if (key === 'entityAttribute') {
        this.refs.idmPropertyName.setValue(null);
        this.refs.idmPropertyEnum.setValue(null);
      }
      if (key === 'passwordAttribute') {
        this.refs.entityAttribute.setValue(false);
        this.refs.extendedAttribute.setValue(false);
        this.refs.idmPropertyName.setValue(null);
        this.refs.idmPropertyEnum.setValue(null);
      }
    });
  }

  _schemaAttributeChange(value) {
    if (!this.refs.name.getValue()) {
      this.refs.name.setValue(value.name);
    }
  }

  _strategyTypeChange(value) {
    if (!value) {
      return;
    }
    this.setState({
      strategyType: value.value
    });
  }

  _onChangeEntityEnum(item) {
    const {_systemMapping} = this.props;
    if (item) {
      const field = SystemEntityTypeEnum.getEntityEnum(_systemMapping ? _systemMapping.entityType : 'IDENTITY').getField(item.value);
      this.refs.idmPropertyName.setValue(field);
    } else {
      this.refs.idmPropertyName.setValue(null);
    }
  }

  _onChangeSelectTabs(activeKey) {
    this.setState({activeKey});
  }

  render() {
    const { _showLoading, _attribute, _systemMapping } = this.props;
    const { disabledAttribute, entityAttribute, extendedAttribute, activeKey, strategyType, passwordAttribute } = this.state;

    const isNew = this._getIsNew();
    const attribute = isNew ? this.state.attribute : _attribute;
    if (!attribute) {
      return <div/>;
    }
    const forceSearchParameters = new Domain.SearchParameters().setFilter('objectClassId',
     attribute && attribute.objectClassId ? attribute.objectClassId : Domain.SearchParameters.BLANK_UUID);
    const controlledValuesForceSearchParameters = new Domain.SearchParameters().setFilter('attributeMappingId', attribute.id).setFilter('historicValue', false);
    const historicValuesForceSearchParameters = new Domain.SearchParameters().setFilter('attributeMappingId', attribute.id).setFilter('historicValue', true);
    const _isDisabled = disabledAttribute;
    const _isEntityAttribute = entityAttribute;
    const _isExtendedAttribute = extendedAttribute;
    const _showNoRepositoryAlert = (!_isExtendedAttribute && !_isEntityAttribute);
    const entityTypeEnum = SystemEntityTypeEnum.getEntityEnum(_systemMapping ? _systemMapping.entityType : 'IDENTITY');
    const _isRequiredIdmField = (_isEntityAttribute || _isExtendedAttribute) && !_isDisabled;
    const isSynchronization = _systemMapping && _systemMapping.operationType && _systemMapping.operationType === 'SYNCHRONIZATION' ? true : false;
    const strategyTypeTemp = strategyType ? strategyType : attribute.strategyType;
    const isMerge = strategyTypeTemp === AttributeMappingStrategyTypeEnum.findKeyBySymbol(AttributeMappingStrategyTypeEnum.MERGE);

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.ContentHeader>
          <Basic.Icon value="list-alt"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header', attribute ? { name: attribute.idmPropertyName} : {})}}/>
        </Basic.ContentHeader>
        <Basic.Tabs
          activeKey={activeKey}
          onSelect={this._onChangeSelectTabs.bind(this)}>
          <Basic.Tab eventKey={1} title={this.i18n('tabs.basic.label')} className="bordered">
          <form onSubmit={this.save.bind(this)}>
            <Basic.Panel className="no-border last" style={{ marginBottom: 0, paddingRight: 15, paddingLeft: 15, paddingTop: 15 }}>
              <Basic.AbstractForm
                ref="form"
                data={ attribute }
                showLoading={ _showLoading }
                readOnly={ !Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                <Basic.Checkbox
                  ref="disabledAttribute"
                  onChange={this._checkboxChanged.bind(this, 'disabledAttribute', null)}
                  tooltip={this.i18n('acc:entity.SystemAttributeMapping.disabledAttribute.tooltip')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.disabledAttribute.label')}/>
                <Basic.SelectBox
                  ref="systemMapping"
                  manager={systemMappingManager}
                  label={this.i18n('acc:entity.SystemAttributeMapping.systemMapping')}
                  readOnly
                  required/>
                <Basic.SelectBox
                  ref="schemaAttribute"
                  manager={schemaAttributeManager}
                  forceSearchParameters={forceSearchParameters}
                  onChange={this._schemaAttributeChange.bind(this)}
                  label={this.i18n('acc:entity.SystemAttributeMapping.schemaAttribute')}
                  required
                  pageSize={ Domain.SearchParameters.MAX_SIZE }/>
                <Basic.TextField
                  ref="name"
                  label={this.i18n('acc:entity.SystemAttributeMapping.name.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.name.help')}
                  required
                  max={255}/>
                <Basic.EnumSelectBox
                  ref="strategyType"
                  enum={AttributeMappingStrategyTypeEnum}
                  onChange={this._strategyTypeChange.bind(this)}
                  label={this.i18n('acc:entity.SystemAttributeMapping.strategyType')}
                  required/>
                <Basic.Checkbox
                  ref="sendAlways"
                  hidden={isSynchronization}
                  tooltip={this.i18n('acc:entity.SystemAttributeMapping.sendAlways.tooltip')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.sendAlways.label')}
                  readOnly = {_isDisabled}/>
                <Basic.Checkbox
                  ref="sendOnlyIfNotNull"
                  hidden={isSynchronization}
                  tooltip={this.i18n('acc:entity.SystemAttributeMapping.sendOnlyIfNotNull.tooltip')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.sendOnlyIfNotNull.label')}
                  readOnly = {_isDisabled}/>
                <Basic.Checkbox
                  ref="uid"
                  onChange={this._checkboxChanged.bind(this, 'uid', null)}
                  tooltip={this.i18n('acc:entity.SystemAttributeMapping.uid.tooltip')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.uid.label')}
                  readOnly = {_isDisabled}/>
                <Basic.Checkbox
                  ref="entityAttribute"
                  onChange={this._checkboxChanged.bind(this, 'entityAttribute', 'extendedAttribute')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.entityAttribute')}
                  readOnly = {_isDisabled || passwordAttribute}/>
                <Basic.Checkbox
                  ref="extendedAttribute"
                  onChange={this._checkboxChanged.bind(this, 'extendedAttribute', 'entityAttribute')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.extendedAttribute.label')}
                  readOnly = {_isDisabled || passwordAttribute}
                  helpBlock={ this.i18n('acc:entity.SystemAttributeMapping.extendedAttribute.help', { escape: false }) }/>
                <Basic.Checkbox
                  ref="confidentialAttribute"
                  label={this.i18n('acc:entity.SystemAttributeMapping.confidentialAttribute')}
                  readOnly = {_isDisabled || !_isRequiredIdmField}/>
                <Basic.Checkbox
                  ref="authenticationAttribute"
                  label={this.i18n('acc:entity.SystemAttributeMapping.authenticationAttribute.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.authenticationAttribute.help')}/>
                <Basic.Checkbox
                  ref="sendOnPasswordChange"
                  label={this.i18n('acc:entity.SystemAttributeMapping.sendOnPasswordChange.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.sendOnPasswordChange.help')}/>
                <Basic.Checkbox
                  ref="passwordAttribute"
                  hidden={isSynchronization}
                  onChange={this._checkboxChanged.bind(this, 'passwordAttribute', null)}
                  label={this.i18n('acc:entity.SystemAttributeMapping.passwordAttribute.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.passwordAttribute.help')}/>
                <Basic.Checkbox
                  ref="cached"
                  label={this.i18n('acc:entity.SystemAttributeMapping.cached.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.cached.help', { escape: false })}/>
                <Basic.Row>
                  <div className="col-lg-6">
                    <Basic.EnumSelectBox
                      ref="idmPropertyEnum"
                      readOnly = {_isDisabled || !_isEntityAttribute || passwordAttribute}
                      enum={entityTypeEnum}
                      onChange={this._onChangeEntityEnum.bind(this)}
                      label={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyEnum')}
                      />
                  </div>
                  <div className="col-lg-6">
                    <Basic.TextField
                      ref="idmPropertyName"
                      readOnly = {_isDisabled || !_isRequiredIdmField || _isEntityAttribute || passwordAttribute}
                      label={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyName.label')}
                      helpBlock={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyName.help')}
                      required = {_isRequiredIdmField}
                      max={255}/>
                  </div>
                </Basic.Row>
                <Basic.LabelWrapper label=" ">
                  <Basic.Alert
                     rendered={_showNoRepositoryAlert && !passwordAttribute}
                     key="no-repository-alert"
                     icon="exclamation-sign"
                     className="no-margin"
                     text={this.i18n('alertNoRepository')}/>
                   <Basic.Alert
                      rendered={passwordAttribute === true}
                      key="password-info-alert"
                      icon="exclamation-sign"
                      className="no-margin"
                      text={this.i18n('infoPasswordMapping')}/>
                </Basic.LabelWrapper>

                <Advanced.ScriptArea
                  ref="transformFromResourceScript"
                  scriptCategory={[Enums.ScriptCategoryEnum.findKeyBySymbol(Enums.ScriptCategoryEnum.TRANSFORM_FROM), Enums.ScriptCategoryEnum.findKeyBySymbol(Enums.ScriptCategoryEnum.DEFAULT)]}
                  headerText={this.i18n('acc:entity.SystemAttributeMapping.transformFromResourceScriptSelectBox.label')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.transformFromResourceScript.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.transformFromResourceScript.help')}
                  scriptManager={scriptManager} />

                <Advanced.ScriptArea
                  ref="transformToResourceScript"
                  rendered={!isMerge}
                  scriptCategory={[Enums.ScriptCategoryEnum.findKeyBySymbol(Enums.ScriptCategoryEnum.DEFAULT), Enums.ScriptCategoryEnum.findKeyBySymbol(Enums.ScriptCategoryEnum.TRANSFORM_TO)]}
                  headerText={this.i18n('acc:entity.SystemAttributeMapping.transformToResourceScriptSelectBox.label')}
                  helpBlock={this.i18n('acc:entity.SystemAttributeMapping.transformToResourceScript.help')}
                  label={this.i18n('acc:entity.SystemAttributeMapping.transformToResourceScript.label')}
                  scriptManager={scriptManager} />

              </Basic.AbstractForm>
              <Basic.PanelFooter>
                <Basic.Button type="button" level="link"
                  onClick={this.context.router.goBack}
                  showLoading={_showLoading}>
                  {this.i18n('button.back')}
                </Basic.Button>
                <Basic.Button
                  level="success"
                  type="submit"
                  showLoading={ _showLoading }
                  rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                  {this.i18n('button.save')}
                </Basic.Button>
              </Basic.PanelFooter>
            </Basic.Panel>
          </form>
        </Basic.Tab>
        <Basic.Tab eventKey={2} rendered={isMerge && !isNew} title={this.i18n('tabs.controlledValues.label')} className="bordered">
          <Basic.ContentHeader text={ this.i18n('tabs.controlledValues.cache.header') }
            style={{ marginBottom: 0, paddingTop: 15, paddingRight: 15, paddingLeft: 15 }}/>
          <Basic.Alert
            level="info"
            showHtmlText
            text={ this.i18n('tabs.controlledValues.cache.helpBlock') }
              style={{ marginBottom: 0, marginRight: 15, marginLeft: 15 }}
            />
          <Basic.Alert
            level="warning"
            rendered={attribute && attribute.evictControlledValuesCache}
            showHtmlText
            text={ this.i18n('tabs.controlledValues.cache.evicted') }
              style={{ marginBottom: 0, marginRight: 15, marginLeft: 15 }}
            />
          <AttributeControlledValueTable
            ref="controlledValuesTable"
            uiKey={'attribute-mapping-controlled-values' + attribute.id}
            showRowSelection={false}
            manager={controlledValueManager}
            forceSearchParameters={controlledValuesForceSearchParameters}
            />
          <Basic.ContentHeader text={ this.i18n('tabs.controlledValues.historic.header') }
            style={{ marginBottom: 0, paddingTop: 15, paddingRight: 15, paddingLeft: 15 }}/>
          <Basic.Alert
            level="info"
            showHtmlText
            text={ this.i18n('tabs.controlledValues.historic.helpBlock') }
            style={{ marginBottom: 0, marginRight: 15, marginLeft: 15 }}
            />
          <AttributeControlledValueTable
            ref="historicValuesTable"
            uiKey={'attribute-mapping-historic-values' + attribute.id}
            manager={controlledValueManager}
            forceSearchParameters={historicValuesForceSearchParameters}
            />
        </Basic.Tab>
      </Basic.Tabs>
      </div>
    );
  }
}

SystemAttributeMappingDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemAttributeMappingDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, manager.getEntityType(), component.params.attributeId);
  let systemMapping = null;
  if (component && component.location && component.location.query.new) {
    systemMapping = Utils.Entity.getEntity(state, systemMappingManager.getEntityType(), component.location.query.mappingId);
  }
  if (entity) {
    systemMapping = entity._embedded && entity._embedded.systemMapping ? entity._embedded.systemMapping : null;
    const schemaAttribute = entity._embedded && entity._embedded.schemaAttribute ? entity._embedded.schemaAttribute : null;
    entity.systemMapping = systemMapping;
    entity.schemaAttribute = schemaAttribute;
    entity.objectClassId = schemaAttribute ? schemaAttribute.objectClass : Domain.SearchParameters.BLANK_UUID;
    entity.idmPropertyEnum = SystemEntityTypeEnum.getEntityEnum(systemMapping ? systemMapping.entityType : 'IDENTITY').getEnum(entity.idmPropertyName);
  }
  return {
    _attribute: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _systemMapping: systemMapping
  };
}

export default connect(select)(SystemAttributeMappingDetail);
