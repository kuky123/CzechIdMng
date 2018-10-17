import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { TreeTypeManager, TreeNodeManager, DataManager, SecurityManager } from '../../redux';
import NodeTable from '../tree/node/NodeTable';

/**
 * List of organizations
 *
 * @author Radek Tomiška
 */
class Organizations extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.treeTypeManager = new TreeTypeManager();
    this.treeNodeManager = new TreeNodeManager();
  }

  getContentKey() {
    return 'content.organizations';
  }

  getNavigationKey() {
    return 'organizations';
  }

  getManager() {
    return this.treeTypeManager;
  }

  componentDidMount() {
    super.componentDidMount();
    this.context.store.dispatch(this.getManager().fetchDefaultTreeType());
  }

  showTreeTypes() {
    this.context.router.push(`/tree/types`);
  }

  render() {
    const { defaultTreeType, showLoading } = this.props;
    //
    if (showLoading) {
      return (
        <div>
          {this.renderPageHeader()}

          <Basic.Panel>
            <Basic.Loading isStatic show/>
          </Basic.Panel>
        </div>
      );
    }
    //
    return (
      <div>
        {this.renderPageHeader()}

        {
          !defaultTreeType
          ?
          <Basic.Alert
            text={this.i18n('defaultTreeType.empty.message')}
            buttons={[
              <Basic.Button
                level="info"
                rendered={ SecurityManager.hasAuthority('TREETYPE_CREATE') }
                onClick={ this.showTreeTypes.bind(this) }>
                {this.i18n('defaultTreeType.empty.button')}
              </Basic.Button>
            ]}>
          </Basic.Alert>
          :
          <NodeTable uiKey="organization-table" type={defaultTreeType} treeNodeManager={this.treeNodeManager} showTreeTypeSelect={false}/>
        }

      </div>
    );
  }
}


Organizations.propTypes = {
  defaultTreeType: PropTypes.object,
  showLoading: PropTypes.bool
};
Organizations.defaultProps = {
  defaultTreeType: null,
  showLoading: true
};

function select(state) {
  return {
    defaultTreeType: DataManager.getData(state, TreeTypeManager.UI_KEY_DEFAULT_TREE_TYPE),
    showLoading: Utils.Ui.isShowLoading(state, TreeTypeManager.UI_KEY_DEFAULT_TREE_TYPE)
  };
}

export default connect(select)(Organizations);
