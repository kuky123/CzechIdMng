import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
//
import * as Basic from '../../basic';
import DateValue from '../DateValue/DateValue';

/**
 * Parameters in errors. Contain names of not success policies
 * @type {String}
 */
const PASSWORD_POLICIES_NAMES = 'policiesNames';

/**
 * Enchanced control, minimal rules to fulfill.
 * Value of parameter MIN_RULES_TO_FULFILL is map with rules
 * @type {String}
 */
const MIN_RULES_TO_FULFILL = 'minRulesToFulfill';

/**
 * Enchanced control, count with minimal rules to fulfill
 * @type {String}
 */
const MIN_RULES_TO_FULFILL_COUNT = 'minRulesToFulfillCount';

/**
 * Special character base for each required policies
 * @type {String}
 */
const SPECIAL_CHARACTER_BASE = 'specialCharacterBase';

/**
 * Forbidden character base for each required policies
 * @type {String}
 */
const FORBIDDEN_CHARACTER_BASE = 'forbiddenCharacterBase';

/**
 * Merge similar password lines
 * @type {String}
 */
const PWD_SIMILAR = 'passwordSimilarPreValidate';

/**
 * Error message with date, date is format by local
 * @type {String}
 */
const DATE = 'date';

/**
 * Array with all validation message parameters, when you add some validation warrning into BE,
 * you also must add this parameters into this arrray.
 * @type {Array}
 */
const VALIDATION_WARNINGS = ['minLength', 'maxLength', 'minUpperChar',
'minLowerChar', 'minNumber', 'minSpecialChar', 'prohibited', 'weakPass',
'minRulesToFulfill', 'minRulesToFulfillCount', 'policiesNames',
'passwordSimilarUsername', 'passwordSimilarEmail', 'passwordSimilarFirstName',
'passwordSimilarLastName', 'maxHistorySimilar',
'passwordSimilarUsernamePreValidate', 'passwordSimilarEmailPreValidate',
'passwordSimilarFirstNamePreValidate', 'passwordSimilarLastNamePreValidate'];

/**
 * @author Ondřej Kopr
 * @author Patrik Stloukal
 */
export default class ValidationMessage extends Basic.AbstractFormComponent {

  constructor(props) {
    super(props);
  }

  componentDidMount() {
    const { error, validationDefinition } = this.props;
    this._prepareValidationMessage(error, validationDefinition);
  }

  componentWillReciveNewProps(nextProps) {
    const { error, validationDefinition } = this.props;

    if (error !== nextProps.error || validationDefinition !== nextProps.validationDefinition) {
      this._prepareValidationMessage(nextProps.error, nextProps.validationDefinition);
    }
  }

  _showCharacterBase(parameter, type) {
    const rules = [];
    const all = [];
    all.push(this.i18n('content.passwordPolicies.validation.' + type + '.list'));
    for (const ruleKey in parameter) {
      if (parameter.hasOwnProperty(ruleKey)) {
        rules.push(_.size(parameter) === 1 ? parameter[ruleKey] : ruleKey + ':  ' + parameter[ruleKey]);
      }
    }
    all.push(this._pointList(rules));
    return all;
  }

  /**
   * Method prepare error from password validation.
   * Only validation message is required, other will be skiped.
   */
  _prepareValidationMessage(error) {
    if (!error || !error.parameters) {
      return null;
    }
    // For pre validate it shows as info (blue)
    const levelWarning = `warning`;
    const levelDanger = `danger`;

    const validationMessage = [];

    // iterate over all parameters in error
    for (const key in error.parameters) {
      // error prameters must contain key and VALIDATION_WARNINGS must also contain key
      if (error.parameters.hasOwnProperty(key) && _.indexOf(VALIDATION_WARNINGS, key) !== -1) {
        // enchanced control special message, minimal rules to fulfill
        if (key === MIN_RULES_TO_FULFILL) {
          // fill rules with not required messages
          const rules = [];
          rules.push(this.i18n('content.passwordPolicies.validation.' + MIN_RULES_TO_FULFILL, {'count': error.parameters[MIN_RULES_TO_FULFILL_COUNT]} ));
          const rule = [];
          for (const ruleKey in error.parameters[key]) {
            if (error.parameters[key].hasOwnProperty(ruleKey)) {
              rule.push(this.i18n('content.passwordPolicies.validation.' + ruleKey) + error.parameters[key][ruleKey]);
            }
          }
          rules.push(this._pointList(rule));
          validationMessage.push(
            <Basic.Alert level={levelWarning} className="no-margin">
              <span>
                {rules}
              </span>
            </Basic.Alert>);
        } else if (key !== PASSWORD_POLICIES_NAMES) {
          // set all attributes as parameter zero
          // validation message with date
          if (key === DATE) {
            validationMessage.push(
              <Basic.Alert level={levelWarning} className="no-margin">
                {this.i18n('content.passwordPolicies.validation.' + key, { 0: <DateValue value={error.parameters[key]}/> } )}
              </Basic.Alert>
            );
          } else {
            // other validation messages
            validationMessage.push(
              <Basic.Alert level={levelWarning} className="no-margin">
                {this.i18n('content.passwordPolicies.validation.' + key, { 0: error.parameters[key] })}
              </Basic.Alert>
            );
          }
        }
      }
    }
    // first message is password policies names, with danger class
    if (error.parameters.hasOwnProperty(PASSWORD_POLICIES_NAMES)) {
      validationMessage.unshift(
        <Basic.Alert level={levelDanger} className="no-margin">
          {this.i18n('content.passwordPolicies.validation.' + PASSWORD_POLICIES_NAMES) + error.parameters[PASSWORD_POLICIES_NAMES]}
        </Basic.Alert>
      );
    }
    //
    return validationMessage;
  }

