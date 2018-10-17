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
import { RoleRequestManager, ConceptRoleRequestManager, IdentityManager, IdentityRoleManager } from '../../redux';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';
import ConceptRoleRequestOperationEnum from '../../enums/ConceptRoleRequestOperationEnum';
import SearchParameters from '../../domain/SearchParameters';
import RoleConceptTable from './RoleConceptTable';

const uiKey = 'role-request';
const uiKeyAttributes = 'concept-role-requests';
const conceptRoleRequestManager = new ConceptRoleRequestManager();
const roleRequestManager = new RoleRequestManager();
const identityManager = new IdentityManager();
const identityRoleManager = new IdentityRoleManager();

/**
 * Detail of the role request
 * @author Vít Švanda
 */
class RoleRequestDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return conceptRoleRequestManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.roleRequestDetail';
  }

  getNavigationKey() {
    return 'role-requests';
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
      this.setState({
        showLoading: false,
        request: {
          applicant: props.location.query.applicantId,
          state: RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.CONCEPT),
          requestedByType: 'MANUALLY'
        }}, () => {
        this.save(this, false);
      });
    } else {
      this.context.store.dispatch(roleRequestManager.fetchEntity(_entityId));
    }
  }

  _initComponentCurrentRoles(props) {
    const { _request } = props;
    //
    if (this._getIsNew(props)) {
      const applicant = props.location.query.applicantId;
      this.context.store.dispatch(identityRoleManager.fetchRoles(applicant, `${uiKey}-${applicant}`, () => {} ));
    } else {
      if (_request) {
        this.context.store.dispatch(identityRoleManager.fetchRoles(_request.applicant, `${uiKey}-${_request.applicant}`, () => {}));
      }
    }
  }

  /**
   * Saves give entity
   */
  save(startRequest, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({showLoading: true});
    const formEntity = this.refs.form.getData();

    if (formEntity.id === undefined) {
      this.context.store.dispatch(roleRequestManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
      }));
    } else {
      if (startRequest) {
        this.context.store.dispatch(roleRequestManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSaveAndStartRequest.bind(this)));
      } else {
        this.context.store.dispatch(roleRequestManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
      }
    }
  }

  afterSave(entity, error) {
    this.setState({showLoading: false});
    if (!error) {
      // this.addMessage({ message: this.i18n('save.success') });
      if (this._getIsNew()) {
        this.context.router.replace(`/role-requests/${entity.id}/detail`);
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

  _removeConcept(data, type) {
    const {_request} = this.props;
    this.setState({showLoadingButtonRemove: true});

    let concept = data;
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.REMOVE)
      || (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE))) {
      for (const conceptRole of _request.conceptRoles) {
        if (conceptRole.identityRole === data) {
          concept = conceptRole;
        }
      }
    }

    conceptRoleRequestManager.getService().deleteById(concept.id)
    .then(() => {
      for (const conceptRole of _request.conceptRoles) {
        if (conceptRole.id === concept.id) {
          _request.conceptRoles.splice(_request.conceptRoles.indexOf(conceptRole), 1);
        }
      }
      this.refs.table.getWrappedInstance().reload();
      this.setState({showLoadingButtonRemove: false});
    })
    .catch(error => {
      this.addError(error);
      this.setState({showLoadingButtonRemove: false});
    });

    // this.context.store.dispatch(conceptRoleRequestManager.deleteEntity(concept, `${uiKeyAttributes}-deleteConcept-${_request.applicant}`, (deletedEntity, error) => {
    //   if (!error) {
    //     for (const conceptRole of _request.conceptRoles) {
    //       if (conceptRole.id === concept.id) {
    //         _request.conceptRoles.splice(_request.conceptRoles.indexOf(conceptRole), 1);
    //       }
    //     }
    //     this.refs.table.getWrappedInstance().reload();
    //   } else {
    //     this.addError(error);
    //   }
    // }));
  }

  _updateConcept(data, type) {
    const {_request} = this.props;
    // this.setState({showLoading: true});

    let concept;
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE)) {
      concept = {
        'id': data.id,
        'operation': type,
        'roleRequest': _request.id,
        'identityContract': data.identityContract,
        'role': data.role,
        'identityRole': data.identityRole,
        'validFrom': data.validFrom,
        'validTill': data.validTill,
      };
    }
    if (type === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.ADD)) {
      concept = {
        'id': data.id,
        'operation': type,
        'roleRequest': _request.id,
        'identityContract': data.identityContract.id,
        'role': data.role,
        'identityRole': data.identityRole,
        'validFrom': data.validFrom,
        'validTill': data.validTill,
      };
    }
    this.context.store.dispatch(conceptRoleRequestManager.updateEntity(concept, `${uiKeyAttributes}-detail`, (updatedEntity, error) => {
      if (!error) {
        for (let i = 0; i < _request.conceptRoles.length; i++) {
          if (_request.conceptRoles[i].id === updatedEntity.id) {
            _request.conceptRoles[i] = updatedEntity;
          }
        }
        this.refs.table.getWrappedInstance().reload();
        // this.setState({showLoading: false});
      } else {
        // this.setState({showLoading: false});
        this.addError(error);
      }
    }));
  }

  _createConcept(data, type) {
    const {_request} = this.props;
    // this.setState({showLoading: true});
    const concept = {
      'operation': type,
      'roleRequest': _request.id,
      'identityContract': data.identityContract.id,
      'role': data._embedded.role.id,
      'identityRole': data.id,
      'validFrom': data.validFrom,
      'validTill': data.validTill,
    };

    conceptRoleRequestManager.getService().create(concept)
    .then(json => {
      // this.setState({showLoading: false});
      _request.conceptRoles.push(json);
      this.refs.table.getWrappedInstance().reload();
    })
    .catch(error => {
      // this.setState({showLoading: false});
      this.addError(error);
    });

    // this.context.store.dispatch(conceptRoleRequestManager.createEntity(concept, `${uiKeyAttributes}-createConcept-${_request.applicant}`, (createdEntity, error) => {
    //   if (!error) {
    //     _request.conceptRoles.push(createdEntity);
    //     this.refs.table.getWrappedInstance().reload();
    //   } else {
    //     this.addError(error);
    //   }
    // }));
  }

  _startRequest(idRequest, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      showLoading: true
    });
    const promise = roleRequestManager.getService().startRequest(idRequest);
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

  _getWfProcessCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.wfProcessId) {
      return '';
    }
    return (
      <Advanced.WorkflowProcessInfo entityIdentifier={entity.wfProcessId}/>
    );
  }

  _renderRoleConceptChangesTable(request, forceSearchParameters, rendered) {
    if (!rendered) {
      return null;
    }
    return (
      <div>
        <Basic.ContentHeader rendered={ request !== null }>
          <Basic.Icon value="list"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('conceptHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={ request !== null }>
          <Advanced.Table
            ref="table"
            uiKey={uiKeyAttributes}
            manager={conceptRoleRequestManager}
            forceSearchParameters={forceSearchParameters}>
            <Advanced.Column
              header={this.i18n('entity.ConceptRoleRequest.role')}
              cell={
                /* eslint-disable react/no-multi-comp */
                ({ rowIndex, data }) => {
                  const role = data[rowIndex]._embedded.role;
                  if (!role) {
                    return '';
                  }
                  return (
                    <Advanced.EntityInfo
                      entityType="role"
                      entityIdentifier={ role.id }
                      entity={ role }
                      face="popover" />
                  );
                }
              }/>
            <Advanced.Column
              property="identityContract"
              header={this.i18n('entity.ConceptRoleRequest.identityContract')}
              cell={
                ({rowIndex, data}) => {
                  const contract = data[rowIndex].identityContract;
                  if (!contract) {
                    return '';
                  }
                  return (
                    <Advanced.IdentityContractInfo
                      entityIdentifier={ contract }
                      entity={ data[rowIndex]._embedded.identityContract }
                      showIdentity={ false }
                      face="popover" />
                  );
                }
              }
              />
            <Advanced.Column property="operation" face="enum" enumClass={ConceptRoleRequestOperationEnum} header={this.i18n('entity.ConceptRoleRequest.operation')} sort/>
            <Advanced.Column property="state" face="enum" enumClass={RoleRequestStateEnum} header={this.i18n('entity.ConceptRoleRequest.state')} sort/>
            <Advanced.Column property="validFrom" face="date" header={this.i18n('entity.ConceptRoleRequest.validFrom')} sort/>
            <Advanced.Column property="validTill" face="date" header={this.i18n('entity.ConceptRoleRequest.validTill')} sort/>
            <Advanced.Column property="wfProcessId" cell={this._getWfProcessCell} header={this.i18n('entity.ConceptRoleRequest.wfProcessId')} sort/>
          </Advanced.Table>
        </Basic.Panel>
      </div>
    );
  }

  _getApplicantAndImplementer(request) {
    return (
      <div>
        <Basic.LabelWrapper
          rendered={ request !== null && request.applicant !== null }
          readOnly
          ref="applicant"
          label={this.i18n('entity.RoleRequest.applicant')}>
          <Advanced.IdentityInfo
            username={request && request.applicant}
            showLoading={!request}/>
        </Basic.LabelWrapper>

        <Basic.LabelWrapper
          rendered={ request !== null && request.creatorId !== null }
          readOnly
          ref="implementer"
          label={this.i18n('entity.RoleRequest.implementer')}>
          <Advanced.IdentityInfo
            face="popover"
            entityIdentifier={request && request.creatorId}
            showLoading={!request}/>
        </Basic.LabelWrapper>
      </div>
    );
  }

  _renderRoleConceptTable(request, rendered, isEditable, showLoading, _currentIdentityRoles, addedIdentityRoles, changedIdentityRoles, removedIdentityRoles, showLoadingButtonRemove) {
    if (!rendered) {
      return null;
    }
    //
    const { _permissions, canExecute } = this.props;
    return (
      <div>
        <Basic.ContentHeader>
          <Basic.Icon value="list"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('conceptWithCurrentRoleHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={ request !== null && _currentIdentityRoles !== null }>
          <RoleConceptTable
            ref="identityRoleConceptTable"
            uiKey="identity-role-concept-table"
            showLoading={showLoading}
            showLoadingButtonRemove={showLoadingButtonRemove}
            className="vertical-scroll"
            readOnly={!isEditable || !roleRequestManager.canSave(request, _permissions) || !canExecute}
            identityUsername={request && request.applicant}
            identityRoles={_currentIdentityRoles}
            addedIdentityRoles={addedIdentityRoles}
            changedIdentityRoles={changedIdentityRoles}
            removedIdentityRoles={removedIdentityRoles}
            removeConceptFunc={this._removeConcept.bind(this)}
            createConceptFunc={this._createConcept.bind(this)}
            updateConceptFunc={this._updateConcept.bind(this)}
            />
        </Basic.Panel>
      </div>
    );
  }

  render() {
    const {
      _showLoading,
      _request,
      _currentIdentityRoles,
      editableInStates,
      showRequestDetail,
      _permissions,
      _identityPermissions } = this.props;
    //
    const forceSearchParameters = new SearchParameters().setFilter('roleRequestId', _request ? _request.id : SearchParameters.BLANK_UUID);
    const isNew = this._getIsNew();
    const request = isNew ? this.state.request : _request;
    // We want show audit fields only for Admin, but not in concept state.
    const _adminMode = Utils.Permission.hasPermission(_permissions, 'ADMIN') && request.state !== RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.CONCEPT);
    const showLoading = !request || _showLoading || this.state.showLoading;
    const isEditable = request && _.includes(editableInStates, request.state);
    const showLoadingButtonRemove = this.state.showLoadingButtonRemove;

    if (this.state.showLoading || !request) {
      return (<div>
        <Basic.ContentHeader rendered={ showRequestDetail }>
          <Basic.Icon value="compressed"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
        <Basic.PanelBody>
          <Basic.Loading show isStatic style={{marginTop: '300px', marginBottom: '300px'}} />
        </Basic.PanelBody>
      </div>);
    }

    const addedIdentityRoles = [];
    const changedIdentityRoles = [];
    const removedIdentityRoles = [];
    if (request && request.conceptRoles) {
      const conceptRoles = request.conceptRoles;
      for (const concept of conceptRoles) {
        if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.ADD)
          && concept.state !== RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXECUTED)) {
          addedIdentityRoles.push(concept);
        }
        if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.UPDATE)
          && concept.state !== RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXECUTED)) {
          changedIdentityRoles.push(concept);
        }
        if (concept.operation === ConceptRoleRequestOperationEnum.findKeyBySymbol(ConceptRoleRequestOperationEnum.REMOVE)
          && concept.state !== RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXECUTED)) {
          removedIdentityRoles.push(concept.identityRole);
        }
      }
    }

    return (
      <div>
        <form onSubmit={this.save.bind(this, false)}>
          <Helmet title={this.i18n('title')} />
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Basic.ContentHeader rendered={ showRequestDetail }>
            <Basic.Icon value="compressed"/>
            {' '}
            <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
          </Basic.ContentHeader>
          <Basic.Panel rendered={ showRequestDetail }>
            <Basic.AbstractForm readOnly={!isEditable} ref="form" data={request} showLoading={showLoading} style={{ padding: '15px 15px 0 15px' }}>
              <Basic.Row>
                <div className="col-lg-6">
                  {this._getApplicantAndImplementer(request)}
                </div>
              </Basic.Row>
              <Basic.EnumLabel
                ref="state"
                enum={RoleRequestStateEnum}
                label={this.i18n('entity.RoleRequest.state')}/>
              <Basic.Checkbox
                ref="executeImmediately"
                hidden={!_adminMode}
                label={this.i18n('entity.RoleRequest.executeImmediately')}/>
              <Basic.TextField
                ref="wfProcessId"
                hidden={!_adminMode}
                readOnly
                label={this.i18n('entity.RoleRequest.wfProcessId')}/>
              <Basic.TextField
                ref="currentActivity"
                hidden={!_adminMode}
                readOnly
                label={this.i18n('entity.RoleRequest.currentActivity')}/>
              <Basic.TextField
                ref="candicateUsers"
                hidden={!_adminMode}
                readOnly
                label={this.i18n('entity.RoleRequest.candicateUsers')}/>
              <Basic.TextArea
                ref="log"
                rows={ 8 }
                hidden={!_adminMode}
                readOnly
                label={this.i18n('entity.RoleRequest.log')}/>
              <Basic.TextArea
                ref="originalRequest"
                rows={ 5 }
                hidden={!_adminMode}
                readOnly
                label={this.i18n('entity.RoleRequest.originalRequest')}/>
              <Basic.TextArea
                ref="description"
                rows={ 3 }
                placeholder={this.i18n('entity.RoleRequest.description.placeholder')}
                label={this.i18n('entity.RoleRequest.description.label')}/>
            </Basic.AbstractForm>
            <div style={{ padding: '15px 15px 0 15px' }}>
              {
                this._renderRoleConceptTable(request, true, isEditable, showLoading, _currentIdentityRoles, addedIdentityRoles, changedIdentityRoles, removedIdentityRoles, showLoadingButtonRemove)
              }
              {
                this._renderRoleConceptChangesTable(request, forceSearchParameters, true)
              }
            </div>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
              <Basic.Button
                onClick={this.save.bind(this, false)}
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
                onClick={this.save.bind(this, true)}
                rendered={ request && roleRequestManager.canSave(request, _permissions)}
                titlePlacement="bottom"
                title={this.i18n('button.createRequest.tooltip')}>
                <Basic.Icon type="fa" icon="object-group"/>
                {' '}
                { this.i18n('button.createRequest.label') }
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
        {
          this._renderRoleConceptChangesTable(request, forceSearchParameters, !showRequestDetail)
        }
        {
          !identityManager.canRead({}, _identityPermissions)
          ||
          this._renderRoleConceptTable(request, !showRequestDetail,
            isEditable, showLoading, _currentIdentityRoles, addedIdentityRoles,
            changedIdentityRoles, removedIdentityRoles)
        }
      </div>
    );
  }
}

