import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import { Basic, Advanced } from 'czechidm-core';
import { VsRequestManager } from '../../redux';

const manager = new VsRequestManager();

/**
 * VsRequest detail with tabs
 *
 * @author Vít Švanda
 */
class VsRequestRoute extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.params;
    // load entity from BE - for nice labels etc.
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId));
  }

  render() {
    const { entity, showLoading } = this.props;

    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          <Basic.Icon value="link"/>
          {' '}
          { this.i18n('vs:content.vs-request.detail.edit.header', { name: manager.getNiceLabel(entity), escape: false }) }
        </Basic.PageHeader>

        <Advanced.TabPanel parentId="vs-requests" params={ this.props.params }>
          {this.props.children}
        </Advanced.TabPanel>
      </div>
    );
  }
}

VsRequestRoute.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
VsRequestRoute.defaultProps = {
  entity: null,
  showLoading: false
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(VsRequestRoute);
