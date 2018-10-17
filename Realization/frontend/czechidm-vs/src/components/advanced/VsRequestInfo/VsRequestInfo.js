import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import {VsRequestManager} from '../../../redux/';
import { Utils, Advanced, Managers, Basic } from 'czechidm-core';
import VsOperationType from '../../../enums/VsOperationType';
import VsRequestState from '../../../enums/VsRequestState';

const manager = new VsRequestManager();


/**
 * Component for rendering nice identifier for virtual system request
 *
 * @author Vít Švanda
 * @author Radek Tomiška
 */
export class VsRequestInfo extends Advanced.AbstractEntityInfo {

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
    if (!Managers.SecurityManager.hasAccess({ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['VSREQUEST_READ']})) {
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
    //
    return `/vs/request/${entityIdentifier}/detail`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:link';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('vs:entity.VsRequest._type');
  }

  _getValueCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity) {
      return '';
    }
    if (entity.label === this.i18n('vs:entity.VsRequest.state.label')) {
      return ((<Basic.Label
          level={VsRequestState.getLevel(entity.value)}
          text={VsRequestState.getNiceLabel(entity.value)}/>));
    }
    if (entity.label === this.i18n('vs:entity.VsRequest.operationType.label')) {
      return ((<Basic.Label
          level={VsOperationType.getLevel(entity.value)}
          text={VsOperationType.getNiceLabel(entity.value)}/>));
    }
    if (entity.label === this.i18n('acc:entity.System.name')) {
      return ((<Advanced.EntityInfo
        entityType="system"
        entityIdentifier={ entity.value }
        face="link" />));
    }
    return entity.value;
  }

  getTableChildren() {
    return [
      <Basic.Column property="label"/>,
      <Basic.Column property="value" cell={this._getValueCell.bind(this)}/>];
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    return [
      {
        label: this.i18n('acc:entity.System.name'),
        value: entity._embedded.system.id
      },
      {
        label: this.i18n('vs:entity.VsRequest.uid.label'),
        value: entity.uid
      },
      {
        label: this.i18n('vs:entity.VsRequest.state.label'),
        value: entity.state
      },
      {
        label: this.i18n('vs:entity.VsRequest.operationType.label'),
        value: entity.operationType,
      },
      {
        label: this.i18n('vs:entity.VsRequest.executeImmediately.label'),
        value: entity.executeImmediately ? this.i18n('label.yes') : this.i18n('label.no'),
      },
      {
        label: this.i18n('vs:entity.VsRequest.creator.label'),
        value: entity.creator
      },
      {
        label: this.i18n('entity.created'),
        value: (<Advanced.DateValue value={ entity.created } showTime/>)
      }
    ];
  }
}

VsRequestInfo.propTypes = {
  ...Advanced.AbstractEntityInfo.propTypes,
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
};
VsRequestInfo.defaultProps = {
  ...Advanced.AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
  showIdentity: true,
};

function select(state, component) {
  return {
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier)
  };
}
export default connect(select)(VsRequestInfo);
