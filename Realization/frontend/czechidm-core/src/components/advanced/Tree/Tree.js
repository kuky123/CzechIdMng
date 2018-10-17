import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
import { Treebeard, decorators } from 'react-treebeard';
import Immutable from 'immutable';
//
import * as Basic from '../../basic';
import defaultStyle from './styles';
import DataManager from '../../../redux/data/DataManager';

/**
 * TODO: set better constant, or check this in BE
 * @type {Number}
 */
const MAX_NODES_IN_ROW = 100000;

/**
* Advanced tree component
*/
class AdvancedTree extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    const { rootNodes, rootNodesCount } = props;
    const data = rootNodes;
    if (rootNodesCount && rootNodes.length < rootNodesCount) {
      // data.push(this._createMoreLink(props, rootNodesCount - rootNodes.length));
    }
    this.state = {
      data,
      cursors: []
    };
    this.dataManager = new DataManager();
  }

  _createMoreLink(props, count) {
    const { propertyId, propertyChildrenCount } = props;
    return {
      [propertyId]: null,
      name: '...',
      [propertyChildrenCount]: count,
      isMoreLink: true,
      isLeaf: true,
      toggled: true,
      loading: false
    };
  }

  /**
  * Manager for entity in tree
  */
  getManager() {
    return this.props.manager;
  }

  componentDidMount() {
    this._reload();
  }

  _mergeSearchParameters(searchParameters) {
    const { defaultSearchParameters, forceSearchParameters } = this.props;
    let _forceSearchParameters = null;
    if (forceSearchParameters) {
      _forceSearchParameters = forceSearchParameters.setSize(null).setPage(null); // we dont want override setted pagination
    }
    return this.getManager().mergeSearchParameters(searchParameters || defaultSearchParameters || this.getManager().getDefaultSearchParameters(), _forceSearchParameters);
  }

  componentWillReceiveProps(nextProps) {
    const { cursors, propertyId } = nextProps;
    // cursors is different
    if (cursors && !_.isEqual(this.props.cursors, cursors)) {
      const { data } = this.state;
      // We find same node in data and merge new cursor to him
      cursors.forEach(cursor => {
        const oldCursor = this._findNode(cursor[propertyId], { children: data } );
        _.merge(oldCursor, cursor);
      });
      this.setState({
        data
      });
    }
  }

  // TODO: this method is not tested .. just snippet
  _reload() {
    const { uiKey, rootNodes } = this.props;
    //
    this.context.store.dispatch(this.dataManager.receiveData(uiKey, new Immutable.Map({})));
    if (!rootNodes || rootNodes.length === 0) {
      return;
    }
    rootNodes.forEach(rootNode => {
      if (!rootNode.isMoreLink && !this._isLeaf(rootNode) && rootNode.toggled) {
        this._onToggle(rootNode, rootNode.toggled);
      }
    });
  }

  /**
   * Returns tree state - is immutable map - key is node id and value is node's children
   *
   * @param  {object} state redux state
   * @param  {string} uiKey uiKey for whole tree
   * @return {Immutable.Map}
   */
  _getTreeState(state, uiKey) {
    const treeState = DataManager.getData(state, uiKey);
    if (treeState == null) {
      return new Immutable.Map({});
    }
    return treeState;
  }

  /**
  * Is call after click on any node in tree
  *
  * @param  {object} node    selected node
  * @param  {boolean} toggled
  */
  _onToggle(node, toggled) {
    if (!node || node.isMoreLink) {
      return;
    }
    const { propertyParent, propertyId, uiKey } = this.props;
    const { cursors } = this.state;
    const state = this.context.store.getState();
    const treeState = this._getTreeState(state, uiKey);
    //
    cursors.forEach(cursor => {
      cursor.active = false;
    });
    const loaded = this._loadNode(node, state);
    if (!loaded) {
      node.loading = true;
    }
    node.active = true;
    node.toggled = toggled;

    // TODO: this is problem - just one node could be active now ...
    cursors.splice(0, cursors.length);
    cursors.push(node);

    this.setState({ cursors }, () => {
      if (!loaded) {
        const filter = this.getManager().getService().getTreeSearchParameters().setFilter(propertyParent, node[propertyId]).setSort('name', true).setSize(MAX_NODES_IN_ROW);
        this.context.store.dispatch(this.getManager().fetchEntities(filter, uiKey, (json, error) => {
          if (!error) {
            const data = json._embedded[this.getManager().getCollectionType()] || [];
            // ids from childen - whole entity could be found in entities state, we dont want duplicates
            const newTreeState = treeState.set(node[propertyId], data.map(item => { return item[propertyId]; }));
            this.context.store.dispatch(this.dataManager.receiveData(uiKey, newTreeState));
          } else {
            this.addErrorMessage({
              level: 'error',
              key: 'error-tree-load'
            }, error);
          }
        }));
      }
    });
  }

  /**
  * Check if is node in Redux state. If is, then set loading attribute to false.
  *
  * @param  {object} node
  * @param  {Immutable.Map} state in redux
  * @return {boolean}  Is node loaded
  */
  _loadNode(node, state) {
    const { uiKey, propertyId } = this.props;
    const treeState = this._getTreeState(state, uiKey);

    const containsParent = treeState.has(node[propertyId]);
    if (containsParent && node.loading && !node.loading) {
      return true;
    }
    if (containsParent) {
      const childrenIds = treeState.get(node[propertyId]);
      if (childrenIds.length !== 0) {
        node.children = [];
        childrenIds.forEach(nodeId => {
          const nodeEntity = this.getManager().getEntity(this.context.store.getState(), nodeId);
          // nodeEntity could not not be null, just for sure
          if (!nodeEntity) {
            // nodeEntity could be deleted
          } else {
            node.children.push(this.trimNode(nodeEntity));
          }
        });
        for (const child of node.children) {
          child.toggled = false;
          this._isLeaf(child);
        }
      } else {
        node.isLeaf = true;
        delete node.children;
      }
      node.loading = false;
      node.toggled = true;
      return true;
    }
    return false;
  }

  /**
   * Trim nod and set nice label (for optimalization size of tree state)
   */
  trimNode(node) {
    const { propertyId, propertyChildrenCount, propertyParent, propertyName } = this.props;
    if (!node) {
      return null;
    }

    const nodeLabel = this.getManager().getNiceLabel(node);
    const trimmedNode = {};
    trimmedNode._nodeNiceLable = nodeLabel;
    trimmedNode[propertyId] = node[propertyId];
    trimmedNode[propertyParent] = node[propertyParent];
    trimmedNode[propertyChildrenCount] = node[propertyChildrenCount];
    trimmedNode[propertyName] = node[propertyName];
    trimmedNode.children = node.children;
    trimmedNode.loading = node.loading;
    trimmedNode.toggled = node.toggled;
    trimmedNode.isLeaf = node.isLeaf;
    trimmedNode.isMoreLink = node.isMoreLink;

    return trimmedNode;
  }

  /**
  * Find node with idNode in element (recursively by children)
  *
  * @param  {string} idNode
  * @param  {object} element
  */
  _findNode(idNode, element) {
    const { propertyId } = this.props;
    //
    if (element[propertyId] === idNode) {
      return element;
    } else if (element.children != null) {
      let result = null;
      for (let i = 0; !result && i < element.children.length; i++) {
        result = this._findNode(idNode, element.children[i]);
      }
      return result;
    }
    return null;
  }

  /**
   * Sets node isLeaf informations and returns true, if node is leaf
   *
   * @param  {object} node
   * @return {Boolean}
   */
  _isLeaf(node) {
    const { propertyChildrenCount } = this.props;
    if (node[propertyChildrenCount] === undefined || node[propertyChildrenCount] === null || node[propertyChildrenCount] > 0) {
      node.children = [];
      return false;
    }
    node.isLeaf = true;
    node.loading = false;
    node.toggled = true;
    return true;
  }


  _getLabel(node) {
    const { propertyName, propertyChildrenCount } = this.props;
    if (propertyName && !node.isMoreLink) {
      return node[propertyName];
    }
    return (
      <span>
        {
          node.isMoreLink
          ?
          node.name
          :
          node._nodeNiceLable
        }
        {
          !node[propertyChildrenCount]
          ||
          <small style={{ color: '#aaa' }}>{' '}({node[propertyChildrenCount]})</small>
        }
      </span>
    );
  }

  /**
  * Get decorators (default or custom form props)
  *
  * @return {object} Decorators
  */
  _getDecorators() {
    const { loadingDecorator, toggleDecorator, headerDecorator } = this.props;
    return {
      Loading: (props) => {
        if (loadingDecorator) {
          return loadingDecorator(props);
        }
        return (
          <div style={props.style}>
            {this.i18n('component.advanced.Tree.loading')}
          </div>
        );
      },
      Toggle: (props) => {
        if (toggleDecorator) {
          return toggleDecorator(props);
        }
        return new decorators.Toggle(props);
      },
      Header: (headerProps) => {
        if (headerDecorator) {
          return headerDecorator(headerProps);
        }
        const style = headerProps.style;
        const icon = headerProps.node.isLeaf ? 'file-text' : 'folder';
        return (
          <div style={ style.base }>
            <div style={ style.title }>
              <Basic.Icon type="fa" value={icon} style={{ marginRight: '5px' }}/>
              { this._getLabel(headerProps.node) }
            </div>
          </div>
        );
      }
    };
  }

  render() {
    const { data } = this.state;
    const { style, showLoading, rendered } = this.props;
    // I have problem with definition Container decorator. I override only Header, Loading and Toggle decorators in default "decorators"
    const customDecorators = this._getDecorators();
    if (!rendered) {
      return null;
    }
    if (showLoading) {
      return (
        <Basic.Loading isStatic showLoading/>
      );
    }
    return (
      <div style={{ overflowX: 'auto' }}>
        <Treebeard
          data={data}
          onToggle={this._onToggle.bind(this)}
          style={ style || defaultStyle }
          decorators={{ ...decorators, Header: customDecorators.Header, Loading: customDecorators.Loading}}/>
      </div>
    );
  }
}

