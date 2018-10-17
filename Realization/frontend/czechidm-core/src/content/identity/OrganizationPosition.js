import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { IdentityContractManager, IdentityManager, SecurityManager, DataManager, TreeNodeManager } from '../../redux';

const uiKey = 'organization-position';

/**
 * Identity position in organization structure
 * * renders prime identity contract's working position from root to position
 * * renders link to profile - basic information
 *
 * @author Radek Tomiška
 */
class OrganizationPosition extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
    this.identityContractManager = new IdentityContractManager();
    this.treeNodeManager = new TreeNodeManager();
  }

  componentDidMount() {
    const { identity, rendered } = this.props;
    if (!rendered || !identity) {
      return;
    }
    this.context.store.dispatch(this.identityManager.fetchWorkPosition(identity, `${uiKey}-${identity}`));
  }

  render() {
    const { identity, rendered, showLoading, _showLoading, _workPosition } = this.props;
    if (!rendered || !identity) {
      return null;
    }
    if (showLoading || _showLoading) {
      /* TODO: UI jumping, when position is null ...);*/
      return (
        <ol className="breadcrumb">
          <li><Basic.Icon value="refresh" showLoading/></li>
        </ol>
      );
    }
    const items = [];
    if (!_workPosition || _.isEmpty(_workPosition)) {
      //
    } else if (!_workPosition.path) {
      items.push(
        <li key={`op-${ _workPosition.contract.id }`}>
          { this.identityContractManager.getNiceLabel(_workPosition.contract) }
        </li>
      );
    } else {
      _workPosition.path.forEach(treeNode => {
        if (!SecurityManager.hasAccess({ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREETYPE_READ']})) {
          items.push(
            <li key={ `op-s-${ treeNode.id }` }>
              { this.treeNodeManager.getNiceLabel(treeNode) }
            </li>
          );
        } else {
          // link to tree node detail
          items.push(
            <li key={ `op-${treeNode.id}` } title={ this.i18n('content.identity.profile.organizationPosition.title') }>
              <Link to={ `/tree/nodes/${ treeNode.id }/detail` }>
                { this.treeNodeManager.getNiceLabel(treeNode) }
              </Link>
            </li>
          );
        }
      });
    }
    items.push(
      <li key={ `op-i-${ identity }` }>
        <Advanced.IdentityInfo username={identity} face="link"/>
      </li>
    );
    //
    return (
      <ol className="breadcrumb" style={{ marginBottom: 10 }}>
        { items }
      </ol>
    );
  }
}

OrganizationPosition.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  identity: PropTypes.string,
  _workPosition: PropTypes.object,
  defaultTreeType: PropTypes.object,
};
OrganizationPosition.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  identity: null,
  _workPosition: null,
  _showLoading: true,
  defaultTreeType: null
};

function select(state, component) {
  const uiKeyId = `${uiKey}-${component.identity}`;
  return {
    _showLoading: Utils.Ui.isShowLoading(state, uiKeyId),
    _workPosition: DataManager.getData(state, uiKeyId)
  };
}

export default connect(select)(OrganizationPosition);
