import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { PasswordPolicyManager, SecurityManager } from '../../redux';
import PasswordPolicyTypeEnum from '../../enums/PasswordPolicyTypeEnum';
import PasswordPolicyGenerateTypeEnum from '../../enums/PasswordPolicyGenerateTypeEnum';
import PasswordPolicyIdentityAttributeEnum from '../../enums/PasswordPolicyIdentityAttributeEnum';

/**
* Basic detail for password policy,
* this detail is also used for create entity
*/

const passwordPolicyManager = new PasswordPolicyManager();

class PasswordPolicyBasic extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.passwordPolicyManager = new PasswordPolicyManager();
    this.state = {
      showLoading: false
    };
    if (this._getIsNew()) {
      this.state = {
        validateType: true,
        defaultPolicy: false
      };
    }
  }

  getContentKey() {
    return 'content.passwordPolicies';
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'password-policies', 'password-policies-basic']);
    const { entityId } = this.props.params;
    if (this._getIsNew()) {
      // if entity is new, set default required rules
      this.context.store.dispatch(passwordPolicyManager.receiveEntity(entityId, {
        type: PasswordPolicyTypeEnum.findKeyBySymbol(PasswordPolicyTypeEnum.VALIDATE),
        generateType: PasswordPolicyGenerateTypeEnum.findKeyBySymbol(PasswordPolicyGenerateTypeEnum.RANDOM),
        passwordLengthRequired: true,
        upperCharRequired: true,
        lowerCharRequired: true,
        numberRequired: true
      }));
    } else {
      this.getLogger().debug(`[TypeContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(passwordPolicyManager.fetchEntity(entityId));
    }
  }

  /**
  * Method check if props in this component is'nt different from new props.
  */
  componentWillReceiveProps(nextProps) {
    // also is necessary to test defautl type, because it may be changed dynamical from others password policy
    if (!this.props.entity || nextProps.entity.id !== this.props.entity.id || nextProps.entity.id !== this.refs.form.getData().id ||
            this.props.entity.defaultPolicy !== nextProps.entity.defaultPolicy) {
      this._initForm(nextProps.entity);
    }
  }

  _getIsNew() {
    return this.props.isNew;
  }

  /**
  * Method for basic initial form
  */
  _initForm(entity) {
    if (entity && this.refs.form) {
      this._changeType(entity.type);
      this._changeDefaultPolicy(entity.defaultPolicy);
      this._changeGenerateType({value: entity.generateType});
      const loadedEntity = _.merge({}, entity);
      loadedEntity.identityAttributeCheck = this._transformAttributeToCheck(entity.identityAttributeCheck);

      this.refs.form.setData(loadedEntity);
      this.refs.name.focus();

      this.setState({
        defaultPolicy: loadedEntity.defaultPolicy
      });
    }
  }

  /**
   * Method tranform password policy attribute to check to
   * enum
   */
  _transformAttributeToCheck(identityAttributeCheck) {
    // transform identityAttributeCheck
    const identityAttributeCheckFinal = [];
    const attrs = _.split(identityAttributeCheck, ', ');
    for (const attribute in attrs) {
      if (attrs.hasOwnProperty(attribute)) {
        identityAttributeCheckFinal.push(PasswordPolicyIdentityAttributeEnum.findSymbolByKey(attrs[attribute]));
      }
    }
    return identityAttributeCheckFinal;
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
    let identityAttributeCheck = [];
    for (const attribute in entity.identityAttributeCheck) {
      if (entity.identityAttributeCheck.hasOwnProperty(attribute)) {
        identityAttributeCheck.push(PasswordPolicyIdentityAttributeEnum.findKeyBySymbol(entity.identityAttributeCheck[attribute]));
      }
    }
    //
    identityAttributeCheck = _.join(identityAttributeCheck, ', ');
    entity.identityAttributeCheck = identityAttributeCheck;

    if (entity.id === undefined) {
      this.context.store.dispatch(this.passwordPolicyManager.createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
        this._afterSave(createdEntity, error, editContinue);
      }));
    } else {
      this.context.store.dispatch(this.passwordPolicyManager.updateEntity(entity, `${uiKey}-detail`, (savedEntity, error) => {
        this._afterSave(entity, error, editContinue);
      }));
    }
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
    if (editContinue === 'SAVE' || editContinue === 'CREATE') {
      this.context.router.goBack();
    } else if (editContinue === 'CREATE_CONTINUE') {
      this.context.router.replace('/password-policies/' + entity.id);
    } else {
      this.refs.form.processEnded();
      this.setState({
        showLoading: false
      });
    }
  }

  _changeType(value) {
    // value can be instance of Symbol or value of enum select box => key for symbol
    if (PasswordPolicyTypeEnum.findSymbolByKey(value.value) === PasswordPolicyTypeEnum.VALIDATE ||
            PasswordPolicyTypeEnum.findSymbolByKey(value) === PasswordPolicyTypeEnum.VALIDATE ||
          value === PasswordPolicyTypeEnum.VALIDATE) {
      this.setState({
        validateType: true
      });
    } else {
      this.setState({
        validateType: false
      });
    }
  }

  _changeGenerateType(value) {
    this.setState({
      generateType: PasswordPolicyGenerateTypeEnum.findSymbolByKey(value.value)
    });
  }

  _changeDefaultPolicy(event) {
    if (event && event.currentTarget) {
      const checked = event.currentTarget.checked;
      this.setState({
        defaultPolicy: checked
      });
    }
  }

  render() {
    const { uiKey, entity, isNew } = this.props;
    const { showLoading, validateType, defaultPolicy, generateType } = this.state;
    //
    const showDefaultAdvanced = validateType && defaultPolicy;

    const isPassphrase = generateType === PasswordPolicyGenerateTypeEnum.PASSPHRASE;
    //
    return (
      <form onSubmit={this.save.bind(this, 'SAVE')}>
        <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
          <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('content.passwordPolicies.basic.title')} />
          <Basic.PanelBody style={Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
            <Basic.AbstractForm
              ref="form"
              uiKey={uiKey}
              readOnly={!SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}
              showLoading={entity === null}>
              {/* readOnly for edit is necessary */}
              <Basic.EnumSelectBox
                readOnly={!Utils.Entity.isNew(entity)}
                ref="type"
                onChange={this._changeType.bind(this)}
                required
                enum={PasswordPolicyTypeEnum}
                label={this.i18n('entity.PasswordPolicy.type.label')}/>
              <Basic.TextField
                ref="name"
                label={this.i18n('entity.PasswordPolicy.name.label')}
                required
                max={255}/>
              <Basic.EnumSelectBox
                ref="generateType" required={!validateType} hidden={validateType}
                enum={PasswordPolicyGenerateTypeEnum}
                  onChange={this._changeGenerateType.bind(this)}
                label={this.i18n('entity.PasswordPolicy.generateType.label')}/>
              <Basic.TextField
                ref="passphraseWords"
                helpBlock={this.i18n('entity.PasswordPolicy.passphraseWords.help')}
                hidden={!isPassphrase}
                type="number"
                validation={Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.passphraseWords.label')} />

              <Basic.Alert
                className="no-margin"
                icon="exclamation-sign"
                key="prefixAndSuffixGenerated"
                text={this.i18n('prefixAndSuffixGeneratedHelp')}
                rendered={ !validateType }/>

              <Basic.TextField
                ref="prefix"
                helpBlock={this.i18n('entity.PasswordPolicy.prefix.help')}
                hidden={validateType}
                label={this.i18n('entity.PasswordPolicy.prefix.label')} />
              <Basic.TextField
                ref="suffix"
                helpBlock={this.i18n('entity.PasswordPolicy.suffix.help')}
                hidden={validateType}
                label={this.i18n('entity.PasswordPolicy.suffix.label')} />

              <Basic.Checkbox ref="disabled" label={this.i18n('entity.PasswordPolicy.disabled.label')}/>

              <Basic.Checkbox
                ref="defaultPolicy"
                onChange={this._changeDefaultPolicy.bind(this)}
                helpBlock={this.i18n('entity.PasswordPolicy.defaultPolicy.help')}
                label={this.i18n('entity.PasswordPolicy.defaultPolicy.label')}/>

              <Basic.TextArea ref="description" label={this.i18n('entity.PasswordPolicy.description.label')}/>

              <Basic.LabelWrapper label=" ">
                <Basic.Alert
                  className="no-margin"
                  icon="exclamation-sign"
                  key="situationActionsAndWfInfo"
                  text={this.i18n('emptyValues')} />
              </Basic.LabelWrapper>

              <Basic.TextField
                ref="minPasswordLength"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.minPasswordLength.label')} />

              <Basic.TextField
                ref="maxPasswordLength"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.maxPasswordLength.label')} />

              <Basic.TextField
                ref="minUpperChar"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.minUpperChar.label')} />

              <Basic.TextField
                ref="minLowerChar"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.minLowerChar.label')} />

              <Basic.TextField
                ref="minNumber"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.minNumber.label')} />

              <Basic.TextField
                ref="minSpecialChar"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                label={this.i18n('entity.PasswordPolicy.minSpecialChar.label')} />

              <Basic.TextField
                ref="maxPasswordAge"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                hidden={!validateType}
                helpBlock={this.i18n('entity.PasswordPolicy.maxPasswordAge.help')}
                label={this.i18n('entity.PasswordPolicy.maxPasswordAge.label')} />

              <Basic.TextField
                ref="minPasswordAge"
                type="number"
                validation={ Utils.Ui.getIntegerValidation() }
                hidden={!validateType}
                helpBlock={this.i18n('entity.PasswordPolicy.minPasswordAge.help')}
                label={this.i18n('entity.PasswordPolicy.minPasswordAge.label')} />

              <Basic.TextField
                ref="maxHistorySimilar"
                type="number"
                hidden={!showDefaultAdvanced}
                validation={ Utils.Ui.getIntegerValidation() }
                helpBlock={this.i18n('entity.PasswordPolicy.maxHistorySimilar.help')}
                label={this.i18n('entity.PasswordPolicy.maxHistorySimilar.label')} />

              <Basic.TextField
                ref="blockLoginTime"
                type="number"
                hidden={!showDefaultAdvanced}
                validation={ Utils.Ui.getIntegerValidation() }
                helpBlock={this.i18n('entity.PasswordPolicy.blockLoginTime.help')}
                label={this.i18n('entity.PasswordPolicy.blockLoginTime.label')} />

              <Basic.TextField
                ref="maxUnsuccessfulAttempts"
                type="number"
                hidden={!showDefaultAdvanced}
                validation={ Utils.Ui.getIntegerValidation() }
                helpBlock={this.i18n('entity.PasswordPolicy.maxUnsuccessfulAttempts.help')}
                label={this.i18n('entity.PasswordPolicy.maxUnsuccessfulAttempts.label')} />
            </Basic.AbstractForm>
          </Basic.PanelBody>
          <Basic.PanelFooter showLoading={showLoading} className="noBorder">
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
            {
              isNew
              ?
              <Basic.SplitButton level="success" title={this.i18n('button.createContinue')}
                onClick={this.save.bind(this, 'CREATE_CONTINUE')}
                rendered={SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}
                showLoading={showLoading} pullRight dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'CREATE')}>{this.i18n('button.create')}</Basic.MenuItem>
              </Basic.SplitButton>
              :
              <Basic.SplitButton level="success" title={this.i18n('button.saveAndContinue')}
                onClick={this.save.bind(this, 'SAVE_CONTINUE')}
                rendered={SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}
                showLoading={showLoading} pullRight dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'SAVE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
              </Basic.SplitButton>
            }
          </Basic.PanelFooter>
        </Basic.Panel>
        {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
        <input type="submit" className="hidden"/>
      </form>
    );
  }
}

PasswordPolicyBasic.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
  isNew: PropTypes.bool
};
PasswordPolicyBasic.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: passwordPolicyManager.getEntity(state, entityId),
    _showLoading: passwordPolicyManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(PasswordPolicyBasic);
