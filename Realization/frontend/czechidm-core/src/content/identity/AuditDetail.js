import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import { IdentityManager, DataManager } from '../../redux';
import IdentityDetail from './IdentityDetail';
import * as Advanced from '../../components/advanced';

const identityManager = new IdentityManager();

const uiKey = 'audit-';

class AuditDetail extends Basic.AbstractContent {

  getContentKey() {
    return 'content.audit';
  }

  componentWillMount() {
    this.setState({
      showLoading: true
    });
  }

  componentDidMount() {
    const { entityId, revID } = this.props.params;
    this.selectSidebarItem('profile-audit-profile-personal');
    this.context.store.dispatch(identityManager.fetchRevision(entityId, revID, uiKey + revID));
  }

  componentDidUpdate() {
  }

  render() {
    const { auditIdentity, showLoading } = this.props;
    const { entityId, revID } = this.props.params;
    return (
      <div>
          <Helmet title={this.i18n('navigation.menu.audit.profile')} />
          <Basic.Loading isStatic showLoading={showLoading} />
          {
            !auditIdentity
            ||
            <div>
              <Basic.PageHeader>
                {identityManager.getNiceLabel(auditIdentity)}
                {' '}
                  <small>
                    {
                      this.i18n('content.audit.profile.userDetail',
                        {
                          revision: revID,
                          name: auditIdentity.modifier ? auditIdentity.modifier : auditIdentity.creator,
                          date: moment(!auditIdentity.modified ? auditIdentity.created : auditIdentity.modified).format(this.i18n('format.datetime'))
                        })
                    }
                  </small>
              </Basic.PageHeader>
              <Basic.Panel>
                <Basic.PanelHeader text={<span>{identityManager.getNiceLabel(auditIdentity)} <small> Detail uživatele</small></span>} className="hidden">
                </Basic.PanelHeader>
                <Advanced.TabPanel parentId="profile-audit" params={this.props.params}>
                  <IdentityDetail identity={auditIdentity} entityId={entityId} readOnly />
                </Advanced.TabPanel>
              </Basic.Panel>
            </div>
          }
      </div>
    );
  }
}

AuditDetail.propTypes = {
  showLoading: PropTypes.bool
};

AuditDetail.defaultProps = {
  showLoading: false
};

function select(state, component) {
  const { revID } = component.params;
  return {
    auditIdentity: DataManager.getData(state, uiKey + revID),
    showLoading: identityManager.isShowLoading(state, uiKey + revID)
  };
}

export default connect(select)(AuditDetail);
