import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Immutable from 'immutable';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { IdentityManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';

const uiKey = 'managers-info';

/**
 * Renders all managers by identity contract
 *
 * @author Radek Tomiška
 */
class ManagersInfo extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
  }

  componentDidMount() {
    const { managersFor, identityContractId } = this.props;
    if (identityContractId) {
      // load managers by Tree
      const uiKeyId = `${uiKey}-${identityContractId}`;
      if (!Utils.Ui.isShowLoading(this.context.store.getState(), uiKeyId)) {
        const searchParameters = new SearchParameters().setName(SearchParameters.NAME_AUTOCOMPLETE).setFilter('managersFor', managersFor).setFilter('managersByContract', identityContractId).setFilter('includeGuarantees', true);
        this.context.store.dispatch(this.identityManager.fetchEntities(searchParameters, uiKeyId));
      }
    }
  }

  getAllManagers() {
    const { _managers } = this.props;
    //
    let managers = new Immutable.OrderedMap();
    _managers.forEach(manager => {
      managers = managers.set(manager.username, manager);
    });
    return managers.toArray();
  }

  render() {
    const { identityContractId, _showLoading, _ui, detailLink } = this.props;
    if (!identityContractId) {
      return null;
    }
    const managers = this.getAllManagers();
    //
    return (
      <span>
        {
          managers.map(identity => {
            return (
              <Advanced.EntityInfo
                entityType="identity"
                entity={ identity }
                entityIdentifier={ identity.username }
                face="popover"
                style={{ marginRight: 0 }}/>
            );
          })
        }
        {
          !_showLoading
          ||
          <Basic.Icon value="refresh" showLoading />
        }
        {
          (!detailLink || _showLoading || !_ui || managers.length >= _ui.total)
          ||
          <small title={ this.i18n('entity.IdentityContract.managers.total') } style={{ whiteSpace: 'nowrap' }}>
            <a href="#" onClick={ (event) => { event.preventDefault(); detailLink(); } }>
              ... ({ _ui.total })
            </a>
          </small>
        }
      </span>
    );
  }
}

ManagersInfo.propTypes = {
  identityContractId: PropTypes.string.required,
  managersFor: PropTypes.string.required,
  _showLoading: PropTypes.bool,
  _managers: PropTypes.arrayOf(PropTypes.object),
  /**
   * link to contract detail
   *
   * @type {[type]}
   */
  detailLink: PropTypes.func
};
ManagersInfo.defaultProps = {
  _managers: [],
  _showLoading: false,
  detailLink: null
};

function select(state, component) {
  if (!component.identityContractId) {
    return {};
  }
  const uiKeyId = `${uiKey}-${component.identityContractId}`;
  return {
    _showLoading: Utils.Ui.isShowLoading(state, uiKeyId),
    _ui: Utils.Ui.getUiState(state, uiKeyId),
    _managers: Utils.Ui.getEntities(state, uiKeyId)
  };
}

export default connect(select)(ManagersInfo);
