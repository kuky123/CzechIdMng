import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Managers, Utils, Advanced } from 'czechidm-core';
import { ProvisioningBreakConfigManager, ProvisioningBreakRecipientManager } from '../../redux';
import ProvisioningOperationTypeEnum from '../../domain/ProvisioningOperationTypeEnum';
import SystemProvisioningBreakConfigRecipientTable from './SystemProvisioningBreakConfigRecipientTable';

const uiKey = 'provisioning-break-config-detail';
const manager = new ProvisioningBreakConfigManager();

const DEFAULT_PERIOD = 20;

/**
 * TODO: problem with attribute period validation trought joi and required not working
 *
 * @author Ondřej Kopr
 */
class SystemProvisioningBreakConfigDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.provisioningBreakRecipientManager = new ProvisioningBreakRecipientManager();
    this.identityManager = new Managers.IdentityManager();
    this.notificationTemplatemanager = new Managers.NotificationTemplateManager();
  }

  getUiKey() {
    return uiKey;
  }

  getNavigationKey() {
    return 'system-provisioning-break-config';
  }

  getManager() {
    return manager;
  }

  getContentKey() {
    return 'acc:content.provisioningBreakConfig';
  }

  componentWillReceiveProps(nextProps) {
    const { mappingId } = nextProps.params;
    if (mappingId && mappingId !== this.props.params.mappingId) {
      this._initComponent(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    super.componentDidMount();
    //
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const { configId } = props.params;

    // fetch break config
    if (!this._getIsNew(props)) {
      this.context.store.dispatch(manager.fetchEntity(configId));
    }
  }

  /**
   * Save entity
   */
  save(event) {
    const { entityId } = this.props.params;
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    const formEntity = this.refs.form.getData();
    const savedEntity = {
      ...formEntity,
      system: entityId
    };
    if (formEntity.id === undefined) {
      this.context.store.dispatch(manager.createEntity(savedEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
      }));
    } else {
      this.context.store.dispatch(manager.updateEntity(savedEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
    }
  }

  afterSave(entity, error) {
    if (!error) {
      if (this._getIsNew()) {
        this.addMessage({ message: this.i18n('create.success', { entityType: entity.entityType, operationType: entity.operationType}) });
      } else {
        this.addMessage({ message: this.i18n('save.success', {entityType: entity.entityType, operationType: entity.operationType}) });
      }
      const { entityId } = this.props.params;
      this.context.router.replace(`/system/${entityId}/break-configs/${entity.id}/detail`, { configId: entity.id });
    } else {
      this.addError(error);
    }
    super.afterSave();
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    if (query && query !== null && query.new !== undefined) {
      return query.new;
    }
    return false;
  }

  _getBoolColumn(value) {
    return (<Basic.BooleanCell propertyValue={ value !== null } className="column-face-bool"/>);
  }

  _onChangeEntityType(entity) {
    this.setState({_entityType: entity.value});
  }

  render() {
    const { _showLoading, entity } = this.props;
    const { configId } = this.props.params;

    const isNew = this._getIsNew();
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>

        <form onSubmit={this.save.bind(this)}>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm
              ref="form"
              data={ entity }
              showLoading={ _showLoading }
              readOnly={ !Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
              <Basic.EnumSelectBox
                ref="operationType"
                useSymbol={false}
                enum={ProvisioningOperationTypeEnum}
                helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.operationType.help')}
                label={this.i18n('acc:entity.ProvisioningBreakConfig.operationType.label')}
                required/>
              <Basic.TextField
                ref="period"
                type="number"
                value={DEFAULT_PERIOD}
                required
                validation={ Utils.Ui.getIntegerValidation() }
                helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.period.help')}
                label={this.i18n('acc:entity.ProvisioningBreakConfig.period.label')}/>
              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField
                    ref="warningLimit"
                    type="number"
                    validation={ Utils.Ui.getIntegerValidation() }
                    helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.warningLimit.help')}
                    label={this.i18n('acc:entity.ProvisioningBreakConfig.warningLimit.label')}/>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.SelectBox
                    ref="warningTemplate"
                    manager={ this.notificationTemplatemanager }
                    placeholder={ this.i18n('acc:entity.ProvisioningBreakConfig.warningTemplate.placeholder') }
                    helpBlock={ this.i18n('acc:entity.ProvisioningBreakConfig.warningTemplate.help') }
                    label={ this.i18n('acc:entity.ProvisioningBreakConfig.warningTemplate.label') }/>
                </Basic.Col>
              </Basic.Row>
              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField
                    ref="disableLimit"
                    type="number"
                    validation={ Utils.Ui.getIntegerValidation() }
                    helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.disableLimit.help')}
                    label={this.i18n('acc:entity.ProvisioningBreakConfig.disableLimit.label')}/>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.SelectBox
                    ref="disableTemplate"
                    manager={this.notificationTemplatemanager}
                    placeholder={ this.i18n('acc:entity.ProvisioningBreakConfig.disableTemplate.placeholder') }
                    helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.disableTemplate.help')}
                    label={this.i18n('acc:entity.ProvisioningBreakConfig.disableTemplate.label')}/>
                </Basic.Col>
              </Basic.Row>

              <Basic.TextField
                ref="actualOperationCount"
                readOnly
                helpBlock={this.i18n('acc:entity.ProvisioningBreakConfig.actualOperationCount.help')}
                label={this.i18n('acc:entity.ProvisioningBreakConfig.actualOperationCount.label')}
                rendered={ !Utils.Entity.isNew(entity) }/>
              <Basic.Checkbox
                ref="disabled"
                label={this.i18n('acc:entity.ProvisioningBreakConfig.disabled')}/>
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
                showLoading={_showLoading}
                rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                {this.i18n('button.saveAndContinue')}
              </Basic.Button>
            </Basic.PanelFooter>

            <Basic.Panel className="no-border" showLoading={_showLoading}>
              <Basic.PanelHeader text={this.i18n('acc:content.provisioningBreakConfigRecipient.title')}/>
              <Basic.Alert level="info" text={this.i18n('recipientInfo')} rendered={isNew}/>
              <SystemProvisioningBreakConfigRecipientTable
                uiKey={ `provisioning-break-config-recipient-${configId}` }
                manager={ this.provisioningBreakRecipientManager }
                forceSearchParameters={ this.provisioningBreakRecipientManager.getSearchParameters().setFilter('breakConfigId', configId) }
                provisioningBreakConfigId={ configId }
                rendered={ !isNew } />
            </Basic.Panel>

          </Basic.Panel>
        </form>
      </div>
    );
  }
}

SystemProvisioningBreakConfigDetail.propTypes = {
  _showLoading: PropTypes.bool,
};
SystemProvisioningBreakConfigDetail.defaultProps = {
  _showLoading: false,
};

function select(state, component) {
  const { configId } = component.params;

  return {
    entity: manager.getEntity(state, configId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemProvisioningBreakConfigDetail);
