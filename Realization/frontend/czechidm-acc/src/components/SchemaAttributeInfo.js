import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { Link } from 'react-router';
//
import { Basic, Utils } from 'czechidm-core';
import { SchemaAttributeManager } from '../redux';

const manager = new SchemaAttributeManager();

export class SchemaAttributeInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    this._loadEntityIfNeeded();
  }

  componentDidUpdate() {
    this._loadEntityIfNeeded();
  }

  /**
   * if id is setted and entity is not - then load entity
   */
  _loadEntityIfNeeded() {
    const { entity, _entity, entityIdentifier } = this.props;
    if (entityIdentifier && !entity && !_entity) {
      const uiKey = manager.resolveUiKey(null, entityIdentifier);
      if (!Utils.Ui.isShowLoading(this.context.store.getState(), uiKey)
          && !Utils.Ui.getError(this.context.store.getState(), uiKey)) { // show loading check has to be here - new state is needed
        this.context.store.dispatch(manager.fetchEntityIfNeeded(entityIdentifier, null, () => {}));
      }
    }
  }

  render() {
    const { rendered, showLoading, className, entityIdentifier, entity, _showLoading } = this.props;
    //
    if (!rendered) {
      return null;
    }
    let _entity = this.props._entity;
    if (entity) { // entity prop has higher priority
      _entity = entity;
    }
    //
    const panelClassNames = classNames(
      className
    );
    if (showLoading || (_showLoading && entityIdentifier && !_entity)) {
      return (
        <Basic.Icon value="refresh" showLoading/>
      );
    }
    if (!_entity || !_entity._embedded) {
      return (<span title={entityIdentifier}>n/a</span>); // deleted mapping
    }
    //
    const systemId = _entity._embedded.objectClass.system.id;
    return (
      <Link to={`/system/${systemId}/schema-attributes/${entityIdentifier}/detail`} className={panelClassNames} title="Zobrazit detail atributu schématu">{_entity.name}</Link>
    );
  }
}

SchemaAttributeInfo.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Selected entity
   */
  entity: PropTypes.object,
  /**
   * Selected entity's id - entity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  /**
   * Internal identity loaded by given username
   */
  _entity: PropTypes.object,
  _showLoading: PropTypes.bool
};
SchemaAttributeInfo.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  entity: null,
  _showLoading: true
};

function select(state, component) {
  return {
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier)
  };
}
export default connect(select)(SchemaAttributeInfo);
