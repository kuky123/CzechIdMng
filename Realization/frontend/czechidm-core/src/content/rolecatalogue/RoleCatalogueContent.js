import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { RoleCatalogueManager } from '../../redux';
import RoleCatalogueDetail from './RoleCatalogueDetail';

const roleCatalogueManager = new RoleCatalogueManager();

/**
 * Role Catalogue detail - first tab on role detail
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class RoleCatalogueContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.setState({
      _showLoading: true
    });
  }

  getContentKey() {
    return 'content.roleCatalogues';
  }

  componentDidMount() {
    const { entityId } = this.props.params;
    this.selectNavigationItems(['role-catalogues', 'role-catalogue-detail']);
    //
    if (this._isNew()) {
      this.context.store.dispatch(roleCatalogueManager.receiveEntity(entityId, { }));
    } else {
      this.getLogger().debug(`[RoleCatalogueContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(roleCatalogueManager.fetchEntity(entityId), entityId, () => {
        this.setState({
          _showLoading: false
        });
      });
    }
  }

  _isNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading } = this.props;
    return (
      <Basic.Row>
        <div className={ this._isNew() ? 'col-lg-offset-1 col-lg-10' : 'col-lg-12' }>
          {
            !entity
            ||
            <RoleCatalogueDetail entity={entity} showLoading={showLoading}/>
          }
        </div>
      </Basic.Row>
    );
  }
}

RoleCatalogueContent.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
RoleCatalogueContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: roleCatalogueManager.getEntity(state, entityId),
    showLoading: roleCatalogueManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(RoleCatalogueContent);
