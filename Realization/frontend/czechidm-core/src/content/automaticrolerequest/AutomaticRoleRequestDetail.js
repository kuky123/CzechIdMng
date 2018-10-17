import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import OperationStateEnum from '../../enums/OperationStateEnum';
import { TreeNodeManager, RoleTreeNodeManager, RoleManager, AutomaticRoleRequestManager, AutomaticRoleAttributeRuleRequestManager, AutomaticRoleAttributeRuleManager, AutomaticRoleAttributeManager } from '../../redux';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';
import ConceptRoleRequestOperationEnum from '../../enums/ConceptRoleRequestOperationEnum';
import AutomaticRoleRequestTypeEnum from '../../enums/AutomaticRoleRequestTypeEnum';
import SearchParameters from '../../domain/SearchParameters';
import AutomaticRoleRuleTable from './AutomaticRoleRuleTable';
import RecursionTypeEnum from '../../enums/RecursionTypeEnum';

const uiKey = 'automatic-role-request';
const uiKeyRuleRequests = 'automatic-role-rule-requests';
const uiKeyRules = 'automatic-role-rules';
const uiKeyAttributes = 'concept-role-requests';
const automaticRoleAttributeRuleRequestManager = new AutomaticRoleAttributeRuleRequestManager();
const automaticRoleRequestManager = new AutomaticRoleRequestManager();
const roleManager = new RoleManager();
const automaticRoleAttributeRuleManager = new AutomaticRoleAttributeRuleManager();
const automaticAttributeRoleManager = new AutomaticRoleAttributeManager();
const treeNodeManager = new TreeNodeManager();
const roleTreeNodeManager = new RoleTreeNodeManager();

/**
 * Detail for automatic role request
 *
 * @author Vít Švanda
 */
class AutomaticRoleRequestDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return automaticRoleAttributeRuleRequestManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.automaticRoleRequestDetail';
  }

  componentWillReceiveProps(nextProps) {
    const { _request } = nextProps;
    const entityId = nextProps.entityId ? nextProps.entityId : nextProps.params.entityId;
    const entityIdCurrent = this.props.entityId ? this.props.entityId : this.props.params.entityId;
    if (entityId && entityId !== entityIdCurrent) {
      this._initComponent(nextProps);
    }
    if (_request && _request !== this.props._request) {
      this._initComponentCurrentRoles(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    super.componentDidMount();
    this.selectNavigationItems(['audit', 'automatic-role-requests']);
    //
    this._initComponent(this.props);
    this._initComponentCurrentRoles(this.props);
    if (this.refs.description) {
      this.refs.description.focus();
    }
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const { entityId} = props;
    const _entityId = entityId ? entityId : props.params.entityId;
    if (this._getIsNew(props)) {
      const _automaticRoleId = props.location.query.automaticRoleId;
      this.setState({
        showLoading: false,
        request: {
          role: props.location.query.roleId,
          automaticRole: _automaticRoleId,
          state: RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.CONCEPT),
          operation: _automaticRoleId ?
                    ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE)
                    : ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.ADD),
          requestedByType: 'MANUALLY'
        }}, () => {
        this.save(this, false, false);
      });
    } else {
      this.context.store.dispatch(automaticRoleRequestManager.fetchEntity(_entityId));
      this._reloadRuleRequests({id: _entityId});
    }
  }

  _initComponentCurrentRoles(props) {
    const { _request, showCurrentRules} = props;
    //
    if (this._getIsNew(props)) {
      const role = props.location.query.automaticRoleId;
      let forceSearchParameters = automaticRoleAttributeRuleManager.getDefaultSearchParameters();
      if (role) {
        forceSearchParameters = forceSearchParameters.setFilter('automaticRoleAttributeId', role);
      }
      if (showCurrentRules) {
        this.context.store.dispatch(automaticRoleAttributeRuleManager.fetchEntities(forceSearchParameters, `${uiKeyRules}-${role}`));
      }
    } else {
      if (_request) {
        let forceSearchParameters = automaticRoleAttributeRuleManager.getDefaultSearchParameters();
        if (_request.automaticRole) {
          forceSearchParameters = forceSearchParameters.setFilter('automaticRoleAttributeId', _request.automaticRole);
        }
        if (showCurrentRules) {
          this.context.store.dispatch(automaticRoleAttributeRuleManager.fetchEntities(forceSearchParameters, `${uiKeyRules}-${_request.automaticRole}`));
        }
      }
    }
  }

  /**
   * Saves give entity
   */
  save(component, startRequest, validate, event) {
    if (event) {
      event.preventDefault();
    }
    if (validate && !this.refs.form.isFormValid()) {
      return;
    }

    this.setState({showLoading: true});
    const formEntity = this.refs.form.getData();

    if (formEntity.id === undefined) {
      this.context.store.dispatch(automaticRoleRequestManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
      }));
    } else {
      if (startRequest) {
        this.context.store.dispatch(automaticRoleRequestManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSaveAndStartRequest.bind(this)));
      } else {
        this.context.store.dispatch(automaticRoleRequestManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
      }
    }
  }

  afterSave(entity, error) {
    this.setState({showLoading: false});
    if (!error) {
      // this.addMessage({ message: this.i18n('save.success') });
      if (this._getIsNew()) {
        this.context.router.replace(`/automatic-role-requests/${entity.id}/detail`);
      }
    } else {
      this.addError(error);
    }
  }

  afterSaveAndStartRequest(entity, error) {
    this.setState({showLoading: false});
    if (!error) {
      this._startRequest(entity.id);
    } else {
      this.addError(error);
    }
  }

  closeDetail() {
    this.refs.form.processEnded();
  }

  _getIsNew(nextProps) {
    if ((nextProps && nextProps.location) || this.props.location) {
      const { query } = nextProps ? nextProps.location : this.props.location;
      return (query) ? query.new : null;
    }
    return false;
  }

  _reloadRuleRequests(_request) {
    let forceSearchParameters = automaticRoleAttributeRuleRequestManager.getDefaultSearchParameters();
    if (_request.id) {
      forceSearchParameters = forceSearchParameters.setFilter('roleRequestId', _request.id);
    } else {
      forceSearchParameters = forceSearchParameters.setFilter('roleRequestId', SearchParameters.BLANK_UUID);
    }
    this.context.store.dispatch(automaticRoleAttributeRuleRequestManager.fetchEntities(forceSearchParameters, `${uiKeyRuleRequests}-${_request.id}`));
  }

  _removeConcept(data, type) {
    const {_request, _roleRuleRequests} = this.props;
    this.setState({showLoadingButtonRemove: true});

    let concept = data;
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.REMOVE)
      || (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE))) {
      for (const conceptRole of _roleRuleRequests) {
        if (conceptRole.rule === data) {
          concept = conceptRole;
        }
      }
    }

    automaticRoleAttributeRuleRequestManager.getService().deleteById(concept.id)
    .then(() => {
      this._reloadRuleRequests(_request);
      this.refs.table.getWrappedInstance().reload();
      this.setState({showLoadingButtonRemove: false});
    })
    .catch(error => {
      this.addError(error);
      this.setState({showLoadingButtonRemove: false});
    });
  }

  _updateConcept(data, type) {
    const {_request} = this.props;
    let concept;
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE)) {
      concept = {
        'id': data.id,
        'operation': type,
        'request': _request.id,
        'rule': data.rule,
        'formAttribute': data.formAttribute,
        'attributeName': data.attributeName,
        'type': data.type,
        'value': data.value,
        'comparison': data.comparison
      };
    }
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.ADD)) {
      concept = {
        'id': data.id,
        'operation': type,
        'request': _request.id,
        'rule': data.rule,
        'formAttribute': data.formAttribute,
        'attributeName': data.attributeName,
        'type': data.type,
        'value': data.value,
        'comparison': data.comparison
      };
    }
    this.context.store.dispatch(automaticRoleAttributeRuleRequestManager.updateEntity(concept, `${uiKeyAttributes}-detail`, (updatedEntity, error) => {
      if (!error) {
        this._reloadRuleRequests(_request);
        this.refs.table.getWrappedInstance().reload();
      } else {
        this.addError(error);
      }
    }));
  }

  _createConcept(data, type) {
    const {_request} = this.props;
    let formAttribute = data.formAttribute;
    if (formAttribute && _.isObject(formAttribute)) {
      formAttribute = formAttribute.id;
    }
    const concept = {
      'operation': type,
      'request': _request.id,
      formAttribute,
      'attributeName': data.attributeName,
      'type': data.type,
      'value': data.value,
      'rule': data.rule,
      'comparison': data.comparison
    };

    automaticRoleAttributeRuleRequestManager.getService().create(concept)
    .then(() => {
      this._reloadRuleRequests(_request);
      this.refs.table.getWrappedInstance().reload();
    })
    .catch(error => {
      this.addError(error);
    });
  }

  _startRequest(idRequest, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      showLoading: true
    });
    const promise = automaticRoleRequestManager.getService().startRequest(idRequest);
    promise.then((json) => {
      this.setState({
        showLoading: false
      });
      this.context.router.goBack();
      if (json.state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.DUPLICATED)) {
        this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.duplicated', { created: moment(json._embedded.duplicatedToRequest.created).format(this.i18n('format.datetime'))}), level: 'warning'});
        return;
      }
      if (json.state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXCEPTION)) {
        this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.exception'), level: 'error' });
        return;
      }
      this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.started') });
    }).catch(ex => {
      this.setState({
        showLoading: false
      });
      this.addError(ex);
      if (this.refs.table) {
        this.refs.table.getWrappedInstance().reload();
      }
    });
    return;
  }

  _renderRoleConceptChangesTable(request, forceSearchParameters, rendered) {
    if (!rendered) {
      return null;
    }
    return (
      <div>
        <Basic.ContentHeader rendered={request}>
          <Basic.Icon value="list"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('conceptHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={request}>
          <Advanced.Table
            ref="table"
            uiKey={uiKeyAttributes}
            manager={automaticRoleAttributeRuleRequestManager}
            forceSearchParameters={forceSearchParameters}
            >
            <Advanced.Column
              property="operation"
              face="enum"
              enumClass={ConceptRoleRequestOperationEnum}
              header={this.i18n('entity.ConceptRoleRequest.operation')}
              sort/>
            <Advanced.Column
              property="attributeName"
              face="text"
              header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.attributeName')}
              sort/>
            <Advanced.Column
              property="formAttribute"
              face="bool"
              rendered={false}
              header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.formAttribute')}
              sort/>
            <Advanced.Column
              property="value"
              face="text"
              header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.value.label')}
              sort/>
            <Advanced.Column
              property="comparison"
              face="text"
              header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.comparison')}
              sort/>
          </Advanced.Table>
        </Basic.Panel>
      </div>
    );
  }

  _getApplicantAndImplementer(request) {
    return (
      <div>
        <Basic.LabelWrapper
          rendered={(request && request.role) ? true : false}
          readOnly
          ref="role"
          label={this.i18n('entity.AutomaticRoleRequest.role')}>
          <Advanced.RoleInfo
            entityIdentifier={request && request.role}
            showLoading={!request}
            face="full"
            showLink/>
        </Basic.LabelWrapper>

        <Basic.LabelWrapper
          rendered={(request && request.creatorId) ? true : false}
          readOnly
          ref="implementer"
          label={this.i18n('entity.AutomaticRoleRequest.implementer')}>
          <Advanced.IdentityInfo
            face="popover"
            entityIdentifier={request && request.creatorId}
            showLoading={!request}/>
        </Basic.LabelWrapper>
      </div>
    );
  }

  _renderRoleConceptTable(request, rendered, isEditable, showLoading, _currentRoleRules, addedConcepts, changedConcepts, removedConcepts, showLoadingButtonRemove) {
    if (!rendered) {
      return null;
    }
    //
    const { _permissions, canExecute } = this.props;
    return (
      <div>
        <Basic.Alert
          level="info"
          title={ this.i18n('rulesIsAnd.title') }
          text={ this.i18n('rulesIsAnd.text') }
          className="no-margin"
        />
        <Basic.ContentHeader>
          <Basic.Icon value="list"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('conceptWithCurrentRulesHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={request}>
          <AutomaticRoleRuleTable
            ref="automaticRoleRuleConceptTable"
            uiKey="automatic-role-rule-concept-table"
            showLoading={showLoading}
            showLoadingButtonRemove={showLoadingButtonRemove}
            className="vertical-scroll"
            readOnly={!isEditable || !automaticRoleRequestManager.canSave(request, _permissions) || !canExecute}
            roleId={request && request.role}
            currentData={_currentRoleRules}
            addedConcepts={addedConcepts}
            changedConcepts={changedConcepts}
            removedConcepts={removedConcepts}
            removeConceptFunc={this._removeConcept.bind(this)}
            createConceptFunc={this._createConcept.bind(this)}
            updateConceptFunc={this._updateConcept.bind(this)}
            />
        </Basic.Panel>
      </div>
    );
  }

  _operationResultComponent(request) {
    if (!request.result || Utils.Entity.isNew(request)) {
      return null;
    }
    return (
      <div>
        <Basic.ContentHeader text={ this.i18n('content.scheduler.all-tasks.detail.result') }/>
        <div style={{ marginBottom: 15 }}>
          <Basic.EnumValue value={ request.result.state } enum={ OperationStateEnum }/>
          {
            (!request.result.code)
            ||
            <span style={{ marginLeft: 15 }}>
              {this.i18n('content.scheduler.all-tasks.detail.resultCode')}: { request.result.code }
            </span>
          }
          <Basic.FlashMessage message={this.getFlashManager().convertFromResultModel(request.result.model)} style={{ marginTop: 15 }}/>
        </div>
        {
          (!request.result.stackTrace)
          ||
          <div>
            <textArea
              rows="10"
              value={ request.result.stackTrace }
              readOnly
              style={{ width: '100%', marginBottom: 15 }}/>
          </div>
        }
      </div>
    );
  }

  _onChangeRequestType(requestType) {
    this.setState({requestType: requestType ? requestType.value : null});
  }

  render() {
    const {
      _showLoading,
      _request,
      _currentRoleRules,
      editableInStates,
      showRequestDetail,
      showCurrentRules,
      _permissions,
      _roleRuleRequests} = this.props;
    //
    const forceSearchParameters = new SearchParameters().setFilter('roleRequestId', _request ? _request.id : SearchParameters.BLANK_UUID);
    const isNew = this._getIsNew();
    const request = isNew ? this.state.request : _request;
    // We want show audit fields only for Admin, but not in concept state.
    const _adminMode = Utils.Permission.hasPermission(_permissions, 'ADMIN');
    const showLoading = !request || _showLoading || this.state.showLoading || this.props.showLoading;
    const isEditable = request && _.includes(editableInStates, request.state);
    const showLoadingButtonRemove = this.state.showLoadingButtonRemove;
    let requestType = request ? request.requestType : null;
    requestType = this.state.requestType ? this.state.requestType : requestType;
    let isAttributeRequest = true;
    if (requestType) {
      isAttributeRequest = AutomaticRoleRequestTypeEnum.findKeyBySymbol(AutomaticRoleRequestTypeEnum.TREE) !== requestType;
    }

    // Use manager for automatic role by type (Attribute / Tree)
    let automaticRoleManager = roleTreeNodeManager;
    if (isAttributeRequest) {
      automaticRoleManager = automaticAttributeRoleManager;
    }
    if (this.state.showLoading || !request) {
      return (<div>
        <Basic.ContentHeader rendered={showRequestDetail}>
          <Basic.Icon value="compressed"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
        <Basic.PanelBody>
          <Basic.Loading show isStatic />
        </Basic.PanelBody>
      </div>);
    }

    const addedConcepts = [];
    const changedConcepts = [];
    const removedConcepts = [];
    if (request && _roleRuleRequests) {
      const concepts = _roleRuleRequests;
      if (request.state !== RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXECUTED)) {
        for (const concept of concepts) {
          if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.ADD)) {
            addedConcepts.push(concept);
          }
          if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE)) {
            changedConcepts.push(concept);
          }
          if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.REMOVE)) {
            removedConcepts.push(concept.rule);
          }
        }
      }
    }

    let isDeleteRequest = false;
    if (request && request.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.REMOVE)) {
      isDeleteRequest = true;
    }

    return (
      <div>
        <form onSubmit={this.save.bind(this, this, false, true)}>
          <Helmet title={this.i18n('title')} />
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Basic.ContentHeader rendered={showRequestDetail}>
            <Basic.Icon value="compressed"/>
            {' '}
            <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
          </Basic.ContentHeader>
          <Basic.Panel rendered={showRequestDetail}>
            <Basic.AbstractForm readOnly={!isEditable} ref="form" data={request} showLoading={showLoading} style={{ padding: '15px 15px 0 15px' }}>
              <Basic.Row>
                <div className="col-lg-6">
                  {this._getApplicantAndImplementer(request)}
                </div>
              </Basic.Row>
              <Basic.EnumSelectBox
                ref="requestType"
                readOnly = {request.automaticRole ? true : false}
                onChange={this._onChangeRequestType.bind(this)}
                required
                enum={AutomaticRoleRequestTypeEnum}
                label={this.i18n('entity.AutomaticRoleRequest.requestType')}/>
              <Basic.TextField
                ref="name"
                required={request.automaticRole ? false : true}
                rendered={request.automaticRole ? false : true}
                label={this.i18n('entity.AutomaticRoleRequest.name')}/>
              <Basic.SelectBox
                ref="automaticRole"
                readOnly
                rendered={request.automaticRole ? true : false}
                manager={automaticRoleManager}
                label={this.i18n('entity.AutomaticRoleRequest.automaticAttributeRole')}/>
              <Basic.EnumLabel
                ref="operation"
                enum={ConceptRoleRequestOperationEnum}
                label={this.i18n('entity.AutomaticRoleRequest.operation')}/>
              <Basic.EnumLabel
                ref="state"
                enum={RoleRequestStateEnum}
                label={this.i18n('entity.AutomaticRoleRequest.state')}/>
              <Basic.SelectBox
                ref="treeNode"
                manager={treeNodeManager}
                label={this.i18n('entity.RoleTreeNode.treeNode')}
                rendered={!isAttributeRequest}
                required={!isAttributeRequest && (request.automaticRole ? false : true)}/>
              <Basic.EnumSelectBox
                ref="recursionType"
                enum={RecursionTypeEnum}
                label={this.i18n('entity.RoleTreeNode.recursionType')}
                rendered={!isAttributeRequest}
                required={!isAttributeRequest && (request.automaticRole ? false : true)}/>
              <Basic.Checkbox
                ref="executeImmediately"
                hidden={!_adminMode}
                label={this.i18n('entity.AutomaticRoleRequest.executeImmediately')}/>
              <Basic.TextArea
                ref="description"
                rows="3"
                placeholder={this.i18n('entity.AutomaticRoleRequest.description.placeholder')}
                label={this.i18n('entity.AutomaticRoleRequest.description.label')}/>
            </Basic.AbstractForm>
            <div style={{ padding: '15px 15px 0 15px' }}>
              {
                this._renderRoleConceptTable(request, showCurrentRules && isAttributeRequest && !isDeleteRequest, isEditable, showLoading, _currentRoleRules, addedConcepts, changedConcepts, removedConcepts, showLoadingButtonRemove)
              }
              {
                this._renderRoleConceptChangesTable(request, forceSearchParameters, isAttributeRequest && !isDeleteRequest)
              }
              <Basic.AbstractForm
                readOnly
                ref="form-wf"
                rendered={request.wfProcessId ? true : false}
                data={request}
                showLoading={showLoading}>
                <Basic.LabelWrapper
                  readOnly
                  hidden={!_adminMode}
                  ref="wfProcessId"
                  label={this.i18n('entity.AutomaticRoleRequest.wfProcessId')}>
                  <Advanced.WorkflowProcessInfo
                    entityIdentifier={request && request.wfProcessId}
                    showLoading={!request}
                    face="full"
                    showLink/>
                </Basic.LabelWrapper>
                <Basic.TextField
                  ref="currentActivity"
                  hidden
                  readOnly
                  label={this.i18n('entity.AutomaticRoleRequest.currentActivity')}/>
                <Basic.TextField
                  ref="candicateUsers"
                  hidden
                  readOnly
                  label={this.i18n('entity.AutomaticRoleRequest.candicateUsers')}/>
              </Basic.AbstractForm>
              { // Renders result of operation (includes stack trace)
                this._operationResultComponent(request)
              }
            </div>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
              <Basic.Button
                onClick={this.save.bind(this, this, false, true)}
                disabled={!isEditable}
                rendered={_adminMode}
                level="success"
                type="submit"
                showLoading={showLoading}>
                {this.i18n('button.save')}
              </Basic.Button>
                {' '}
              <Basic.Button
                level="success"
                disabled={!isEditable}
                showLoading={showLoading}
                onClick={this.save.bind(this, this, true, true)}
                rendered={ request && automaticRoleRequestManager.canSave(request, _permissions)}
                titlePlacement="bottom"
                title={this.i18n('button.createRequest.tooltip')}>
                <Basic.Icon type="fa" icon="object-group"/>
                {' '}
                { this.i18n('button.createRequest.label') }
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
      </div>
    );
  }
}

