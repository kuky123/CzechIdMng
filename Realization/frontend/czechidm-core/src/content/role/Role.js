import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { RoleManager, RequestManager, ConfigurationManager, SecurityManager} from '../../redux';
import * as Advanced from '../../components/advanced';

let manager = null;
// For call directly role api
const originalRoleManager = new RoleManager();
const requestManager = new RequestManager();

/**
 * Role's tab panel
 *
 * @author Radek Tomiška
 */
class Role extends Basic.AbstractContent {

  componentDidMount() {
    super.componentDidMount();
    const { entityId } = this.props.params;

    // Init manager - evaluates if we want to use standard (original) manager or
    // universal request manager (depends on existing of 'requestId' param)
    manager = this.getRequestManager(this.props.params, new RoleManager());

    this.context.store.dispatch(manager.fetchEntity(entityId, null, (entity, error) => {
      this.handleError(error);
    }));
  }

  _createRequestRole() {
    const { entity} = this.props;
    const promise = requestManager.getService().createRequest('roles', entity);
    promise.then((json) => {
      // Init universal request manager (manually)
      manager = this.getRequestManager({requestId: json.id}, new RoleManager());
      // Fetch entity - we need init permissions for new manager
      this.context.store.dispatch(manager.fetchEntityIfNeeded(entity.id, null, (e, error) => {
        this.handleError(error);
      }));
      // Redirect to new request
      this.context.router.push(`${this.addRequestPrefix('role', {requestId: json.id})}/${entity.id}/detail`);
    }).catch(ex => {
      this.setState({
        showLoading: false
      });
      this.addError(ex);
    });
  }

  render() {
    const { entity, showLoading, _requestsEnabled, _permissions } = this.props;
    if (!manager) {
      return null;
    }
    const isRequest = this.isRequest(this.props.params);
    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          <Basic.Icon value="fa:universal-access"/>
          {' '}
          { manager.getNiceLabel(entity)} <small> {this.i18n('content.roles.edit.header') }</small>
        </Basic.PageHeader>
        <Basic.Row rendered={_requestsEnabled
            && !isRequest
            && originalRoleManager.canSave(entity, _permissions)
            && SecurityManager.hasAuthority('REQUEST_CREATE')
          }>
          <Basic.Col lg={ 12 }>
            <Basic.Alert
              level="info"
              title={ this.i18n('content.roles.button.createRequest.header') }
              text={ this.i18n('content.roles.button.createRequest.text') }
              className="no-margin"
              buttons={[
                <Basic.Button
                  level="info"
                  onClick={ this._createRequestRole.bind(this) }
                  titlePlacement="bottom">
                  <Basic.Icon type="fa" icon="key"/>
                  {' '}
                  { this.i18n('content.roles.button.createRequest.label') }
                </Basic.Button>
              ]}/>
          </Basic.Col>
        </Basic.Row>
        <Advanced.TabPanel parentId={this.isRequest(this.props.params) ? 'request-roles' : 'roles'} params={this.props.params}>
          { this.props.children }
        </Advanced.TabPanel>
      </div>
    );
  }
}

Role.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
Role.defaultProps = {
  entity: null,
  showLoading: false
};

function select(state, component) {
  if (!manager) {
    return {};
  }
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId),
    _requestsEnabled: ConfigurationManager.getPublicValueAsBoolean(state, manager.getEnabledPropertyKey()),
    _permissions: manager.getPermissions(state, null, entityId)
  };
}

export default connect(select)(Role);
