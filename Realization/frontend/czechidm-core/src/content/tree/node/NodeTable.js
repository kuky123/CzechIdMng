import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { SecurityManager, TreeTypeManager, IdentityManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';
import IdentityTable from '../../identity/IdentityTable';
import TypeConfiguration from '../type/TypeConfiguration';

// Root nodes  key for tree
const rootNodesKey = 'tree-node-table-roots';
const treeTypeManager = new TreeTypeManager();
const identityManager = new IdentityManager();

/**
 * TODO: Add better constant for max roots count
 * @type {Number}
 */
const MAX_ROOTS_COUNT = 100000;

/**
* Table of tree nodes
*/
class NodeTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: true,
      showLoading: true,
      type: props.type,
      rootNodes: null,
      rootNodesCount: null,
      activeTab: props.activeTab
    };
  }

  getContentKey() {
    return 'content.tree.nodes';
  }

  getManager() {
    return this.props.treeNodeManager;
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { treeNodeManager } = this.props;
    const { type } = this.state;

    const searchParametersRoots = treeNodeManager.getService().getRootSearchParameters().setFilter('treeTypeId', type.id).setSort('name', true).setSize(MAX_ROOTS_COUNT);
    this.context.store.dispatch(treeNodeManager.fetchEntities(searchParametersRoots, rootNodesKey, (loadedRoots) => {
      // get redux state for get total roots count
      const uiState = Utils.Ui.getUiState(this.context.store.getState(), rootNodesKey);
      const rootNodes = loadedRoots._embedded[treeNodeManager.getCollectionType()];
      this.setState({
        rootNodes,
        rootNodesCount: uiState.total,
        showLoading: false
      });
    }));
  }

  getDefaultSearchParameters() {
    return this.getManager().getDefaultSearchParameters().setFilter('recursively', 'true');
  }

  useFilter(event) {
    const { type } = this.state;

    if (event) {
      event.preventDefault();
    }
    const data = {
      ... this.refs.filterForm.getData(),
      treeNodeId: this.refs.filterForm.getData().treeNodeId,
      treeTypeId: type.id
    };
    this.refs.table.getWrappedInstance().useFilterData(data);
  }

  _useFilterByTree(nodeId, event) {
    if (event) {
      event.preventDefault();
      // Stop propagation is important for prohibition of node tree expand.
      // After click on link node, we want only filtering ... not node expand.
      event.stopPropagation();
    }
    if (!nodeId) {
      return;
    }
    const data = {
      ... this.refs.filterForm.getData(),
      treeNodeId: nodeId
    };
    this.refs.treeNodeId.setValue(nodeId);
    this.refs.table.getWrappedInstance().useFilterData(data);
    this.refs.identityTable.getWrappedInstance().filterByTreeNodeId(nodeId);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  onDelete(bulkActionValue, selectedRows) {
    const { treeNodeManager, uiKey } = this.props;
    const { type } = this.state;
    const selectedEntities = treeNodeManager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: treeNodeManager.getNiceLabel(selectedEntities[0]), records: treeNodeManager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: treeNodeManager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(treeNodeManager.deleteEntities(selectedEntities, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: treeNodeManager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          this.context.store.dispatch(treeNodeManager.clearEntities());
          this.refs.table.getWrappedInstance().reload();
          this._changeTree(type);
        }
      }));
    }, () => {
      // nothing
    });
  }

  /**
   * Recive new form for create new node else show detail for existing org.
   */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    if (entity.id === undefined) {
      const { type } = this.state;
      const uuidId = uuid.v1();
      this.context.router.push(`/tree/nodes/${uuidId}/new?new=1&type=${type.id}`);
    } else {
      this.context.router.push(`/tree/nodes/${entity.id}/detail`);
    }
  }

  _changeTree(entity, event) {
    if (event) {
      event.preventDefault();
    }
    const { treeNodeManager, showTreeTypeSelect } = this.props;
    //
    if (!entity.id) {
      return;
    }
    //
    this.setState({
      showLoading: true
    }, () => {
      const searchParametersRoot = treeNodeManager.getService().getRootSearchParameters().setFilter('treeTypeId', entity.id);
      this.context.store.dispatch(treeNodeManager.fetchEntities(searchParametersRoot, rootNodesKey, (loadedRoot) => {
        if (loadedRoot !== null) {
          this.setState({
            rootNodes: loadedRoot._embedded[treeNodeManager.getCollectionType()],
            type: entity,
            showLoading: false
          });
        } else {
          this.setState({
            type: entity,
            showLoading: false
          });
        }
        this.cancelFilter();
        this.refs.identityTable.getWrappedInstance().filterByTreeNodeId(null);
      }));
      if (showTreeTypeSelect) {
        this.context.router.push('/tree/nodes/?type=' + entity.id);
      }
    });
  }

