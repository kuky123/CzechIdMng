import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { ContractSliceManager } from '../../../redux';
import * as Advanced from '../../../components/advanced';
import OrganizationPosition from '../OrganizationPosition';

const manager = new ContractSliceManager();

/**
 * Identity contract tabs - entry point
 *
 * @author Radek Tomiška
 */
class ContractSlice extends Basic.AbstractContent {

  componentDidMount() {
    this._selectNavigationItem();
    const { entityId } = this.props.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId));
  }

  componentDidUpdate() {
    this._selectNavigationItem();
  }

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

    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          {manager.getNiceLabel(entity)} <small> {this.i18n('content.contract-slice.detail.header')}</small>
        </Basic.PageHeader>

        <OrganizationPosition identity={ params.identityId }/>

        <Advanced.TabPanel parentId="identity-contract-slices" params={this.props.params}>
          {this.props.children}
        </Advanced.TabPanel>
      </div>
    );
  }
}

ContractSlice.propTypes = {
  entity: PropTypes.object,
  userContext: PropTypes.object,
  showLoading: PropTypes.bool
};
ContractSlice.defaultProps = {
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

export default connect(select)(ContractSlice);
