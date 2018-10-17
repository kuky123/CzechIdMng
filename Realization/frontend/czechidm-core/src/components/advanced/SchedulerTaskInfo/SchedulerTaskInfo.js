import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { SchedulerManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';

const manager = new SchedulerManager();

/**
 * Scheduler task basic information (info card)
 *
 * @author Radek Tomiška
 */
export class SchedulerTaskInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    //
    // evaluate authorization policies
    const { _permissions } = this.props;
    if (!manager.canRead(this.getEntity(), _permissions)) {
      return false;
    }
    return true;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:calendar-times-o';
  }

  getNiceLabel() {
    const _entity = this.getEntity();
    //
    return this.getManager().getNiceLabel(_entity, false);
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.SchedulerTask._type');
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    return [
      {
        label: this.i18n('entity.SchedulerTask.taskType'),
        value: this.getNiceLabel()
      },
      {
        label: this.i18n('entity.SchedulerTask.description'),
        value: entity.description
      },
      {
        label: this.i18n('entity.SchedulerTask.parameters.label'),
        value: _.keys(entity.parameters).map(parameterName => {
          if (parameterName.lastIndexOf('core:', 0) === 0) {
            return null;
          }
          return (<div>{parameterName}: { entity.parameters[parameterName] }</div>);
        })
      }
    ];
  }
}

SchedulerTaskInfo.propTypes = {
  ...AbstractEntityInfo.propTypes,
  /**
   * Selected entity - has higher priority
   */
  entity: PropTypes.object,
  /**
   * Selected entity's id - entity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  //
  _showLoading: PropTypes.bool,
};
SchedulerTaskInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
};

function select(state, component) {
  return {
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier),
  };
}
export default connect(select)(SchedulerTaskInfo);
