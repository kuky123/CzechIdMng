import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import { Basic } from 'czechidm-core';
import { ExampleProductManager } from '../../redux';
import ExampleProductDetail from './ExampleProductDetail';

const manager = new ExampleProductManager();

/**
 * Example product detait wrapper
 *
 * @author Radek Tomiška
 */
class ExampleProductContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'acc:content.system.detail';
  }

  /**
   * Selected navigation item
   */
  getNavigationKey() {
    return 'example-product-detail';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    if (this._isNew()) {
      // persist new entity to redux
      this.context.store.dispatch(manager.receiveEntity(entityId, { }));
    } else {
      // load entity from BE - we need load actual entity and set her to the form
      this.getLogger().debug(`[ExampleProductContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId));
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
    const { entity, showLoading } = this.props;
    return (
      <Basic.Row>
        <div className={this._isNew() ? 'col-lg-offset-1 col-lg-10' : 'col-lg-12'}>
          {
            showLoading
            ?
            <Basic.Loading isStatic showLoading />
            :
            <ExampleProductDetail uiKey="example-product-detail" entity={entity} />
          }
        </div>
      </Basic.Row>
    );
  }
}

ExampleProductContent.propTypes = {
  /**
   * Loaded entity
   */
  entity: PropTypes.object,
  /**
   * Entity is currently loaded from BE
   */
  showLoading: PropTypes.bool
};
ExampleProductContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(ExampleProductContent);
