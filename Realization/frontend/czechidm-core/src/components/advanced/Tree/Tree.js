import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import Immutable from 'immutable';
import classNames from 'classnames';
//
import * as Basic from '../../basic';
import * as Domain from '../../../domain';
import DetailButton from '../Table/DetailButton';

const BASE_ICON_WIDTH = 15; // TODO: how to get dynamic padding from css?

/**
* Advanced tree component
*
* TODO: support multiselect
* TODO: use redux state to prevent reload whole tree after active operations
* TODO: search
*
* @author Radek Tomiška
*/
class Tree extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      selected: null,
      nodes: new Immutable.Map(), // loaded node ids
      ui: new Immutable.Map() // ui state (loading decorator, last search parameters, total)
    };
  }

  componentDidMount() {
    this._loadNodes(null);
  }

  componentWillReceiveProps(newProps) {
    if (!Domain.SearchParameters.is(newProps.forceSearchParameters, this.props.forceSearchParameters)
        || this.props.rendered !== newProps.rendered) {
      this.reload();
    }
  }

  getComponentKey() {
    return 'component.advanced.Tree';
  }

  getUiKey() {
    return this.props.uiKey;
  }

  /**
  * Manager for entity in tree
  */
  getManager() {
    return this.props.manager;
  }

  /**
   * Reload tree
   */
  reload() {
    this.setState({
      selected: null,
      nodes: new Immutable.Map(),
      ui: new Immutable.Map()
    }, () => {
      const { onSelect } = this.props;
      //
      this._loadNodes();
      if (onSelect) {
        onSelect(null);
      }
    });
  }

  onExpand(nodeId, event) {
    if (event) {
      event.preventDefault();
    }
    this._loadNodes(nodeId);
  }

  /**
   * Hide node children
   *
   * @param  {String} nodeId
   * @param  {func} cb
   * @param  {event} event
   */
  onCollapse(nodeId, event = null) {
    if (event) {
      event.preventDefault();
    }
    //
    let { nodes, ui } = this.state;
    if (nodes.has(nodeId)) {
      nodes = nodes.delete(nodeId);
    }
    if (ui.has(nodeId)) {
      ui = ui.delete(nodeId);
    }
    //
    this.setState({
      nodes,
      ui
    });
  }

  onNextPage(nodeId, event) {
    if (event) {
      event.preventDefault();
    }
    //
    this._loadNodes(nodeId);
  }

  onSelect(nodeId, event) {
    if (event) {
      event.preventDefault();
    }
    //
    // TODO: deselect node on click on selected node?
    this.setState({
      selected: nodeId
    }, () => {
      const { onSelect, traverse } = this.props;
      const { nodes } = this.state;
      const node = this._getNode(nodeId); // root can be given
      if (traverse && (nodeId == null || node.childrenCount > 0) && !nodes.has(nodeId)) {
        // reload
        this._loadNodes(nodeId);
      }
      if (onSelect) {
        onSelect(nodeId);
      }
    });
  }

  onDoubleClick(nodeId, event) {
    if (event) {
      event.preventDefault();
    }
    //
    const { onDoubleClick } = this.props;
    if (onDoubleClick) {
      onDoubleClick(nodeId);
    }
  }

  onDetail(nodeId, event) {
    if (event) {
      event.preventDefault();
    }
    const { onDetail } = this.props;
    if (!onDetail) {
      return;
    }
    //
    onDetail(nodeId);
  }

  _loadNodes(nodeId = null, props = null) {
    const _props = props ? props : this.props;
    const { forceSearchParameters } = _props;
    if (!_props.rendered) {
      // component is not rendered ... loading is not needed
      return;
    }
    //
    let searchParameters;
    let uiState = {};
    if (this.state.ui.has(nodeId)) {
      uiState = this.state.ui.get(nodeId);
    }
    searchParameters = null;
    if (uiState.searchParameters) {
      // next page
      searchParameters = uiState.searchParameters.setPage(uiState.searchParameters.getPage() + 1);
    } else if (nodeId === null) {
      // load roots
      searchParameters = this.getManager().getService().getRootSearchParameters();
    } else {
      searchParameters = this.getManager().getService().getTreeSearchParameters().setFilter('parent', nodeId);
    }
    //
    this.setState({
      ui: this.state.ui.set(nodeId, {
        ...uiState,
        searchParameters,
        showLoading: true
      })
    }, () => {
      let _forceSearchParameters = null;
      if (forceSearchParameters) {
        _forceSearchParameters = forceSearchParameters.setSize(null).setPage(null); // we dont want override setted pagination
      }
      searchParameters = this.getManager().mergeSearchParameters(searchParameters, _forceSearchParameters);
      this.context.store.dispatch(this.getManager().fetchEntities(searchParameters, nodeId === null ? this.getUiKey() : `${this.getUiKey()}-${nodeId}`, (json, error) => {
        let { nodes, ui } = this.state;
        if (!error) {
          let data = json._embedded[this.getManager().getCollectionType()] || [];
          data = data.map(node => node.id); // only ids are stored in state; TODO: move state to redux store (e.g. Data)
          if (nodes.has(nodeId) && searchParameters.getPage() > 0) {
            // push at end
            nodes = nodes.set(nodeId, nodes.get(nodeId).concat(data));
          } else {
            nodes = nodes.set(nodeId, data); // parentId -> children
          }
          ui = ui.set(nodeId, {
            searchParameters,
            total: json.page ? json.page.totalElements : data.length,
            showLoading: false
          });
        } else {
          this.addErrorMessage({
            level: 'error',
            key: 'error-tree-load'
          }, error);
        }
        this.setState({
          nodes,
          ui
        });
      }));
    });
  }

  /**
   * Get node from redux store
   *
   * @param  {string} nodeId
   * @return {object}
   */
  _getNode(nodeId) {
    return this.getManager().getEntity(this.context.store.getState(), nodeId);
  }

  _getNoData() {
    const { noData } = this.props;
    //
    if (noData) {
      return noData;
    }
    // default noData
    return this.i18n('noData', { defaultValue: 'No record found' });
  }

  _renderHeader() {
    const { header } = this.props;
    const { selected } = this.state;
    //
    if (selected) {
      const selectedNode = this._getNode(selected);
      const parents = [];
      let _node = selectedNode;
      while (_node !== null && _node.parent !== null) {
        _node = this._getNode(_node.parent);
        if (!_node) {
          // just for sure - redux store doesn't contain parent node
          break;
        }
        parents.push(_node);
      }
      //
      return (
        <ol
          className="breadcrumb"
          style={{
            padding: '0px 2px',
            marginBottom: 0,
            marginRight: 3,
            backgroundColor: 'transparent'
          }}>
          <li>
            <Basic.Button
              level="link"
              className="embedded"
              onClick={ this.onSelect.bind(this, null) }
              title={ this.i18n('root.link.title') }
              titlePlacement="bottom">
              { this.i18n('root.link.label') }
            </Basic.Button>
          </li>
          {
            !parents.length > 0
            ||
            <li>
              <Basic.Button
                level="link"
                className="embedded"
                onClick={ this.onSelect.bind(this, parents[0].id) }>
                <Basic.ShortText text={ this.getManager().getNiceLabel(parents[0]) }/>
              </Basic.Button>
            </li>
          }
          <li>
            <Basic.ShortText text={ this.getManager().getNiceLabel(selectedNode) }/>
          </li>
        </ol>
      );
    }
    //
    if (header) {
      return header;
    }
    return this.i18n('header');
  }

  /**
   * Render parent's child nodes
   *
   * @param  {[type]} parentId node id (null is root)
   */
  _renderNodes(parentId = null, level = 0) {
    const { traverse } = this.props;
    const { nodes, ui, selected } = this.state;
    //
    if (!nodes.has(parentId)) {
      return null;
    }
    let parentUiState = {};
    if (ui.has(parentId)) {
      parentUiState = ui.get(parentId);
    }
    //
    if (nodes.get(parentId).length === 0) {
      return (
        <Basic.Alert text={ this._getNoData() } className="no-data"/>
      );
    }
    //
    return (
      <div>
        {
          nodes.get(parentId).map(nodeId => {
            const node = this._getNode(nodeId);
            if (!node) {
              return true;
            }
            //
            let uiState = {};
            if (ui.has(node.id)) {
              uiState = ui.get(node.id);
            }

            const iconClassNames = classNames(
              'node-icon',
              { folder: node.childrenCount > 0 },
              { file: node.childrenCount === 0 },
              { showLoading: uiState.showLoading }
            );

            let icon = 'fa:file-o';
            if (node.childrenCount > 0) {
              if (nodes.has(node.id) && !traverse) {
                icon = 'fa:folder-open';
              } else {
                icon = 'fa:folder';
              }
            }
            // selected item decorator
            const nodeClassNames = classNames(
              'tree-node-row',
              { selected: selected === node.id }
            );
            //
            return (
              <div>
                <div className={ nodeClassNames }>
                  {/* Expand button */}
                  <Basic.Icon
                    rendered={ !traverse }
                    value={
                      !nodes.has(node.id)
                      ?
                      'fa:plus-square-o'
                      :
                      'fa:minus-square-o'
                    }
                    onClick={
                      !nodes.has(node.id)
                      ?
                      this.onExpand.bind(this, node.id)
                      :
                      this.onCollapse.bind(this, node.id)
                    }
                    className={ classNames(
                      'expand-icon',
                      { visible: node.childrenCount > 0 }
                    )}
                    style={{ marginLeft: 2 + (level * BASE_ICON_WIDTH) }}/> {/* dynamic margin by node level */}

                  {/* Node icon + label */}
                  <Basic.Button
                    level="link"
                    className="embedded"
                    onClick={ this.onSelect.bind(this, node.id) }
                    onDoubleClick={ this.onDoubleClick.bind(this, node.id) }>
                    <Basic.Icon
                      value={ icon }
                      className={ iconClassNames }
                      showLoading={ uiState.showLoading }/>
                    {
                      node.childrenCount
                      ?
                      `[${ this.getManager().getNiceLabel(node) }]`
                      :
                      this.getManager().getNiceLabel(node)
                    }
                  </Basic.Button>
                  {
                    !node.childrenCount
                    ||
                    <small style={{ marginLeft: 3 }}>({ node.childrenCount })</small>
                  }
                </div>
                {
                  traverse
                  ||
                  this._renderNodes(node.id, level + 1)
                }
              </div>
            );
          })
        }
        {
          !parentUiState.total
          ||
          nodes.get(parentId).length >= parentUiState.total
          ||
          <Basic.Button
            level="link"
            className="embedded"
            style={{
              marginLeft: BASE_ICON_WIDTH + (level * BASE_ICON_WIDTH)
            }}
            showLoading={ parentUiState.showLoading }
            showLoadingIcon
            onClick={ this.onNextPage.bind(this, parentId) }>
            <small>
              { this.i18n('component.advanced.Tree.moreRecords', { counter: nodes.get(parentId).length, total: parentUiState.total, escape: false } ) }
            </small>
          </Basic.Button>
        }
      </div>
    );
  }

  render() {
    const {
      rendered,
      showLoading,
      traverse,
      onDetail,
      className,
      style,
      bodyStyle
    } = this.props;
    const {
      nodes,
      ui,
      selected
    } = this.state;
    //
    const selectedNode = this._getNode(selected);
    let root = null; // root
    let parent = null; // root
    let uiState = {};
    if (traverse && selectedNode) {
      if (selectedNode.childrenCount > 0) {
        root = selectedNode.id;
        parent = selectedNode.parent;
      } else {
        root = selectedNode.parent; // parent or null as root
        const parentNode = this._getNode(selectedNode.parent);
        if (parentNode) {
          parent = parentNode.parent;
        } else {
          parent = selectedNode.parent;
        }
      }
      if (ui.has(selected)) {
        uiState = ui.get(selected);
      }
    }
    //
    if (!rendered) {
      return null;
    }
    const _showLoading = !nodes.has(null) || showLoading;
    //
    return (
      <div className={ classNames('advanced-tree', className) } style={ style }>
        <div className="basic-toolbar tree-header">
          <div className="tree-header-text">
            { this._renderHeader() }
          </div>
          <div className="tree-header-buttons">
            <DetailButton
              rendered={ selectedNode !== null && onDetail ? true : false }
              onClick={ this.onDetail.bind(this, selected) }
              title={ this.i18n('detail.link.title') }/>

            {/* RT: search prepare  */}
            <Basic.Button
              className="btn-xs hidden"
              showLoading={ _showLoading }
              icon="filter"
              style={{ marginLeft: 3 }}/>

            <Basic.Button
              title={ this.i18n('reload') }
              titlePlacement="bottom"
              className="btn-xs"
              onClick={ this.reload.bind(this) }
              showLoading={ _showLoading }
              icon="fa:refresh"
              style={{ marginLeft: 3 }}/>
          </div>
        </div>
        {/* RT: RT: search prepare */}
        <div className="basic-toolbar hidden" style={{ display: 'flex', marginBottom: 0 }}>
          <div style={{ flex: 1 }}>
            <Basic.TextField
              label={ null }
              placeholder={ 'Search ...' }
              className="small"
              style={{ marginBottom: 0 }}/>
          </div>
          <div className="text-right">
            <Basic.Button
              className="btn-xs"
              showLoading={ _showLoading }
              icon="filter"
              style={{ marginLeft: 3 }}/>
            <Basic.Button
              className="btn-xs"
              showLoading={ _showLoading }
              icon="remove"
              style={{ marginLeft: 3 }}/>
          </div>
        </div>
        <div className="tree-body" style={ bodyStyle }>
          {
            _showLoading
            ?
            <Basic.Loading isStatic show />
            :
            <div>
              {
                !traverse || !root
                ||
                <Basic.Button
                  level="link"
                  className="embedded parent-link"
                  onClick={ this.onSelect.bind(this, parent) }
                  showLoading={ uiState.showLoading }
                  title={ this.i18n('parent.link.title') }>
                  <Basic.Icon
                    value="fa:level-down"
                    className="fa-rotate-180 parent-icon"/>
                  [{ this.i18n('parent.link.label') }]
                  <Basic.Icon
                    value="refresh"
                    showLoading
                    rendered={ uiState.showLoading === true }
                    style={{ marginLeft: 5 }}/>
                </Basic.Button>
              }
              { this._renderNodes(root) }
            </div>
          }
        </div>
      </div>
    );
  }
}

