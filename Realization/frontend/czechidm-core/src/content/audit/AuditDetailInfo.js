import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';

/**
 * Audit detail with info about class IdmAudit
 *
 * @author Ondřej Kopr
 */
class AuditDetailInfo extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.audit';
  }

  _getType(name) {
    const type = name.split('.');
    return type[type.length - 1];
  }


  render() {
    const { auditDetail, cbChangeSecondRev, useAsSelect, noVersion,
      forceSearchParameters, auditManager, showLoading} = this.props;

    const data = {
      ...auditDetail,
      revisionDiff: auditDetail
    };

    return (
      <Basic.AbstractForm ref="form" data={noVersion ? {revisionDate: null} : data} showLoading={showLoading}>
        <Basic.TextField hidden={useAsSelect}
          ref="id" readOnly
          label={this.i18n('revision.id')}/>
        {
          !useAsSelect
          ||
          <Basic.SelectBox
            ref="revisionDiff"
            clearable={false}
            label={this.i18n('revision.id')}
            onChange={cbChangeSecondRev}
            forceSearchParameters={forceSearchParameters}
            manager={auditManager}/>
        }
        <Basic.TextField
          ref="entityId"
          readOnly
          label={this.i18n('revision.entityId')}/>
        <Basic.TextField
          ref="type"
          readOnly
          label={this.i18n('revision.type')}/>
        <Basic.TextField
          ref="modification"
          readOnly
          label={this.i18n('revision.modification')}/>
        <Basic.TextField
          ref="modifier"
          readOnly
          label={this.i18n('revision.modifier')}/>
        <Basic.DateTimePicker
          ref="revisionDate"
          readOnly
          label={this.i18n('revision.revisionDate')}
          timeFormat={ this.i18n('format.times') }/>
      </Basic.AbstractForm>
    );
  }
}

AuditDetailInfo.propTypes = {
  auditDetail: PropTypes.object,
  showLoading: PropTypes.bool,
  cbChangeSecondRev: PropTypes.object,
  forceSearchParameters: PropTypes.object,
  auditManager: PropTypes.object,
  useAsSelect: PropTypes.bool,
  revID: PropTypes.number,
  noVersion: PropTypes.bool
};

AuditDetailInfo.defaultProps = {
  useAsSelect: false,
  noVersion: false
};

function select() {
  return {
  };
}

export default connect(select)(AuditDetailInfo);
