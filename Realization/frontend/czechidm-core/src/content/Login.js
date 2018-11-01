import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../components/basic';
import { SecurityManager } from '../redux';

const securityManager = new SecurityManager();

/**
 * Login box
 *
 * @author Radek Tomiška
 */
class Login extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.login';
  }

  getNavigationKey() {
    return 'home';
  }

  hideFooter() {
    return true;
  }

  _redirectIfIsAuthenticated() {
    if (this.props.userContext.isAuthenticated) {
      // redirection to requested page before login
      const { location } = this.props;
      if (location.state && location.state.nextPathname) {
        this.context.router.replace(location.state.nextPathname);
      } else {
        this.context.router.replace('/');
      }
    }
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this._redirectIfIsAuthenticated();
    this.refs.form.setData({});
    this.refs.username.focus();
    this.context.store.dispatch(securityManager.remoteLogin());
  }

  componentDidUpdate() {
    this._redirectIfIsAuthenticated();
  }

  handleSubmit(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const formData = this.refs.form.getData();
    this.context.store.dispatch(securityManager.login(formData.username, formData.password));
  }

  render() {
    const { userContext } = this.props;
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <div>
          <form onSubmit={this.handleSubmit.bind(this)}>
            <Basic.Panel className="login-container" showLoading={userContext.showLoading}>
              <Basic.PanelHeader text={this.i18n('header')}/>
              <Basic.PanelBody>
                <Basic.AbstractForm ref="form" className="form-horizontal" style={{ padding: 0, backgroundColor: '#fff' }}>
                  <Basic.TextField
                    ref="username"
                    labelSpan="col-sm-5"
                    componentSpan="col-sm-7"
                    label={this.i18n('username')}
                    placeholder={this.i18n('username')}
                    required/>
                  <Basic.TextField
                    type="password"
                    ref="password"
                    labelSpan="col-sm-5"
                    componentSpan="col-sm-7"
                    className="last"
                    label={this.i18n('password')}
                    placeholder={this.i18n('password')}
                    required/>
                </Basic.AbstractForm>
              </Basic.PanelBody>
              <Basic.PanelFooter>
                <Basic.Button type="submit" level="success">
                  {this.i18n('button.login')}
                </Basic.Button>
              </Basic.PanelFooter>
            </Basic.Panel>
          </form>
        </div>
      </div>
    );
  }
}

Login.propTypes = {
  ...Basic.AbstractContent.propTypes,
  userContext: PropTypes.object
};

Login.defaultProps = {
  ...Basic.AbstractContent.defaultProps,
  userContext: { isAuthenticated: false }
};

function select(state) {
  return {
    i18nReady: state.config.get('i18nReady'),
    userContext: state.security.userContext
  };
}

export default connect(select)(Login);
