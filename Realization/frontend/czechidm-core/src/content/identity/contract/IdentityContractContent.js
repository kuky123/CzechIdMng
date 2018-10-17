import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { IdentityContractManager } from '../../../redux';
import IdentityContractDetail from './IdentityContractDetail';

const manager = new IdentityContractManager();

/**
 * Identity contract's content with detail form
 *
 * @author Radek Tomiška
 */
class IdentityContractContent extends Basic.AbstractContent {

  constructor(props) {
    super(props);
  }

  getContentKey() {
    return 'content.identity-contract.detail';
  }

  componentDidMount() {
    this.selectSidebarItem('identity-contract-detail');
    //
    const { entityId } = this.props.params;
    if (this._isNew()) {
      this.context.store.dispatch(manager.receiveEntity(entityId, { }));
    } else {
      this.context.store.dispatch(manager.fetchEntity(entityId));
    }
  }

  componentDidUpdate() {
  }

  _isNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading, params } = this.props;
    return (
      <Basic.Row>
        <div className={this._isNew() ? 'col-lg-offset-1 col-lg-10' : 'col-lg-12'}>
          {
            !entity
            ||
            <IdentityContractDetail entity={entity} showLoading={showLoading} params={params} />
          }
        </div>
      </Basic.Row>
    );
  }
}
IdentityContractContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};

IdentityContractContent.defaultProps = {
  entity: null,
  showLoading: true
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(IdentityContractContent);
