import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import {Basic} from 'czechidm-core';
import packageInfo from '../../package.json';

/**
 * IdM footer with links
 *
 * @author Radek Tomiška
 */
class Footer extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  showAbout(event) {
    if (event) {
      event.preventDefault();
    }
    this.context.router.replace('/about');
  }

  /**
   * Jump to page top
   */
  jumpTop() {
    $('html, body').animate({
      scrollTop: 0
    }, 'fast');
  }

  render() {
    const { backendVersion } = this.props;

    return (
      <footer>
        <div className="pull-left">
          <span title={this.i18n('app.version.backend') + ': ' + backendVersion} className="hidden">
            {this.i18n('app.version.frontend')} {packageInfo.version}
          </span>
          <span style={{margin: '0 10px'}} className="hidden">|</span>
          <a href={this.i18n('app.author.homePage')} target="_blank">{this.i18n('app.author.name')}</a>
          <span style={{margin: '0 10px'}}>|</span>
          <a href={ `${ this.i18n('app.documentation.url') }/start`} target="_blank">{this.i18n('app.helpDesk')}</a>
          <span style={{margin: '0 10px'}}>|</span>
          <a href="http://redmine.czechidm.com/projects/czechidmng" target="_blank">{this.i18n('app.serviceDesk')}</a>
          <span style={{margin: '0 10px'}}>|</span>
          <a href="#" onClick={this.showAbout.bind(this)} title={this.i18n('content.about.link')}>{this.i18n('content.about.link')}</a>
        </div>
        <div className="pull-right">
          <Basic.Button type="button" className="btn-xs" aria-label="Left Align"
                  onClick={this.jumpTop.bind(this)}>
            <Basic.Icon icon="chevron-up"/>
          </Basic.Button>
        </div>
        <div className="clearfix"></div>
      </footer>
    );
  }
}

Footer.propTypes = {
  backendVersion: PropTypes.string
};

Footer.defaultProps = {
  backendVersion: null
};

// Which props do we want to inject, given the global state?
// Note: use https://github.com/faassen/reselect for better performance.
function select() {
  return {
    backendVersion: 'x.x.x'// settingManager.getValue(state, 'environment.version')
  };
}

// Wrap the component to inject dispatch and state into it
export default connect(select)(Footer);
