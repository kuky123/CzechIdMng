import React from 'react';
import PropTypes from 'prop-types';

const PROPERTY_RENDERED = 'rendered';
const PROPERTY_SHOW_LOADING = 'showLoading';

/**
 * Super class for all components in application.
 *
 * @author Radek Tomiška
 */
export default class AbstractComponent extends React.Component {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * Returs true, if given component supports given property (by propTypes)
   *
   * @param  {AbstractComponent} component
   * @param  {string} supportedProperty
   * @return {bool}
   */
  static supportsProperty(component, supportedProperty) {
    for (const property in component.propTypes) {
      if (property === supportedProperty) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returs true, if given component supports rendered property (by propTypes)
   *
   * @param  {AbstractComponent} component
   * @return {bool}
   */
  static supportsRendered(component) {
    return AbstractComponent.supportsProperty(component, PROPERTY_RENDERED);
  }

  /**
   * Returs true, if given component supports showLoading property (by propTypes)
   *
   * @param  {AbstractComponent} component
   * @return {bool}
   */
  static supportsShowLoading(component) {
    return AbstractComponent.supportsProperty(component, PROPERTY_SHOW_LOADING);
  }
}

AbstractComponent.propTypes = {
  /**
   * loadinig indicator
   */
  [PROPERTY_SHOW_LOADING]: PropTypes.bool,
  /**
   * If component is rendered on page
   */
  [PROPERTY_RENDERED]: PropTypes.bool
};

AbstractComponent.defaultProps = {
  [PROPERTY_SHOW_LOADING]: false,
  [PROPERTY_RENDERED]: true
};
