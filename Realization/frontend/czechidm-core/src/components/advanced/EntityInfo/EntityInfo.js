import React, { PropTypes } from 'react';
import _ from 'lodash';
import classnames from 'classnames';
//
import * as Basic from '../../basic';
import ComponentService from '../../../services/ComponentService';
import UuidInfo from '../UuidInfo/UuidInfo';
//
const componentService = new ComponentService();

/**
 * Show entity info by given type and identifier.
 *
 * @author Radek Tomiška
 */
export default class EntityInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * Returns entity info component by given type
   *
   * @param  {string} entityType
   * @return {object} component or null
   */
  static _getComponent(entityType) {
    if (!entityType) {
      return null;
    }
    return componentService.getComponentDefinitions(ComponentService.ENTITY_INFO_COMPONENT_TYPE).find(component => {
      if (!component.entityType) {
        return false;
      }
      // multiple types
      if (_.isArray(component.entityType)) {
        for (const entityTypeItem of component.entityType) {
          if (entityTypeItem.toLowerCase() === entityType.toLowerCase()) {
            return true;
          }
        }
        return false;
      }
      // single value
      return component.entityType.toLowerCase() === entityType.toLowerCase();
    });
  }

  /**
   * Returns entity's nice label. Useful for localization params etc.
   *
   * @param  {string} entityType
   * @param  {entity} entity
   * @return {string}
   */
  static getNiceLabel(entityType, entity) {
    if (!entityType || !entity) {
      return null;
    }
    //
    const component = EntityInfo._getComponent(entityType);
    if (!component || !component.manager) {
      return null;
    }
    const ManagerType = component.manager;
    const manager = new ManagerType();
    return manager.getNiceLabel(entity);
  }

  render() {
    const { rendered, showLoading, entity, entityType, entityIdentifier, face, style, className, showLink, showEntityType } = this.props;
    // standard rendered - we dont propagate rendered to underliyng component
    if (!rendered) {
      return null;
    }
    // we don't have anything to render
    if (!entityType || (!entity && !entityIdentifier)) {
      return null;
    }
    //
    const classNames = classnames(
      'entity-info',
      className
    );
    // find underliyng component by entity type
    const component = EntityInfo._getComponent(entityType);
    if (component) {
      const EntityInfoComponent = component.component;
      return (
        <span>
          <EntityInfoComponent
            entity={ entity }
            entityIdentifier={ entityIdentifier }
            face={ face }
            style={ style}
            className={ classNames }
            showLoading={ showLoading }
            showLink={ showLink }/>
        </span>
      );
    }
    //
    // entity type is not registered
    this.getLogger().debug(`[Advanced.EntityInfo]: Entity info for type [${entityType}] is not supported.`);
    return (
      <div
        style={ style }
        className={ classNames }>
        {
          !showEntityType
          ||
          <span className="entity-type-wrapper">
            <span className="entity-type-label">
              { entityType }
            </span>
            <span className="entity-type-separator">
              :
            </span>
          </span>
        }
        <UuidInfo value={ entityIdentifier }/>
      </div>
    );
  }
}

EntityInfo.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Selected entity - externally loaded.  Has higher priority, when is given, then loading is not needed.
   */
  entity: PropTypes.object,
  /**
   * Entity type (e.g. identity, role ...)
   */
  entityType: PropTypes.string.isRequired,
  /**
   * Entity identifier
   */
  entityIdentifier: PropTypes.string.isRequired,
  /**
   * Decorator
   */
  face: PropTypes.oneOf(['full', 'popover', 'link', 'text']),
  /**
   * Shows link to full entity detail (if currently logged user has appropriate permission)
   */
  showLink: PropTypes.bool,
  /**
   * Shows entity type, when no entity info component is found.
   * Set to `false` when type is rendered extrnally (e.g. in different table column)
   */
  showEntityType: PropTypes.bool
};
EntityInfo.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  face: 'full',
  showLink: true,
  showEntityType: true
};