RoleRequestDetail.propTypes = {
  _showLoading: PropTypes.bool,
  editableInStates: PropTypes.arrayOf(PropTypes.string),
  showRequestDetail: PropTypes.bool,
  canExecute: PropTypes.bool,
};
RoleRequestDetail.defaultProps = {
  editableInStates: ['CONCEPT', 'EXCEPTION', 'DUPLICATED'],
  showRequestDetail: true,
  canExecute: true
};

function select(state, component) {
  const entityId = component.entityId ? component.entityId : component.params.entityId;
  const entity = roleRequestManager.getEntity(state, entityId);
  let _currentIdentityRoles = null;
  const applicantFromUrl = component.location ? component.location.query.applicantId : null;
  const identityId = entity ? entity.applicant : applicantFromUrl;
  if (identityId) {
    _currentIdentityRoles = identityRoleManager.getEntities(state, `${uiKey}-${identityId}`);
  }
  if (entity && entity._embedded && entity._embedded.wfProcessId) {
    entity.currentActivity = entity._embedded.wfProcessId.name;
    entity.candicateUsers = entity._embedded.wfProcessId.candicateUsers;
  }
  return {
    _request: entity,
    _showLoading: entity ? false : true,
    _currentIdentityRoles,
    _permissions: roleRequestManager.getPermissions(state, null, entity),
    _identityPermissions: identityManager.getPermissions(state, null, identityId)
  };
}

export default connect(select)(RoleRequestDetail);
