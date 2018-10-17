import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { AuditManager } from '../../redux';

const auditManager = new AuditManager();

const MOD_ADD = 'ADD';

/**
* Table for detail audits
*
* @author Ondřej Kopr
*/
export class AuditDetailTable extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  componentDidMount() {
  }

  componentWillUnmount() {
  }

  getContentKey() {
    return 'content.audit';
  }

  /**
   * Method for show detail of revision, redirect to detail
   *
   * @param entityId id of revision
   */
  showDetail(entityId) {
    this.context.router.push(`/audit/entities/${entityId}`);
  }

  _getForceSearchParameters() {
    const { entityId, entityClass } = this.props;

    if (entityId !== undefined || entityClass !== undefined) {
      return auditManager.getDefaultSearchParameters().setFilter('type', entityClass).setFilter('entityId', entityId);
    }
    return null;
  }

  _prepareData(revisionValues) {
    const transformData = [];
    let index = 0;
    for (const key in revisionValues) {
      if (revisionValues.hasOwnProperty(key)) {
        if (revisionValues[key] instanceof Object) {
          for (const keySec in revisionValues[key]) {
            if (revisionValues[key].hasOwnProperty(keySec)) {
              const row = {
                'key': keySec,
                'value': revisionValues[key][keySec]
              };
              transformData[index] = row;

              index++;
            }
          }
        } else {
          const row = {
            key,
            'value': revisionValues[key]
          };
          transformData[index] = row;

          index++;
        }
      }
    }
    return transformData;
  }

  render() {
    const { detail, weight, diffValues, diffRowClass, showLoading } = this.props;
    if (detail === null || detail.revisionValues === null) {
      return null;
    }

    // transform revision values for table, key=>value
    const transformData = this._prepareData(detail.revisionValues);

    return (
      <div className={weight}>
        <Basic.Table
          showLoading={showLoading}
          data={transformData}
          noData={detail.modification === MOD_ADD ? this.i18n('revision.created') : this.i18n('revision.deleted') }
          rowClass={({ rowIndex, data }) => {
            if (diffValues && diffValues[data[rowIndex].key] !== undefined) {
              return diffRowClass;
            }
          }}>
          <Basic.Column
            property="key"
            header={ this.i18n('entity.Audit.key') }/>
          <Basic.Column
            property="value"
            header={ this.i18n('entity.Audit.value') }
            cell={
              ({ data, rowIndex }) => {
                const rowData = data[rowIndex];
                const propertyName = rowData.key;
                const propertyValue = rowData.value;
                //
                if (typeof propertyValue === 'boolean') {
                  return propertyValue ? 'true' : 'false'; // TODO? localized?
                } else if (propertyValue === null) {
                  return 'null';
                }
                // reserver audit constants
                if ((propertyName === 'modifier' || propertyName === 'creator' || propertyName === 'originalModifier' || propertyName === 'originalCreator')
                    && propertyValue !== '[SYSTEM]' && propertyValue !== '[GUEST]') {
                  return (
                    <Advanced.EntityInfo entityType="identity" entityIdentifier={ propertyValue } face="popover" />
                  );
                }
                if (Advanced.EntityInfo.getComponent(propertyName)) {
                  return (
                    <Advanced.EntityInfo entityType={ propertyName } entityIdentifier={ propertyValue } face="popover" />
                  );
                }
                return (
                  <span>{ propertyValue }</span>
                );
              }
            }/>
        </Basic.Table>
      </div>
    );
  }
}

AuditDetailTable.propTypes = {
  // columns for display, check default props.
  columns: PropTypes.arrayOf(PropTypes.string),
  // detail for audit
  detail: PropTypes.object,
  // diff value for check
  diffValues: PropTypes.object,
  // weight of table
  weight: PropTypes.string,
  // Class for row when diffValues contains key
  diffRowClass: PropTypes.string,
  showLoading: PropTypes.bool
};

AuditDetailTable.defaultProps = {
  columns: ['id', 'type', 'modification', 'modifier', 'revisionDate', 'changedAttributes'],
  weight: 'col-md-6',
  diffRowClass: 'warning'
};

function select() {
  return {
  };
}

export default connect(select)(AuditDetailTable);
