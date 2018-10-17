import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import { TreeNodeManager, SecurityManager, DataManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';
import EntityInfo from '../EntityInfo/EntityInfo';

const manager = new TreeNodeManager();


/**
 * Component for rendering nice identifier for tree node info, similar function as roleInfo
 *
 * @author Radek Tomiška (main component)
 * @author Ondrej Kopr
 * @author Patrik Stloukal
 */
export class TreeNodeInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  _onEnter() {
    super._onEnter();
    //
    this.context.store.dispatch(this.getManager().fetchDefaultTreeNode());
  }

  getManager() {
    return manager;
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    if (!SecurityManager.hasAccess({ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREENODE_READ']})) {
      return false;
    }
    return true;
  }

  /**
   * Get link to detail (`url`).
   *
   * @return {string}
   */
  getLink() {
    const { entityIdentifier } = this.props;
    //
    return `/tree/nodes/${entityIdentifier}/detail`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'apple';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.TreeNode._type');
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    const { defaultTreeNode } = this.props;
    let id;
    if (defaultTreeNode !== null) {
      id = defaultTreeNode.id;
    }
    return [
      {
        label: this.i18n('entity.TreeNode.name'),
        value: entity.name
      },
      {
        label: this.i18n('entity.TreeNode.code'),
        value: entity.code
      },
      {
        label: this.i18n('entity.TreeNode.parent.name'),
        value: entity.parent === null ? this.i18n('entity.TreeNode.parent.false') :
        <EntityInfo entityType="treeNode" entity={ entity._embedded ? entity._embedded.parent : null } entityIdentifier={ entity.parent } face="link" />
      },
      {
        label: this.i18n('entity.TreeNode.treeType.name'),
        value: !entity._embedded
        ||
        <EntityInfo
          entityType="treeType"
          entity={ entity._embedded.treeType }
          entityIdentifier={ entity._embedded.treeType.id }
          face="popover" />
      },
      {
        label: this.i18n('entity.TreeNode.defaultTreeNode.label'),
        value: id === entity.id ? this.i18n('entity.TreeNode.defaultTreeNode.true') : this.i18n('entity.TreeNode.defaultTreeNode.false')
      }
    ];
  }
}

TreeNodeInfo.propTypes = {
  ...AbstractEntityInfo.propTypes,
  /**
   * Selected entity - has higher priority
   */
  entity: PropTypes.object,
  /**
   * Selected entity's id - entity will be loaded automatically
   */
  entityIdentifier: PropTypes.string,
  /**
   * Internal entity loaded by given identifier
   */
  _entity: PropTypes.object,
  _showLoading: PropTypes.bool,
  defaultTreeNode: PropTypes.object
};
TreeNodeInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  entity: null,
  face: 'link',
  _showLoading: true,
  defaultTreeNode: null
};

function select(state, component) {
  return {
    defaultTreeNode: DataManager.getData(state, TreeNodeManager.UI_KEY_DEFAULT_TREE_NODE),
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier)
  };
}
export default connect(select)(TreeNodeInfo);