AdvancedTree.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
  * EntityManager for fetching entities in tree
  */
  manager: PropTypes.object.isRequired,
  /**
  * Inicial root nodes of tree
  */
  rootNodes: PropTypes.arrayOf(PropTypes.object).isRequired,
  /**
  * Total root count
  */
  rootNodesCount: PropTypes.number,
  /**
  * Key for save data to redux store
  */
  uiKey: PropTypes.string.isRequired,
  /**
  * Define attribute in entity which will be used for show name of node
  */
  propertyName: PropTypes.string,
  /**
  * Define attribute in entity which will be used as node id
  */
  propertyId: PropTypes.string,
  /**
  * Define attribute in entity which will be used as children count
  */
  propertyChildrenCount: PropTypes.string,
  /**
  * Define attribute in entity which will be used for search children nodes
  */
  propertyParent: PropTypes.string,
  /**
  * Define styles in object
  */
  style: PropTypes.object,
  /**
  * Can be use for override loading decorator
  */
  loadingDecorator: PropTypes.func,
  /**
  * Can be use for override toggle decorator
  */
  toggleDecorator: PropTypes.func,
  /**
  * Can be use for override header decorator
  */
  headerDecorator: PropTypes.func
};

AdvancedTree.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  propertyId: 'id',
  propertyChildrenCount: 'childrenCount',
  propertyParent: 'parent',
  propertyName: 'name',
  rootNodesCount: null
};

function select(state) {
  const wrappedInstance = this ? this.getWrappedInstance() : null;
  if (wrappedInstance && wrappedInstance.state.cursors) {
    const cursors = wrappedInstance.state.cursors.map(cursor => {
      wrappedInstance._loadNode(cursor, state);
      // we need new instance ... we create clone
      return _.merge({}, cursor);
    });
    return {
      cursors
    };
  }
  return {};
}

export default connect(select, null, null, { withRef: true })(AdvancedTree);
