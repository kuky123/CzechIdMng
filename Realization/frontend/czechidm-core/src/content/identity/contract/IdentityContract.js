import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { IdentityContractManager } from '../../../redux';
import * as Advanced from '../../../components/advanced';
import OrganizationPosition from '../OrganizationPosition';
import _ from 'lodash';

const manager = new IdentityContractManager();

/**
 * Identity contract tabs - entry point
 *
 * @author Radek Tomiška
 */
class IdentityContract extends Basic.AbstractContent {

  componentDidMount() {
    this._selectNavigationItem();
    const { entityId } = this.props.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId));
  }

  componentDidUpdate() {
    this._selectNavigationItem();
  }

  /**
   * Lookot: getNavigationKey cannot be used -> profile vs users main tab
   */
  _selectNavigationItem() {
    const { identityId } = this.props.params;
    const { userContext } = this.props;
    if (identityId === userContext.username) {
      this.selectNavigationItems(['identity-profile', null]);
    } else {
      this.selectNavigationItems(['identities', null]);
    }
  }

  render() {
    const { entity, showLoading, params } = this.props;
    const paramsResult = _.merge(params, { controlledBySlices: entity && entity.controlledBySlices ? true : false});
    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          {manager.getNiceLabel(entity)} <small> {this.i18n('content.identity-contract.detail.header')}</small>
        </Basic.PageHeader>

        <OrganizationPosition identity={ params.identityId }/>
        <Basic.Alert
          rendered={entity && entity.controlledBySlices ? true : false}
          level="info"
          text={this.i18n('content.identity-contract.detail.alert.controlledBySlices')}/>

        <Advanced.TabPanel parentId="profile-contracts" params={paramsResult}>
          {this.props.children}
        </Advanced.TabPanel>
      </div>
    );
  }
}

IdentityContract.propTypes = {
  entity: PropTypes.object,
  userContext: PropTypes.object,
  showLoading: PropTypes.bool
};
IdentityContract.defaultProps = {
  entity: null,
  userContext: null,
  showLoading: false
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    userContext: state.security.userContext,
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(IdentityContract);
