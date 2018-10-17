import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Utils from '../../utils';
import * as Basic from '../../components/basic';
import { FormDefinitionManager, DataManager } from '../../redux';

const manager = new FormDefinitionManager();

const TYPES_UIKEY = 'typesUiKey';

/**
* Form detail
*
* @author Ondřej Kopr
* @author Radek Tomiška
*/
class FormDefinitionDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      _showLoading: true
    };
  }

  getContentKey() {
    return 'content.formDefinitions';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    const { isNew } = this.props;
    this.context.store.dispatch(manager.fetchTypes(TYPES_UIKEY));
    if (isNew) {
      this.context.store.dispatch(manager.receiveEntity(entityId, { unmodifiable: false }));
    } else {
      this.getLogger().debug(`[FormDetail] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId, null, () => {
        // set focus into name
        this.refs.code.focus();
      }));
    }
  }

  getNavigationKey() {
    return 'forms-detail';
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
    this.setState({
      _showLoading: false
    });
    this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    if (isNew) {
      this.context.router.replace(`/forms`);
    }
  }

  render() {
    const { uiKey, entity, showLoading, types, _permissions } = this.props;
    //
    return (
      <form onSubmit={this.save.bind(this)}>
        <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
          <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('content.formDefinitions.detail.title')} />
          <Basic.PanelBody style={Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
            <Basic.AbstractForm
              ref="form"
              uiKey={uiKey}
              data={entity}
              rendered={types !== undefined}
              readOnly={ !manager.canSave(entity, _permissions) }>
              <Basic.EnumSelectBox
                ref="type"
                label={this.i18n('entity.FormDefinition.type')}
                placeholder={this.i18n('entity.FormDefinition.type')}
                required
                readOnly={!entity || entity.unmodifiable || !Utils.Entity.isNew(entity)}
                options={types}
                searchable/>
              <Basic.TextField
                ref="code"
                label={this.i18n('entity.FormDefinition.code')}
                readOnly={!entity || entity.unmodifiable}
                max={255}
                required/>
              <Basic.TextField
                ref="name"
                label={this.i18n('entity.FormDefinition.name')}
                max={255}
                required/>
              <Basic.Checkbox
                ref="main"
                label={this.i18n('entity.FormDefinition.main.label')}
                helpBlock={this.i18n('entity.FormDefinition.main.help')}/>
              <Basic.Checkbox
                ref="unmodifiable"
                readOnly
                label={this.i18n('entity.FormDefinition.unmodifiable.label')}
                helpBlock={this.i18n('entity.FormDefinition.unmodifiable.help')}/>
              <Basic.TextArea
                ref="description"
                label={ this.i18n('entity.FormDefinition.description') }
                rows={4}
                max={1000}/>
            </Basic.AbstractForm>
          </Basic.PanelBody>
          <Basic.PanelFooter showLoading={showLoading} >
            <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
            <Basic.Button
              type="submit"
              level="success"
              showLoadingIcon
              showLoadingText={ this.i18n('button.saving') }
              rendered={ manager.canSave(entity, _permissions) }>
              {this.i18n('button.save')}
            </Basic.Button>
          </Basic.PanelFooter>
        </Basic.Panel>
      </form>
    );
  }
}

FormDefinitionDetail.propTypes = {
  uiKey: PropTypes.string,
  definitionManager: PropTypes.object,
  isNew: PropTypes.bool,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
FormDefinitionDetail.defaultProps = {
  isNew: false,
  _permissions: null
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId),
    types: DataManager.getData(state, TYPES_UIKEY),
    _permissions: manager.getPermissions(state, null, entityId)
  };
}

export default connect(select)(FormDefinitionDetail);
