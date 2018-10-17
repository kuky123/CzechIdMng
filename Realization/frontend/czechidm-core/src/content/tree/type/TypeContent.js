import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import * as Basic from '../../../components/basic';
import * as Utils from '../../../utils';
import { TreeTypeManager } from '../../../redux';
import TypeDetail from './TypeDetail';
import TypeConfiguration from './TypeConfiguration';

const treeTypeManager = new TreeTypeManager();

/**
 * Type detail content
 */
class TypeContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.tree.types';
  }

  getNavigationKey() {
    return 'tree-types';
  }

  componentDidMount() {
    const { entityId } = this.props.params;
    if (this._getIsNew()) {
      this.context.store.dispatch(treeTypeManager.receiveEntity(entityId, { }));
    } else {
      this.getLogger().debug(`[TypeContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(treeTypeManager.fetchEntity(entityId));
    }
  }

  _getIsNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading } = this.props;
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        {
          !entity
          ||
          <Basic.PageHeader>
            <Basic.Icon value="fa:server"/>
            {' '}
            {
              this._getIsNew()
              ?
              this.i18n('create')
              :
              <span>{entity.name} <small>{this.i18n('edit')}</small></span>
            }
          </Basic.PageHeader>
        }

        {
          (!entity || Utils.Entity.isNew(entity))
          ||
          <TypeConfiguration treeTypeId={entity.id}/>
        }

        <Basic.Panel>
          <Basic.Loading isStatic showLoading={showLoading} />
          {
            !entity
            ||
            <TypeDetail entity={entity} />
          }
        </Basic.Panel>

      </div>
    );
  }
}

TypeContent.propTypes = {
  node: PropTypes.object,
  showLoading: PropTypes.bool
};
TypeContent.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: treeTypeManager.getEntity(state, entityId),
    showLoading: treeTypeManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(TypeContent);
