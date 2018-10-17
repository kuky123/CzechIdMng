import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Advanced, Domain, Managers, Utils } from 'czechidm-core';
import { SystemMappingManager, SystemManager } from '../../redux';
import uuid from 'uuid';
import SystemEntityTypeEnum from '../../domain/SystemEntityTypeEnum';
import SystemOperationTypeEnum from '../../domain/SystemOperationTypeEnum';

const uiKey = 'system-mappings-table';
const manager = new SystemMappingManager();
const systemManager = new SystemManager();

class SystemMappings extends Advanced.AbstractTableContent {

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
    return 'acc:content.system.mappings';
  }

  componentDidMount() {
    this.selectNavigationItems(['sys-systems', 'system-mappings']);
  }

  showDetail(entity, add) {
    const systemId = entity._embedded && entity._embedded.system ? entity._embedded.system.id : this.props.params.entityId;
    if (add) {
      // When we add new object class, then we need use "new" url
      const uuidId = uuid.v1();
      this.context.router.push(`system/${systemId}/mappings/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`system/${systemId}/mappings/${entity.id}/detail`);
    }
  }

  render() {
    const { entityId } = this.props.params;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('systemId', entityId);
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
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
                  onClick={this.showDetail.bind(this, { }, true)}
                  rendered={Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE'])}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }>
            <Advanced.Column
              property=""
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex], false)}/>
                  );
                }
              }/>
            <Advanced.Column
              property="operationType"
              width="100px"
              face="enum"
              enumClass={SystemOperationTypeEnum}
              header={this.i18n('acc:entity.SystemMapping.operationType')}
              sort/>
            <Advanced.ColumnLink
              to={`system/${entityId}/mappings/:id/detail`}
              property="name"
              face="text"
              header={this.i18n('acc:entity.SystemMapping.name')}
              sort/>
            <Advanced.Column
              property="_embedded.objectClass.objectClassName"
              face="text"
              header={this.i18n('acc:entity.SystemMapping.objectClass')}
              sort/>
            <Advanced.Column
              property="entityType"
              face="enum"
              enumClass={SystemEntityTypeEnum}
              header={this.i18n('acc:entity.SystemMapping.entityType')}
              sort/>
          </Advanced.Table>
        </Basic.Panel>
      </div>
    );
  }
}

SystemMappings.propTypes = {
  system: PropTypes.object,
  _showLoading: PropTypes.bool,
};
SystemMappings.defaultProps = {
  system: null,
  _showLoading: false,
};

function select(state, component) {
  return {
    system: Utils.Entity.getEntity(state, systemManager.getEntityType(), component.params.entityId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SystemMappings);
