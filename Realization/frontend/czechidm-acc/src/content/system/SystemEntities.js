import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Advanced, Domain, Managers, Utils } from 'czechidm-core';
import { SystemEntityManager, SystemManager } from '../../redux';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';

const uiKey = 'system-entities-table';
const manager = new SystemEntityManager();
const systemManager = new SystemManager();

/**
 * Entities in target system
 *
 * @author Radek Tomiška
 */
class SystemEntitiesContent extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.entities';
  }

  getNavigationKey() {
    return 'system-entities';
  }

  showDetail(entity) {
    const entityFormData = _.merge({}, entity, {
      system: entity._embedded && entity._embedded.system ? entity._embedded.system.id : this.props.params.entityId
    });
    if (!Utils.Entity.isNew(entity)) {
      manager.getService().getConnectorObject(entity.id)
      .then(json => {
        const detail = this.state.detail;
        detail.connectorObject = json;
        this.setState({detail});
      })
      .catch(error => {
        this.addError(error);
      });
    }
    //
    super.showDetail(entityFormData, () => {
      this.refs.uid.focus();
    });
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      this.addMessage({ message: this.i18n('save.success', { name: entity.uid }) });
    }
    super.afterSave();
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoading } = this.props;
    const { detail } = this.state;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('systemId', entityId);

    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          { this.i18n('header', { escape: false }) }
        </Basic.ContentHeader>

        <Basic.Panel className="no-border last">
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={this.getManager()}
            forceSearchParameters={forceSearchParameters}
            showRowSelection={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])}
            actions={
              Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])
              ?
              [{ value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }]
              :
              null
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { entityType: SystemEntityTypeEnum.findKeyBySymbol(SystemEntityTypeEnum.IDENTITY) })}
                  rendered={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }
            filter={
              <Filter
                ref="filterForm"
                onSubmit={ this.useFilter.bind(this) }
                onCancel={ this.cancelFilter.bind(this) } />
            }
            _searchParameters={ this.getSearchParameters() }>
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_READ']) }
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex])}/>
                  );
                }
              }/>
            <Advanced.ColumnLink
              to={
                ({ rowIndex, data }) => {
                  this.showDetail(data[rowIndex]);
                }
              }
              property="uid"
              header={this.i18n('acc:entity.SystemEntity.uid')}
              sort />
            <Advanced.Column property="entityType" width="75px" header={this.i18n('acc:entity.SystemEntity.entityType')} sort face="enum" enumClass={SystemEntityTypeEnum} />
            <Advanced.Column
              property="wish"
              sort
              face="boolean" />
          </Advanced.Table>
        </Basic.Panel>

        <Basic.Modal
          bsSize="large"
          show={detail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static"
          keyboard={!_showLoading}>

          <form onSubmit={this.save.bind(this, {})}>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('create.header')} rendered={ Utils.Entity.isNew(detail.entity) }/>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('edit.header', { name: detail.entity.name })} rendered={ !Utils.Entity.isNew(detail.entity) }/>
            <Basic.Modal.Body>
              <Basic.AbstractForm
                ref="form"
                showLoading={ _showLoading }
                readOnly={ !Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                <Basic.SelectBox
                  ref="system"
                  manager={systemManager}
                  label={this.i18n('acc:entity.SystemEntity.system')}
                  readOnly
                  required/>
                <Basic.TextField
                  ref="uid"
                  label={this.i18n('acc:entity.SystemEntity.uid')}
                  required
                  max={1000}/>
                <Basic.EnumSelectBox
                  ref="entityType"
                  enum={SystemEntityTypeEnum}
                  label={this.i18n('acc:entity.SystemEntity.entityType')}
                  required/>
                <Basic.Checkbox
                  ref="wish"
                  label={ this.i18n('acc:entity.SystemEntity.wish.label') }
                  helpBlock={ this.i18n('acc:entity.SystemEntity.wish.help', { escape: false }) }/>
              </Basic.AbstractForm>

              <Basic.ContentHeader text={ this.i18n('acc:entity.SystemEntity.attributes') } rendered={ !Utils.Entity.isNew(detail.entity) }/>

              <Basic.Table
                showLoading = {!detail || !detail.hasOwnProperty('connectorObject')}
                data={detail && detail.connectorObject ? detail.connectorObject.attributes : null}
                noData={this.i18n('component.basic.Table.noData')}
                className="table-bordered"
                rendered={ !Utils.Entity.isNew(detail.entity) }>
                <Basic.Column property="name" header={this.i18n('label.property')}/>
                <Basic.Column property="values" header={this.i18n('label.value')}/>
              </Basic.Table>
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.closeDetail.bind(this)}
                showLoading={_showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}
                rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

SystemEntitiesContent.propTypes = {
  system: PropTypes.object,
  _showLoading: PropTypes.bool,
};
SystemEntitiesContent.defaultProps = {
  system: null,
  _showLoading: false,
};

function select(state, component) {
  return {
    system: Utils.Entity.getEntity(state, systemManager.getEntityType(), component.params.entityId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
    _searchParameters: Utils.Ui.getSearchParameters(state, uiKey)
  };
}

export default connect(select)(SystemEntitiesContent);

/**
 * Table filter component
 *
 * @author Radek Tomiška
 */
class Filter extends Advanced.Filter {

  render() {
    const { onSubmit, onCancel } = this.props;
    //
    return (
      <Advanced.Filter onSubmit={ onSubmit }>
        <Basic.AbstractForm ref="filterForm">
          <Basic.Row className="last">
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.EnumSelectBox
                ref="entityType"
                placeholder={ this.i18n('acc:entity.SystemEntity.entityType') }
                enum={ SystemEntityTypeEnum }/>
            </Basic.Col>
            <Basic.Col lg={ 4 }>
              <Advanced.Filter.TextField
                ref="text"
                placeholder={ this.i18n('acc:content.system.entities.filter.text.placeholder') }/>
            </Basic.Col>
            <Basic.Col lg={ 4 } className="text-right">
              <Advanced.Filter.FilterButtons cancelFilter={ onCancel }/>
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
    );
  }
}
