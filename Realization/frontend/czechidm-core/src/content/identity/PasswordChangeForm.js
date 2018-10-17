import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import ComponentService from '../../services/ComponentService';
import * as Basic from '../../components/basic';

const componentService = new ComponentService();

/**
 * Password change component contains all components defined in component descriptor
 * with type password-change-component
 *
 * @author Ondřej Kopr
 */
class PasswordChangeForm extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * Return all components of password-change-component type
   */
  getComponents() {
    return componentService.getPasswordChangeComponents();
  }

  /**
   * Method return password change content defined by component descriptor
   * To every component will be add these prosp:
   * - userContext    - user context
   * - accountOptions - all available account options
   * - entityId       - entity id of user for that will be password changed
   */
  getContent() {
    const {
      userContext,
      accountOptions,
      entityId
    } = this.props;
    //
    const components = this.getComponents();
    const finalComponents = [];
    components.forEach(component => {
      const Component = component.component;
      finalComponents.push(
        <Basic.Col lg={component.col}>
          <Component
            userContext={userContext}
            accountOptions={accountOptions}
            entityId={entityId}/>
        </Basic.Col>
      );
    });
    return finalComponents;
  }

  getContentKey() {
    return 'content.identity.passwordChange';
  }

  componentDidMount() {
    this.selectSidebarItem('profile-password');
  }

  render() {
    let components = this.getContent();
    //
    // if no one components exists show warning
    if (_.size(components) === 0) {
      components = (
        <Basic.Alert level="warning" text={this.i18n('message.noContent')}/>
      );
    }
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        {/* TODO: check span counter - the same alghoritm as on dashboard */}
        <Basic.Row>
          { components }
        </Basic.Row>
      </div>
    );
  }
}

PasswordChangeForm.propTypes = {
  userContext: PropTypes.object,
  accountOptions: PropTypes.object,
  entityId: PropTypes.string
};
PasswordChangeForm.defaultProps = {
  userContext: null
};

export default connect()(PasswordChangeForm);
