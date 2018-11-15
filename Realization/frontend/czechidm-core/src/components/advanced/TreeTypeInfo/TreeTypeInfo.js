import { PropTypes } from 'react';
import { connect } from 'react-redux';
//
import { TreeTypeManager, SecurityManager, DataManager } from '../../../redux/';
import AbstractEntityInfo from '../EntityInfo/AbstractEntityInfo';

const manager = new TreeTypeManager();


/**
 * Component for rendering nice identifier for tree type info, similar function as roleInfo
 *
 * @author Radek Tomiška (main component)
 * @author Ondrej Kopr
 * @author Patrik Stloukal
 */
export class TreeTypeInfo extends AbstractEntityInfo {

  constructor(props, context) {
    super(props, context);
  }

  _onEnter() {
    super._onEnter();
    //
    this.context.store.dispatch(this.getManager().fetchDefaultTreeType());
  }

  getManager() {
    return manager;
  }

  showLink() {
    if (!super.showLink()) {
      return false;
    }
    if (!SecurityManager.hasAccess({ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREETYPE_READ']})) {
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
    return `/tree/types/${encodeURIComponent(this.getEntityId())}`;
  }

  /**
   * Returns entity icon (null by default - icon will not be rendered)
   *
   * @param  {object} entity
   */
  getEntityIcon() {
    return 'fa:folder-open';
  }

  /**
   * Returns popovers title
   *
   * @param  {object} entity
   */
  getPopoverTitle() {
    return this.i18n('entity.TreeType._type');
  }

  /**
   * Returns popover info content
   *
   * @param  {array} table data
   */
  getPopoverContent(entity) {
    const { defaultTreeType } = this.props;
    let id;
    if (defaultTreeType !== null) {
      id = defaultTreeType.id;
    }
    return [
      {
        label: this.i18n('entity.TreeType.name'),
        value: entity.name
      },
      {
        label: this.i18n('entity.TreeType.code'),
        value: entity.code
      },
      {
        label: this.i18n('entity.TreeType.defaultTreeType.label'),
        value: id === entity.id ? this.i18n('entity.TreeType.defaultTreeType.true') : this.i18n('entity.TreeType.defaultTreeType.false')
      }
    ];
  }
}

TreeTypeInfo.propTypes = {
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
  defaultTreeType: PropTypes.object
};
TreeTypeInfo.defaultProps = {
  ...AbstractEntityInfo.defaultProps,
  defaultTreeType: null,
  entity: null,
  face: 'link',
  _showLoading: true,
};

function select(state, component) {
  return {
    defaultTreeType: DataManager.getData(state, TreeTypeManager.UI_KEY_DEFAULT_TREE_TYPE),
    _entity: manager.getEntity(state, component.entityIdentifier),
    _showLoading: manager.isShowLoading(state, null, component.entityIdentifier)
  };
}
export default connect(select)(TreeTypeInfo);
