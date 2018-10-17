import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import _ from 'lodash';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import RoleTypeEnum from '../../enums/RoleTypeEnum';
import RolePriorityEnum from '../../enums/RolePriorityEnum';
import { RoleManager, SecurityManager, RequestManager } from '../../redux';
import RequestTable from '../request/RequestTable';
import SearchParameters from '../../domain/SearchParameters';

let roleManager = null;
const uiKeyRoleRequest = 'role-universal-request-table';
const requestManager = new RequestManager();

/**
 * Role detail
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class RoleDetail extends Basic.AbstractContent {

  constructor(props) {
    super(props);
    this.state = {
      _showLoading: true,
      oldCode: null,
      activeKey: 1
    };
  }

  getContentKey() {
    return 'content.roles';
  }

  componentDidMount() {
    const { entity } = this.props;
    // Init manager - evaluates if we want to use standard (original) manager or
    // universal request manager (depends on existing of 'requestId' param)
    roleManager = this.getRequestManager(this.props.params, new RoleManager());

    if (Utils.Entity.isNew(entity)) {
      entity.priorityEnum = RolePriorityEnum.NONE;
      entity.priority = RolePriorityEnum.getPriority(RolePriorityEnum.NONE) + '';
      this._setSelectedEntity(entity);
    } else {
      this._setSelectedEntity(this._prepareEntity(entity));
    }
  }

  componentWillReceiveProps(nextProps) {
    const { entity } = this.props;
    if ((nextProps.params && this.props.params && nextProps.params.requestId !== this.props.params.requestId)
      || (nextProps.entity && nextProps.entity !== entity)) {
      // Init manager - evaluates if we want to use standard (original) manager or
      // universal request manager (depends on existing of 'requestId' param)
      roleManager = this.getRequestManager(nextProps.params, new RoleManager());
    }
    if (nextProps.entity && nextProps.entity !== entity && nextProps.entity) {
      this._setSelectedEntity(this._prepareEntity(nextProps.entity));
    }
  }

  _prepareEntity(entity) {
    const copyOfEntity = _.merge({}, entity); // we can not modify given entity
    // we dont need to load entities again - we have them in embedded objects
    copyOfEntity.priorityEnum = RolePriorityEnum.getKeyByPriority(copyOfEntity.priority);
    copyOfEntity.priority = copyOfEntity.priority + ''; // We have to do convert form int to string (cause TextField and validator)
    return copyOfEntity;
  }

  _setSelectedEntity(entity) {
    this.setState({
      _showLoading: false,
      oldCode: entity.code
    }, () => {
      this.refs.form.setData(entity);
      this.refs.code.focus();
    });
  }

  save(afterAction, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    this.setState({
      _showLoading: true
    }, () => {
      const entity = this.refs.form.getData();
      this.refs.form.processStarted();
      // append selected authorities
      if (this.refs.authorities) {
        entity.authorities = this.refs.authorities.getWrappedInstance().getSelectedAuthorities();
      }
      //
      this.getLogger().debug('[RoleDetail] save entity', entity);
      if (Utils.Entity.isNew(entity)) {
        this.context.store.dispatch(roleManager.createEntity(entity, null, (createdEntity, error) => {
          this._afterSave(createdEntity, error, afterAction);
        }));
      } else {
        this.context.store.dispatch(roleManager.updateEntity(entity, null, (patchedEntity, error) => {
          this._afterSave(patchedEntity, error, afterAction);
        }));
      }
    });
  }

  _afterSave(entity, error, afterAction = 'CLOSE') {
    this.setState({
      _showLoading: false
    }, () => {
      this.refs.form.processEnded();
      if (error) {
        this.addError(error);
        return;
      }
      //
      this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
      if (afterAction === 'CLOSE') {
        this.context.router.replace(this.addRequestPrefix('roles', this.props.params));
      } else if (afterAction === 'NEW') {
        const uuidId = uuid.v1();
        const newEntity = {
          roleType: RoleTypeEnum.findKeyBySymbol(RoleTypeEnum.TECHNICAL),
          priority: RolePriorityEnum.getPriority(RolePriorityEnum.NONE) + '',
          priorityEnum: RolePriorityEnum.findKeyBySymbol(RolePriorityEnum.NONE)
        };
        this.context.store.dispatch(roleManager.receiveEntity(uuidId, newEntity));
        this.context.router.replace(`${this.addRequestPrefix('role', this.props.params)}/${uuidId}/new?new=1`);
        this._setSelectedEntity(newEntity);
      } else {
        this.context.router.replace(`${this.addRequestPrefix('role', this.props.params)}/${entity.id}/detail`);
      }
    });
  }

  _onChangePriorityEnum(item) {
    if (item) {
      const priority = RolePriorityEnum.getPriority(item.value);
      this.refs.priority.setValue(priority + '');
    } else {
      this.refs.priority.setValue(null);
    }
  }

  /**
   * TODO: move to new Codeable component
   */
  _onChangeCode(event) {
    // check guarded depents on new entity name
    const name = this.refs.name.getValue();
    const code = event.currentTarget.value;
    //
    if (!name || this.state.oldCode === name) {
      this.setState({
        oldCode: code,
      }, () => {
        this.refs.name.setValue(code);
      });
    }
  }

  _onChangeSelectTabs(activeKey) {
    this.setState({
      activeKey
    });
  }

  render() {
    const { entity, showLoading, _permissions, _requestUi} = this.props;
    const { _showLoading, activeKey } = this.state;
    if (!roleManager || !entity) {
      return null;
    }
    let requestsForceSearch = new SearchParameters();
    requestsForceSearch = requestsForceSearch.setFilter('ownerId', entity.id ? entity.id : SearchParameters.BLANK_UUID);
    requestsForceSearch = requestsForceSearch.setFilter('ownerType', 'eu.bcvsolutions.idm.core.api.dto.IdmRoleDto');
    requestsForceSearch = requestsForceSearch.setFilter('states', ['IN_PROGRESS', 'CONCEPT', 'EXCEPTION']);
    //
    return (
      <div>
        <Helmet title={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('edit.title') } />
        <Basic.Tabs
          activeKey={ activeKey }
          onSelect={ this._onChangeSelectTabs.bind(this)}
          style={{ paddingTop: 15 }}>
          <Basic.Tab eventKey={ 1 } title={ this.i18n('header') } className="bordered">
            <form onSubmit={ this.save.bind(this, 'CONTINUE') }>
              <Basic.Panel
                className="no-border last">
                <Basic.PanelHeader
                  text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('tabs.basic') }
                  style={{ paddingLeft: 15, paddingRight: 15 }}/>
                <Basic.PanelBody style={{ paddingTop: 0, paddingBottom: 0 }}>
                  <Basic.AbstractForm
                    ref="form"
                    showLoading={ _showLoading || showLoading }
                    readOnly={ !roleManager.canSave(entity, _permissions) }>

                    <Basic.Row>
                      <Basic.Col lg={ 4 }>
                        <Basic.TextField
                          ref="code"
                          label={ this.i18n('entity.Role.code.label') }
                          required
                          max={ 255 }
                          onChange={ this._onChangeCode.bind(this) }/>
                      </Basic.Col>
                      <Basic.Col lg={ 8 }>
                        <Basic.TextField
                          ref="name"
                          label={this.i18n('entity.Role.name')}
                          required
                          min={ 0 }
                          max={ 255 }/>
                      </Basic.Col>
                    </Basic.Row>

                    <Basic.EnumSelectBox
                      ref="roleType"
                      label={ this.i18n('entity.Role.roleType') }
                      enum={ RoleTypeEnum }
                      required
                      readOnly={ !Utils.Entity.isNew(entity) }
                      rendered={ false }/>
                    <Basic.EnumSelectBox
                      ref="priorityEnum"
                      label={this.i18n('entity.Role.priorityEnum')}
                      enum={RolePriorityEnum}
                      onChange={this._onChangePriorityEnum.bind(this)}/>
                    <Basic.TextField
                      ref="priority"
                      label={this.i18n('entity.Role.priority')}
                      readOnly
                      required/>
                    <Basic.Checkbox
                      ref="approveRemove"
                      label={this.i18n('entity.Role.approveRemove')}/>
                    <Basic.Checkbox
                      ref="canBeRequested"
                      label={this.i18n('entity.Role.canBeRequested')}/>
                    <Basic.TextArea
                      ref="description"
                      label={this.i18n('entity.Role.description')}
                      max={2000}/>
                    <Basic.Checkbox
                      ref="disabled"
                      label={this.i18n('entity.Role.disabled')}/>
                  </Basic.AbstractForm>
                </Basic.PanelBody>

                <Basic.PanelFooter style={{ paddingLeft: 15, paddingRight: 15 }}>
                  <Basic.Button
                    type="button"
                    level="link"
                    onClick={this.context.router.goBack}
                    showLoading={_showLoading}>{this.i18n('button.back')}
                  </Basic.Button>
                  <Basic.SplitButton
                    level="success"
                    title={ this.i18n('button.saveAndContinue') }
                    onClick={ this.save.bind(this, 'CONTINUE') }
                    showLoading={ _showLoading }
                    showLoadingIcon
                    showLoadingText={ this.i18n('button.saving') }
                    rendered={ roleManager.canSave(entity, _permissions) }
                    pullRight
                    dropup>
                    <Basic.MenuItem
                      eventKey="1"
                      onClick={this.save.bind(this, 'CLOSE')}>
                      {this.i18n('button.saveAndClose')}
                    </Basic.MenuItem>
                    <Basic.MenuItem
                      eventKey="2"
                      onClick={ this.save.bind(this, 'NEW') }
                      rendered={ SecurityManager.hasAuthority('ROLE_CREATE') }>
                      { this.i18n('button.saveAndNew') }
                    </Basic.MenuItem>
                  </Basic.SplitButton>
                </Basic.PanelFooter>
              </Basic.Panel>
              {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
              <input type="submit" className="hidden"/>
            </form>
          </Basic.Tab>
          <Basic.Tab
            eventKey={ 2 }
            rendered={entity.id ? true : false}
            disabled={roleManager.isRequestModeEnabled() || !entity.id}
            title={
              <span>
                { this.i18n('content.requests.header') }
                <Basic.Badge
                  level="warning"
                  style={{ marginLeft: 5 }}
                  text={ _requestUi ? _requestUi.total : null }
                  rendered={!roleManager.isRequestModeEnabled() && _requestUi && _requestUi.total > 0 }
                  title={ this.i18n('content.requests.header') }/>
              </span>
            }
            className="bordered">
              <Basic.ContentHeader style={{ marginBottom: 0, paddingTop: 15, paddingRight: 15, paddingLeft: 15 }}>
                <Basic.Icon type="fa" icon="universal-access"/>
                {' '}
                <span dangerouslySetInnerHTML={{ __html: this.i18n('content.requests.header') }}/>
              </Basic.ContentHeader>
                <RequestTable
                  ref="table"
                  uiKey={uiKeyRoleRequest}
                  forceSearchParameters={ requestsForceSearch }
                  showFilter={false}
                  showLoading={_showLoading}
                  manager={requestManager}
                  columns= {['state', 'created', 'modified', 'wf', 'detail']}/>
          </Basic.Tab>
        </Basic.Tabs>
      </div>
    );
  }
}

RoleDetail.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
RoleDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  if (!roleManager) {
    return null;
  }
  if (!component.entity) {
    return {};
  }
  return {
    _permissions: roleManager.getPermissions(state, null, component.entity.id),
    _requestUi: Utils.Ui.getUiState(state, uiKeyRoleRequest)
  };
}

export default connect(select)(RoleDetail);