  _pointList(field, secondFiled) {
    const listItems = field.map((number) =>
      <li>{number}</li>
    );
    let secondListItems;
    if (secondFiled !== undefined) {
      secondListItems = secondFiled.map((number) =>
        <li>{number}</li>
      );
    }
    return (
      <ul style={{ paddingLeft: 20 }}>{listItems} {secondListItems}</ul>
    );
  }

  _preparePreValidationMessage(error) {
    if (!error || !error.parameters) {
      return null;
    }

    const validationMessage = [];
    const lines = []; // every one of these rules must be met
    const rules = []; // one of two following rules must be met....
    const charBase = []; // for shown special character base
    const forbiddenBase = []; // for shown forbidden character base
    const similar = []; // for merging pwd must not be similar to name, mail, username
    const bases = [];

    // iterate over all parameters in error
    for (const key in error.parameters) {
      // error prameters must contain key and VALIDATION_WARNINGS must also contain key
      if (error.parameters.hasOwnProperty(key) && _.indexOf(VALIDATION_WARNINGS, key) !== -1) {
        // enchanced control special message, minimal rules to fulfill
        if (key === MIN_RULES_TO_FULFILL) {
          // fill rules with not required messages
          rules.push(this.i18n('content.passwordPolicies.validation.' + MIN_RULES_TO_FULFILL, {'count': error.parameters[MIN_RULES_TO_FULFILL_COUNT]} ));
          const rule = [];
          for (const ruleKey in error.parameters[key]) {
            if (error.parameters[key].hasOwnProperty(ruleKey)) {
              rule.push(this.i18n('content.passwordPolicies.validation.' + ruleKey) + error.parameters[key][ruleKey]);
            }
          }
          rules.push(this._pointList(rule));
          // to merge - pwd must not be similar to name, mail, username
        } else if (key === 'passwordSimilarUsernamePreValidate' || key === 'passwordSimilarEmailPreValidate' || key === 'passwordSimilarFirstNamePreValidate' || key === 'passwordSimilarLastNamePreValidate') {
          similar.push(this.i18n('content.passwordPolicies.validation.' + key));
        } else if ( key !== MIN_RULES_TO_FULFILL_COUNT ) {
          // other validation messages
          lines.push(this.i18n('content.passwordPolicies.validation.' + key, { 0: error.parameters[key] }));
        }
      }
      if (key === SPECIAL_CHARACTER_BASE) {
        if (_.size(error.parameters[key]) === 1) {
          for (const ruleKey in error.parameters[key]) {
            if (Object.prototype.hasOwnProperty.call(error.parameters[key], ruleKey)) {
              bases.push(this.i18n('content.passwordPolicies.validation.' + SPECIAL_CHARACTER_BASE + '.text') + error.parameters[key][ruleKey]);
            }
          }
        } else {
          charBase.push(this._showCharacterBase(error.parameters[key], SPECIAL_CHARACTER_BASE, validationMessage, `info`));
        }
      }
      if (key === FORBIDDEN_CHARACTER_BASE) {
        if (_.size(error.parameters[key]) === 1) {
          for (const ruleKey in error.parameters[key]) {
            if (Object.prototype.hasOwnProperty.call(error.parameters[key], ruleKey)) {
              bases.push(this.i18n('content.passwordPolicies.validation.' + FORBIDDEN_CHARACTER_BASE + '.text') + error.parameters[key][ruleKey]);
            }
          }
        } else {
          forbiddenBase.push(this._showCharacterBase(error.parameters[key], FORBIDDEN_CHARACTER_BASE, validationMessage, `info`));
        }
      }
    }
    if (similar.length > 0) {
      lines.push(this.i18n('content.passwordPolicies.validation.' + PWD_SIMILAR) + ' ' + similar.join(', ') + '.');
    }
    const result = [];
    result.push(this._pointList(lines, bases));
    result.push(rules);
    result.push(charBase);
    result.push(forbiddenBase);
    validationMessage.push(
      <span>
        { result }
      </span>
    );
    return validationMessage;
  }

  _preparePreValidationComponent(errorMessage) {
    return (
      <Basic.Alert
        icon="info-sign"
        text={ this.i18n('content.passwordPolicies.validation.passwordHintPreValidate') }
        className="no-margin">
        <Basic.Popover
          ref="popover"
          trigger={['click']}
          value={
            <Basic.Panel level="info">
              <Basic.PanelHeader>
                {this.i18n('content.passwordPolicies.validation.passwordHintPreValidateHeader')}
              </Basic.PanelHeader>
              <Basic.PanelBody>
                {this._preparePreValidationMessage(errorMessage)}
              </Basic.PanelBody>
            </Basic.Panel>
          }
          className="abstract-entity-info-popover"
          placement="right">
          {
            <Basic.Button
              level="link"
              style={{ padding: 0, whiteSpace: 'normal', verticalAlign: 'baseline' }}
              title={ this.i18n('content.passwordPolicies.validation.prevalidationLink.title') }>
              { this.i18n('content.passwordPolicies.validation.passwordHintPreValidatePwd') }
            </Basic.Button>
          }
        </Basic.Popover>
      </Basic.Alert>
    );
  }

  render() {
    const { rendered, error, validationDefinition } = this.props;
    if (!rendered || !error) {
      return null;
    }

    let validation;
    if (validationDefinition) {
      validation = this._preparePreValidationComponent(error);
    } else {
      validation = this._prepareValidationMessage(error);
    }

    return (
      <div>
        {validation}
      </div>
    );
  }
}

ValidationMessage.propTypes = {
  ...Basic.AbstractFormComponent.propTypes,
  error: PropTypes.object,
  validationDefinition: PropTypes.object
};

ValidationMessage.defaultProps = {
  ...Basic.AbstractFormComponent.defaultProps
};
