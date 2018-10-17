import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Utils from '../../../utils';
import * as Basic from '../../basic';
import { ContractPositionManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';
import EntityInfo from '../EntityInfo/EntityInfo';

const manager = new ContractPositionManager();


/**
 * Component for rendering contract position.
 *
 * @author Radek Tomiška
 */
export class ContractPositionInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  getNiceLabel() {
    const _entity = this.getEntity();
    const { showIdentity } = this.props;
    //
    return this.getManager().getNiceLabel(_entity, showIdentity); // ~ show identity contract
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:building';
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    const { _permissions } = this.props;
    if (!this.getManager().canRead(this.getEntity(), _permissions)) {
      return false;
    }
    return true;
  }

  /**
   * Returns true, when disabled decorator has to be used
   *
   * @param  {object} entity
   * @return {bool}
   */
  isDisabled(entity) {
    if (!entity._embedded) {
      return false;
    }
    //
    return !Utils.Entity.isValid(entity._embedded.identityContract);
  }

  /**
   * Get link to detail (`url`).
   *
   * @return {string}
   */
  getLink() {
    const _entity = this.getEntity();
    //
    if (!_entity._embedded || !_entity._embedded.identityContract._embedded) {
      return null;
    }
    return `/identity/${encodeURIComponent(_entity._embedded.identityContract._embedded.identity.username)}/identity-contract/${_entity.identityContract}/positions`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:building';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.ContractPosition._type');
  }

  getTableChildren() {
    // component are used in #getPopoverContent => skip default column resolving
    return [
      <Basic.Column property="label"/>,
      <Basic.Column property="value"/>
    ];
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    return [
      {
        label: this.i18n('entity.ContractPosition.position.label'),
        value: entity.position
      },
      {
        label: this.i18n('entity.ContractPosition.workPosition.label'),
        value: !entity._embedded || !entity._embedded.workPosition ||
          <EntityInfo
            entityType="treeNode"
            entity={ entity._embedded.workPosition }
            entityIdentifier={ entity._embedded.workPosition.id }
            face="link" />
      },
      {
        label: this.i18n('entity.TreeType._type'),
        value: !entity._embedded || !entity._embedded.workPosition ||
          <EntityInfo
            entityType="treeType"
            entity={ entity._embedded.workPosition._embedded.treeType }
            entityIdentifier={ entity._embedded.workPosition.treeType }
            face="link" />
      }
    ];
  }
}

ContractPositionInfo.propTypes = {
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
   * Show contract's identity
   */
  showIdentity: PropTypes.bool,
  //
  _showLoading: PropTypes.bool,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ])
};
ContractPositionInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
  showIdentity: true,
};

function select(state, component) {
  const entity = manager.getEntity(state, component.entityIdentifier);
  return {
    _entity: entity,
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier),
    _permissions: manager.getPermissions(state, null, component.entityIdentifier)
  };
}
export default connect(select)(ContractPositionInfo);