/**
 * Decorator for organization tree. Add custom icons and allow filtering after click on node
 */
  _orgTreeHeaderDecorator(props) {
    const style = props.style;
    const icon = props.node.isLeaf ? 'group' : 'building';
    return (
      <div style={ style.base }>
        <div style={style.title}>
          <Basic.Icon type="fa" icon={icon} style={{ marginRight: '5px' }}/>
          <Basic.Button
            level="link"
            title={props.node.name}
            onClick={this._useFilterByTree.bind(this, props.node.id)}
            style={{padding: '0px 0px 0px 0px'}}>
            { props.node.name }
            {
              !props.node.childrenCount
              ||
              <small style={{ color: '#aaa' }}>{' '}({props.node.childrenCount})</small>
            }
          </Basic.Button>
        </div>
      </div>
    );
  }

  showTypeDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/tree/types/${uuidId}?new=1&b=nodes`);
    } else {
      this.context.router.push('/tree/types/' + entity.id);
    }
  }

  _onChangeSelectTabs(activeTab) {
    this.setState({activeTab});
  }

  render() {
    const { treeNodeManager, uiKey, showTreeTypeSelect } = this.props;
    const { filterOpened, rootNodes, showLoading, type, rootNodesCount, activeTab } = this.state;
    const showTree = !showLoading && rootNodes && rootNodes.length !== 0;
    //
    return (
      <div>
        <TypeConfiguration treeTypeId={type.id}/>

        <Basic.Panel>
          <Basic.Row>
            <Basic.Col lg={ 3 } style={{ paddingRight: 0, paddingLeft: 0, marginLeft: 15, marginRight: -15 }}>
              <div className="basic-toolbar">
                <div className="pull-left">
                  <h3 style={{ margin: 0 }}>
                    { showTreeTypeSelect ? this.i18n('content.tree.typePick') : this.i18n('entity.TreeType._type') }
                  </h3>
                </div>
                <div className="pull-right">
                  <Basic.Button
                    level="success"
                    title={ this.i18n('addType') }
                    titlePlacement="bottom"
                    className="btn-xs"
                    style={{ marginRight: 3 }}
                    onClick={ this.showTypeDetail.bind(this, {}) }
                    rendered={ false }>
                    <Basic.Icon value="fa:plus"/>
                  </Basic.Button>
                  <Basic.Button
                    level="primary"
                    title={this.i18n('reloadTree')}
                    titlePlacement="bottom"
                    className="btn-xs"
                    onClick={this._changeTree.bind(this, type)}>
                    <Basic.Icon value="fa:refresh"/>
                  </Basic.Button>
                </div>
                <div className="clearfix"></div>
              </div>
              <div style={{ paddingLeft: 15, paddingRight: 15, paddingTop: 15 }}>
                {
                  (!type || !showTreeTypeSelect)
                  ||
                  <Basic.SelectBox
                    ref="treeTypeId"
                    value={ type }
                    manager={ treeTypeManager }
                    onChange={ this._changeTree.bind(this) }
                    clearable={ false } />
                }
                {
                  !showTree
                  ||
                  <Advanced.Tree
                    ref="organizationTree"
                    rootNodes={ rootNodes }
                    rootNodesCount={ rootNodesCount }
                    propertyName="name"
                    headerDecorator={this._orgTreeHeaderDecorator.bind(this)}
                    uiKey="orgTree"
                    manager={treeNodeManager}
                    />
                }
              </div>
            </Basic.Col>
            <Basic.Col lg={ 9 }>
              <Basic.Confirm ref="confirm-delete" level="danger"/>

              <Basic.Tabs activeKey={activeTab} onSelect={this._onChangeSelectTabs.bind(this)} className="tab-embedded" style={{ marginBottom: 0 }}>
                <Basic.Tab eventKey={1} title={ this.i18n('tab.identities') } style={{ borderBottom: 0, borderRight: 0, borderRadius: 0 }}>
                  <IdentityTable
                    ref="identityTable"
                    uiKey={`${uiKey}-identity`}
                    identityManager={identityManager}
                    filterOpened={filterOpened}
                    treeType={type}
                    showRowSelection/>
                </Basic.Tab>

                <Basic.Tab eventKey={2} title={ this.i18n('tab.nodes') } style={{ borderBottom: 0, borderRight: 0, borderRadius: 0 }}>

                  <Advanced.Table
                    ref="table"
                    uiKey={uiKey}
                    forceSearchParameters={new SearchParameters().setFilter('treeTypeId', type.id)}
                    manager={treeNodeManager}
                    showRowSelection={SecurityManager.hasAuthority('TREENODE_DELETE')}
                    rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
                    showLoading={showLoading}
                    filter={
                      <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                        <Basic.AbstractForm ref="filterForm">
                          <Basic.Row>
                            <Basic.Col lg={ 6 }>
                              <Advanced.Filter.TextField
                                ref="text"
                                placeholder={this.i18n('entity.TreeNode.code') + ' / ' + this.i18n('entity.TreeNode.name') }
                                help={ Advanced.Filter.getTextHelp() }/>
                            </Basic.Col>
                            <Basic.Col lg={ 6 } className="text-right">
                              <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                            </Basic.Col>
                          </Basic.Row>
                          <Basic.Row className="last">
                            <Basic.Col lg={ 6 }>
                              <Advanced.Filter.SelectBox
                                ref="treeNodeId"
                                placeholder={this.i18n('entity.TreeNode.parentId')}
                                forceSearchParameters={treeNodeManager.getDefaultSearchParameters().setFilter('treeTypeId', type.id)}
                                manager={treeNodeManager}/>
                            </Basic.Col>
                            <Basic.Col lg={ 6 }>
                              <Advanced.Filter.BooleanSelectBox
                                ref="recursively"
                                placeholder={ this.i18n('content.identities.filter.recursively.placeholder') }
                                options={ [
                                  { value: 'true', niceLabel: this.i18n('content.identities.filter.recursively.yes') },
                                  { value: 'false', niceLabel: this.i18n('content.identities.filter.recursively.no') }
                                ]}/>
                            </Basic.Col>
                          </Basic.Row>
                        </Basic.AbstractForm>
                      </Advanced.Filter>
                    }
                    filterOpened={filterOpened}
                    actions={
                      [
                        { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
                      ]
                    }
                    buttons={
                      [
                        <Basic.Button level="success" key="add_button" className="btn-xs" onClick={this.showDetail.bind(this, {})} rendered={SecurityManager.hasAuthority('TREENODE_CREATE')}>
                          <Basic.Icon type="fa" icon="plus"/>
                          {' '}
                          {this.i18n('button.add')}
                        </Basic.Button>
                      ]
                    }
                    _searchParameters={ this.getSearchParameters() }>
                    <Advanced.Column
                      header=""
                      className="detail-button"
                      cell={
                        ({ rowIndex, data }) => {
                          return (
                            <Advanced.DetailButton
                              title={this.i18n('button.detail')}
                              onClick={this.showDetail.bind(this, data[rowIndex])}/>
                          );
                        }
                      }
                      sort={false}/>
                    <Advanced.Column property="code" width="125px" sort face="text"/>
                    <Advanced.ColumnLink to="/tree/nodes/:id/detail" property="name" width="20%" sort face="text"/>
                    <Advanced.ColumnLink
                      header={ this.i18n('entity.TreeNode.parent.name') }
                      to="/tree/nodes/:_target/detail"
                      target="parent"
                      property="_embedded.parent.name"
                      sort
                      sortProperty="parent.name"/>
                    <Advanced.Column property="treeType.name" sort rendered={false}/>
                    <Advanced.Column property="disabled" sort face="bool"/>
                    <Advanced.Column property="shortName" sort rendered={false}/>
                    <Advanced.Column property="parentId" sort rendered={false}/>
                  </Advanced.Table>
                </Basic.Tab>
              </Basic.Tabs>
            </Basic.Col>
          </Basic.Row>
        </Basic.Panel>
      </div>
    );
  }
}

NodeTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  type: PropTypes.object.isRequired,
  treeNodeManager: PropTypes.object.isRequired,
  /**
   * Show tree type select
   */
  showTreeTypeSelect: PropTypes.bool,
  /**
   * Active tab
   */
  activeTab: PropTypes.number
};

NodeTable.defaultProps = {
  uiKey: 'tree-node-table',
  showTreeTypeSelect: true,
  activeTab: 1
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(NodeTable);
