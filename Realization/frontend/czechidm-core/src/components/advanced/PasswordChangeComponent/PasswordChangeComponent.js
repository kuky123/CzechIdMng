import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Utils from '../../../utils';
import * as Basic from '../../basic';
import PasswordField from '../PasswordField/PasswordField';
import ValidationMessage from '../ValidationMessage/ValidationMessage';
import { IdentityService } from '../../../services';
import { IdentityManager, SecurityManager, ConfigurationManager } from '../../../redux';

const IDM_NAME = Utils.Config.getConfig('app.name', 'CzechIdM');
const RESOURCE_IDM = `0:${IDM_NAME}`;

const PASSWORD_DOES_NOT_MEET_POLICY = 'PASSWORD_DOES_NOT_MEET_POLICY';
const PASSWORD_PREVALIDATION = 'PASSWORD_PREVALIDATION';

/**
 * Basic password change fields (old password and new password) and validation message
 *
 * @author Ondřej Kopr
 */

const identityManager = new IdentityManager();
const identityService = new IdentityService();
const securityManager = new SecurityManager();

class PasswordChangeComponent extends Basic.AbstractFormComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      preload: true,
      showLoading: false
    };
  }

  getComponentKey() {
    return 'content.identity.passwordChange';
  }

  componentDidMount() {
    if (this.props._permissions !== undefined) {
      this._initForm(this.props._permissions);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps._permissions !== undefined && !_.isEqual(this.props._permissions, nextProps._permissions)) {
      this._initForm(nextProps._permissions);
    }
  }

  _initForm(permissions) {
    const { accountOptions } = this.props;
    if (this._canPasswordChange(permissions)) {
      this.setState({
        preload: false
      }, () => {
        this.refs.form.setData({
          accounts: accountOptions,
          oldPassword: ''
        });
        // focus old password
        this.refs.oldPassword.focus();
      });
    }
    this._preValidate(accountOptions);
  }

  /**
   * Return true when currently logged user can change password
   *
   */
  _canPasswordChange(permissions) {
    const { passwordChangeType } = this.props;
    //
    return identityManager.canChangePassword(passwordChangeType, permissions);
  }

  /*
   * Method shows password rules before applying change of password
   */
  _preValidate(options) {
    const requestData = {
      accounts: []
    };

    options.forEach(resourceValue => {
      if (resourceValue.value === RESOURCE_IDM) {
        requestData.idm = true;
      } else {
        requestData.accounts.push(resourceValue.value);
      }
    });
    identityManager.preValidate(requestData)
    .then(response => {
      if (response.status === 204) {
        const error = undefined;
        this.setState({
          validationError: error,
          validationDefinition: true
        });

        throw error;
      }
      return response.json();
    })
    .then(json => {
      let error;
      if (Utils.Response.getFirstError(json)) {
        error = Utils.Response.getFirstError(json);
      } else if (json._errors) {
        error = json._errors.pop();
      }

      if (error) {
        this.setState({
          validationError: error,
          validationDefinition: true
        });

        throw error;
      }
      return json;
    })
    .catch(error => {
      if (!error) {
        return {};
      }
      if (error.statusEnum === PASSWORD_PREVALIDATION) {
        this.addErrorMessage({hidden: true}, error);
      } else {
        this.addError(error);
      }
    });
  }

  save(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const { entityId,
      userContext,
      passwordChangeType } = this.props;
    const formData = this.refs.form.getData();

    // add data from child component to formData
    formData.newPassword = this.refs.passwords.getValue();

    if (!this.refs.passwords.validate()) {
      return;
    }

    this.setState({
      showLoading: true
    }, this.refs.form.processStarted());
    //
    const requestData = {
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
      accounts: []
    };
    if (passwordChangeType === IdentityManager.PASSWORD_ALL_ONLY && !SecurityManager.isAdmin(userContext)) {
      requestData.all = true;
      requestData.idm = true;
    } else {
      formData.accounts.map(resourceValue => {
        if (resourceValue === RESOURCE_IDM) {
          requestData.idm = true;
        } else {
          requestData.accounts.push(resourceValue);
        }
      });
    }
    //
    identityService.passwordChange(entityId, requestData)
    .then(response => {
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      if (response.status === 204) {
        return {};
      }
      return response.json();
    })
    .then(json => {
      if (Utils.Response.hasError(json)) {
        const error = Utils.Response.getFirstError(json);

        this.setState({
          validationError: error,
          validationDefinition: false
        });

        throw error;
      }
      return json;
    })
    .then(json => {
      const successAccounts = [];
      const failedAccounts = [];
      json.forEach(result => {
        const account = result.model.parameters.account;
        const accountName = `${account.idm ? IDM_NAME : account.systemName} (${account.uid})`;
        //
        if (result.model.statusCode === 200) { // success
          successAccounts.push(accountName);
        } else {
          failedAccounts.push(accountName);
        }
      });
      if (successAccounts.length > 0) {
        this.addMessage({ message: this.i18n('message.success', { accounts: successAccounts.join(', '), username: entityId }) });
      }
      if (failedAccounts.length > 0) {
        this.addMessage({ level: 'warning', message: this.i18n('message.failed', { accounts: failedAccounts.join(', '), username: entityId }) });
      }

      this.setState({
        validationError: null
      }, () => {
        // new token has to be set to security to prevent user logout
        this.context.store.dispatch(securityManager.reloadToken());
        //
        this.refs.form.processEnded();
        //
        // we want clear password input after change
        this.refs.form.setData({
          accounts: formData.accounts,
          oldPassword: null,
          newPassword: null,
          newPasswordAgain: null
        });
        this._preValidate(this.props.accountOptions);
      });
    })
    .catch(error => {
      if (error.statusEnum === PASSWORD_DOES_NOT_MEET_POLICY) {
        this.addErrorMessage({hidden: true}, error);
      } else {
        this.addError(error);
        this._preValidate(this.props.accountOptions);
      }

      this.refs.form.setData({
        accounts: formData.accounts,
        oldPassword: formData.oldPassword
      });
      this.refs.passwords.setValue(formData.newPassword);
    });
  }


  focus() {
    this.refs.oldPassword.focus();
  }

  render() {
    const {
      rendered,
      passwordChangeType,
      _permissions,
      userContext,
      accountOptions,
      entityId,
      requireOldPasswordConfig
    } = this.props;
    const { preload, validationError, validationDefinition } = this.state;
    //
    if (!rendered && rendered !== undefined) {
      return null;
    }
    //
    const allOnlyWarningClassNames = classnames(
      'form-group',
      { 'hidden': passwordChangeType !== IdentityManager.idm || SecurityManager.isAdmin(userContext) }
    );
    //
    // if current user is admin, old password is never required
    let oldPasswordRequired = (entityId === userContext.username) && !SecurityManager.isAdmin(userContext);
    if (oldPasswordRequired) {
      oldPasswordRequired = requireOldPasswordConfig;
    }
    //
    const accountsExits = accountOptions.length !== 0;
    //
    const content = [];
    if (this._canPasswordChange(_permissions) && !preload) {
      content.push(
        <Basic.Alert
          icon="info-sign"
          text={this.i18n('message.isAdmin')}
          rendered={ SecurityManager.isAdmin(userContext) }
          style={{ margin: '15px 0'}}/>
      );
      content.push(
        <ValidationMessage error={validationError} validationDefinition={validationDefinition} />
      );

      content.push(
        <Basic.AbstractForm ref="form">
          <Basic.TextField type="password" ref="oldPassword" label={this.i18n('password.old')}
            hidden={!oldPasswordRequired}
            disabled={!accountsExits}
            required={oldPasswordRequired}/>

          <PasswordField className="form-control" ref="passwords" disabled={!accountsExits}/>

          <Basic.EnumSelectBox
            ref="accounts"
            label={this.i18n('accounts.label')}
            placeholder={this.i18n('accounts.placeholder')}
            multiSelect
            options={accountOptions}
            required
            disabled={(passwordChangeType === IdentityManager.PASSWORD_ALL_ONLY && !SecurityManager.isAdmin(userContext)) || !accountsExits}
            onChange={ this._preValidate.bind(this) }/>

          <div className={allOnlyWarningClassNames}>
            <Basic.Alert key="changeAllOnly" icon="exclamation-sign" text={this.i18n('changeType.ALL_ONLY')} className="last no-margin"/>
          </div>
        </Basic.AbstractForm>
      );
      content.push(
        <Basic.PanelFooter>
          <Basic.Button
            type="submit"
            level="success"
            disabled={!accountsExits}
            showLoading={this.state.showLoading}>{this.i18n('button.change')}
          </Basic.Button>
        </Basic.PanelFooter>
      );
    }
    //
    return (
      <form onSubmit={this.save.bind(this)}>
        <Basic.Panel className="no-border">
          <Basic.PanelHeader text={this.i18n('header')}/>
          <Basic.Loading className="static" showLoading={preload && this._canPasswordChange(_permissions)}/>
          <Basic.Alert
            level="warning"
            icon="exclamation-sign"
            text={ this.i18n('changeType.DISABLED') }
            rendered={ passwordChangeType === IdentityManager.PASSWORD_DISABLED && !SecurityManager.isAdmin(userContext)}/>
          <Basic.Alert
            level="warning"
            icon="exclamation-sign"
            text={ this.i18n('permission.failed') }
            rendered={ _permissions !== undefined && !this._canPasswordChange(_permissions) && passwordChangeType !== IdentityManager.PASSWORD_DISABLED }/>
          <Basic.Alert
            level="info"
            icon="exclamation-sign"
            rendered={ !accountsExits }
            text={ this.i18n('message.noAccounts') }/>
          { content }
        </Basic.Panel>
      </form>
    );
  }
}

PasswordChangeComponent.propTypes = {
  requireOldPassword: PropTypes.bool,
  userContext: PropTypes.object,
  accountOptions: PropTypes.object,
  entityId: PropTypes.string,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
PasswordChangeComponent.defaultProps = {
  requireOldPassword: true,
  userContext: null,
  _permissions: undefined
};

function select(state, component) {
  return {
    passwordChangeType: ConfigurationManager.getPublicValue(state, 'idm.pub.core.identity.passwordChange'),
    requireOldPasswordConfig: ConfigurationManager.getPublicValueAsBoolean(state, 'idm.pub.core.identity.passwordChange.requireOldPassword'),
    _permissions: identityManager.getPermissions(state, null, component.entityId)
  };
}

export default connect(select, null, null, { withRef: true})(PasswordChangeComponent);
