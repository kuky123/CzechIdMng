import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { RoleManager } from '../../redux';

const uiKey = 'eav-role';
let manager = null;

/**
 * Extended role attributes
 *
 * @author Radek Tomiška
 */
class RoleEav extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    // Init manager - evaluates if we want to use standard (original) manager or
    // universal request manager (depends on existing of 'requestId' param)
    manager = this.getRequestManager(this.props.params, new RoleManager());
  }

  getContentKey() {
    return 'content.role.eav';
  }

  getNavigationKey() {
    return this.getRequestNavigationKey('role-eav', this.props.params);
  }

  render() {
    const { entityId } = this.props.params;
    const { _entity, _permissions } = this.props;
    //
    return (
      <Advanced.EavContent
        uiKey={ uiKey }
        formableManager={ manager }
        entityId={ entityId }
        contentKey={ this.getContentKey() }
        showSaveButton={ manager.canSave(_entity, _permissions) }/>
    );
  }
}

RoleEav.propTypes = {
  _entity: PropTypes.object,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
RoleEav.defaultProps = {
  _entity: null,
  _permissions: null
};

function select(state, component) {
  if (!manager) {
    return {};
  }
  return {
    _entity: manager.getEntity(state, component.params.entityId),
    _permissions: manager.getPermissions(state, null, component.params.entityId)
  };
}

export default connect(select)(RoleEav);
