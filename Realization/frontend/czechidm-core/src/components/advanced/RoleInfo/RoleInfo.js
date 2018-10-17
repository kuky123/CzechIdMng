import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../basic';
import * as Utils from '../../../utils';
import { RoleManager, RoleCompositionManager, SecurityManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';
import RolePriorityEnum from '../../../enums/RolePriorityEnum';
import SearchParameters from '../../../domain/SearchParameters';

const uiKeyRoles = 'role-composition-sub-table';
const manager = new RoleManager();
const roleCompositionManager = new RoleCompositionManager();

/**
 * Role basic information (info card)
 *
 * @author Radek Tomiška
 */
export class RoleInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  onEnter() {
    super.onEnter();

    const entityId = this.getEntityId();
    if (entityId) {
      if (SecurityManager.hasAuthority('ROLECOMPOSITION_AUTOCOMPLETE')) {
        const forceSubSearchParameters = new SearchParameters(SearchParameters.NAME_AUTOCOMPLETE).setFilter('superiorId', entityId);
        this.context.store.dispatch(roleCompositionManager.fetchEntities(forceSubSearchParameters, `${uiKeyRoles}-${entityId}`));
      }
    }
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    //
    // evaluate authorization policies
    const { _permissions } = this.props;
    if (!manager.canRead(this.getEntity(), _permissions)) {
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
    return `/role/${encodeURIComponent(this.getEntityId())}/detail`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:universal-access';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.Role._type');
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
    const { _subRoles, _subRolesUi } = this.props;
    //
    const content = [
      {
        label: this.i18n('entity.name'),
        value: manager.getNiceLabel(entity)
      },
      {
        label: this.i18n('entity.Role.priorityEnum'),
        value: (<Basic.EnumValue enum={ RolePriorityEnum } value={ RolePriorityEnum.findKeyBySymbol(RolePriorityEnum.getKeyByPriority(entity.priority)) } />)
      }
    ];
    // subroles
    if (_subRolesUi) {
      content.push({
        label: this.i18n('entity.Role.subRoles'),
        value: (
          _subRolesUi.showLoading
          ?
          <Basic.Icon value="refresh" showLoading />
          :
          <span>
            {
              _subRoles
                .map(subRole => {
                  return subRole._embedded.sub.name;
                })
                .join(', ')
            }
            {' '}
            ({ _subRolesUi.total })
          </span>
        )
      });
    }
    //
    return content;
  }
}

RoleInfo.propTypes = {
  ...AbstractEntityInfo.propTypes,
  /**
   * Selected entity - has higher priority
   */
  entity: PropTypes.object,
  /**
   * Selected entity's id - entity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  //
  _showLoading: PropTypes.bool,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ])
};
RoleInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
};

function select(state, component) {
  const { entityIdentifier, entity } = component;
  let entityId = entityIdentifier;
  if (!entityId && entity) {
    entityId = entity.id;
  }
  return {
    _entity: manager.getEntity(state, entityId),
    _showLoading: manager.isShowLoading(state, null, entityId),
    _permissions: manager.getPermissions(state, null, entityId),
    _subRoles: roleCompositionManager.getEntities(state, `${uiKeyRoles}-${entityId}`),
    _subRolesUi: Utils.Ui.getUiState(state, `${uiKeyRoles}-${entityId}`)
  };
}
export default connect(select)(RoleInfo);
