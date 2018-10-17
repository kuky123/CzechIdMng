import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { RoleCatalogueManager } from '../../redux';
import * as Advanced from '../../components/advanced';

const manager = new RoleCatalogueManager();

/**
 * Role's catalogue tab panel
 *
 * @author Kučera
 */
class RoleCatalogue extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId, null, (entity, error) => {
      this.handleError(error);
    }));
  }

  render() {
    const { entity, showLoading } = this.props;

    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          { manager.getNiceLabel(entity)} <small> {this.i18n('content.roleCatalogues.edit.header') }</small>
        </Basic.PageHeader>

        <Advanced.TabPanel parentId="role-catalogues" params={this.props.params}>
          { this.props.children }
        </Advanced.TabPanel>
      </div>
    );
  }
}

RoleCatalogue.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
RoleCatalogue.defaultProps = {
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

export default connect(select)(RoleCatalogue);
