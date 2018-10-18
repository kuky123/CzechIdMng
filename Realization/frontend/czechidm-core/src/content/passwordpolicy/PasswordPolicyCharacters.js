import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { PasswordPolicyManager, SecurityManager } from '../../redux';
import PasswordPolicyIdentityAttributeEnum from '../../enums/PasswordPolicyIdentityAttributeEnum';
import PasswordPolicyTypeEnum from '../../enums/PasswordPolicyTypeEnum';

/**
* Character detail for password policy
* Bases and prohibited characters
*/

const passwordPolicyManager = new PasswordPolicyManager();

class PasswordPolicyCharacters extends Basic.AbstractContent {

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
    this.selectNavigationItems(['system', 'password-policies', 'password-policies-characters']);
    const { entityId } = this.props.params;
    this.getLogger().debug(`[TypeContent] loading entity detail [id:${entityId}]`);
    this.context.store.dispatch(passwordPolicyManager.fetchEntity(entityId));
  }

  /**
  * Method check if props in this component is'nt different from new props.
  */
  componentWillReceiveProps(nextProps) {
    // check id of old and new entity
    if (!this.props.entity || nextProps.entity.id !== this.props.entity.id || nextProps.entity.id !== this.refs.form.getData().id) {
      this._initForm(nextProps.entity);
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
    identityAttributeCheck = _.join(identityAttributeCheck, ', ');
    entity.identityAttributeCheck = identityAttributeCheck;

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

  _isValidateType(entity) {
    return !entity || PasswordPolicyTypeEnum.findSymbolByKey(entity.type) === PasswordPolicyTypeEnum.VALIDATE;
  }

  render() {
    const { uiKey, entity } = this.props;
    const { showLoading } = this.state;
    return (
      <div>
        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className="no-border last">
            <Basic.PanelHeader text={this.i18n('content.passwordPolicies.characters.title')} />
            <Basic.PanelBody style={{ padding: 0 }}>
              <Basic.AbstractForm
                ref="form"
                uiKey={uiKey}
                readOnly={!SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'PASSWORDPOLICY_CREATE' : 'PASSWORDPOLICY_UPDATE')}
                showLoading={entity === null}>
                <Basic.TextField
                  ref="prohibitedCharacters"
                  helpBlock={this.i18n('entity.PasswordPolicy.prohibitedCharacters.help')}
                  label={this.i18n('entity.PasswordPolicy.prohibitedCharacters.label')} />

                <Basic.Checkbox
                  ref="weakPassRequired"
                  hidden
                  label={this.i18n('entity.PasswordPolicy.weakPassRequired.label')}/>
                <Basic.TextField
                  ref="weakPass"
                  hidden
                  label={this.i18n('entity.PasswordPolicy.weakPass.label')} />

                <div hidden={this._isValidateType(entity)}>
                  <Basic.LabelWrapper label=" ">
                    <Basic.Alert
                      className="no-margin"
                      icon="exclamation-sign"
                      key="passwordPolicyBasesInfo"
                      text={this.i18n('entity.PasswordPolicy.bases.help')} />
                  </Basic.LabelWrapper>

                  <Basic.TextField
                    ref="lowerCharBase"
                    label={this.i18n('entity.PasswordPolicy.lowerCharBase.label')}
                    required/>
                  <Basic.TextField
                    ref="upperCharBase"
                    label={this.i18n('entity.PasswordPolicy.upperCharBase.label')}
                    required/>
                  <Basic.TextField
                    ref="numberBase"
                    label={this.i18n('entity.PasswordPolicy.numberBase.label')}
                    required/>
                </div>

                <Basic.TextField
                  ref="specialCharBase"
                  label={this.i18n('entity.PasswordPolicy.specialCharBase.label')}
                  required/>
              </Basic.AbstractForm>
            </Basic.PanelBody>
            <Basic.PanelFooter showLoading={showLoading} >
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

PasswordPolicyCharacters.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
};
PasswordPolicyCharacters.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: passwordPolicyManager.getEntity(state, entityId),
    showLoading: passwordPolicyManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(PasswordPolicyCharacters);
