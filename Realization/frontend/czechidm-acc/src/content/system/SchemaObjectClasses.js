import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Advanced, Domain, Managers, Utils } from 'czechidm-core';
import { SchemaObjectClassManager, SystemManager } from '../../redux';
import uuid from 'uuid';

const uiKey = 'schema-object-classes-entities-table';
const manager = new SchemaObjectClassManager();
const systemManager = new SystemManager();

class SchemaObjectClasses extends Advanced.AbstractTableContent {

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
    return 'acc:content.system.schemaObjectClasses';
  }

  componentDidMount() {
    this.selectNavigationItems(['sys-systems', 'schema-object-classes']);
  }

  showDetail(entity, add) {
    const system = entity._embedded && entity._embedded.system ? entity._embedded.system.id : this.props.params.entityId;
    if (add) {
      const uuidId = uuid.v1();
      this.context.router.push(`/system/${system}/object-classes/${uuidId}/new?new=1&systemId=${system}`);
    } else {
      this.context.router.push(`/system/${system}/object-classes/${entity.id}/detail`);
    }
  }

  save(entity, event) {
    const formEntity = this.refs.form.getData();
    //
    super.save(formEntity, event);
  }

  afterSave(entity, error) {
    if (!error) {
      this.addMessage({ message: this.i18n('save.success', { name: entity.objectClassName }) });
    } else {
      this.addError(error);
    }
    super.afterSave();
  }

  _generateSchema(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs[`confirm-delete`].show(
      this.i18n(`action.generateSchema.message`),
      this.i18n(`action.generateSchema.header`)
    ).then(() => {
      const {entityId} = this.props.params;
      this.setState({
        showLoading: true
      });
      const promise = systemManager.getService().generateSchema(entityId);
      promise.then((json) => {
        this.setState({
          showLoading: false
        });
        this.refs.table.getWrappedInstance().reload();
        this.addMessage({ message: this.i18n('action.generateSchema.success', { name: json.name }) });
      }).catch(ex => {
        this.setState({
          showLoading: false
        });
        this.addError(ex);
        this.refs.table.getWrappedInstance().reload();
      });
    }, () => {
      // Rejected
    });
  }

  render() {
    const { entityId } = this.props.params;
    const { _showLoading } = this.props;
    const { showLoading } = this.state;
    const innerShowLoading = _showLoading || showLoading;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('systemId', entityId);
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <Basic.Icon type="fa" icon="object-group"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
        <Basic.PanelBody>
          <div style={{ textAlign: 'center', display: 'block', margin: 'auto'}}>
            <Basic.Button
              level="success"
              showLoading={innerShowLoading}
              onClick={this._generateSchema.bind(this)}
              rendered={Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')}
              title={ this.i18n('generateSchemaBtnTooltip') }>
              <Basic.Icon type="fa" icon="object-group"/>
              {' '}
              { this.i18n('generateSchemaBtn') }
            </Basic.Button>
          </div>
        </Basic.PanelBody>

        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          <Basic.Icon value="compressed"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('schemaObjectClassesHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel className="no-border last">
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            showLoading={innerShowLoading}
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
              [<span>
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
              </span>
              ]
            }
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="objectClassName"
                        placeholder={this.i18n('filter.objectClassName.placeholder')}/>
                    </div>
                    <div className="col-lg-2"/>
                    <div className="col-lg-4 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
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
            <Advanced.ColumnLink
              to={
                ({ rowIndex, data }) => {
                  this.showDetail(data[rowIndex]);
                }
              }
              property="objectClassName"
              header={this.i18n('acc:entity.SchemaObjectClass.objectClassName')}
              sort />
            <Advanced.Column property="auxiliary" face="boolean" header={this.i18n('acc:entity.SchemaObjectClass.auxiliary')} hidden sort/>
            <Advanced.Column property="container" face="boolean" header={this.i18n('acc:entity.SchemaObjectClass.container')} hidden sort/>
          </Advanced.Table>
        </Basic.Panel>
      </div>
    );
  }
}

SchemaObjectClasses.propTypes = {
  system: PropTypes.object,
  _showLoading: PropTypes.bool,
};
SchemaObjectClasses.defaultProps = {
  system: null,
  _showLoading: false,
};

function select(state, component) {
  return {
    system: Utils.Entity.getEntity(state, systemManager.getEntityType(), component.params.entityId),
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SchemaObjectClasses);
