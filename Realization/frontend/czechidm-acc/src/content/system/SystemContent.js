import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import { Basic, Managers } from 'czechidm-core';
import { SystemManager } from '../../redux';
import SystemDetail from './SystemDetail';

const manager = new SystemManager();

/**
 * Target system detail content
 */
class SystemContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'acc:content.system.detail';
  }

  componentDidMount() {
    this.selectNavigationItems(['sys-systems', 'system-detail']);
    const { entityId } = this.props.params;

    this.context.store.dispatch(manager.fetchAvailableFrameworks());
    this.context.store.dispatch(manager.fetchAvailableRemoteConnector(entityId));
    if (this._isNew()) {
      this.context.store.dispatch(manager.receiveEntity(entityId, { }));
    } else {
      this.getLogger().debug(`[SystemContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId));
    }
  }

  _isNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading, availableFrameworks } = this.props;
    return (
      <Basic.Row>
        <div className={this._isNew() ? 'col-lg-offset-1 col-lg-10' : 'col-lg-12'}>
          {
            showLoading || !availableFrameworks
            ?
            <Basic.Loading isStatic showLoading />
            :
            <SystemDetail uiKey="system-detail" entity={entity} />
          }
        </div>
      </Basic.Row>
    );
  }
}

SystemContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
SystemContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId),
    availableFrameworks: Managers.DataManager.getData(state, SystemManager.AVAILABLE_CONNECTORS),
    availableRemoteFrameworks: Managers.DataManager.getData(state, SystemManager.AVAILABLE_REMOTE_CONNECTORS)
  };
}

export default connect(select)(SystemContent);
