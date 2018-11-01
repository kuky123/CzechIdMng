import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import moment from 'moment';
//
import { Basic, Advanced, Managers, Utils } from 'czechidm-core';
import { ProvisioningOperationManager, ProvisioningArchiveManager } from '../../redux';
import ProvisioningOperationTableComponent, { ProvisioningOperationTable } from './ProvisioningOperationTable';
import ProvisioningOperationTypeEnum from '../../domain/ProvisioningOperationTypeEnum';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
//
const manager = new ProvisioningOperationManager();
const archiveManager = new ProvisioningArchiveManager();

/**
 * Active and archived provisioning operations
 *
 * @author Radek Tomiška
 */
class ProvisioningOperations extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      detail: {
        show: false,
        entity: null
      },
      retryDialog: {
        show: false,
        bulkActionValue: null,
        ids: []
      }
    };
  }

  getManager() {
    return manager;
  }

  getContentKey() {
    return 'acc:content.provisioningOperations';
  }

  /**
   * Shows retry dialog
   *
   * @return {[type]} [description]
   */
  showRetryDialog(bulkActionValue, ids) {
    this.setState({
      retryDialog: {
        show: true,
        bulkActionValue,
        ids
      }
    });
  }

  onRetry(batch = true) {
    const { retryDialog } = this.state;
    this.closeRetryDialog();
    this.context.store.dispatch(manager.retry(retryDialog.ids, retryDialog.bulkActionValue, batch, () => {
      // clear selected rows and reload
      this.refs.table.getWrappedInstance().clearSelectedRows();
      this.refs.table.getWrappedInstance().reload();
      this.refs.archiveTable.getWrappedInstance().reload();
    }));
  }

  /**
   * Close modal retry dialog
   */
  closeRetryDialog() {
    this.setState({
      retryDialog: {
        show: false,
        bulkActionValue: null,
        ids: []
      }
    });
  }

  /**
   * Shows modal detail with given entity
   */
  showDetail(entity, isArchive) {
    this.setState({
      detail: {
        show: true,
        entity,
        isArchive
      }
    });
  }

  /**
   * Close modal detail
   */
  closeDetail() {
    this.setState({
      detail: {
        show: false,
        entity: null
      }
    });
  }

  /**
   * Transforma account or connector object value into FE property values
   *
   * @param  {object} objectValue
   * @return {string}
   */
  _toPropertyValue(objectValue) {
    return Utils.Ui.toStringValue(objectValue);
  }

  render() {
    const { forceSearchParameters, columns, uiKey, showDeleteAllButton } = this.props;
    const { detail, retryDialog } = this.state;
    // accountObject to table
    const accountData = [];
    if (detail.entity && detail.entity.provisioningContext.accountObject) {
      const accountObject = detail.entity.provisioningContext.accountObject;
      for (const schemaAttributeId in accountObject) {
        if (!accountObject.hasOwnProperty(schemaAttributeId)) {
          continue;
        }
        accountData.push({
          property: schemaAttributeId,
          value: this._toPropertyValue(accountObject[schemaAttributeId])
        });
      }
    }
    //
    const connectorData = [];
    if (detail.entity && detail.entity.provisioningContext.connectorObject) {
      const connectorObject = detail.entity.provisioningContext.connectorObject;
      connectorObject.attributes.forEach(attribute => {
        connectorData.push({
          property: attribute.name,
          value: this._toPropertyValue(attribute.values)
        });
      });
    }
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Basic.Tabs>
          <Basic.Tab eventKey={1} title={this.i18n('tabs.active.label')}>
            <ProvisioningOperationTableComponent
              ref="table"
              uiKey={ uiKey }
              manager={manager}
              isArchive={false}
              showDetail={this.showDetail.bind(this)}
              showRowSelection={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_ADMIN'])}
              forceSearchParameters={forceSearchParameters}
              columns={columns}
              showDeleteAllButton={ showDeleteAllButton }
              actions={
                [
                  { value: 'retry', niceLabel: this.i18n('action.retry.action'), action: this.showRetryDialog.bind(this) },
                  { value: 'cancel', niceLabel: this.i18n('action.cancel.action'), action: this.showRetryDialog.bind(this) }
                ]
              }/>
          </Basic.Tab>

          <Basic.Tab eventKey={2} title={this.i18n('tabs.archive.label')}>
            <ProvisioningOperationTableComponent
              ref="archiveTable"
              uiKey={ `archive-${uiKey}` }
              manager={ archiveManager }
              isArchive
              showDetail={ this.showDetail.bind(this) }
              forceSearchParameters={ forceSearchParameters }
              columns={columns}/>
          </Basic.Tab>
        </Basic.Tabs>

        <Basic.Modal
          bsSize="large"
          show={detail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static">
          <Basic.Modal.Header closeButton text={this.i18n('detail.header')}/>
          <Basic.Modal.Body>
            {
              !detail.entity
              ||
              <div>
                <Basic.AbstractForm data={detail.entity} readOnly>

                  <Basic.Row>
                    <Basic.Col lg={ 4 }>
                      <Basic.LabelWrapper label={this.i18n('entity.created')}>
                        <div style={{ margin: '7px 0' }}>
                          <Advanced.DateValue value={detail.entity.created} showTime/>
                        </div>
                      </Basic.LabelWrapper>
                    </Basic.Col>
                    <Basic.Col lg={ 8 }>
                      <Basic.EnumLabel ref="operationType" label={this.i18n('acc:entity.ProvisioningOperation.operationType')} enum={ProvisioningOperationTypeEnum}/>
                    </Basic.Col>
                  </Basic.Row>

                  <Basic.Row>
                    <Basic.Col lg={ 4 }>
                      <Basic.EnumLabel ref="entityType" label={this.i18n('acc:entity.SystemEntity.entityType')} enum={SystemEntityTypeEnum}/>
                    </Basic.Col>
                    <Basic.Col lg={ 8 }>
                      <Basic.LabelWrapper label={this.i18n('acc:entity.ProvisioningOperation.entity')}>
                        {
                          !detail.entity.entityIdentifier
                          ?
                          <span>N/A</span>
                          :
                          <Advanced.EntityInfo entityType={detail.entity.entityType} entityIdentifier={detail.entity.entityIdentifier} style={{ margin: 0 }} face="popover"/>
                        }
                      </Basic.LabelWrapper>
                    </Basic.Col>
                  </Basic.Row>

                  <Basic.Row>
                    <Basic.Col lg={ 4 }>
                      <Basic.LabelWrapper label={this.i18n('acc:entity.System.name')}>
                        <Advanced.EntityInfo
                          entityType="system"
                          entityIdentifier={ detail.entity.system }
                          entity={ detail.entity._embedded.system }
                          style={{ margin: 0 }}
                          face="popover"/>
                      </Basic.LabelWrapper>
                    </Basic.Col>
                    <Basic.Col lg={ 8 }>
                      <Basic.LabelWrapper label={this.i18n('acc:entity.SystemEntity.uid')}>
                        <div style={{ margin: '7px 0' }}>
                          {detail.isArchive ? detail.entity.systemEntityUid : detail.entity._embedded.systemEntity.uid}
                        </div>
                      </Basic.LabelWrapper>
                    </Basic.Col>
                  </Basic.Row>

                </Basic.AbstractForm>

                <Advanced.OperationResult value={ detail.entity.result } face="full"/>

                { // look out - archive doesn't have batch
                  (!detail.entity._embedded || !detail.entity._embedded.batch || !detail.entity._embedded.batch.nextAttempt)
                  ||
                  <div style={{ marginBottom: 15 }}>
                    <Basic.ContentHeader text={ this.i18n('detail.nextAttempt.header') }/>
                    <span dangerouslySetInnerHTML={{__html: this.i18n('detail.nextAttempt.label', {
                      currentAttempt: detail.entity.currentAttempt,
                      maxAttempts: detail.entity.maxAttempts,
                      nextAttempt: moment(detail.entity._embedded.batch.nextAttempt).format(this.i18n('format.datetime'))
                    })}}/>
                  </div>
                }

                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Basic.Table
                      data={accountData}
                      noData={this.i18n('component.basic.Table.noData')}
                      className="table-bordered"
                      header={ this.i18n('detail.accountObject') }>
                      <Basic.Column property="property" header={this.i18n('label.property')}/>
                      <Basic.Column property="value" header={this.i18n('label.value')}/>
                    </Basic.Table>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Basic.Table
                      data={connectorData}
                      noData={this.i18n('component.basic.Table.noData')}
                      className="table-bordered"
                      header={ this.i18n('detail.connectorObject') }>
                      <Basic.Column property="property" header={this.i18n('label.property')}/>
                      <Basic.Column property="value" header={this.i18n('label.value')}/>
                    </Basic.Table>
                  </Basic.Col>
                </Basic.Row>
              </div>
            }
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={this.closeDetail.bind(this)}>
              {this.i18n('button.close')}
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>

        <Basic.Modal
          show={retryDialog.show}
          onHide={this.closeRetryDialog.bind(this)}
          backdrop="static">
          <Basic.Modal.Header closeButton text={this.i18n(`action.${retryDialog.bulkActionValue}.header`, { count: retryDialog.ids.length})}/>
          <Basic.Modal.Body>
            <span dangerouslySetInnerHTML={{__html: this.i18n(`action.${retryDialog.bulkActionValue}.message`, { count: retryDialog.ids.length, name: manager.getNiceLabel(manager.getEntity(this.context.store.getState(), retryDialog.ids[0])) })}}/>
          </Basic.Modal.Body>
          <Basic.Modal.Footer>
            <Basic.Button
              level="warning"
              onClick={this.onRetry.bind(this, false)}>
              {this.i18n(`action.${retryDialog.bulkActionValue}.button.selected`)}
            </Basic.Button>
          </Basic.Modal.Footer>
          <Basic.Modal.Body>
            <span dangerouslySetInnerHTML={{__html: this.i18n(`action.${retryDialog.bulkActionValue}.batchMessage`, { count: retryDialog.ids.length, name: manager.getNiceLabel(manager.getEntity(this.context.store.getState(), retryDialog.ids[0])) })}}/>
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={this.closeRetryDialog.bind(this)}>
              {this.i18n('button.close')}
            </Basic.Button>
            <Basic.Button
              level="success"
              onClick={this.onRetry.bind(this, true)}>
              {this.i18n(`action.${retryDialog.bulkActionValue}.button.batch`)}
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>

      </div>
    );
  }
}

ProvisioningOperations.propTypes = {
  uiKey: PropTypes.string.isRequired,
  /**
   * Force searchparameters - system id
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Show delete all button
   */
  showDeleteAllButton: PropTypes.bool
};
ProvisioningOperations.defaultProps = {
  forceSearchParameters: null,
  columns: ProvisioningOperationTable.defaultProps.columns,
  showDeleteAllButton: true
};

function select() {
  return {
  };
}

export default connect(select)(ProvisioningOperations);
