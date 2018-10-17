import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import { TreeNodeManager } from '../../../redux';
import * as Advanced from '../../../components/advanced';

const manager = new TreeNodeManager();

class Node extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId));
  }

  componentDidUpdate() {
  }

  render() {
    const { entity, showLoading } = this.props;

    return (
      <div>
        <Helmet title={this.i18n('navigation.menu.profile')} />

        <Basic.PageHeader showLoading={!entity && showLoading}>
          {manager.getNiceLabel(entity)} <small> {this.i18n('content.tree.node.detail.header')}</small>
        </Basic.PageHeader>

        <Advanced.TabPanel parentId="tree-nodes" params={this.props.params}>
          {this.props.children}
        </Advanced.TabPanel>
      </div>
    );
  }
}

Node.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
Node.defaultProps = {
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

export default connect(select)(Node);
