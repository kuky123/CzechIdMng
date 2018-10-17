import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import { AuditManager, DataManager } from '../../redux';
import * as Basic from '../../components/basic';
import AuditDetailTable from './AuditDetailTable';
import AuditDetailInfo from './AuditDetailInfo';

const auditManager = new AuditManager();

/**
 * uiKey for diff detail values
 */
const AUDIT_DETAIL_DIFF = 'auditDiff';

/**
 * uiKey for previous version
 */
const AUDIT_PREVIOUS_VERSION = 'auditPreviousVersion';

const FIRST_ENTITY_UIKEY = 'firstEntityUiKey';
const SECOND_ENTITY_UIKEY = 'secondEntityUiKey';

/**
 * Audit detail content
 *
 * @author Ondřej Kopr
 */
class AuditDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    // showLoadingSelect is used for reload versions and diff
    this.state = {
      noVersion: false,
      showLoadingSelect: true
    };
  }

  getContentKey() {
    return 'content.audit';
  }

  componentDidMount() {
    this.selectNavigationItems(['audit', 'audit-entities']);
    this._reloadComponent(this.props);
  }

  /**
   * After change props is necessary to reload tables with data
   */
  componentWillReceiveProps(nextProps) {
    const { entityId, revID } = this.props.params;

    if (entityId !== nextProps.params.entityId || revID !== nextProps.params.revID) {
      this._reloadComponent(nextProps);
    }
  }

  _reloadComponent(props) {
    const { entityId, revID } = props.params;
    // fetch first audit detail
    this.context.store.dispatch(auditManager.fetchEntity(entityId, FIRST_ENTITY_UIKEY));
    if (revID) {
      // if exist revID (params), fetch second audit detail, also is needed set showLoadingSelect
      this.context.store.dispatch(auditManager.fetchEntity(revID, SECOND_ENTITY_UIKEY, () => {
        // just set showLoadingSelect to false, we have second revision, or not?
        this.setState({
          showLoadingSelect: false
        });
      }));
      // fetch diff between audit details
      this.context.store.dispatch(auditManager.fetchDiffBetweenVersion(entityId, revID, AUDIT_DETAIL_DIFF));
    } else {
      // fetch previous version, revID not exist
      this.context.store.dispatch(auditManager.fetchPreviousVersion(entityId, AUDIT_PREVIOUS_VERSION, (previousVersion) => {
        // if previousVersion is null then audit detail first hasn't other version
        if (previousVersion === null) {
          this.setState({
            noVersion: true,
            showLoadingSelect: false
          });
        }
        if (previousVersion) {
          this.context.router.replace(`/audit/entities/${entityId}/diff/${previousVersion.id}`);
          this.setState({
            noVersion: false
          });
        }
      }));
    }
  }

  changeSecondRevision(rev) {
    const { entityId, revID } = this.props.params;
    // if bouth ids are same do nothing
    if (parseInt(revID, 10) !== rev.id) {
      this.setState({
        noVersion: false,
        showLoadingSelect: true
      });
      if (rev) {
        this.context.router.replace(`/audit/entities/${entityId}/diff/${rev.id}`);
      } else {
        this.context.router.replace(`/audit/entities/${entityId}/diff`);
      }
    }
  }

  /**
   * Method check if version types are same
   */
  _sameType(firstVersion, secondRevision) {
    return firstVersion && secondRevision && firstVersion.entityId === secondRevision.entityId;
  }

  render() {
    const { auditDetailFirst, auditDetailSecond, diffValues, previousVersion } = this.props;
    const { noVersion, showLoadingSelect } = this.state;
    const auditDetailSecondFinal = auditDetailSecond !== null ? auditDetailSecond : previousVersion;

    return (
      <Basic.Row>
        <Helmet title={this.i18n('title')} />

        <Basic.PageHeader>
          <Basic.Icon value="eye-open"/>
          {' '}
          {this.i18n('header')}
          {' '}
          <small>{this.i18n('detail')}</small>
        </Basic.PageHeader>

        <Basic.Panel>
          <Basic.PanelBody style={{ padding: '0 15px' }}>
            <Basic.Row >
              <div className="col-md-6">
                <AuditDetailInfo
                  ref="detailFirst"
                  showLoading={auditDetailFirst === null}
                  auditDetail={auditDetailFirst} />
              </div>
              <div className="col-md-6">
                <AuditDetailInfo ref="detailSecond"
                  auditDetail={auditDetailSecondFinal} useAsSelect
                  noVersion={noVersion}
                  showLoading={showLoadingSelect}
                  cbChangeSecondRev={this.changeSecondRevision.bind(this)}
                  auditManager={auditManager}
                  forceSearchParameters={auditManager.getDefaultSearchParameters().setFilter('entityId', auditDetailFirst ? auditDetailFirst.entityId : null)} />
              </div>
            </Basic.Row>
            <Basic.Row >
              <AuditDetailTable detail={auditDetailFirst} showLoading={auditDetailFirst === null} />

              {
                noVersion
                ?
                <div className="col-md-6">
                  <Basic.Alert text={this.i18n('noPreviousRevision')} className="no-margin"/>
                </div>
                :
                <AuditDetailTable
                  showLoading={showLoadingSelect}
                  detail={this._sameType(auditDetailFirst, auditDetailSecondFinal) ? auditDetailSecondFinal : null}
                  diffValues={diffValues ? diffValues.diffValues : null}/>
              }
            </Basic.Row>
          </Basic.PanelBody>
          <Basic.PanelFooter>
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
      </Basic.Row>
    );
  }
}

AuditDetail.propTypes = {
  auditDetailFirst: PropTypes.object
};

AuditDetail.defaultProps = {
};

function select(state, component) {
  const { entityId, revID } = component.params;

  return {
    userContext: state.security.userContext,
    auditDetailFirst: auditManager.getEntity(state, entityId),
    auditDetailSecond: auditManager.getEntity(state, revID),
    previousVersion: DataManager.getData(state, AUDIT_PREVIOUS_VERSION),
    diffValues: DataManager.getData(state, AUDIT_DETAIL_DIFF)
  };
}

export default connect(select)(AuditDetail);
