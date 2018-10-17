import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import classNames from 'classnames';

//
import Footer from './Footer';
import { Basic, Advanced, Managers } from 'czechidm-core';
import ConfigLoader from 'czechidm-core/src/utils/ConfigLoader';
//
const securityManager = new Managers.SecurityManager();

/**
 * Application entry point
 *
 * @author Radek Tomiška
 */
export class App extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getChildContext() {
    return {
    };
  }

  /**
  * Look out: This method is aplication entry point
  */
  componentDidUpdate() {
    const { location, routes, userContext, appReady } = this.props;
    // select navigation
    if (location.pathname === '/') {
      this.selectNavigationItem('home');
    }
    if (this.refs.form) { // when modal is closed, form is not defined
      this.refs.form.setData({
        username: userContext.username,
        password: this.refs.password.getValue() // preserve filled password
      });
      this.refs.password.focus();
    }

    // onEnter makes this redirection now
    // console.log('router', this.context.router);
    // console.log('location', this.props.location);
    // check if is user logged, if not will do redirect to login page
    /* if (!this.props.userContext.isAuthenticated && this.props.location.pathname !== '/login') {
      this.context.router.replace('/login');
    }*/

    // check access to disable module route - has to be here, because SecurityManager.checkaccess is called to early (configuration is loaded asynchronouslly).
    if (appReady) {
      const currentRoute = routes[routes.length - 1];
      if (currentRoute.module && !ConfigLoader.isEnabledModule(currentRoute.module)) {
        this.context.router.replace('/unavailable');
      }
      this._handleRemoteAuth();
      this._handleTokenRefresh();
    }
  }

  login(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const formData = this.refs.form.getData();
    // refresh login
    this.context.store.dispatch(securityManager.login(formData.username, formData.password));
  }

  logout(event) {
    if (event) {
      event.preventDefault();
    }
    this.context.store.dispatch(securityManager.logout(() => {
      this.context.router.replace('/login');
    }));
  }

  _handleRemoteAuth() {
    const { userContext } = this.props;
    // handle expiration
    if (userContext && userContext.isTryRemoteLogin) {
      this.context.store.dispatch(securityManager.remoteLogin());
    }
  }

  _handleTokenRefresh() {
    const { userContext } = this.props;
    // handle token expiration extension
    if (userContext) {
      this.context.store.dispatch(securityManager.checkRefreshedToken());
    }
  }

  render() {
    const { userContext, bulk, appReady, navigationCollapsed, hideFooter } = this.props;
    const titleTemplate = '%s | ' + this.i18n('app.name');
    const classnames = classNames(
      { 'with-sidebar': !userContext.isExpired && Managers.SecurityManager.isAuthenticated(userContext) },
      { 'collapsed': navigationCollapsed }
    );
    //
    return (
      <div id="content-wrapper">
        <Basic.FlashMessages ref="messages"/>
        {
          !appReady
          ?
          <Basic.Loading className="global" showLoading/>
          :
          <div>
            <Helmet title={this.i18n('navigation.menu.home')} titleTemplate={titleTemplate}/>
            <Advanced.Navigation/>
            <div id="content-container" className={classnames}>
              {/* children is hidden only - prevent to lost form data, when token is expired */}
              <div style={ userContext.isExpired ? { display: 'none'} : {} }>
                { this.props.children }
              </div>
              {
                !userContext.isExpired && !hideFooter
                ?
                <Footer />
                :
                null
              }

              <Advanced.ModalProgressBar
                show={bulk.showLoading}
                text={bulk.action.title}
                count={bulk.size}
                counter={bulk.counter}/>

              <Basic.Modal dialogClassName="login-container" show={userContext.isExpired}>
                <form onSubmit={this.login.bind(this)}>
                  <Basic.Modal.Header text={this.i18n('error.LOG_IN.title')} />
                  <Basic.Modal.Body>
                    <Basic.Loading showLoading={userContext.showLoading}>
                      <Basic.Alert text={this.i18n('error.LOG_IN.message')}/>
                      <Basic.AbstractForm ref="form" className="form-horizontal" style={{ padding: 0, backgroundColor: '#fff' }}>
                        <Basic.TextField
                          ref="username"
                          labelSpan="col-sm-5"
                          componentSpan="col-sm-7"
                          label={this.i18n('content.login.username')}
                          placeholder={this.i18n('content.login.username')}
                          required
                          readOnly/>
                        <Basic.TextField
                          type="password"
                          ref="password"
                          labelSpan="col-sm-5"
                          componentSpan="col-sm-7"
                          className="last"
                          label={this.i18n('content.login.password')}
                          placeholder={this.i18n('content.login.password')}
                          required/>
                      </Basic.AbstractForm>
                    </Basic.Loading>
                  </Basic.Modal.Body>
                  <Basic.Modal.Footer>
                    <Basic.Button
                      level="link"
                      onClick={this.logout.bind(this)}
                      showLoading={userContext.showLoading}
                      title={this.i18n('content.login.button.logout.title')}
                      titlePlacement="bottom">
                      {this.i18n('content.login.button.logout.value')}
                    </Basic.Button>
                    <Basic.Button type="submit" level="success" showLoading={userContext.showLoading}>
                      {this.i18n('content.login.button.login')}
                    </Basic.Button>
                  </Basic.Modal.Footer>
                </form>
              </Basic.Modal>
            </div>
          </div>
        }
      </div>
    );
  }
}

App.propTypes = {
  /**
   * Logged user context
   */
  userContext: PropTypes.object,
  /**
   * Globally bulk action
   */
  bulk: PropTypes.object,
  appReady: PropTypes.bool,
  i18nReady: PropTypes.string,
  navigationCollapsed: PropTypes.bool,
  /**
   * Footer will be hidden
   */
  hideFooter: PropTypes.bool
};

App.defaultProps = {
  userContext: null,
  bulk: { action: {} },
  appReady: false,
  i18nReady: null,
  navigationCollapsed: false,
  hideFooter: false
};

App.childContextTypes = {
};

// Which props do we want to inject, given the global state?
// Note: use https://github.com/faassen/reselect for better performance.
function select(state) {
  return {
    userContext: state.security.userContext,
    bulk: state.data.bulk,
    appReady: state.config.get('appReady'),
    i18nReady: state.config.get('i18nReady'),
    navigationCollapsed: state.security.userContext.navigationCollapsed,
    hideFooter: state.config.get('hideFooter')
  };
}

// Wrap the component to inject dispatch and state into it
export default connect(select)(App);
