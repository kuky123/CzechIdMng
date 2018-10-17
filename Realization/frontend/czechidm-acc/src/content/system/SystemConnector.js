import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Utils, Managers } from 'czechidm-core';
import { SystemManager } from '../../redux';

const uiKey = 'eav-connector-';
const manager = new SystemManager();

/**
 * System connector configuration
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class SystemConnectorContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      error: null,
      emptyConnector: false, // for first time is not choosen connector -> show alert block with info
      remoteConnectorError: false // dont show options for invalid password
    };
  }

  getContentKey() {
    return 'acc:content.system.connector';
  }

  componentDidMount() {
    this.selectNavigationItems(['sys-systems', 'system-connector']);
    // load definition and values
    const { entityId } = this.props.params;
    this.context.store.dispatch(manager.fetchAvailableFrameworks());
    this.context.store.dispatch(manager.fetchAvailableRemoteConnector(entityId, (remoteConnectors, error) => {
      if (error) {
        if (error.statusEnum === 'REMOTE_SERVER_INVALID_CREDENTIAL' ||
                  error.statusEnum === 'REMOTE_SERVER_CANT_CONNECT') {
          this.setState({
            remoteConnectorError: true
          });
        }
      }
    }));
    this.context.store.dispatch(manager.fetchEntity(entityId));
    this.reloadConnectorConfiguration(entityId);
  }

  reloadConnectorConfiguration(entityId) {
    this.context.store.dispatch(manager.fetchConnectorConfiguration(entityId, `${uiKey}-${entityId}`, (formInstance, error) => {
      if (error) {
        if (error.statusEnum === 'CONNECTOR_CONFIGURATION_FOR_SYSTEM_NOT_FOUND') {
          this.addErrorMessage({ hidden: true, level: 'info' }, error);
          this.setState({ error });
        } else if (error.statusEnum === 'CONNECTOR_FORM_DEFINITION_NOT_FOUND') {
          // dont set error, just show alert block
          this.addErrorMessage({ hidden: true, level: 'info' }, error);
          this.setState({ error });
        } else {
          this.addError(error);
          this.setState({ error: null });
        }
      } else {
        this.setState({
          emptyConnector: false
        });
        this.getLogger().debug(`[EavForm]: Loaded form definition [${formInstance.getDefinition().type}|${formInstance.getDefinition().name}]`);
      }
    }));
  }

  showDetail() {
    const { entityId } = this.props.params;
    this.context.router.push(`/system/${entityId}/detail`);
  }

  saveConnector(value, event) {
    const {error} = this.state;
    if (event) {
      event.preventDefault();
    }
    //
    if (!this.refs.formConnector.isFormValid()) {
      return;
    }
    //
    if (error && error.statusEnum === 'CONNECTOR_FORM_DEFINITION_NOT_FOUND') {
      this._saveConnectorInternal(value.value);
    } else {
      //
      // modal window with confirm
      this.refs[`confirm-change-connector`].show(
        this.i18n(`action.changeConnector.message`),
        this.i18n(`action.changeConnector.header`)
      ).then(() => {
        //
        this._saveConnectorInternal(value.value);
      }, () => {
        // Rejected
      });
    }
  }

  _saveConnectorInternal(data) {
    const { availableFrameworks, availableRemoteFrameworks, entity } = this.props;

    if (data === null) {
      return;
    }

    this.setState({
      showLoading: true,
      error: null
    }, () => {
      let connector = null;
      if (entity.remote) {
        connector = availableRemoteFrameworks.get(data.split(':')[0]).get(data);
      } else {
        connector = availableFrameworks.get(data.split(':')[0]).get(data);
      }

      let saveEntity = { };

      const connectorServer = {
        ...entity.connectorServer,
        password: null
      };
      if (connector !== null) {
        saveEntity = {
          ...entity,
          connectorKey: {
            framework: connector.connectorKey.framework,
            connectorName: connector.connectorKey.connectorName,
            bundleName: connector.connectorKey.bundleName,
            bundleVersion: connector.connectorKey.bundleVersion
          },
          connectorServer
        };
      }

      // we dont must check is new, on this component will be always old entity
      this.context.store.dispatch(manager.updateEntity(saveEntity, `${uiKey}-detail`, (patchedEntity, newError) => {
        this.reloadConnectorConfiguration(patchedEntity.id);
        this._afterSave(patchedEntity, newError);
      }));
    });
  }

  _afterSave(entity, error) {
    this.setState({
      showLoading: false
    }, () => {
      if (error) {
        this.addError(error);
        return;
      }
      // now we do not want to see success message
      // this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    });
  }

  save(check = false, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.eav.isValid()) {
      return;
    }
    //
    const { entityId } = this.props.params;
    const filledFormValues = this.refs.eav.getValues();
    this.getLogger().debug(`[EavForm]: Saving form [${this.refs.eav.getFormDefinition().type}|${this.refs.eav.getFormDefinition().name}]`);
    // save values
    this.context.store.dispatch(manager.saveConnectorConfiguration(entityId, filledFormValues, `${uiKey}-${entityId}`, (savedFormInstance, error) => {
      if (error) {
        this.addError(error);
      } else {
        const system = manager.getEntity(this.context.store.getState(), entityId);
        if (!check) {
          this.addMessage({ message: this.i18n('save.success', { name: system.name }) });
        }
        this.getLogger().debug(`[EavForm]: Form [${this.refs.eav.getFormDefinition().type}|${this.refs.eav.getFormDefinition().name}] saved`);

        // We will call check connector
        if (check) {
          this.setState({showLoading: true});
          const promise = manager.getService().checkSystem(entityId);
          promise.then(() => {
            this.addMessage({ message: this.i18n('checkSystem.success')});
            this.setState({showLoading: false});
          }).catch(ex => {
            this.addMessage({ level: 'warning', title: this.i18n('checkSystem.error'), message: ex.message});
            this.setState({showLoading: false});
          });
        }
      }
    }));
  }

  _getConnectorOptions(availableFrameworks, availableRemoteFrameworks, entity) {
    const options = [];
    if (entity) {
      if (entity.remote && availableRemoteFrameworks) {
        availableRemoteFrameworks.forEach((connectors, framework) => {
          connectors.forEach((connector, fullName) => {
            options.push({
              value: fullName,
              niceLabel: `${connector.connectorDisplayName} (${framework})`
            });
          });
        });
      } else if (availableFrameworks) {
        availableFrameworks.forEach((connectors, framework) => {
          connectors.forEach((connector, fullName) => {
            options.push({
              value: fullName,
              niceLabel: `${connector.connectorDisplayName} (${framework})`
            });
          });
        });
      }
    }
    return options;
  }

  render() {
    const { formInstance, availableFrameworks, availableRemoteFrameworks, entity } = this.props;
    const { error, showLoading, remoteConnectorError } = this.state;
    const _showLoading = showLoading || this.props._showLoading;
    const _availableConnectors = this._getConnectorOptions(availableFrameworks, availableRemoteFrameworks, entity);

    let pickConnector = null;
    if (entity && entity.connectorKey) {
      pickConnector = _.find(_availableConnectors, { 'value': entity.connectorKey.fullName });
    }

    let content;
    if (error) {
      // connector is wrong configured
      content = (
        <Basic.Alert level="info">
          {this.i18n(`${error.module}:error.${error.statusEnum}.message`, error.parameters)}
        </Basic.Alert>
      );
    } else if (!formInstance && !_showLoading || remoteConnectorError) {
      // connector not found on BE
      content = null;
    } else {
      // connector setting is ready
      content = (
        <form style={{ marginTop: 15 }} onSubmit={this.save.bind(this, false)}>
          <Advanced.EavForm
            ref="eav"
            formInstance={ formInstance }
            readOnly={ !Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') }
            useDefaultValue/>
          <Basic.PanelFooter rendered={ Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') }>
            <Basic.Button
              type="submit"
              level="success"
              showLoadingIcon
              showLoadingText={this.i18n('button.saving')}>
              {this.i18n('button.save')}
            </Basic.Button>
          </Basic.PanelFooter>
        </form>
      );
    }

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-change-connector" level="danger"/>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <Basic.Panel showLoading={_showLoading} className="no-border no-margin">
          <Basic.AbstractForm
            rendered={_availableConnectors.length !== 0}
            ref="formConnector"
            uiKey={uiKey}
            readOnly={!Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')}
            style={{ paddingBottom: 0 }}>

            <div style={{ display: 'inline-flex', width: '100%'}}>
              <Basic.EnumSelectBox
                ref="connector"
                placeholder={ this.i18n('acc:entity.System.connectorKey.connectorName') }
                value={ pickConnector ? pickConnector.value : null }
                options={ _availableConnectors }
                readOnly={ !Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') || remoteConnectorError }
                clearable={ false }
                onChange={ this.saveConnector.bind(this) }
                style={{ width: '100%'}} />

              <Basic.Button
                style={{ marginLeft: 5 }}
                level="success"
                disabled={error || remoteConnectorError}
                onClick={this.save.bind(this, true)}
                rendered={ Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') && pickConnector !== undefined }
                title={ this.i18n('button.checkSystemTooltip') }
                titlePlacement="bottom">
                <Basic.Icon type="fa" icon="check-circle"/>
                {' '}
                { this.i18n('button.checkSystem') }
              </Basic.Button>
            </div>

          </Basic.AbstractForm>

          <hr style={{ margin: 0 }}/>

          { content }
        </Basic.Panel>
      </div>
    );
  }
}

SystemConnectorContent.propTypes = {
  formInstance: PropTypes.oject,
  _showLoading: PropTypes.bool,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
SystemConnectorContent.defaultProps = {
  formInstance: null,
  _showLoading: false,
  _permissions: null
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-${entityId}`),
    formInstance: Managers.DataManager.getData(state, `${uiKey}-${entityId}`),
    availableFrameworks: Managers.DataManager.getData(state, SystemManager.AVAILABLE_CONNECTORS),
    availableRemoteFrameworks: Managers.DataManager.getData(state, SystemManager.AVAILABLE_REMOTE_CONNECTORS),
    _permissions: manager.getPermissions(state, null, entityId)
  };
}

export default connect(select)(SystemConnectorContent);
