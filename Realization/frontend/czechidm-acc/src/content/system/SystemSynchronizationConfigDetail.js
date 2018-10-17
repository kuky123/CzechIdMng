import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
import moment from 'moment';
//
import { Basic, Domain, Managers, Utils, Advanced, Services} from 'czechidm-core';
import { SynchronizationConfigManager, SynchronizationLogManager, SystemMappingManager, SystemAttributeMappingManager} from '../../redux';
import ReconciliationMissingAccountActionTypeEnum from '../../domain/ReconciliationMissingAccountActionTypeEnum';
import SynchronizationLinkedActionTypeEnum from '../../domain/SynchronizationLinkedActionTypeEnum';
import SynchronizationMissingEntityActionTypeEnum from '../../domain/SynchronizationMissingEntityActionTypeEnum';
import SynchronizationUnlinkedActionTypeEnum from '../../domain/SynchronizationUnlinkedActionTypeEnum';
import IcFilterOperationTypeEnum from '../../domain/IcFilterOperationTypeEnum';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
import SyncIdentityConfig from '../sync/SyncIdentityConfig';
import SyncContractConfig from '../sync/SyncContractConfig';

const uiKey = 'system-synchronization-config';
const uiKeyLogs = 'system-synchronization-logs';
const syncActionWfKey = 'eu.bcvsolutions.sync.action';
const synchronizationLogManager = new SynchronizationLogManager();
const synchronizationConfigManager = new SynchronizationConfigManager();
const systemMappingManager = new SystemMappingManager();
const systemAttributeMappingManager = new SystemAttributeMappingManager();
const workflowProcessDefinitionManager = new Managers.WorkflowProcessDefinitionManager();

class SystemSynchronizationConfigDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      systemMappingId: null, // dependant select box
      activeKey: 1,
      forceSearchCorrelationAttribute: new Domain.SearchParameters()
        .setFilter('mappingId', Domain.SearchParameters.BLANK_UUID), // dependant select box
      enabled: null
    };
  }

  getManager() {
    return synchronizationLogManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.systemSynchronizationConfigDetail';
  }

  showDetail(entity) {
    const {entityId} = this.props.params;
    this.context.router.push(`/system/${entityId}/synchronization-logs/${entity.id}/detail`);
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
    if (this.refs.name) {
      this.refs.name.focus();
    }
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const {configId} = props.params;
    if (this._getIsNew(props)) {
      this.setState({
        synchronizationConfig: {
          system: props.location.query.systemId,
          linkedAction: SynchronizationLinkedActionTypeEnum.findKeyBySymbol(SynchronizationLinkedActionTypeEnum.UPDATE_ENTITY),
          unlinkedAction: SynchronizationUnlinkedActionTypeEnum.findKeyBySymbol(SynchronizationUnlinkedActionTypeEnum.LINK_AND_UPDATE_ACCOUNT),
          missingEntityAction: SynchronizationMissingEntityActionTypeEnum.findKeyBySymbol(SynchronizationMissingEntityActionTypeEnum.CREATE_ENTITY),
          missingAccountAction: ReconciliationMissingAccountActionTypeEnum.findKeyBySymbol(ReconciliationMissingAccountActionTypeEnum.IGNORE),
          filterOperation: IcFilterOperationTypeEnum.findKeyBySymbol(IcFilterOperationTypeEnum.GREATER_THAN),
          enabled: false
        },
        enabled: false
      });
    } else {
      this.context.store.dispatch(synchronizationConfigManager.fetchEntity(configId));
    }
    this.selectNavigationItems(['sys-systems', 'system-synchronization-configs']);
  }

  /**
   * Saves give entity
   */
  save(startSynchronization, close, entityType, event) {
    if (event) {
      event.preventDefault();
    }
    const formValid = this.refs.form.isFormValid();
    const formFilterValid = this.refs.formFilter.isFormValid();
    if (this.refs.formSpecific) {
      const formSpecificValid = this.refs.formSpecific.isFormValid();
      if (!formSpecificValid) {
        this.setState({activeKey: 2});
        return;
      }
    }
    if (!formValid) {
      this.setState({activeKey: 1});
      return;
    }
    if (!formFilterValid) {
      this.setState({activeKey: 3});
      return;
    }
    this.setState({showLoading: true});
    const formEntity = this.refs.form.getData();
    const filterData = this.refs.formFilter.getData(false);
    if (this.refs.formSpecific) {
      const specificData = this.refs.formSpecific.getData(false);
        // Merge specific data to form.
      _.merge(formEntity, specificData);
    }
    // Merge filter data to form.
    _.merge(formEntity, filterData);

    if (entityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.CONTRACT)) {
      formEntity._type = 'SysSyncContractConfigDto';
    } else if (entityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.CONTRACT_SLICE)) {
      formEntity._type = 'SysSyncContractConfigDto';
    } else if (entityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY)) {
      formEntity._type = 'SysSyncIdentityConfigDto';
    } else {
      formEntity._type = 'SysSyncConfigDto';
    }
    //
    if (formEntity.id === undefined) {
      this.context.store.dispatch(synchronizationConfigManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        if (startSynchronization) {
          this.afterSaveAndStartSynchronization(createdEntity, error);
        } else {
          this.afterSave(createdEntity, error, close);
        }
        if (!error && this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    } else {
      this.context.store.dispatch(synchronizationConfigManager.updateEntity(formEntity, `${uiKey}-detail`, (updatedEntity, error) => {
        if (startSynchronization) {
          this.afterSaveAndStartSynchronization(updatedEntity, error);
        } else {
          this.afterSave(updatedEntity, error, close);
        }
        if (!error && this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }
  }

  afterSave(entity, error, close) {
    const { entityId } = this.props.params;
    if (!error) {
      if (this._getIsNew()) {
        this.addMessage({ message: this.i18n('create.success', { name: entity.name}) });
      } else {
        this.addMessage({ message: this.i18n('save.success', {name: entity.name}) });
      }
      this.context.router.replace(`/system/${entityId}/synchronization-configs/${entity.id}/detail`, { configId: entity.id });
      if (close) {
        this.context.router.replace(`/system/${entityId}/synchronization-configs/`);
      }
    } else {
      this.addError(error);
    }
    super.afterSave();
    this.setState({showLoading: false});
  }

  afterSaveAndStartSynchronization(entity, error) {
    const { entityId } = this.props.params;
    if (!error) {
      this.context.router.replace(`/system/${entityId}/synchronization-configs/${entity.id}/detail`, { configId: entity.id });
      this._startSynchronization(entity);
    } else {
      this.addError(error);
    }
    super.afterSave();
    this.setState({showLoading: false});
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    return (query) ? query.new : null;
  }

  _startSynchronization(sync) {
    this.refs[`confirm-delete`].show(
      this.i18n(`acc:content.system.systemSynchronizationConfigs.action.startSynchronization.message`, {name: sync.name}),
      this.i18n(`acc:content.system.systemSynchronizationConfigs.action.startSynchronization.header`)
    ).then(() => {
      this.setState({
        showLoading: true
      });
      const promise = synchronizationConfigManager.getService().startSynchronization(sync.id);
      promise.then((json) => {
        this.setState({
          showLoading: false
        });
        this.addMessage({ level: 'info', message: this.i18n('acc:content.system.systemSynchronizationConfigs.action.startSynchronization.started', { name: json.name }) });
        this.context.store.dispatch(synchronizationConfigManager.fetchEntity(sync.id));
      }).catch(ex => {
        this.setState({
          showLoading: false
        });
        this.addError(ex);
        this.context.store.dispatch(synchronizationConfigManager.fetchEntity(sync.id));
      });
    }, () => {
      this.setState({
        showLoading: false
      });
    });
    return;
  }

  /**
   * Call after system mapping selection was changed. Create filter for correlationAttribute
   */
  _onChangeSystemMapping(systemMapping) {
    const systemMappingId = systemMapping ? systemMapping.id : null;
    const entityType = systemMapping ? systemMapping.entityType : null;
    const isSelectedTree = entityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.TREE);
    this.setState({
      systemMappingId,
      entityType
    }, () => {
      // clear selected correlationAttribute
      this.refs.correlationAttribute.setValue(null);
      // clear selected tokenAttribute
      this.refs.tokenAttribute.setValue(null);
      // clear selected filterAttribute
      this.refs.filterAttribute.setValue(null);

      if (isSelectedTree) {
        this.refs.reconciliation.setValue(true);
      }
    });
  }

  /**
   * Call after filter attribute selection was changed.
   */
  _onChangeFilterAttribute(filterAttribute) {
    if (!this.refs.tokenAttribute.getValue()) {
      this.refs.tokenAttribute.setValue(filterAttribute);
    }
  }

  _generateResultCell(rowIndex, data) {
    const actions = [];
    if (!data[rowIndex].syncActionLogs) {
      return actions;
    }
    for (const action of data[rowIndex].syncActionLogs) {
      let level = 'default';
      if (action.operationResult === 'SUCCESS') {
        level = 'success';
      }
      if (action.operationResult === 'ERROR') {
        level = 'danger';
      }
      if (action.operationResult === 'WARNING') {
        level = 'warning';
      }
      if (action.operationResult === 'WF') {
        level = 'warning';
      }
      if (action.operationResult === 'IGNORE') {
        level = 'primary';
      }
      actions.push(
        <div>
          <Basic.Label style={{marginRight: '5px'}} level={level} text={action.operationCount}/>
          <label>{this.i18n(`acc:entity.SynchronizationLog.actions.${action.operationResult}.${action.syncAction}`)} </label>
        </div>
      );
    }
    return actions;
  }

  _generateStatisticCell(rowIndex, data) {
    const actions = [];
    if (!data[rowIndex].syncActionLogs) {
      return actions;
    }
    const started = data[rowIndex].started;
    const ended = data[rowIndex].ended ? data[rowIndex].ended : moment().utc().valueOf();
    const timeDiff = moment.utc(moment.duration(moment(ended).diff(moment(started))).asMilliseconds());
    const timeDiffHumanized = moment.duration(moment(ended).diff(moment(started))).locale(Services.LocalizationService.getCurrentLanguage()).humanize();
    let allOperationsCount = 0;
    for (const action of data[rowIndex].syncActionLogs) {
      allOperationsCount = allOperationsCount + action.operationCount;
    }
    const itemsPerSec = Math.round((allOperationsCount / timeDiff * 1000) * 100) / 100;
    if (data[rowIndex].running || data[rowIndex].ended) {
      actions.push(
        <div>
          <Basic.Label
            style={{marginRight: '5px'}}
            level="info"
            title={timeDiffHumanized}
            text={timeDiff.format(this.i18n('format.times'))}/>
          <label>{this.i18n(`acc:entity.SynchronizationLog.statistic.timeDiff`)} </label>
        </div>
      );
    }
    actions.push(
      <div>
        <Basic.Label style={{marginRight: '5px'}} level="info" text={allOperationsCount}/>
        <label>{this.i18n(`acc:entity.SynchronizationLog.statistic.allOperations`)} </label>
      </div>
    );
    if (data[rowIndex].running || data[rowIndex].ended) {
      actions.push(
        <div>
          <Basic.Label style={{marginRight: '5px'}} level="info" text={itemsPerSec}/>
          <label>{this.i18n(`acc:entity.SynchronizationLog.statistic.itemsPerSec`)} </label>
        </div>
      );
    }
    return actions;
  }

  _onChangeSelectTabs(activeKey) {
    this.setState({activeKey});
  }

  _onChangeEnabled(event) {
    this.setState({
      enabled: event.currentTarget.checked
    });
  }

  _getEntityType(synchronizationConfig, entityType) {
    if (entityType !== undefined) {
      return entityType;
    }
    if (synchronizationConfig && synchronizationConfig._embedded && synchronizationConfig._embedded.systemMapping) {
      return synchronizationConfig._embedded.systemMapping.entityType;
    }
    return null;
  }

  getHelp() {
    let helpContent = new Domain.HelpContent();
    helpContent = helpContent.setHeader(
      <span>
        <Basic.Icon value="filter"/> { this.i18n('help.header') }
      </span>
    );
    helpContent = helpContent.setBody(this.i18n('help.body', { escape: false }));
    //
    return helpContent;
  }

  render() {
    const { _showLoading, _synchronizationConfig } = this.props;
    const { systemMappingId, showLoading, activeKey, entityType, enabled } = this.state;
    const isNew = this._getIsNew();
    const innerShowLoading = isNew ? showLoading : (_showLoading || showLoading);
    const systemId = this.props.params.entityId;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('synchronizationConfigId', _synchronizationConfig ? _synchronizationConfig.id : Domain.SearchParameters.BLANK_UUID);
    const forceSearchMappingAttributes = new Domain.SearchParameters().setFilter('systemId', systemId || Domain.SearchParameters.BLANK_UUID);
    const forceSearchSyncActionWfKey = new Domain.SearchParameters().setFilter('category', syncActionWfKey);
    const synchronizationConfig = isNew ? this.state.synchronizationConfig : _synchronizationConfig;
    const attributeMappingIdFromEntity = synchronizationConfig && synchronizationConfig.systemMapping ? synchronizationConfig.systemMapping : null;
    const forceSearchCorrelationAttribute = new Domain.SearchParameters().setFilter('systemMappingId', systemMappingId || attributeMappingIdFromEntity || Domain.SearchParameters.BLANK_UUID);
    let isSelectedTree = false;
    let specificConfiguration = null;
    const finalEntityType = this._getEntityType(synchronizationConfig, entityType);
    if (finalEntityType) {
      if (finalEntityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.TREE)) {
        isSelectedTree = true;
      }
    }
    if (finalEntityType) {
      if (finalEntityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.CONTRACT)) {
        specificConfiguration = <SyncContractConfig ref="formSpecific" synchronizationConfig={synchronizationConfig} showLoading={innerShowLoading} isNew={isNew} className="panel-body"/>;
      } else if (finalEntityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.CONTRACT_SLICE)) {
        specificConfiguration = <SyncContractConfig ref="formSpecific" synchronizationConfig={synchronizationConfig} showLoading={innerShowLoading} isNew={isNew} className="panel-body"/>;
      } else if (finalEntityType === SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY)) {
        specificConfiguration = <SyncIdentityConfig ref="formSpecific" synchronizationConfig={synchronizationConfig} showLoading={innerShowLoading} isNew={isNew} className="panel-body"/>;
      }
    }
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <Basic.Tabs activeKey={activeKey} onSelect={this._onChangeSelectTabs.bind(this)}>
          <Basic.Tab eventKey={1} title={this.i18n('tabs.basicConfiguration.label')} className="bordered">
            <form onSubmit={this.save.bind(this, false, false, finalEntityType)}>
              <Basic.Panel className="no-border">
                <Basic.AbstractForm
                  ref="form"
                  data={synchronizationConfig}
                  showLoading={innerShowLoading}
                  className="panel-body"
                  readOnly={ !Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                  <Basic.Checkbox
                    ref="enabled"
                    label={this.i18n('acc:entity.SynchronizationConfig.enabled')}
                    onChange={ this._onChangeEnabled.bind(this) }/>
                  <Basic.Checkbox
                    ref="reconciliation"
                    readOnly={isSelectedTree}
                    label={this.i18n('acc:entity.SynchronizationConfig.reconciliation.label')}
                    helpBlock={this.i18n('acc:entity.SynchronizationConfig.reconciliation.help')}/>
                  <Basic.LabelWrapper
                    hidden={!isSelectedTree}
                    label=" ">
                    <Basic.Alert
                       key="treeInfo"
                       level="warning"
                       icon="exclamation-sign"
                       className="no-margin"
                       text={this.i18n('treeInfo')}/>
                  </Basic.LabelWrapper>
                  <Basic.TextField
                    ref="name"
                    label={this.i18n('acc:entity.SynchronizationConfig.name')}
                    required/>
                  <Basic.SelectBox
                    ref="systemMapping"
                    manager={systemMappingManager}
                    forceSearchParameters={forceSearchMappingAttributes}
                    onChange={this._onChangeSystemMapping.bind(this)}
                    label={this.i18n('acc:entity.SynchronizationConfig.systemMapping')}
                    required/>
                  <Basic.ScriptArea
                    ref="rootsFilterScript"
                    height="20em"
                    hidden={!isSelectedTree}
                    helpBlock={this.i18n('acc:entity.SynchronizationConfig.rootsFilterScript.help')}
                    label={this.i18n('acc:entity.SynchronizationConfig.rootsFilterScript.label')}/>
                  <Basic.SelectBox
                    ref="correlationAttribute"
                    manager={systemAttributeMappingManager}
                    forceSearchParameters={forceSearchCorrelationAttribute}
                    label={this.i18n('acc:entity.SynchronizationConfig.correlationAttribute')}
                    required/>
                  <Basic.TextField
                    ref="token"
                    label={this.i18n('acc:entity.SynchronizationConfig.token')}/>
                  <Basic.TextArea
                    ref="description"
                    label={this.i18n('acc:entity.SynchronizationConfig.description')}/>
                  <Basic.LabelWrapper label=" ">
                    <Basic.Alert
                       key="situationActionsAndWfInfo"
                       icon="exclamation-sign"
                       className="no-margin"
                       text={this.i18n('situationActionsAndWf')}/>
                  </Basic.LabelWrapper>
                  <Basic.ContentHeader text={ this.i18n('acc:entity.SynchronizationConfig.linkedAction') } className="marginable"/>
                  <Basic.EnumSelectBox
                    className=""
                    ref="linkedAction"
                    enum={SynchronizationLinkedActionTypeEnum}
                    label={this.i18n('situationAction')}
                    required/>
                  <Basic.SelectBox
                    ref="linkedActionWfKey"
                    label={this.i18n('situationActionWf')}
                    forceSearchParameters={forceSearchSyncActionWfKey}
                    multiSelect={false}
                    manager={workflowProcessDefinitionManager}/>

                  <Basic.ContentHeader text={ this.i18n('acc:entity.SynchronizationConfig.unlinkedAction') } className="marginable"/>
                  <Basic.EnumSelectBox
                    ref="unlinkedAction"
                    enum={SynchronizationUnlinkedActionTypeEnum}
                    label={this.i18n('situationAction')}
                    required/>
                  <Basic.SelectBox
                    ref="unlinkedActionWfKey"
                    label={this.i18n('situationActionWf')}
                    forceSearchParameters={forceSearchSyncActionWfKey}
                    multiSelect={false}
                    manager={workflowProcessDefinitionManager}/>

                  <Basic.ContentHeader text={ this.i18n('acc:entity.SynchronizationConfig.missingEntityAction') } className="marginable"/>
                  <Basic.EnumSelectBox
                    ref="missingEntityAction"
                    enum={SynchronizationMissingEntityActionTypeEnum}
                    label={this.i18n('situationAction')}
                    required/>
                  <Basic.SelectBox
                    ref="missingEntityActionWfKey"
                    label={this.i18n('situationActionWf')}
                    forceSearchParameters={forceSearchSyncActionWfKey}
                    multiSelect={false}
                    manager={workflowProcessDefinitionManager}/>

                  <Basic.ContentHeader text={ this.i18n('acc:entity.SynchronizationConfig.missingAccountAction') } className="marginable"/>
                  <Basic.EnumSelectBox
                    ref="missingAccountAction"
                    enum={ReconciliationMissingAccountActionTypeEnum}
                    label={this.i18n('situationAction')}
                    required/>
                  <Basic.SelectBox
                    ref="missingAccountActionWfKey"
                    label={this.i18n('situationActionWf')}
                    forceSearchParameters={forceSearchSyncActionWfKey}
                    multiSelect={false}
                    manager={workflowProcessDefinitionManager}/>
                </Basic.AbstractForm>
                <Basic.PanelFooter>
                  <Basic.Button type="button" level="link"
                    onClick={this.context.router.goBack}
                    showLoading={innerShowLoading}>
                    {this.i18n('button.back')}
                  </Basic.Button>
                  <Basic.SplitButton
                    level="success"
                    title={this.i18n('button.saveAndContinue')}
                    onClick={this.save.bind(this, false, false, finalEntityType)}
                    showLoading={innerShowLoading}
                    type="submit"
                    showLoadingIcon
                    showLoadingText={this.i18n('button.saving')}
                    rendered={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')}
                    pullRight
                    dropup>
                    <Basic.MenuItem
                      eventKey="1"
                      rendered={ (enabled === null || enabled === true) && synchronizationConfig && synchronizationConfig.enabled && Managers.SecurityManager.hasAuthority('SYNCHRONIZATION_CREATE') }
                      onClick={this.save.bind(this, true, false, finalEntityType)}>
                      {this.i18n('button.saveAndStartSynchronization')}
                    </Basic.MenuItem>
                    <Basic.MenuItem
                      eventKey="2"
                      onClick={this.save.bind(this, false, true, finalEntityType)}>
                      {this.i18n('button.saveAndClose')}
                    </Basic.MenuItem>
                  </Basic.SplitButton>
                </Basic.PanelFooter>
              </Basic.Panel>
            </form>
          </Basic.Tab>
          <Basic.Tab rendered={specificConfiguration} eventKey={2} title={this.i18n('tabs.specificConfiguration.label')} className="bordered">
            <form onSubmit={this.save.bind(this)}>
              <Basic.Panel className="no-border">
                {specificConfiguration}
                <Basic.PanelFooter>
                  <Basic.Button type="button" level="link"
                    onClick={this.context.router.goBack}
                    showLoading={innerShowLoading}>
                    {this.i18n('button.back')}
                  </Basic.Button>
                  <Basic.SplitButton
                    level="success"
                    title={this.i18n('button.saveAndContinue')}
                    onClick={this.save.bind(this, false, false, finalEntityType)}
                    showLoading={innerShowLoading}
                    type="submit"
                    showLoadingIcon
                    showLoadingText={this.i18n('button.saving')}
                    rendered={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')}
                    pullRight
                    dropup>
                    <Basic.MenuItem
                      eventKey="1"
                      rendered={ (enabled === null || enabled === true) && synchronizationConfig && synchronizationConfig.enabled && Managers.SecurityManager.hasAuthority('SYNCHRONIZATION_CREATE') }
                      onClick={this.save.bind(this, true, false, finalEntityType)}>
                      {this.i18n('button.saveAndStartSynchronization')}
                    </Basic.MenuItem>
                    <Basic.MenuItem
                      eventKey="2"
                      onClick={this.save.bind(this, false, true, finalEntityType)}>
                      {this.i18n('button.saveAndClose')}
                    </Basic.MenuItem>
                  </Basic.SplitButton>
                </Basic.PanelFooter>
              </Basic.Panel>
            </form>
          </Basic.Tab>
          <Basic.Tab eventKey={3} title={this.i18n('tabs.filterConfiguration.label')} className="bordered">
            <form onSubmit={this.save.bind(this)}>
              <Basic.Panel className="no-border">
                <Basic.AbstractForm ref="formFilter" data={synchronizationConfig} showLoading={innerShowLoading} className="panel-body">
                  <Basic.Checkbox
                    ref="customFilter"
                    label={this.i18n('acc:entity.SynchronizationConfig.customFilter.label')}
                    helpBlock={this.i18n('acc:entity.SynchronizationConfig.customFilter.help')}/>
                  <Basic.LabelWrapper label=" ">
                    <Basic.Alert
                       key="customFilterInfo"
                       icon="exclamation-sign"
                       className="no-margin"
                       text={this.i18n('customFilterInfo')}/>
                  </Basic.LabelWrapper>
                  <Basic.SelectBox
                    ref="filterAttribute"
                    manager={systemAttributeMappingManager}
                    forceSearchParameters={forceSearchCorrelationAttribute}
                    label={this.i18n('acc:entity.SynchronizationConfig.filterAttribute.label')}
                    onChange={this._onChangeFilterAttribute.bind(this)}
                    tooltip={this.i18n('acc:entity.SynchronizationConfig.filterAttribute.help')}/>
                  <Basic.EnumSelectBox
                    ref="filterOperation"
                    enum={IcFilterOperationTypeEnum}
                    label={this.i18n('acc:entity.SynchronizationConfig.filterOperation')}
                    required/>
                  <Basic.SelectBox
                    ref="tokenAttribute"
                    manager={systemAttributeMappingManager}
                    forceSearchParameters={forceSearchCorrelationAttribute}
                    label={this.i18n('acc:entity.SynchronizationConfig.tokenAttribute.label')}
                    helpBlock={this.i18n('acc:entity.SynchronizationConfig.tokenAttribute.help')}/>
                  <Basic.ScriptArea
                    ref="customFilterScript"
                    height="20em"
                    helpBlock={this.i18n('acc:entity.SynchronizationConfig.customFilterScript.help')}
                    label={this.i18n('acc:entity.SynchronizationConfig.customFilterScript.label')}
                    help={ this.getHelp() }/>
                </Basic.AbstractForm>
                <Basic.PanelFooter>
                  <Basic.Button type="button" level="link"
                    onClick={this.context.router.goBack}
                    showLoading={innerShowLoading}>
                    {this.i18n('button.back')}
                  </Basic.Button>
                  <Basic.SplitButton
                    level="success"
                    title={this.i18n('button.saveAndContinue')}
                    onClick={this.save.bind(this, false, false, finalEntityType)}
                    showLoading={innerShowLoading}
                    type="submit"
                    showLoadingIcon
                    showLoadingText={this.i18n('button.saving')}
                    rendered={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')}
                    pullRight
                    dropup>
                    <Basic.MenuItem
                      eventKey="1"
                      rendered={ (enabled === null || enabled === true) && synchronizationConfig && synchronizationConfig.enabled && Managers.SecurityManager.hasAuthority('SYNCHRONIZATION_CREATE') }
                      onClick={this.save.bind(this, true, false, finalEntityType)}>
                      {this.i18n('button.saveAndStartSynchronization')}
                    </Basic.MenuItem>
                    <Basic.MenuItem
                      eventKey="2"
                      onClick={this.save.bind(this, false, true, finalEntityType)}>
                      {this.i18n('button.saveAndClose')}
                    </Basic.MenuItem>
                  </Basic.SplitButton>
                </Basic.PanelFooter>
              </Basic.Panel>
            </form>
          </Basic.Tab>
          <Basic.Tab eventKey={4} title={this.i18n('tabs.logs.label')}>
            <Basic.ContentHeader rendered={synchronizationConfig} style={{ marginBottom: 0, paddingLeft: 15 }}>
              <Basic.Icon value="transfer"/>
              {' '}
              <span dangerouslySetInnerHTML={{ __html: this.i18n('synchronizationLogsHeader') }}/>
            </Basic.ContentHeader>

            <Advanced.Table
              ref="table"
              uiKey={uiKeyLogs}
              manager={synchronizationLogManager}
              showLoading={innerShowLoading}
              forceSearchParameters={forceSearchParameters}
              showRowSelection={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])}
              rowClass={({rowIndex, data}) => { return data[rowIndex].containsError ? 'danger' : ''; }}
              actions={
                Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])
                ?
                [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
                :
                null
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
                <Advanced.Column
                  property="syncActionLogs"
                  header={this.i18n('acc:entity.SynchronizationLog.results')}
                  cell={
                    ({ rowIndex, data }) => {
                      return this._generateResultCell(rowIndex, data);
                    }
                  }
                  />
                <Advanced.Column property="started" face="datetime" header={this.i18n('acc:entity.SynchronizationLog.started')} sort/>
                <Advanced.Column property="ended" face="datetime" header={this.i18n('acc:entity.SynchronizationLog.ended')} sort/>
                <Advanced.Column
                  property="statistic"
                  header={this.i18n('acc:entity.SynchronizationLog.statistic.label')}
                  cell={
                    ({ rowIndex, data }) => {
                      return this._generateStatisticCell(rowIndex, data);
                    }
                  }
                  />
            </Advanced.Table>
            </Basic.Tab>
          </Basic.Tabs>
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
  return {
    _synchronizationConfig: entity,
    _showLoading: entity ? false : true,
  };
}

export default connect(select)(SystemSynchronizationConfigDetail);
