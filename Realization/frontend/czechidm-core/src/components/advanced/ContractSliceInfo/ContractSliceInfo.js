import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Utils from '../../../utils';
import * as Basic from '../../basic';
import { ContractSliceManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';
import DateValue from '../DateValue/DateValue';
import EntityInfo from '../EntityInfo/EntityInfo';

const manager = new ContractSliceManager();

/**
 * Component for rendering nice identifier for contract slices, similar function as roleInfo
 *
 * @author Vít Švanda
 */
export class ContractSliceInfo extends AbstractEntityInfo {

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
    return this.getManager().getNiceLabel(_entity, showIdentity);
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
    return !Utils.Entity.isValid(entity);
  }

  /**
   * Get link to detail (`url`).
   *
   * @return {string}
   */
  getLink() {
    const { entityIdentifier } = this.props;
    const _entity = this.getEntity();
    //
    return `/identity/${encodeURIComponent(_entity._embedded.identity.username)}/contract-slice/${entityIdentifier}/detail`;
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
    return this.i18n('entity.ContractSlice._type');
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
        label: this.i18n('entity.Identity._type'),
        value: !entity._embedded ||
          <EntityInfo
            entityType="identity"
            entity={ entity._embedded.identity }
            entityIdentifier={ entity.identity }
            face="link" />
      },
      {
        label: this.i18n('entity.ContractSlice.position'),
        value: entity.position
      },
      {
        label: this.i18n('entity.ContractSlice.workPosition'),
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
      },
      {
        label: this.i18n('entity.validFrom'),
        value: (<DateValue value={ entity.validFrom }/>)
      },
      {
        label: this.i18n('entity.validTill'),
        value: (<DateValue value={ entity.validTill }/>)
      }
    ];
  }
}

ContractSliceInfo.propTypes = {
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
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
ContractSliceInfo.defaultProps = {
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
    _permissions: manager.getPermissions(state, null, entity)
  };
}
export default connect(select)(ContractSliceInfo);
