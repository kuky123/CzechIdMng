import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
//
import AbstractContextComponent from '../AbstractContextComponent/AbstractContextComponent';
import PageHeader from '../PageHeader/PageHeader';
import ContentHeader from '../ContentHeader/ContentHeader';
import Icon from '../Icon/Icon';
import ConfigurationManager from '../../../redux/data/ConfigurationManager';
import { selectNavigationItems, selectNavigationItem, getNavigationItem, hideFooter } from '../../../redux/config/actions';

/**
* Basic content = page representation
* Requires store and router context
*
* @author Radek Tomiška
*/
export default class AbstractContent extends AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * Don't forget to call super.componentDidMount() in subclass
   * - solves selected navigation by defined navigation key
   * - solves hide footer
   *
   * @return
   */
  componentDidMount() {
    if (this.getNavigationKey()) {
      this.selectNavigationItem(this.getNavigationKey());
    }
    if (this.hideFooter()) {
      this.context.store.dispatch(hideFooter(true));
    }
  }

  /**
   * Don't forget to call super.componentWillUnmount() in subclass
   * - solves hide footer
   *
   * @return
   */
  componentWillUnmount() {
    if (this.hideFooter()) {
      this.context.store.dispatch(hideFooter(false));
    }
  }

  /**
   * select items in navigation by content / page
   *
   * @param  {array} selectedNavigationItems Array of selected navigation item. Can contains null values for select specified navigation level
   */
  selectNavigationItems(selectedNavigationItems) {
    this.context.store.dispatch(selectNavigationItems(selectedNavigationItems));
  }

  /**
   * select item in navigation by content / page id
   *
   * @param  {string} selectedNavigationItem id
   */
  selectNavigationItem(selectedNavigationItem) {
    this.context.store.dispatch(selectNavigationItem(selectedNavigationItem));
  }

  /**
   * Returns navigation for given id. If no navigationId is given, then returns navigation item by defined navigation key.
   *
   * @param  {string} navigationId
   * @return {object} navigationItem
   */
  getNavigationItem(navigationId = null) {
    if (!navigationId) {
      navigationId = this.getNavigationKey();
    }
    if (!navigationId) {
      return null;
    }
    return this.context.store.dispatch(getNavigationItem(this.context.store.getState().config, navigationId));
  }

  /**
   * @deprecated - use #selectNavigationItems
   * select item in sidebar by content / page
   *
   * @param  {string} selectedSidebarItem
   */
  selectSidebarItem(selectedSidebarItem) {
    this.context.store.dispatch(selectNavigationItems([null, selectedSidebarItem]));
  }

  /**
   * Return content identifier, with can be used in localization etc.
   *
   * @return {string} content identifier
   */
  getContentKey() {
    return null;
  }

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return this.getContentKey() || super.getComponentKey();
  }

  /**
   * Return navigation identifier, with can be used to show content header, title, icon ...
   *
   * @return {string} navigation item identifier
   */
  getNavigationKey() {
    return null;
  }

  /**
   * Reloads current route
   */
  reloadRoute() {
    this.context.router.replace(this.context.store.getState().routing.location.pathname);
  }

  /**
   * Makes redirect to error pages or show given error.
   *
   * @param  {error} error from BE
   */
  handleError(error) {
    if (!error) {
      return;
    }
    //
    const message = {};
    if (error.statusCode === 403) {
      this.context.router.push('/error/403');
      message.hidden = true;
    } else if (error.statusCode === 404) {
      if (error.parameters && error.parameters.entity) {
        this.context.router.push(`/error/404?id=${error.parameters.entity}`);
        message.hidden = true;
      } else {
        this.context.router.push(`/error/404`);
        message.hidden = true;
      }
    }
    //
    this.addErrorMessage(message, error);
  }

  /**
   * Hide footer on some contents.
   *
   * @return {bool} true - content footer will not be rendered
   */
  hideFooter() {
    return false;
  }

  /**
   * Returns true if is content Universal request. This is based on exists of 'requestId' param.
   * @param  params
   * @return {Boolean}
   */
  isRequest(params) {
    if (!params) {
      return false;
    }
    if (params.requestId) {
      return true;
    }
    return false;
  }

  /**
   * Returns manager. If it is evaluated, that should be using universal request managers, then
   * is created new instance and returned. Is given params doesn't contains
   * 'requestId' attribute, then original manager will be returned.
   * @param  params
   * @param  originalManager
   * @return Original or request manager
   */
  getRequestManager(params, originalManager) {
    if (this.isRequest(params)) {
      originalManager.setRequestId(params.requestId);
      return originalManager;
    }
    originalManager.setRequestsEnabled(ConfigurationManager.getPublicValueAsBoolean(this.context.store.getState(), originalManager.getEnabledPropertyKey()));
    originalManager.setRequestId(null);
    return originalManager;
  }

  addRequestPrefix(path, params) {
    if (this.isRequest(params)) {
      return `requests/${params.requestId}/${path}`;
    }
    return path;
  }

  /**
   * Return navigation identifier, with can be used to show content header, title, icon ...
   * If is content request, then navigation key for request will be
   * returned else the standard navigation key will be returned.
   *
   * @return {string} navigation item identifier
   */
  getRequestNavigationKey(navigationKey, params) {
    if (navigationKey && this.isRequest(params)) {
      return `request-${navigationKey}`;
    }
    return navigationKey;
  }


  /**
   * Default Page header with page title based on navigation item
   *
   * @param {object} props PageHeader properties e.g. style, className
   * @return {element} react element
   */
  renderPageHeader(props = {}) {
    const navigationItem = this.getNavigationItem() || {};
    //
    return (
      <PageHeader {...props}>
        <Helmet title={ props.title || this.i18n('title') } />
        <Icon value={props.icon || navigationItem.icon}/>
        {' '}
        <span dangerouslySetInnerHTML={{__html: props.header || this.i18n('header')}}/>
      </PageHeader>
    );
  }

  /**
   * Default content header with title based on navigation item
   *
   * @param {object} props ContentHeader properties e.g. style, className
   * @return {element} react element
   */
  renderContentHeader(props) {
    const navigationItem = this.getNavigationItem() || {};
    //
    return (
      <ContentHeader {...props}>
        <Helmet title={ props.title || this.i18n('title') } />
        <Icon value={navigationItem.icon}/>
        {' '}
        <span dangerouslySetInnerHTML={{__html: props.header || this.i18n('header')}}/>
      </ContentHeader>
    );
  }
}

AbstractContent.propTypes = {
  ...AbstractContextComponent.propTypes
};

AbstractContent.defaultProps = {
  ...AbstractContextComponent.defaultProps
};

AbstractContent.contextTypes = {
  ...AbstractContextComponent.contextTypes,
  router: PropTypes.object // .isRequired
};
