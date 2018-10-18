import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import { connect } from 'react-redux';
import classnames from 'classnames';
//
import * as Basic from '../../basic';
import { LocalizationService } from '../../../services';
import { ConfigurationManager } from '../../../redux/data';
import { SecurityManager, IdentityManager } from '../../../redux';
import { getNavigationItems, resolveNavigationParameters, collapseNavigation, i18nChange, selectNavigationItems } from '../../../redux/config/actions';
import NavigationItem from './NavigationItem';
import NavigationSeparator from './NavigationSeparator';

const identityManager = new IdentityManager();

/**
 * Top navigation
 *
 * @author Radek Tomiška
 */
export class Navigation extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  renderNavigationItems(section = 'main') {
    const { navigation, userContext, selectedNavigationItems } = this.props;
    const items = getNavigationItems(navigation, null, section, userContext, null, true);
    return this._renderNavigationItems(items, userContext, selectedNavigationItems);
  }

  _renderNavigationItems(items, userContext, selectedNavigationItems) {
    if (!items) {
      return null;
    }
    const renderedItems = [];
    for (const item of items) {
      const renderedItem = this.renderNavigationItem(item, userContext, selectedNavigationItems[0]);
      if (renderedItem) { // can be null
        renderedItems.push(renderedItem);
      }
    }
    return renderedItems;
  }

  _resolveNavigationItemText(item, userContext) {
    const labelParams = resolveNavigationParameters(userContext);
    labelParams.defaultValue = item.label;

    if (item.labelKey) {
      return (
          <span>{this.i18n(item.labelKey, labelParams)}</span>
      );
    }
    if (item.label) {
      return (
          <span>{item.label}</span>
      );
    }
    return (
      <span className="visible-xs-inline"> { this.i18n(item.titleKey, { defaultValue: item.title }) }</span>
    );
  }

  renderNavigationItem(item, userContext, activeItem, titlePlacement = 'bottom') {
    switch (item.type) {
      case 'DYNAMIC': {
        return (
          <NavigationItem
            id={ `nav-item-${item.id}` }
            key={ `nav-item-${item.id}` }
            to={ item.to }
            title={ this.i18n(item.titleKey, { defaultValue: item.title }) }
            titlePlacement={ titlePlacement }
            icon={ item.icon }
            iconColor={ item.iconColor }
            active={ activeItem === item.id }
            text={ this._resolveNavigationItemText(item, userContext) }/>
        );
      }
      case 'TAB': {
        // tab is not visible in menu
        return null;
      }
      case 'SEPARATOR': {
        return (
          <NavigationSeparator
            id={ `nav-item-${item.id}` }
            key={ `nav-item-${item.id}` }
            text={ this._resolveNavigationItemText(item, userContext) } />
        );
      }
      default: {
        this.getLogger().warn('[Advanced.Navigation] ' + item.type + ' type not implemeted for item id [' + item.id + ']');
        return null;
      }
    }
  }

  toogleNavigationCollapse(navigationCollapsed, event) {
    if (event) {
      event.preventDefault();
    }
    // FE change
    this.context.store.dispatch(collapseNavigation(!navigationCollapsed));
    // BE save navigation is collapsed
    const { userContext } = this.props;
    if (SecurityManager.isAuthenticated(userContext)) {
      this.context.store.dispatch(identityManager.saveCurrentProfile(userContext.id, {
        navigationCollapsed: !navigationCollapsed
      }));
    }
  }

  toogleNavigationItem(item, level, isActive, event) {
    if (event) {
      event.preventDefault();
    }
    const { selectedNavigationItems } = this.props;
    const newNavigationState = level > 0 ? selectedNavigationItems.slice(0, level - 1) : [];
    if (!isActive) {
      // show another level
      newNavigationState.push(item.id);
    }
    //
    this.context.store.dispatch(selectNavigationItems(newNavigationState));
    // prevent default link
    return false;
  }

  renderSidebarItems(parentId = null, level = 0) {
    const { navigation, navigationCollapsed, userContext, selectedNavigationItems } = this.props;
    level = level + 1;
    const levelItems = getNavigationItems(navigation, parentId, 'main', userContext, null, true);
    if (!levelItems || levelItems.length === 0) {
      return null;
    }

    const items = [];
    for (const levelItem of levelItems) {
      if (levelItem.type !== 'DYNAMIC' && levelItem.type !== 'SEPARATOR') {
        continue;
      }
      const childrenItems = getNavigationItems(navigation, levelItem.id, 'main', userContext, null, true);
      //
      if (childrenItems.length === 1 && childrenItems[0].path === levelItem.path) {
        // if menu contains only one subitem, which leeds to the same path - sub menu is truncated
        const item = this.renderNavigationItem(levelItem, userContext, selectedNavigationItems.length >= level ? selectedNavigationItems[level - 1] : null, 'right');
        if (item) {
          items.push(item);
        }
      } else {
        const children = this.renderSidebarItems(levelItem.id, level);
        const isActive = selectedNavigationItems.length >= level && selectedNavigationItems[level - 1] === levelItem.id;
        //
        if (children && !navigationCollapsed) {
          items.push(
            <li
              key={ `nav-item-${levelItem.id}` }
              className={ isActive ? 'has-children active' : 'has-children' }>
              <Basic.Tooltip
                id={ `${levelItem.id}-tooltip` }
                placement="right"
                value={ this.i18n(levelItem.titleKey, { defaultValue: levelItem.title }) }>
                <a href="#" onClick={ this.toogleNavigationItem.bind(this, levelItem, level, isActive) }>
                  <Basic.Icon icon={ levelItem.icon } color={ levelItem.iconColor }/>
                  {
                    navigationCollapsed
                    ?
                    null
                    :
                    <span>
                      { this._resolveNavigationItemText(levelItem, userContext) }
                      <Basic.Icon value={ `fa:angle-${ isActive ? 'down' : 'left' }` } className="arrow-icon" />
                    </span>
                  }
                </a>
              </Basic.Tooltip>
              { children }
            </li>
          );
        } else {
          const item = this.renderNavigationItem(
            levelItem,
            userContext,
            selectedNavigationItems.length >= level ? selectedNavigationItems[level - 1] : null,
            'right');
          if (item) {
            items.push(item);
          }
        }
      }
    }

    if (items.length === 0) {
      return null;
    }

    if (level === 1) { // collapse menu
      items.push(
        <li key="navigation-collapse">
          <Basic.Tooltip
            id={ `navigation-collapse-tooltip` }
            placement="right"
            value={ navigationCollapsed ? this.i18n('navigation.expand.label') : this.i18n('navigation.collapse.label') }>
            <a href="#" onClick={ this.toogleNavigationCollapse.bind(this, navigationCollapsed) }>
              <Basic.Icon value={ `arrow-${navigationCollapsed ? 'right' : 'left'}` }/>
              <span className="item-text" style={{ color: '#bbb' }}>
                {
                  navigationCollapsed
                  ?
                  <span>{ this.i18n('navigation.expand.label') }</span>
                  :
                  <span>{ this.i18n('navigation.collapse.label') }</span>
                }
              </span>
            </a>
          </Basic.Tooltip>
        </li>
      );
    }

    const classNames = classnames(
      'nav',
      { 'nav-second-level': level === 2 },
      { 'nav-third-level': level === 3 },
      { 'hidden': (level > 3 || (navigationCollapsed && level > 1)) }, // only three levels are supported
      { 'in': (selectedNavigationItems.length > level - 1 && selectedNavigationItems[level - 2]) === parentId },
      { 'collapse': parentId && !this._isSelected(selectedNavigationItems, parentId) && (selectedNavigationItems.length > level - 1 && selectedNavigationItems[level - 2]) !== parentId }
    );
    return (
      <ul
        id={ level === 1 ? 'side-menu' : 'side-menu-' + level }
        className={ classNames }>
        { items }
      </ul>
    );
  }

  _isSelected(selectedNavigationItems, parentId) {
    if (!parentId || !selectedNavigationItems) {
      return false;
    }
    for (const selectedNavigationItem of selectedNavigationItems) {
      if (parentId === selectedNavigationItem) {
        return true;
      }
    }
    return false;
  }

  _i18nChange(lng, event) {
    if (event) {
      event.preventDefault();
    }
    //
    this.context.store.dispatch(i18nChange(lng, () => {
      // RT: reload is not needed anymore, most of component was refectored to listen redux state.
      // RT: filled form values are not rerendered (e.g. filled filters), when locale is changed, but i think is trivial issue
      // window.location.reload();
      //
      const { userContext } = this.props;
      if (SecurityManager.isAuthenticated(userContext)) {
        this.context.store.dispatch(identityManager.saveCurrentProfile(userContext.id, {
          preferredLanguage: lng
        }));
      }
    }));
  }

  render() {
    const { environment, userContext, navigationCollapsed, rendered, i18nReady } = this.props;
    //
    if (!rendered) {
      return false;
    }

    let environmentLabel = null;
    if (environment) {
      const environmentClassName = classnames(
        'label',
        {'label-success': environment === 'development'},
        {'label-warning': environment !== 'development'},
        {'hidden': environment === 'production'}
      );
      environmentLabel = (
        <div className="navbar-text hidden-xs" title={this.i18n('environment.' + environment + '.title', { defaultValue: environment })}>
          <span className={environmentClassName}>
            <span className="hidden-sm">{this.i18n('environment.' + environment + '.label', { defaultValue: environment })}</span>
            <span className="visible-sm-inline">{this.i18n('environment.' + environment + '.short', { defaultValue: environment })}</span>
          </span>
        </div>
      );
    }

    const supportedLanguages = LocalizationService.getSupportedLanguages();
    let flags = null;
    if (supportedLanguages && supportedLanguages.length > 1) {
      flags = (
        <div className="navbar-text hidden-xs">
          <div className="flags-container">
            <div className="flags">
              {
                supportedLanguages.map((lng, i) => {
                  const lgnClassName = classnames(
                    'flag',
                    lng,
                    { 'active': i18nReady === lng },
                    { 'last': i === supportedLanguages.length - 1 }
                  );
                  return (
                    <span
                      key={`locale-${lng}`}
                      className={lgnClassName}
                      onClick={ this._i18nChange.bind(this, lng) }>
                    </span>
                  );
                })
              }
            </div>
          </div>
        </div>
      );
    }

    const mainItems = this.renderNavigationItems('main');
    const systemItems = this.renderNavigationItems('system');
    const sidebarItems = this.renderSidebarItems();

    const sidebarClassName = classnames(
      'navbar-default',
      'sidebar',
      { collapsed: navigationCollapsed }
    );

    return (
      <div>
        <header>
          <nav className="navbar navbar-default navbar-static-top" style={{ marginBottom: 0 }}>
            <div className="navbar-header">
              <button type="button" className="navbar-toggle collapsed" data-toggle="collapse" data-target=".navbar-collapse">
                <span className="sr-only">{ this.i18n('navigation.toogle') }</span>
                <span className="icon-bar"></span>
                <span className="icon-bar"></span>
                <span className="icon-bar"></span>
              </button>
              <Link to="/" title="Úvodní stránka" className="home">
                {' '}
              </Link>
            </div>
            <div id="navbar" className="navbar-collapse">
              {
                !userContext.isExpired && !SecurityManager.isAuthenticated(userContext)
                ?
                <ul className="nav navbar-nav">
                  { mainItems }
                </ul>
                :
                null
              }
              <div className="navbar-right">
                { environmentLabel }
                { flags }
                <ul className="nav navbar-nav">
                  {
                    userContext.isExpired
                    ||
                    systemItems
                  }
                </ul>
              </div>
            </div>
            {
              !userContext.isExpired && SecurityManager.isAuthenticated(userContext)
              ?
              <div className={sidebarClassName} role="navigation">
                <div className="sidebar-nav navbar-collapse">
                  { sidebarItems }
                </div>
              </div>
              :
              null
            }
          </nav>
        </header>
      </div>
    );
  }
}

Navigation.propTypes = {
  rendered: PropTypes.bool,
  navigation: PropTypes.object,
  navigationCollapsed: PropTypes.bool,
  selectedNavigationItems: PropTypes.array,
  environment: PropTypes.string,
  userContext: PropTypes.object,
  i18nReady: PropTypes.string
};

Navigation.defaultProps = {
  rendered: true,
  navigation: null,
  navigationCollapsed: false,
  selectedNavigationItems: null,
  environment: null,
  userContext: null,
  i18nReady: null
};

Navigation.contextTypes = {
  ...Basic.AbstractContextComponent.contextTypes,
  router: PropTypes.object.isRequired
};

function select(state) {
  return {
    navigation: state.config.get('navigation'),
    navigationCollapsed: state.security.userContext.navigationCollapsed,
    selectedNavigationItems: state.config.get('selectedNavigationItems'),
    environment: ConfigurationManager.getEnvironmentStage(state),
    userContext: state.security.userContext,
    i18nReady: state.config.get('i18nReady')
  };
}

export default connect(select)(Navigation);
