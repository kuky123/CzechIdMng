import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import { Basic } from 'czechidm-core';
import { VsRequestManager } from '../../redux';
import VsRequestDetail from './VsRequestDetail';

const manager = new VsRequestManager();

/**
 * Virtual system request detail wrapper
 *
 * @author Vít Švanda
 */
class VsRequestContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'vs:content.vs-request.detail';
  }

  /**
   * Selected navigation item
   */
  getNavigationKey() {
    return 'vs-requests';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    if (this._isNew()) {
      // persist new entity to redux
      this.context.store.dispatch(manager.receiveEntity(entityId, { }));
    } else {
      this.context.store.dispatch(manager.fetchEntity(entityId));
    }
  }

  /**
   * Component will receive new props, try to compare with actual,
   * then init form
   */
  componentWillReceiveProps(nextProps) {
    const { entityId } = this.props.params;
    if (entityId && nextProps.params.entityId && entityId !== nextProps.params.entityId) {
      if (this._isNew()) {
        // persist new entity to redux
        this.context.store.dispatch(manager.receiveEntity(nextProps.params.entityId, { }));
      } else {
        this.context.store.dispatch(manager.fetchEntity(nextProps.params.entityId));
      }
    }
  }

  /**
   * Helper - returns `true`, when new entity is created
   */
  _isNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading} = this.props;

    return (
      <Basic.Row>
        <div className={this._isNew() ? 'col-lg-offset-1 col-lg-10' : 'col-lg-12'}>
          {
            <VsRequestDetail uiKey="vs-request-detail" entity={entity} showLoading={showLoading} />
          }
        </div>
      </Basic.Row>
    );
  }
}

VsRequestContent.propTypes = {
  /**
   * Loaded entity
   */
  entity: PropTypes.object,
  /**
   * Entity is currently loaded from BE
   */
  showLoading: PropTypes.bool
};
VsRequestContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(VsRequestContent);
