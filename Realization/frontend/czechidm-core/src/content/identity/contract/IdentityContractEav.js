import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import { IdentityContractManager } from '../../../redux';

const uiKey = 'eav-identity-contract';
const manager = new IdentityContractManager();

/**
 * Extended identity contract's attributes
 *
 * @author Radek Tomiška
 */
class IdentityContractEav extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
    this.selectSidebarItem('identity-contract-eav');
  }

  getContentKey() {
    return 'content.identity-contract.eav';
  }

  render() {
    const { entityId} = this.props.params;
    const { _entity, _permissions } = this.props;
    //
    return (
      <Advanced.EavContent
        uiKey={uiKey}
        formableManager={manager}
        entityId={entityId}
        contentKey={this.getContentKey()}
        showSaveButton={ manager.canSaveContract(_entity, _permissions) }/>
    );
  }
}

IdentityContractEav.propTypes = {
  _entity: PropTypes.object,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
IdentityContractEav.defaultProps = {
  _entity: null,
  _permissions: null
};

function select(state, component) {
  return {
    _entity: manager.getEntity(state, component.params.entityId),
    _permissions: manager.getPermissions(state, null, component.params.entityId)
  };
}

export default connect(select)(IdentityContractEav);
