import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { ContractSliceManager } from '../../../redux';
import ContractSliceDetail from './ContractSliceDetail';

const manager = new ContractSliceManager();

/**
 * Contract's slice content with detail form
 *
 * @author Radek Tomiška
 */
class ContractSliceContent extends Basic.AbstractContent {

  constructor(props) {
    super(props);
  }

  getContentKey() {
    return 'content.contract-slice.detail';
  }

  componentDidMount() {
    this.selectSidebarItem('contract-slice-detail');
    //
    const { entityId } = this.props.params;
    const { query } = this.props.location;
    const contractId = (query) ? query.contractId : null;

    if (this._isNew()) {
      this.context.store.dispatch(manager.receiveEntity(entityId, {'parentContract': contractId }));
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
            <ContractSliceDetail entity={entity} showLoading={showLoading} params={params} />
          }
        </div>
      </Basic.Row>
    );
  }
}
ContractSliceContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};

ContractSliceContent.defaultProps = {
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

export default connect(select)(ContractSliceContent);
