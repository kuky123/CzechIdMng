import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { connect } from 'react-redux';
import { Link } from 'react-router';
//
import * as Basic from '../../basic';
import { WorkflowHistoricProcessInstanceManager } from '../../../redux/';
import UuidInfo from '../UuidInfo/UuidInfo';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';

const manager = new WorkflowHistoricProcessInstanceManager();

/**
 * WorkflowProcess basic information (info card)
 *
 * @author Švanda
 */
export class WorkflowProcessInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    return true;
  }

  /**
   * Get link to detail (`url`).
   *
   * @return {string}
   */
  getLink() {
    const { entityIdentifier, entity } = this.props;
    if (entity && entity.id) {
      return `/workflow/history/processes/${entity.id}`;
    }
    return `/workflow/history/processes/${entityIdentifier}`;
  }

  /**
   * TODO: implement different face
   */
  render() {
    const { rendered, showLoading, className, entity, entityIdentifier, _showLoading, style } = this.props;
    //
    if (!rendered) {
      return null;
    }
    let _entity = this.props._entity;
    if (entity) { // entity prop has higher priority
      _entity = entity;
    }
    //
    const classNames = classnames(
      'wf-info',
      className
    );
    if (showLoading || (_showLoading && entityIdentifier && !_entity)) {
      return (
        <Basic.Icon className={ classNames } value="refresh" showLoading style={style}/>
      );
    }
    if (!_entity) {
      if (!entityIdentifier) {
        return null;
      }
      return (<UuidInfo className={ classNames } value={ entityIdentifier } style={style}/>);
    }
    //
    const niceLabel = this.getManager().localize(_entity, 'name');
    if (!this.showLink()) {
      return (
        <span className={ classNames }>{ niceLabel }</span>
      );
    }
    return (
      <Link className={ classNames } to={ this.getLink() }>{ niceLabel }</Link>
    );
  }
}

WorkflowProcessInfo.propTypes = {
  ...AbstractEntityInfo.propTypes,
  /**
   * Selected entity - has higher priority
   */
  entity: PropTypes.object,
  /**
   * Selected entity's id - entity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  /**
   * Internal entity loaded by given identifier
   */
  _entity: PropTypes.object,
  _showLoading: PropTypes.bool
};
WorkflowProcessInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
};

function select(state, component) {
  return {
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier)
  };
}
export default connect(select)(WorkflowProcessInfo);
