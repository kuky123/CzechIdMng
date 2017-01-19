import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Utils, Domain} from 'czechidm-core';
import { SystemMappingManager, SystemAttributeMappingManager, SchemaAttributeManager} from '../../redux';

const uiKey = 'system-attribute-mapping';
const manager = new SystemAttributeMappingManager();
const systemMappingManager = new SystemMappingManager();
const schemaAttributeManager = new SchemaAttributeManager();

class SystemAttributeMappingDetail extends Basic.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
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
    const { attributeId} = nextProps.params;
    if (attributeId && attributeId !== this.props.params.attributeId) {
      this._initComponent(nextProps);
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
          objectClassId: props.location.query.objectClassId
        }
      });
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
    formEntity.systemMapping = systemMappingManager.getSelfLink(formEntity.systemMapping);
    formEntity.schemaAttribute = schemaAttributeManager.getSelfLink(formEntity.schemaAttribute);
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

  _uidChanged(event) {
    const checked = event.currentTarget.checked;
    // I need set value direct to checkbox (this event is run befor state is set, but I need him in render mothod now)
    this.refs.uid.setState({value: checked}, () => {
      this.forceUpdate();
    });
  }

  _disabledChanged(key, event) {
    const checked = event.currentTarget.checked;
    // I need set value direct to checkbox (this event is run befor state is set, but I need him in render mothod now)
    this.refs[key].setState({value: checked}, () => {
      this.forceUpdate();
    });
  }

  _checkboxChanged(key, disableKey, event) {
    const checked = event.currentTarget.checked;
    // I need set value direct to checkbox (this event is run befor state is set, but I need him in render mothod now)
    if (checked) {
      this.refs[disableKey].setState({value: false});
    }
    this.refs[key].setState({value: checked}, () => {
      this.forceUpdate();
    });
  }

  _schemaAttributeChange(value) {
    if (!this.refs.name.getValue()) {
      this.refs.name.setValue(value.name);
    }
  }

  render() {
    const { _showLoading, _attribute} = this.props;
    const isNew = this._getIsNew();
    const attribute = isNew ? this.state.attribute : _attribute;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('objectClassId', attribute && attribute.objectClassId ? attribute.objectClassId : Domain.SearchParameters.BLANK_UUID);

    const _isDisabled = this.refs.disabledAttribute ? this.refs.disabledAttribute.getValue() : false;
    const _isEntityAttribute = this.refs.entityAttribute ? this.refs.entityAttribute.getValue() : false;
    const _isExtendedAttribute = this.refs.extendedAttribute ? this.refs.extendedAttribute.getValue() : false;
    const _showNoRepositoryAlert = (!_isExtendedAttribute && !_isEntityAttribute);

    const _isRequiredIdmField = (_isEntityAttribute || _isExtendedAttribute) && !_isDisabled;

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <Basic.Icon value="list-alt"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header', attribute ? { name: attribute.idmPropertyName} : {})}}/>
        </Basic.ContentHeader>
        <form onSubmit={this.save.bind(this)}>
          <Basic.Panel className="no-border last">
            <Basic.AbstractForm ref="form" data={attribute} showLoading={_showLoading} className="form-horizontal">
              <Basic.Checkbox
                ref="disabledAttribute"
                onChange={this._disabledChanged.bind(this, 'disabledAttribute')}
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
                required/>
              <Basic.TextField
                ref="name"
                label={this.i18n('acc:entity.SystemAttributeMapping.name.label')}
                helpBlock={this.i18n('acc:entity.SystemAttributeMapping.name.help')}
                required
                max={255}/>
              <Basic.Checkbox
                ref="uid"
                onChange={this._uidChanged.bind(this)}
                tooltip={this.i18n('acc:entity.SystemAttributeMapping.uid.tooltip')}
                label={this.i18n('acc:entity.SystemAttributeMapping.uid.label')}
                readOnly = {_isDisabled}/>
              <Basic.Checkbox
                ref="entityAttribute"
                onChange={this._checkboxChanged.bind(this, 'entityAttribute', 'extendedAttribute')}
                label={this.i18n('acc:entity.SystemAttributeMapping.entityAttribute')}
                readOnly = {_isDisabled}/>
              <Basic.Checkbox
                ref="extendedAttribute"
                onChange={this._checkboxChanged.bind(this, 'extendedAttribute', 'entityAttribute')}
                label={this.i18n('acc:entity.SystemAttributeMapping.extendedAttribute')}
                readOnly = {_isDisabled}/>
              <Basic.Checkbox
                ref="confidentialAttribute"
                label={this.i18n('acc:entity.SystemAttributeMapping.confidentialAttribute')}
                readOnly = {_isDisabled || !_isRequiredIdmField}/>
              <Basic.TextField
                ref="idmPropertyName"
                readOnly = {_isDisabled || !_isRequiredIdmField}
                label={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyName.label')}
                helpBlock={this.i18n('acc:entity.SystemAttributeMapping.idmPropertyName.help')}
                required = {_isRequiredIdmField}
                max={255}/>
              <Basic.LabelWrapper label=" ">
                <Basic.Alert
                   rendered={_showNoRepositoryAlert}
                   key="no-repository-alert"
                   icon="exclamation-sign"
                   className="no-margin"
                   text={this.i18n('alertNoRepository')}/>
              </Basic.LabelWrapper>
              <Basic.ScriptArea
                ref="transformFromResourceScript"
                helpBlock={this.i18n('acc:entity.SystemAttributeMapping.transformFromResourceScript.help')}
                readOnly = {_isDisabled}
                label={this.i18n('acc:entity.SystemAttributeMapping.transformFromResourceScript.label')}/>
              <Basic.ScriptArea
                ref="transformToResourceScript"
                helpBlock={this.i18n('acc:entity.SystemAttributeMapping.transformToResourceScript.help')}
                readOnly = {_isDisabled}
                label={this.i18n('acc:entity.SystemAttributeMapping.transformToResourceScript.label')}/>
            </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={_showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
              <Basic.Button
                onClick={this.save.bind(this)}
                level="success"
                type="submit"
                showLoading={_showLoading}>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
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
  if (entity) {
    const systemMapping = entity._embedded && entity._embedded.systemMapping ? entity._embedded.systemMapping : null;
    const schemaAttribute = entity._embedded && entity._embedded.schemaAttribute ? entity._embedded.schemaAttribute : null;
    entity.systemMapping = systemMapping;
    entity.schemaAttribute = schemaAttribute;
    entity.objectClassId = systemMapping ? systemMapping.objectClass.id : Domain.SearchParameters.BLANK_UUID;
  }
  return {
    _attribute: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemAttributeMappingDetail);