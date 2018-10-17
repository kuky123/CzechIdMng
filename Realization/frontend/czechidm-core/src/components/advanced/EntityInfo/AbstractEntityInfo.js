import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { Link } from 'react-router';
//
import * as Basic from '../../basic';
import * as Utils from '../../../utils';
import UuidInfo from '../UuidInfo/UuidInfo';

/**
 * Entity info renderer - common methods.
 *
 * @author Radek Tomiška
 */
export default class AbstractEntityInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    //
    this.state = {
      error: null
    };
  }

  /**
   * Returns entity manager
   */
  getManager() {
    return this.props.manager;
  }

  componentDidMount() {
    this.loadEntityIfNeeded();
  }

  componentDidUpdate() {
    this.loadEntityIfNeeded();
  }

  /**
   * if entityIdentifier is setted and entity not - then loads entity from BE.
   */
  loadEntityIfNeeded() {
    const { entity, _entity, face } = this.props;
    const manager = this.getManager();
    if (!manager || !this.getEntityId()) {
      // nothing to load
      return;
    }
    //
    const entityId = this.getEntityId();
    if (entityId) {
      const uiKey = manager.resolveUiKey(null, entityId);
      // load entity
      if (!entity && !_entity) {
        const error = Utils.Ui.getError(this.context.store.getState(), uiKey) || this.state.error;
        if (!Utils.Ui.isShowLoading(this.context.store.getState(), uiKey)
            && (!error || error.statusCode === 401)) { // show loading check has to be here - new state is needed
          this.context.store.dispatch(manager.queueAutocompleteEntityIfNeeded(entityId, uiKey, (e, ex) => {
            // TODO: move to other place - is called only when entity is not given
            if (!ex && (face === 'full' || (face === 'link' && this.getLink()))) {
              this.onEnter();
            }
            this.setState({
              error: ex
            });
          }));
        }
      }
    }
  }

  /**
   * Load permissions if needed if pover is opened
   *
   * @return {[type]} [description]
   */
  onEnter() {
    const { _permissions } = this.props;
    const manager = this.getManager();
    const entityId = this.getEntityId();
    //
    if (!manager || !entityId || !manager.supportsAuthorization()) {
      // nothing to load
      return;
    }
    if (!_permissions && entityId) {
      const uiKey = manager.resolveUiKey(null, entityId);
      this.context.store.dispatch(manager.queueFetchPermissions(entityId, uiKey));
    }
  }

  /**
   * Returns entity identifier
   *
   * @return {string} entity identifier
   */
  getEntityId() {
    const { entityIdentifier, entity } = this.props;
    //
    if (entityIdentifier) {
      return entityIdentifier;
    }
    if (entity) {
      return entity.id;
    }
    return null;
  }

  getEntity() {
    const { entity, _entity } = this.props;
    //
    if (entity) { // entity is given by props
      return entity;
    }
    return _entity; // loaded by redux
  }

  /**
   * Get link to detail (`url`).
   *
   * @return {string}
   */
  getLink() {
    return null;
  }

  /**
   * Returns true, when link to detail could be shown
   *
   * @return {bool} Returns true, when link to detail could be shown
   */
  showLink() {
    const { showLink } = this.props;
    if (!showLink) {
      // disabled by props
      return false;
    }
    if (!this.getEntityId()) {
      // disabled by unknown entity id
      return false;
    }
    if (!this.getLink()) {
      // link to detail is not provided
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
    return Utils.Entity.isDisabled(entity);
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon(/* entity*/) {
    return null;
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle(entity) {
    return this.getManager().getNiceLabel(entity);
  }

  /**
   * Returns popover info content
   *
   * @param  {object} entity
   * @return {arrayOf(object)} table data
   */
  getPopoverContent(/* entity*/) {
    return null;
  }

  getNiceLabel() {
    const _entity = this.getEntity();
    //
    return this.getManager().getNiceLabel(_entity);
  }

  /**
   * Allows define columns for table in children Info component.
   */
  getTableChildren() {
    return null;
  }

/**
 * Render icon (rendered only if props showIcon is true)
 * @return {[type]} [description]
 */
  _renderIcon() {
    const {showIcon} = this.props;
    if (!showIcon) {
      return '';
    }
    const _entity = this.getEntity();
    return (
    <span className="pull-left">
      <Basic.Icon value={ this.getEntityIcon(_entity) } title={ this.getPopoverTitle(_entity) } style={{ marginRight: 5 }}/>
    </span>
    );
  }

  /**
   * Renders nicelabel used in text and link face
   */
  _renderNiceLabel() {
    const { className, style } = this.props;
    //
    return (
      <span className={className} style={style}>{ this.getNiceLabel() }</span>
    );
  }

  /**
   * Renders text face
   */
  _renderText() {
    return (
      <span>
        {this._renderIcon()}
        {this._renderNiceLabel()}
      </span>
    );
  }

  /**
   * Renders link face
   */
  _renderLink() {
    if (!this.showLink()) {
      return this._renderNiceLabel();
    }
    return (
      <span>
        {this._renderIcon()}
        <Link
          to={ this.getLink() }
          title={ this.i18n('component.advanced.EntityInfo.link.detail.label') }>
          { this.getNiceLabel() }
        </Link>
      </span>
    );
  }

  /**
   * Renders popover info card
   */
  _renderPopover() {
    const { style } = this.props;
    //
    return (
      <Basic.Popover
        trigger={['click']}
        value={ this._renderFull() }
        className="abstract-entity-info-popover"
        onEnter={ this.onEnter.bind(this) }>
        {
          <span
            style={ style }>
            { this._renderIcon() }
            <Basic.Button
              level="link"
              style={{ padding: 0, whiteSpace: 'normal', textAlign: 'left' }}
              title={ this.i18n('component.advanced.EntityInfo.link.popover.title') }>
              { this._renderNiceLabel() }
            </Basic.Button>
          </span>
        }
      </Basic.Popover>
    );
  }

  /**
   * Renders full info card - its used ass popover content too
   */
  _renderFull() {
    const { className, style } = this.props;
    const _entity = this.getEntity();
    //
    const panelClassNames = classNames(
      'abstract-entity-info',
      { 'panel-success': _entity && !this.isDisabled(_entity) },
      { 'panel-warning': _entity && this.isDisabled(_entity) },
      className
    );
    //
    return (
      <Basic.Panel className={panelClassNames} style={style}>
        <Basic.PanelHeader>
          <div className="pull-left">
            <Basic.Icon value={ this.getEntityIcon(_entity) } style={{ marginRight: 5 }}/>
            { this.getPopoverTitle(_entity) }
          </div>
          {
            !this.isDisabled(_entity)
            ||
            <div className="pull-right">
              <Basic.Label text={ this.i18n('label.disabled') } className="label-disabled"/>
            </div>
          }
          <div className="clearfix"/>
        </Basic.PanelHeader>

        <Basic.Table
          condensed
          hover={ false }
          noHeader
          data={ this.getPopoverContent(_entity) }
          children={ this.getTableChildren() }/>

        {
          !this.showLink()
          ||
          <Basic.PanelFooter>
            <Link to={ this.getLink() }>
              <Basic.Icon value="fa:angle-double-right"/>
              {' '}
              {this.i18n('component.advanced.EntityInfo.link.detail.label')}
            </Link>
          </Basic.PanelFooter>
        }
      </Basic.Panel>
    );
  }

  render() {
    const { rendered, showLoading, className, entity, face, _showLoading, style } = this.props;
    //
    if (!rendered) {
      return null;
    }
    let _entity = this.props._entity;
    if (entity) { // identity prop has higher priority
      _entity = entity;
    }
    const entityId = this.getEntityId();
    //
    if (showLoading || (_showLoading && entityId && !_entity)) {
      switch (face) {
        case 'text':
        case 'link':
        case 'popover': {
          return (
            <Basic.Icon value="refresh" showLoading className={ className } style={ style } title={ entityId }/>
          );
        }
        default: {
          return (
            <Basic.Well showLoading className={ classNames('abstract-entity-info', className) } style={ style }/>
          );
        }
      }
    }
    if (!_entity) {
      if (!this.getEntityId()) {
        return null;
      }
      return (
        <UuidInfo className={ className } value={ this.getEntityId() } style={ style } />
      );
    }
    //
    switch (face) {
      case 'text': {
        return this._renderText();
      }
      case 'link': {
        return this._renderLink();
      }
      case 'popover': {
        return this._renderPopover();
      }
      default: {
        return this._renderFull();
      }
    }
  }
}

AbstractEntityInfo.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Internal entity loaded by given identifier
   */
  _entity: PropTypes.object,
  /**
   * Decorator
   */
  face: PropTypes.oneOf(['text', 'link', 'popover', 'full']),
  /**
   * Shows link to full identity detail (if currently logged user has appropriate permission)
   */
  showLink: PropTypes.bool,
  /**
   * Shows icon for text', 'link', 'popover' face
   */
  showIcon: PropTypes.bool,
  /**
   * Entity manager
   */
  manager: PropTypes.object
};
AbstractEntityInfo.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  face: 'full',
  showLink: true,
  showIcon: false
};
