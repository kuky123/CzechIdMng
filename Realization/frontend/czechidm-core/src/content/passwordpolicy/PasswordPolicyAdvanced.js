import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { PasswordPolicyManager, SecurityManager } from '../../redux';
import PasswordPolicyTypeEnum from '../../enums/PasswordPolicyTypeEnum';
import PasswordPolicyIdentityAttributeEnum from '../../enums/PasswordPolicyIdentityAttributeEnum';

/**
* Advanced detail for password policy
*/

const passwordPolicyManager = new PasswordPolicyManager();

class PasswordPolicyAdvanced extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.passwordPolicyManager = new PasswordPolicyManager();
    this.state = {
      showLoading: false
    };
  }

  getContentKey() {
    return 'content.passwordPolicies';
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'password-policies', 'password-policies-advanced']);
    const { entityId } = this.props.params;
    this.getLogger().debug(`[TypeContent] loading entity detail [id:${entityId}]`);
    this.context.store.dispatch(passwordPolicyManager.fetchEntity(entityId));
  }

  /**
  * Method check if props in this component is'nt different from new props.
  */
  componentWillReceiveProps(nextProps) {
    // check id of old and new entity
    const validateType = nextProps.entity && PasswordPolicyTypeEnum.findSymbolByKey(nextProps.entity.type) === PasswordPolicyTypeEnum.VALIDATE;
    if (validateType) {
      if (!this.props.entity || nextProps.entity.id !== this.props.entity.id || nextProps.entity.id !== this.refs.form.getData().id) {
        this._initForm(nextProps.entity);
      }
    }
  }

  /**
  * Method for basic initial form
  */
  _initForm(entity) {
    if (entity && this.refs.form) {
      const loadedEntity = _.merge({}, entity);
      loadedEntity.identityAttributeCheck = this._transformAttributeToCheck(entity.identityAttributeCheck);

      this.refs.form.setData(loadedEntity);
    }
  }

  /**
   * Method tranform password policy attribute to check to
   * enum
   */
  _transformAttributeToCheck(identityAttributeCheck) {
    if (!identityAttributeCheck) {
      return null;
    }
    // transform identityAttributeCheck
    return _.split(identityAttributeCheck, ', ');
  }

  /**
  * Default save method that catch save event from form.
  */
  save(editContinue = 'CLOSE', event) {
    const { uiKey } = this.props;

    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      showLoading: true
    },
    this.refs.form.processStarted());

    // get data from forms on different tabs
    const entity = this.refs.form.getData();

    // transform identityAttributeCheck
    entity.identityAttributeCheck = _.join(entity.identityAttributeCheck, ', ');

    this.context.store.dispatch(this.passwordPolicyManager.updateEntity(entity, `${uiKey}-detail`, (savedEntity, error) => {
      this._afterSave(entity, error, editContinue);
    }));
  }

  /**
  * Method set showLoading to false and if is'nt error then show success message
  */
  _afterSave(entity, error, editContinue) {
    if (error) {
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      this.addError(error);
      return;
    }
    this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    if (editContinue === 'SAVE') {
      this.context.router.goBack();
    } else {
      this.refs.form.processEnded();
      this.setState({
        showLoading: false
      });
    }
  }

  render() {
    const { uiKey, entity } = this.props;
    const { showLoading } = this.state;

    const validateType = entity && PasswordPolicyTypeEnum.findSymbolByKey(entity.type) === PasswordPolicyTypeEnum.VALIDATE;

    return (
      <div>
        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className="no-border last">
            <Basic.PanelHeader text={this.i18n('content.passwordPolicies.advanced.title')} />
            <Basic.Loading showLoading={showLoading} />
            <Basic.PanelBody style={{ padding: 0, paddingTop: '15px' }} rendered={!validateType} >
              <Basic.Alert
                className="no-margin"
                icon="exclamation-sign"
                key="passwordPolicyAdvancedControlValidateType"
                text={this.i18n('validation.advancedControlValidateType')} />
            </Basic.PanelBody>
            <Basic.PanelBody style={{ padding: 0 }} rendered={validateType}>
              <Basic.AbstractForm
                ref="form"
                uiKey={uiKey}
                readOnly={!SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}>
                <Basic.Checkbox ref="enchancedControl"
                  helpBlock={this.i18n('entity.PasswordPolicy.enchancedControl.help')}
                  label={this.i18n('entity.PasswordPolicy.enchancedControl.label')}/>
                <Basic.LabelWrapper label=" ">
                  <Basic.Alert
                    className="no-margin"
                    icon="exclamation-sign"
                    key="passwordPolicyHelpRules"
                    text={this.i18n('rulesHelp')} />
                </Basic.LabelWrapper>
                <Basic.Checkbox ref="passwordLengthRequired"
                  label={this.i18n('entity.PasswordPolicy.passwordLengthRequired.label')}/>

                <Basic.Checkbox ref="upperCharRequired"
                  label={this.i18n('entity.PasswordPolicy.upperCharRequired.label')}/>

                <Basic.Checkbox ref="lowerCharRequired"
                  label={this.i18n('entity.PasswordPolicy.lowerCharRequired.label')}/>

                <Basic.Checkbox ref="numberRequired"
                  label={this.i18n('entity.PasswordPolicy.numberRequired.label')}/>

                <Basic.Checkbox ref="specialCharRequired"
                  label={this.i18n('entity.PasswordPolicy.specialCharRequired.label')}/>

                <Basic.TextField ref="minRulesToFulfill"
                  type="number"
                  validation={ Utils.Ui.getIntegerValidation() }
                  helpBlock={this.i18n('entity.PasswordPolicy.minRulesToFulfill.help')}
                  label={this.i18n('entity.PasswordPolicy.minRulesToFulfill.label')} />

                <Basic.EnumSelectBox
                  ref="identityAttributeCheck"
                  helpBlock={ this.i18n('entity.PasswordPolicy.identityAttributeCheck.help') }
                  enum={ PasswordPolicyIdentityAttributeEnum }
                  multiSelect
                  label={ this.i18n('entity.PasswordPolicy.identityAttributeCheck.label') }
                  useSymbol={ false }/>
              </Basic.AbstractForm>
            </Basic.PanelBody>
            <Basic.PanelFooter showLoading={showLoading} rendered={validateType} >
              <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
              <Basic.SplitButton level="success" title={this.i18n('button.saveAndContinue')}
                onClick={this.save.bind(this, 'CONTINUE')}
                rendered={SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}
                showLoading={showLoading} pullRight dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'SAVE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          </Basic.Panel>
          {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
          <input type="submit" className="hidden"/>
        </form>
      </div>
    );
  }
}

PasswordPolicyAdvanced.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
};
PasswordPolicyAdvanced.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: passwordPolicyManager.getEntity(state, entityId),
    showLoading: passwordPolicyManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(PasswordPolicyAdvanced);