Tree.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Key prefix in redux (loading / store data).
   */
  uiKey: PropTypes.string.isRequired,
  /**
  * EntityManager for fetching entities in tree. Manager's underlying service should support methods:
  * - getRootSearchParameters() - returns search parameters to find roots
  * - getTreeSearchParameters() - returns search parameters to find children with 'parent' paremeter filter.
  */
  manager: PropTypes.object.isRequired,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * On select node callback. Selected node is given as parameter
   */
  onSelect: PropTypes.func,
  /**
   * On double click node callback. Selected node is given as parameter
   */
  onDoubleClick: PropTypes.func,
  /**
   * Show detail function.
   */
  onDetail: PropTypes.func,
  /**
   * Traverse to selected folder
   */
  traverse: PropTypes.bool,
  /**
   * Tree header
   */
  header: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element
  ]),
  /**
   * If tree roots are empty, then this text will be shown
   */
  noData: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  /**
   * Tree css
   */
  className: PropTypes.string,
  /**
   * Tree styles
   */
  style: PropTypes.object,
  /**
   * Tree body css
   */
  bodyClassName: PropTypes.string,
  /**
   * Tree body styles
   */
  bodyStyle: PropTypes.object
};

Tree.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  traverse: false
};

function select(state, component) {
  const manager = component.manager;
  const uiKey = component.uiKey;
  //
  return {
    _showLoading: manager.isShowLoading(state, uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(Tree);
