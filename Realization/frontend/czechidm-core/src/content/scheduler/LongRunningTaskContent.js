import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import { LongRunningTaskManager } from '../../redux';
import LongRunningTaskDetail from './LongRunningTaskDetail';

const manager = new LongRunningTaskManager();

/**
 * Detail of the LRT. Iniciate entity by ID and than forms table.
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
class LongRunningTaskContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    const { entityId } = this.props.params;
    this.selectNavigationItem('long-running-task-detail');
    this.context.store.dispatch(manager.fetchEntity(entityId));
  }

  render() {
    const { entity, showLoading } = this.props;
    //
    if (showLoading) {
      return (
        <Basic.Loading isStatic showLoading />
      );
    }
    //
    return entity && (
      <LongRunningTaskDetail entity={ entity } />
    );
  }
}

LongRunningTaskContent.propTypes = {
  /**
   * Loaded entity
   */
  entity: PropTypes.object,
  /**
   * Entity is currently loaded from BE
   */
  showLoading: PropTypes.bool
};

LongRunningTaskContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(LongRunningTaskContent);
