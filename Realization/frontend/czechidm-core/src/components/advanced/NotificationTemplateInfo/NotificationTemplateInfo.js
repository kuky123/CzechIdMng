import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../basic';
import { NotificationTemplateManager, SecurityManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';

const manager = new NotificationTemplateManager();


/**
 * Component for rendering nice identifier for notification template info
 *
 * @author Radek Tomiška (main component)
 * @author Peter Sourek
 * @author Petr Hanák
 */
export class NotificationTemplateInfo extends AbstractEntityInfo {

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
    if (!SecurityManager.hasAccess({ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_TEMPLATE_READ']})) {
      return false;
    }
    return true;
  }

  getLink() {
    return `/notification/templates/${encodeURIComponent(this.getEntityId())}`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:envelope-square';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.NotificationTemplate._type');
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    return [
      {
        label: this.i18n('entity.NotificationTemplate.name'),
        value: entity.name
      },
      {
        label: this.i18n('entity.NotificationTemplate.code'),
        value: entity.code
      },
      {
        label: this.i18n('entity.NotificationTemplate.subject'),
        value: (<Basic.ShortText value={ entity.subject } maxLength={ 30 } />)
      },
      {
        label: this.i18n('entity.NotificationTemplate.bodyText'),
        value: (<Basic.ShortText value={ entity.bodyText } maxLength={ 30 } />)
      }
    ];
  }
}

NotificationTemplateInfo.propTypes = {
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
NotificationTemplateInfo.defaultProps = {
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
export default connect(select)(NotificationTemplateInfo);
