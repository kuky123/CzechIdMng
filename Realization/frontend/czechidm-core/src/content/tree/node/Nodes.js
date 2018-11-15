import React from 'react';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { TreeNodeManager, TreeTypeManager, SecurityManager } from '../../../redux';
import NodeTable from './NodeTable';
import uuid from 'uuid';

// Table uiKey
const uiKey = 'tree-node-content';

/**
* Nodes list
*
* @author Radek Tomiška
*/
class Nodes extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showLoading: true,
      isNoType: false,
    };
    this.treeNodeManager = new TreeNodeManager();
    this.treeTypeManager = new TreeTypeManager();
  }

  componentDidMount() {
    super.componentDidMount();
    //
    if (this._getTypeIdFromParam()) {
      this.context.store.dispatch(this.getTypeManager().autocompleteEntityIfNeeded(this._getTypeIdFromParam(), uiKey, (type) => {
        // TODO 404
        this.setState({
          showLoading: false,
          type
        });
      }));
    } else {
      const searchParameters = this.getTypeManager().getDefaultSearchParameters().setName('autocomplete');
      this.context.store.dispatch(this.getTypeManager().fetchEntities(searchParameters, uiKey, (types) => {
        const isNoType = !types || types._embedded.treeTypes.length === 0 ? true : false;

        if (types && !isNoType) {
          this.props.history.push('/tree/nodes/', { type: types._embedded.treeTypes[0].id });
        }

        this.setState({
          showLoading: false,
          type: types ? types._embedded.treeTypes[0] : null,
          isNoType
        });
      }));
    }
  }

  getContentKey() {
    return 'content.tree.nodes';
  }

  getNavigationKey() {
    return 'tree-nodes';
  }

  getManager() {
    return this.treeNodeManager;
  }

  getTypeManager() {
    return this.treeTypeManager;
  }

  _getTypeIdFromParam() {
    const { query } = this.props.location;
    return (query) ? query.type : null;
  }

  onCreateType(event) {
    if (event) {
      event.preventDefault();
    }
    //
    const uuidId = uuid.v1();
    this.context.router.push(`/tree/types/${uuidId}?new=1`);
  }

  render() {
    const { showLoading, type, isNoType } = this.state;
    //
    return (
      <div>
        { this.renderPageHeader() }
        {
          showLoading
          ?
          <Basic.Loading isStatic showLoading/>
          :
          <span>
            <NodeTable uiKey={ uiKey } treeNodeManager={ this.getManager() } type={ type } activeTab={ 2 } rendered={ !isNoType }/>
            <Basic.Alert
              level="info"
              rendered={ isNoType }
              buttons={[
                <Basic.Button
                  level="primary"
                  text={ this.i18n('content.tree.newType.label') }
                  rendered={ SecurityManager.hasAuthority('TREE_TYPE_CREATE') }
                  onClick={ this.onCreateType.bind(this) }/>
              ]}>
              { this.i18n('content.tree.typeNotFound') }
              {' '}
              {
                !SecurityManager.hasAuthority('TREE_TYPE_CREATE')
                ||
                <a href="#" className="alert-link" onClick={ this.onCreateType.bind(this) }>{ this.i18n('content.tree.newType.title') }</a>
              }
            </Basic.Alert>
          </span>
        }
      </div>
    );
  }
}

Nodes.propTypes = {
};

Nodes.defaultProps = {
};

export default connect()(Nodes);
