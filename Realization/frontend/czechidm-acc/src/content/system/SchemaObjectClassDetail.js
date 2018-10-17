import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import { Basic, Domain, Managers, Utils, Advanced } from 'czechidm-core';
import { SchemaObjectClassManager, SystemManager, SchemaAttributeManager} from '../../redux';

const uiKey = 'schema-object-classes';
const uiKeyAttributes = 'schema-attributes';
const schemaAttributeManager = new SchemaAttributeManager();
const systemManager = new SystemManager();
const schemaObjectClassManager = new SchemaObjectClassManager();

class SchemaObjectClassDetail extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
  }

  getManager() {
    return schemaAttributeManager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'acc:content.system.objectClassDetail';
  }

  showDetail(entity, add) {
    const systemId = this.props.params.entityId;
    if (add) {
      const uuidId = uuid.v1();
      const objectClassId = this.props._schemaObjectClass.id;
      this.context.router.push(`system/${systemId}/schema-attributes/${uuidId}/new?new=1&objectClassId=${objectClassId}`);
    } else {
      this.context.router.push(`/system/${systemId}/schema-attributes/${entity.id}/detail`);
    }
  }

  componentWillReceiveProps(nextProps) {
    const {objectClassId} = nextProps.params;
    if (objectClassId && objectClassId !== this.props.params.objectClassId) {
      this._initComponent(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    const { objectClassId} = props.params;
    if (this._getIsNew(props)) {
      this.setState({schemaObjectClass: {system: props.location.query.systemId}});
    } else {
      this.context.store.dispatch(schemaObjectClassManager.fetchEntity(objectClassId));
    }
    this.selectNavigationItems(['sys-systems', 'schema-object-classes']);
  }

  /**
   * Saves give entity
   */
  save(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    const formEntity = this.refs.form.getData();

    if (formEntity.id === undefined) {
      this.context.store.dispatch(schemaObjectClassManager.createEntity(formEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this.afterSave(createdEntity, error);
      }));
    } else {
      this.context.store.dispatch(schemaObjectClassManager.updateEntity(formEntity, `${uiKey}-detail`, this.afterSave.bind(this)));
    }
  }

  afterSave(entity, error) {
    if (!error) {
      const systemId = this.props.params.entityId;
      this.addMessage({ message: this.i18n('save.success', { name: entity.objectClassName }) });
      if (this._getIsNew()) {
        this.context.router.replace(`/system/${systemId}/object-classes/${entity.id}/detail`, {objectClassId: entity.id});
      }
    } else {
      this.addError(error);
    }
  }

  closeDetail() {
    this.refs.form.processEnded();
  }

  _getIsNew(nextProps) {
    const { query } = nextProps ? nextProps.location : this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { _showLoading, _schemaObjectClass} = this.props;
    const forceSearchParameters = new Domain.SearchParameters().setFilter('objectClassId', _schemaObjectClass ? _schemaObjectClass.id : Domain.SearchParameters.BLANK_UUID);
    const isNew = this._getIsNew();
    const schemaObjectClass = isNew ? this.state.schemaObjectClass : _schemaObjectClass;
    const systemId = this.props.params.entityId;
    return (
      <div>
        <form onSubmit={this.save.bind(this)}>
          <Helmet title={this.i18n('title')} />
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Basic.ContentHeader>
            <Basic.Icon value="compressed"/>
            {' '}
            <span dangerouslySetInnerHTML={{ __html: this.i18n('objectClassHeader') }}/>
          </Basic.ContentHeader>
          <Basic.Panel className="no-border">
            <Basic.AbstractForm ref="form" data={schemaObjectClass} showLoading={_showLoading}>
              <Basic.SelectBox
                ref="system"
                manager={systemManager}
                label={this.i18n('acc:entity.SchemaObjectClass.system')}
                readOnly
                required/>
              <Basic.TextField
                ref="objectClassName"
                label={this.i18n('acc:entity.SchemaObjectClass.objectClassName')}
                required
                max={255}/>
              <Basic.Checkbox hidden
                ref="container"
                label={this.i18n('acc:entity.SchemaObjectClass.container')}/>
              <Basic.Checkbox hidden
                ref="auxiliary"
                label={this.i18n('acc:entity.SchemaObjectClass.auxiliary')}/>
            </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link"
                onClick={this.context.router.goBack}
                showLoading={_showLoading}>
                {this.i18n('button.back')}
              </Basic.Button>
              <Basic.Button
                onClick={this.save.bind(this)}
                level="success"
                type="submit"
                rendered={ Managers.SecurityManager.hasAnyAuthority(['SYSTEM_UPDATE']) }
                showLoading={_showLoading}>
                {this.i18n('button.saveAndContinue')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
        <Basic.ContentHeader rendered={schemaObjectClass && !isNew} style={{ marginBottom: 0 }}>
          <Basic.Icon value="list"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('schemaAttributesHeader') }}/>
        </Basic.ContentHeader>
        <Basic.Panel rendered={schemaObjectClass && !isNew} className="no-border last">
          <Advanced.Table
            ref="table"
            uiKey={uiKeyAttributes}
            manager={schemaAttributeManager}
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
            }
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.name.placeholder')}/>
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
                to={`system/${systemId}/schema-attributes/:id/detail`}
                property="name"
                header={this.i18n('acc:entity.SchemaAttribute.name')}
                sort />
              <Advanced.Column property="classType" header={this.i18n('acc:entity.SchemaAttribute.classType')} sort/>
              <Advanced.Column property="required" face="boolean" header={this.i18n('acc:entity.SchemaAttribute.required')} sort/>
              <Advanced.Column property="multivalued" face="boolean" header={this.i18n('acc:entity.SchemaAttribute.multivalued')} sort/>
            </Advanced.Table>
          </Basic.Panel>
        </div>
    );
  }
}

SchemaObjectClassDetail.propTypes = {
  system: PropTypes.object,
  _showLoading: PropTypes.bool,
};
SchemaObjectClassDetail.defaultProps = {
  system: null,
  _showLoading: false,
};

function select(state, component) {
  const entity = Utils.Entity.getEntity(state, schemaObjectClassManager.getEntityType(), component.params.objectClassId);
  if (entity) {
    const system = entity._embedded && entity._embedded.system ? entity._embedded.system.id : null;
    entity.system = system;
  }
  return {
    _schemaObjectClass: entity,
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-detail`),
  };
}

export default connect(select)(SchemaObjectClassDetail);
