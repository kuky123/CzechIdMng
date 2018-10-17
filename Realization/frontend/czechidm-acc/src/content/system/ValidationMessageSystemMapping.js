import React from 'react';
import PropTypes from 'prop-types';
//
import { Basic } from 'czechidm-core';

/**
* Message for system mapping validation
*
* @author Patrik Stloukal
*/
const SYSTEM_MAPPING_VALIDATION = 'SYSTEM_MAPPING_VALIDATION';

export default class ValidationMessageSystemMapping extends Basic.AbstractFormComponent {

  constructor(props) {
    super(props);
  }

  componentDidMount() {
    const { error } = this.props;
    this._prepareValidationMessage(error);
  }

  componentWillReciveNewProps(nextProps) {
    const { error } = this.props;

    if (error !== nextProps.error) {
      this._prepareValidationMessage(nextProps.error);
    }
  }


  _prepareValidationMessage(error) {
    if (!error || !error.parameters) {
      return null;
    }

    if (error.statusEnum !== SYSTEM_MAPPING_VALIDATION) {
      return null;
    }

    const validationMessage = [];

    for (const key in error.parameters) {
      if (error.parameters.hasOwnProperty(key)) {
        validationMessage.push(
          <Basic.Alert level="warning" className="no-margin">
            <div>{this.i18n(`acc:content.system.mappingDetail.validation.${key}.message`)}</div>
            <div>{this.i18n(`acc:content.system.mappingDetail.validation.${key}.info`)}</div>
          </Basic.Alert>);
      }
    }
    return validationMessage;
  }

  render() {
    const { rendered, error } = this.props;
    if (!rendered || !error) {
      return null;
    }
    const validation = this._prepareValidationMessage(error);


    return (
      <div>
        {validation}
      </div>
    );
  }
  }

ValidationMessageSystemMapping.propTypes = {
  ...Basic.AbstractFormComponent.propTypes,
  error: PropTypes.object
};

ValidationMessageSystemMapping.defaultProps = {
  ...Basic.AbstractFormComponent.defaultProps
};
