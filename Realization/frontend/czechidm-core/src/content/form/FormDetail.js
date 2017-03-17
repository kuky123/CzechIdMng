import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import { FormDefinitionManager, SecurityManager } from '../../redux';

const manager = new FormDefinitionManager();

/**
* Form detail
*/
class FormDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      _showLoading: true,
    };
  }

  getContentKey() {
    return 'content.formDefinitions';
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'forms', 'forms-detail']);
    const { entityId } = this.props.params;
    const { isNew } = this.props;

    if (isNew) {
      this.context.store.dispatch(manager.fetchEntities(manager.getDefinitionTypesSearchParameters(), null, (typesFromBe) => {
        const types = typesFromBe._embedded.resources.map(item => { return {value: item, niceLabel: item }; });
        this.setState({
          types,
          _showLoading: false
        });
      }));
      this.context.store.dispatch(manager.receiveEntity(entityId, { }));
    } else {
      this.getLogger().debug(`[FormDetail] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId, null, () => {
        this.setState({
          _showLoading: false
        });
      }));
    }
  }

  save(event) {
    const { uiKey } = this.props;

    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      _showLoading: true
    }, this.refs.form.processStarted());

    const entity = this.refs.form.getData();

    if (entity.id === undefined) {
      this.context.store.dispatch(manager.createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
        this._afterSave(createdEntity, error);
      }));
    } else {
      this.context.store.dispatch(manager.patchEntity(entity, `${uiKey}-detail`, this._afterSave.bind(this)));
    }
  }

  /**
  * Just set showloading to false and set processEnded to form.
  * Call after save/create
  */
  _afterSave(entity, error) {
    const { isNew } = this.props;
    if (error) {
      this.setState({
        _showLoading: false
      }, this.refs.form.processEnded());
      this.addError(error);
      return;
    }
    this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    if (isNew) {
      this.context.router.replace(`/forms`);
    }
  }

  render() {
    const { uiKey, entity, showLoading, isNew } = this.props;
    const { types, _showLoading } = this.state;

    return (
      <form onSubmit={this.save.bind(this)}>
        <Basic.Panel className={isNew ? '' : 'no-border last'} showLoading={showLoading || _showLoading}>
          <Basic.PanelHeader text={isNew ? this.i18n('create.header') : this.i18n('content.formDefinitions.detail.title')} />
          <Basic.PanelBody style={isNew ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
            <Basic.AbstractForm ref="form" uiKey={uiKey} data={entity}
            readOnly={!SecurityManager.hasAuthority('EAV_FORM_DEFINITIONS_WRITE')} rendered={!(showLoading || _showLoading)}>
            <Basic.TextField
              ref="name"
              label={this.i18n('entity.FormDefinition.name')}
              readOnly={!isNew}
              max={255}/>
            {
              isNew
              ?
              <Basic.EnumSelectBox
                ref="type"
                placeholder={this.i18n('entity.FormDefinition.type')}
                required
                showLoading={types}
                options={types}/>
              :
              <Basic.TextField
                ref="type"
                readOnly={!isNew}
                label={this.i18n('entity.FormDefinition.type')}
                max={255}/>
            }
          </Basic.AbstractForm>
          </Basic.PanelBody>
          <Basic.PanelFooter showLoading={showLoading} >
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
            <Basic.Button
              type="submit"
              level="success"
              showLoadingIcon
              showLoadingText={this.i18n('button.saving')}
              rendered={SecurityManager.hasAuthority('EAV_FORM_DEFINITIONS_WRITE')}>
              {this.i18n('button.save')}
            </Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
      </form>
    );
  }
}

FormDetail.propTypes = {
  uiKey: PropTypes.string,
  definitionManager: PropTypes.object,
  isNew: PropTypes.bool
};
FormDetail.defaultProps = {
  isNew: false,
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(FormDetail);
