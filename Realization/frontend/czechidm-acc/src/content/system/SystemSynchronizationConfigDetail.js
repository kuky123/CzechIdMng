import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Domain, Managers, Utils, Advanced } from 'czechidm-core';
import { SynchronizationConfigManager, SynchronizationLogManager, SystemEntityHandlingManager, SchemaAttributeHandlingManager} from '../../redux';
import ReconciliationMissingAccountActionTypeEnum from '../../domain/ReconciliationMissingAccountActionTypeEnum';
import SynchronizationLinkedActionTypeEnum from '../../domain/SynchronizationLinkedActionTypeEnum';
import SynchronizationMissingEntityActionTypeEnum from '../../domain/SynchronizationMissingEntityActionTypeEnum';
import SynchronizationUnlinkedActionTypeEnum from '../../domain/SynchronizationUnlinkedActionTypeEnum';
import uuid from 'uuid';

const uiKey = 'system-synchronization-config';
const uiKeyLogs = 'system-synchronization-logs';
const synchronizationLogManager = new SynchronizationLogManager();
const synchronizationConfigManager = new SynchronizationConfigManager();
const systemEntityHandlingManager = new SystemEntityHandlingManager();
const schemaAttributeHandlingManager = new SchemaAttributeHandlingManager();

class SystemSynchronizationConfigDetail extends Basic.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      attributeMappingId: null, // dependant select box
      forceSearchCorrelationAttribute: new Domain.SearchParameters()
        .setFilter('entityHandlingId', Domain.SearchParameters.BLANK_UUID) // dependant select box
    };
  }

  getManager() {
    return synchronizationConfigManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.systemSynchronizationConfigDetail';
  }

  showDetail(entity, add) {
    const synchronizationConfigId = this.props._synchronizationConfig.id;
    const {systemId} = this.props.params;
    if (add) {
      const uuidId = uuid.v1();
      this.context.router.push(`/system/${systemId}/synchronization-log/${uuidId}/new?new=1&synchronizationConfigId=${synchronizationConfigId}&systemId=${systemId}`);
    } else {
      this.context.router.push(`/system/${systemId}/synchronization-log/${entity.id}/detail`);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { synchronizationConfigId} = nextProps.params;
    if (synchronizationConfigId && synchronizationConfigId !== this.props.params.synchronizationConfigId) {
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
    const {configId} = props.params;
    if (this._getIsNew(props)) {
      this.setState({synchronizationConfig: {
        system: props.location.query.systemId,
        linkedAction: SynchronizationLinkedActionTypeEnum.findKeyBySymbol(SynchronizationLinkedActionTypeEnum.UPDATE_ENTITY),
        unlinkedAction: SynchronizationUnlinkedActionTypeEnum.findKeyBySymbol(SynchronizationUnlinkedActionTypeEnum.LINK),
        missingEntityAction: SynchronizationMissingEntityActionTypeEnum.findKeyBySymbol(SynchronizationMissingEntityActionTypeEnum.CREATE_ENTITY),
        missingAccountAction: ReconciliationMissingAccountActionTypeEnum.findKeyBySymbol(ReconciliationMissingAccountActionTypeEnum.IGNORE)
      }});
    } else {
      this.context.store.dispatch(synchronizationConfigManager.fetchEntity(configId));
    }
    this.selectNavigationItems(['sys-systems', 'system-synchronization-configs']);
  }

  /**
   * Saves give entity
   */
  save(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    const formEntity = this.refs.form.getData();
    formEntity.attributeMapping = systemEntityHandlingManager.getSelfLink(formEntity.attributeMapping);
    formEntity.correlationAttribute = schemaAttributeHandlingManager.getSelfLink(formEntity.correlationAttribute);
    if (formEntity.id === undefined) {
      this.context.store.dispatch(synchronizationConfigManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
        if (!error && this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    } else {
      this.context.store.dispatch(synchronizationConfigManager.patchEntity(formEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
    }
  }

  afterSave(entity, error) {
    if (!error) {
      if (this._getIsNew()) {
        this.addMessage({ message: this.i18n('create.success', { name: entity.name}) });
      } else {
        this.addMessage({ message: this.i18n('save.success', {name: entity.name}) });
      }
      const { entityId } = this.props.params;
      this.context.router.replace(`/system/${entityId}/synchronization-configs/`);
    } else {
      this.addError(error);
    }
    super.afterSave();
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    return (query) ? query.new : null;
  }

  /**
   * Call after attribute mapping selection was changed. Create filter for correlationAttribute
   */
  _onChangeAttributeMapping(attributeMapping) {
    const attributeMappingId = attributeMapping ? attributeMapping.id : null;
    this.setState({
      attributeMappingId,
      forceSearchCorrelationAttribute: this.state.forceSearchCorrelationAttribute.setFilter('entityHandlingId', attributeMappingId || Domain.SearchParameters.BLANK_UUID)
    }, () => {
      // clear selected correlationAttribute
      this.refs.correlationAttribute.setValue(null);
    });
  }

  render() {
    const { _showLoading, _synchronizationConfig} = this.props;
    const {forceSearchCorrelationAttribute} = this.state;
    const systemId = this.props.params.entityId;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('synchronizationConfigId', _synchronizationConfig ? _synchronizationConfig.id : Domain.SearchParameters.BLANK_UUID);
    const forceSearchMappingAttributes = new Domain.SearchParameters().setFilter('systemId', systemId ? systemId : Domain.SearchParameters.BLANK_UUID);
    const isNew = this._getIsNew();
    const synchronizationConfig = isNew ? this.state.synchronizationConfig : _synchronizationConfig;

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <form onSubmit={this.save.bind(this)}>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm ref="form" data={synchronizationConfig} showLoading={_showLoading} className="form-horizontal">
              <Basic.Checkbox
                ref="enabled"
                label={this.i18n('acc:entity.SynchronizationConfig.enabled')}/>
              <Basic.TextField
                ref="name"
                label={this.i18n('acc:entity.SynchronizationConfig.name')}
                required/>
              <Basic.TextField
                ref="runOnServer"
                label={this.i18n('acc:entity.SynchronizationConfig.runOnServer')}
                readOnly/>
              <Basic.Checkbox
                ref="reconciliation"
                label={this.i18n('acc:entity.SynchronizationConfig.reconciliation')}/>
              <Basic.DateTimePicker
                ref="timestamp"
                label={this.i18n('acc:entity.SynchronizationConfig.timestamp')}/>
              <Basic.Checkbox
                ref="customFilter"
                label={this.i18n('acc:entity.SynchronizationConfig.customFilter')}/>
              <Basic.ScriptArea
                ref="customFilterScript"
                helpBlock={this.i18n('acc:entity.SynchronizationConfig.customFilterScript.help')}
                label={this.i18n('acc:entity.SynchronizationConfig.customFilterScript.label')}/>
              <Basic.SelectBox
                ref="attributeMapping"
                manager={systemEntityHandlingManager}
                forceSearchParameters={forceSearchMappingAttributes}
                onChange={this._onChangeAttributeMapping.bind(this)}
                label={this.i18n('acc:entity.SynchronizationConfig.attributeMapping')}
                required/>
              <Basic.SelectBox
                ref="correlationAttribute"
                manager={schemaAttributeHandlingManager}
                forceSearchParameters={forceSearchCorrelationAttribute}
                label={this.i18n('acc:entity.SynchronizationConfig.correlationAttribute')}
                required/>
              <Basic.EnumSelectBox
                ref="linkedAction"
                enum={SynchronizationLinkedActionTypeEnum}
                label={this.i18n('acc:entity.SynchronizationConfig.linkedAction')}
                required/>
              <Basic.EnumSelectBox
                ref="unlinkedAction"
                enum={SynchronizationUnlinkedActionTypeEnum}
                label={this.i18n('acc:entity.SynchronizationConfig.unlinkedAction')}
                required/>
              <Basic.EnumSelectBox
                ref="missingEntityAction"
                enum={SynchronizationMissingEntityActionTypeEnum}
                label={this.i18n('acc:entity.SynchronizationConfig.missingEntityAction')}
                required/>
              <Basic.EnumSelectBox
                ref="missingAccountAction"
                enum={ReconciliationMissingAccountActionTypeEnum}
                label={this.i18n('acc:entity.SynchronizationConfig.missingAccountAction')}
                required/>
              <Basic.TextField
                ref="token"
                label={this.i18n('acc:entity.SynchronizationConfig.token')}
                readOnly/>
              <Basic.TextArea
                ref="description"
                label={this.i18n('acc:entity.SynchronizationConfig.description')}/>
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
        <Basic.ContentHeader rendered={synchronizationConfig && !isNew} style={{ marginBottom: 0 }}>
          <Basic.Icon value="transfer"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('synchronizationLogsHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={synchronizationConfig && !isNew} className="no-border">
          <Advanced.Table
            ref="table"
            uiKey={uiKeyLogs}
            manager={synchronizationLogManager}
            forceSearchParameters={forceSearchParameters}
            showRowSelection={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_WRITE'])}
            rowClass={({rowIndex, data}) => { return data[rowIndex].disabledAttribute ? 'disabled' : ''; }}
            actions={
              Managers.SecurityManager.hasAnyAuthority(['SYSTEM_WRITE'])
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { }, true)}
                  rendered={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_WRITE'])}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm" className="form-horizontal">
                  <Basic.Row className="last">
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="idmPropertyName"
                        label={this.i18n('filter.idmPropertyName.label')}
                        placeholder={this.i18n('filter.idmPropertyName.placeholder')}/>
                    </div>
                    <div className="col-lg-2"/>
                    <div className="col-lg-4 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }>
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex], false)}/>
                  );
                }
              }/>
              <Advanced.Column property="running" face="boolean" header={this.i18n('acc:entity.SynchronizationLog.running')} sort/>
              <Advanced.Column property="started" face="dateTime" header={this.i18n('acc:entity.SynchronizationLog.started')} sort/>
              <Advanced.Column property="ended" face="dateTime" header={this.i18n('acc:entity.SynchronizationLog.ended')} sort/>
              <Advanced.Column property="token" face="text" header={this.i18n('acc:entity.SynchronizationLog.token')} sort/>
            </Advanced.Table>
          </Basic.Panel>
        </div>
    );
  }
}

SystemSynchronizationConfigDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemSynchronizationConfigDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, synchronizationConfigManager.getEntityType(), component.params.configId);
  if (entity) {
    const correlationAttribute = entity._embedded && entity._embedded.correlationAttribute ? entity._embedded.correlationAttribute.id : null;
    entity.correlationAttribute = correlationAttribute;
    const attributeMapping = entity._embedded && entity._embedded.attributeMapping ? entity._embedded.attributeMapping.id : null;
    entity.attributeMapping = attributeMapping;
  }
  return {
    _synchronizationConfig: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemSynchronizationConfigDetail);
