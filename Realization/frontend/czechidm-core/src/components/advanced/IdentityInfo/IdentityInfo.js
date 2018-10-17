import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import classNames from 'classnames';
//
import * as Basic from '../../basic';
import { IdentityManager, DataManager } from '../../../redux';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';

const manager = new IdentityManager();

/**
 * Identity basic information (info card)
 *
 * @author Radek Tomiška
 */
export class IdentityInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  onEnter() {
    super.onEnter();
    //
    if (this.getEntityId()) {
      this.context.store.dispatch(this.getManager().downloadProfileImage(this.getEntityId()));
    }
  }

  /**
   * Override super.getEntityId() - adds username for backward compatibility
   *
   * @return {string} id
   */
  getEntityId() {
    const { username, entityIdentifier, entity } = this.props;
    // id has higher priority
    if (entityIdentifier) {
      return entityIdentifier;
    }
    if (username) {
      return username;
    }
    if (entity) {
      return entity.id;
    }
    return null;
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    // evaluate authorization policies
    const { _permissions } = this.props;
    if (!this.getManager().canRead(this.getEntity(), _permissions)) {
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
    return `/identity/${encodeURIComponent(this.getEntityId())}/profile`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon(entity) {
    if (this.isDisabled(entity)) {
      return 'fa:user-times';
    }
    return 'user';
  }

  /**
   * @Override
   * Renders nicelabel used in text and link face
   */
  _renderNiceLabel() {
    const{ showOnlyUsername } = this.props;
    //
    if (!showOnlyUsername) {
      return super._renderNiceLabel();
    }
    const _entity = this.getEntity();
    if (_entity && _entity.username) {
      return _entity.username;
    }
    return '';
  }

  renderImage(entity) {
    const { _imageLoading, _imageUrl } = this.props;
    //
    let content = null;
    if (_imageLoading) {
      content = (
        <div className="text-center img-thumbnail img-loading" style={{ backgroundColor: '#DFF0D8' }}>
          <Basic.Icon
            value="fa:refresh"
            showLoading
            color={ '#FFFFFF' }
            />
        </div>
      );
    } else if (_imageUrl) {
      content = (
        <img src={ _imageUrl } className="img-thumbnail" />
      );
    } else {
      content = (
        <Basic.Icon
          value="user"
          className="text-center img-thumbnail img-none"
          style={{ backgroundColor: this.isDisabled(entity) ? '#FCF8E3' : '#DFF0D8' }}
          color={ '#FFFFFF' }
          />
      );
    }
    //
    return (
      <div className="image-wrapper">
        { content }
      </div>
    );
  }

  renderRow(icon, entityAttr) {
    if (entityAttr) {
      return (
        <tr>
          <td>
            <Basic.Icon value={ icon } style={{ marginRight: 5 }} />
            { entityAttr }
          </td>
        </tr>
      );
    }
  }

  _renderFull() {
    const { className, style } = this.props;
    const _entity = this.getEntity();
    //
    const panelClassNames = classNames(
      'abstract-entity-info',
      'identity-info',
      { 'panel-success': _entity && !this.isDisabled(_entity) },
      { 'panel-warning': _entity && this.isDisabled(_entity) },
      className
    );
    //
    return (
      <Basic.Panel className={panelClassNames} style={style}>
        <Basic.PanelHeader>
          <Basic.Icon value={ this.getEntityIcon(_entity) } style={{ marginRight: 5 }}/>
          { this.getPopoverTitle(_entity) }
          {
            !this.isDisabled(_entity)
            ||
            <div className="pull-right">
              <Basic.Label text={ this.i18n('label.disabled') } className="label-disabled"/>
            </div>
          }
        </Basic.PanelHeader>
        <div className="image-field-container">
          <div className="image-col">
            { this.renderImage(_entity) }
          </div>
          <div className="field-col">
            <table className="table table-condensed">
              <tbody>
                { this.renderRow('fa:envelope', _entity.email) }
                { this.renderRow('fa:phone', _entity.phone) }
                { this.renderRow(null, (
                  <Link to={ this.getLink() }>
                    <Basic.Icon value="fa:angle-double-right"/>
                    {' '}
                    {this.i18n('component.advanced.EntityInfo.link.detail.label')}
                  </Link>
                )) }
              </tbody>
            </table>
          </div>
        </div>
      </Basic.Panel>
    );
  }
}

IdentityInfo.propTypes = {
  ...AbstractEntityInfo.propTypes,
  /**
   * Selected entity - has higher priority
   */
  entity: PropTypes.object,
  /**
   * Selected identity's username - identity will be loaded automatically  (username entityIdentifier)
   */
  username: PropTypes.string,
  /**
   * Selected identity's id (username alias) - identity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  /**
   * If true, then show only username instead niceLabel
   */
  showOnlyUsername: PropTypes.bool,
  //
  _showLoading: PropTypes.bool,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ]),
  _imageLoading: PropTypes.bool,
};
IdentityInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'full',
  showOnlyUsername: false,
  _showLoading: true,
  _imageLoading: true,
  _imageUrl: null
};

function select(state, component) {
  const identifier = component.entityIdentifier || component.username;
  const identity = component.entity || manager.getEntity(state, identifier);
  const profileUiKey = manager.resolveProfileUiKey(identifier);
  const profile = DataManager.getData(state, profileUiKey);
  //
  return {
    _entity: identity,
    _showLoading: manager.isShowLoading(state, null, identifier),
    _imageLoading: DataManager.isShowLoading(state, profileUiKey),
    _imageUrl: profile ? profile.imageUrl : null,
    userContext: state.security.userContext, // is needed for refresh after login
    _permissions: manager.getPermissions(state, null, identity)
  };
}
export default connect(select)(IdentityInfo);
