import React, { PropTypes } from 'react';
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
const treeTypeManager = new TreeTypeManager();
const identityManager = new IdentityManager();

/**
* Table of tree nodes
*
* @author Radek Tomiška
*/
class NodeTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      selectedNodeId: null, // selected node
      filterOpened: true,
      type: props.type,
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
    const data = {
      ... this.refs.filterForm.getData(),
      treeNodeId: nodeId
    };
    this.setState({
      selectedNodeId: nodeId
    }, () => {
      this.refs.treeNodeId.setValue(nodeId);
      this.refs.table.getWrappedInstance().useFilterData(data);
      this.refs.identityTable.getWrappedInstance().filterByTreeNodeId(nodeId);
    });
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  onDelete(bulkActionValue, selectedRows) {
    const { treeNodeManager, uiKey } = this.props;
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
          this.refs.organizationTree.getWrappedInstance().reload();
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
    //
    if (!entity.id) {
      return;
    }
    //
    this.setState({
      type: entity
    }, () => {
      this.cancelFilter();
      this.refs.identityTable.getWrappedInstance().filterByTreeNodeId(null);
      //
      const { showTreeTypeSelect } = this.props;
      if (showTreeTypeSelect) {
        this.context.router.push('/tree/nodes/?type=' + entity.id);
      }
    });
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
    this.setState({ activeTab });
  }

  _renderSelectedNode() {
    const { selectedNodeId } = this.state;
    if (!selectedNodeId) {
      return null;
    }
    const selectedNode = this.getManager().getEntity(this.context.store.getState(), selectedNodeId);
    if (!selectedNode) {
      return null;
    }
    return (
      <div className="basic-toolbar">
        <Basic.Alert
          title={ this.i18n('label.selected') }
          level="info"
          style={{ margin: 0, maxWidth: 450 }}>
          <div style={{ display: 'flex'}}>
            <div style={{ flex: 1}}>
              <Basic.ShortText text={ this.getManager().getNiceLabel(selectedNode) } maxLength={ 40 }/>
            </div>
            <Basic.Button
              type="button"
              level="primary"
              className="btn-xs"
              icon="fa:search"
              onClick={ this.showDetail.bind(this, selectedNode) }>
              { this.i18n('component.advanced.EntityInfo.link.detail.label') }
            </Basic.Button>
          </div>
        </Basic.Alert>
      </div>
    );
  }

  render() {
    const { treeNodeManager, uiKey, showTreeTypeSelect, showLoading, rendered } = this.props;
    const { filterOpened, type, activeTab } = this.state;
    const showTree = !showLoading;
    const forceSearchParameters = new SearchParameters().setFilter('treeTypeId', type.id).setSort('name', true);
    //
    if (!rendered) {
      return null;
    }
    //
    return (
      <div>
        <TypeConfiguration treeTypeId={type.id}/>

        <Basic.Panel>
          <Basic.Row>{/* FIXME: resposive design - wrong wrapping on mobile */}
            <Basic.Col lg={ 3 } style={{ paddingRight: 0 }}>
              <Advanced.Tree
                ref="organizationTree"
                uiKey="organization-tree"
                manager={ treeNodeManager }
                forceSearchParameters={ forceSearchParameters }
                onSelect={ this._useFilterByTree.bind(this) }
                ŕendered={ showTree }
                traverse
                header={
                  !type || !showTreeTypeSelect
                  ?
                  null
                  :
                  <Basic.SelectBox
                    ref="treeTypeId"
                    value={ type }
                    manager={ treeTypeManager }
                    onChange={ this._changeTree.bind(this) }
                    clearable={ false }
                    className="small"
                    style={{ marginBottom: 0 }}/>
                }
                />
            </Basic.Col>
            <Basic.Col lg={ 9 } style={{ paddingLeft: 0 }}>
              <Basic.Confirm ref="confirm-delete" level="danger"/>

              <Basic.Tabs activeKey={activeTab} onSelect={this._onChangeSelectTabs.bind(this)} className="tab-embedded" style={{ marginBottom: 0 }}>
                <Basic.Tab eventKey={2} title={ this.i18n('tab.nodes') } style={{ borderBottom: 0, borderRight: 0, borderRadius: 0 }}>

                  { this._renderSelectedNode() }

                  <Advanced.Table
                    ref="table"
                    uiKey={uiKey}
                    forceSearchParameters={new SearchParameters().setFilter('treeTypeId', type.id)}
                    manager={treeNodeManager}
                    showRowSelection={SecurityManager.hasAuthority('TREENODE_DELETE')}
                    rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
                    showLoading={ showLoading }
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
                              <Advanced.Filter.TreeNodeSelect
                                ref="treeNodeId"
                                header={ this.i18n('entity.TreeNode.parentId') }
                                placeholder={ this.i18n('entity.TreeNode.parentId') }
                                label={ null }
                                useFirstType
                                forceSearchParameters={ treeNodeManager.getDefaultSearchParameters().setFilter('treeTypeId', type.id) }/>
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

                <Basic.Tab eventKey={ 1 } title={ this.i18n('tab.identities') } style={{ borderBottom: 0, borderRight: 0, borderRadius: 0 }}>
                  <IdentityTable
                    ref="identityTable"
                    uiKey={ `${uiKey}-identity` }
                    identityManager={ identityManager }
                    filterOpened={ filterOpened }
                    treeType={ type }
                    showRowSelection/>
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
  ...Advanced.AbstractTableContent.propTypes,
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
  ...Advanced.AbstractTableContent.defaultProps,
  uiKey: 'tree-node-table',
  showTreeTypeSelect: true,
  activeTab: 2
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(NodeTable);