AutomaticRoleRequestDetail.propTypes = {
  _showLoading: PropTypes.bool,
  editableInStates: PropTypes.arrayOf(PropTypes.string),
  showRequestDetail: PropTypes.bool,
  showCurrentRules: PropTypes.bool,
  canExecute: PropTypes.bool,
};
AutomaticRoleRequestDetail.defaultProps = {
  editableInStates: ['CONCEPT', 'EXCEPTION', 'DUPLICATED'],
  showRequestDetail: true,
  showCurrentRules: true,
  canExecute: true
};

function select(state, component) {
  const entityId = component.entityId ? component.entityId : component.params.entityId;
  const entity = automaticRoleRequestManager.getEntity(state, entityId);
  let _currentRoleRules = null;
  let _roleRuleRequests = null;
  if (entityId) {
    _roleRuleRequests = automaticRoleAttributeRuleRequestManager.getEntities(state, `${uiKeyRuleRequests}-${entityId}`);
  }
  const roleAutoFromUrl = component.location ? component.location.query.automaticRoleId : null;
  const automaticRoleId = entity ? entity.automaticRole : roleAutoFromUrl;
  if (automaticRoleId) {
    _currentRoleRules = automaticRoleAttributeRuleManager.getEntities(state, `${uiKeyRules}-${automaticRoleId}`);
  }
  const roleFromUrl = component.location ? component.location.query.roleId : null;
  const roleId = entity ? entity.role : roleFromUrl;

  if (entity && entity._embedded && entity._embedded.wfProcessId) {
    entity.currentActivity = entity._embedded.wfProcessId.name;
    entity.candicateUsers = entity._embedded.wfProcessId.candicateUsers;
  }
  return {
    _request: entity,
    _showLoading: entity ? false : true,
    _currentRoleRules,
    _roleRuleRequests,
    _permissions: automaticRoleRequestManager.getPermissions(state, null, entity),
    _rolePermissions: roleManager.getPermissions(state, null, roleId)
  };
}

export default connect(select)(AutomaticRoleRequestDetail);
