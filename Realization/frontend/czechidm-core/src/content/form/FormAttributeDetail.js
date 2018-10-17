import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Joi from 'joi';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { FormAttributeManager } from '../../redux';
import PersistentTypeEnum from '../../enums/PersistentTypeEnum';
import ComponentService from '../../services/ComponentService';
//
const componentService = new ComponentService();
const manager = new FormAttributeManager();

/**
 * Form attribute detail
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
class FormAttributeDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      _showLoading: false,
      persistentType: null
    };
  }

  getContentKey() {
    return 'content.formAttributes';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;
    const { isNew, formDefinitionId } = this.props;
    if (isNew) {
      this.context.store.dispatch(manager.receiveEntity(entityId,
        {
          persistentType: PersistentTypeEnum.TEXT,
          seq: 0,
          unmodifiable: false,
          formDefinition: formDefinitionId
        }, null, () => {
          this.refs.code.focus();
          this.setState({
            persistentType: PersistentTypeEnum.findKeyBySymbol(PersistentTypeEnum.TEXT)
          });
        }));
    } else {
      this.getLogger().debug(`[FormAttributeDetail] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId, null, (entity) => {
        this.refs.code.focus();
        this.setState({
          persistentType: entity.persistentType
        });
      }));
    }
  }

  getNavigationKey() {
    return 'forms-attribute-detail';
  }

  /**
   * Default save method that catch save event from form.
   */
  save(event) {
    const { uiKey, formDefinition } = this.props;

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
    const saveEntity = {
      ...entity
    };

    if (entity.id === undefined) {
      saveEntity.formDefinition = formDefinition;
      this.context.store.dispatch(manager.createEntity(saveEntity, `${uiKey}-detail`, (createdEntity, error) => {
        this._afterSave(createdEntity, error);
      }));
    } else {
      this.context.store.dispatch(manager.patchEntity(saveEntity, `${uiKey}-detail`, this._afterSave.bind(this)));
    }
  }

  _isUnmodifiable() {
    const { entity } = this.props;
    return entity ? entity.unmodifiable : false;
  }

  /**
   * Method set showLoading to false and if is'nt error then show success message
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
      this.context.router.goBack();
    }
  }

  /**
   * Change persistent type listener
   * @param  {SelectBox.option} persistentType option from enum select box
   */
  onChangePersistentType(persistentType) {
    this.setState( {
      persistentType: persistentType.value
    }, () => {
      // clear selected face type
      this.refs.faceType.setValue(null);
    });
  }

  /**
   * Face types enum select box options by selected persistent type
   *
   * @return {arrayOf(SelectBox.option)}
   */
  getFaceTypes() {
    const { persistentType } = this.state;
    if (!persistentType) {
      return [];
    }
    //
    const types = componentService.getComponentDefinitions(ComponentService.FORM_ATTRIBUTE_RENDERER)
      .filter(component => {
        if (!component.persistentType) {
          // persistent type is required
          return false;
        }
        // persistent type has to fit
        return component.persistentType === persistentType;
      })
      .toArray()
      .map(component => {
        const _faceType = component.faceType || component.persistentType;
        return {
          value: _faceType,
          niceLabel: `${ component.labelKey ? this.i18n(component.labelKey) : _faceType }${ _faceType === component.persistentType ? ` (${ this.i18n('label.default') })` : '' }`
        };
      });
    return types;
  }

  render() {
    const { entity, showLoading, _permissions } = this.props;
    const { _showLoading } = this.state;
    //
    return (
      <div>
        <form onSubmit={this.save.bind(this)}>
          <Basic.Panel className={ Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('content.formAttributes.detail.title') } />
            <Basic.PanelBody style={ Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 } }>
              <Basic.AbstractForm
                ref="form"
                data={ entity }
                showLoading={showLoading || _showLoading}
                readOnly={ !manager.canSave(entity, _permissions) }>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.TextField
                      ref="code"
                      label={ this.i18n('entity.FormAttribute.code.label') }
                      helpBlock={ this.i18n('entity.FormAttribute.code.help') }
                      readOnly={ this._isUnmodifiable() }
                      required
                      max={ 255 }/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField
                      ref="name"
                      label={ this.i18n('entity.FormAttribute.name.label') }
                      helpBlock={ this.i18n('entity.FormAttribute.name.help') }
                      max={ 255 }
                      required/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 8 } className="col-lg-offset-4">
                    <Basic.TextField
                      ref="placeholder"
                      label={this.i18n('entity.FormAttribute.placeholder.label')}
                      helpBlock={ this.i18n('entity.FormAttribute.placeholder.help') }
                      max={255}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.EnumSelectBox
                      ref="persistentType"
                      enum={ PersistentTypeEnum }
                      readOnly={this._isUnmodifiable()}
                      label={this.i18n('entity.FormAttribute.persistentType')}
                      onChange={ this.onChangePersistentType.bind(this) }
                      max={255}
                      useSymbol={ false }
                      required
                      clearable={ false }/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField
                      ref="defaultValue"
                      label={this.i18n('entity.FormAttribute.defaultValue')}
                      max={255}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.EnumSelectBox
                      ref="faceType"
                      options={ this.getFaceTypes() }
                      label={ this.i18n('entity.FormAttribute.faceType.label') }
                      helpBlock={ this.i18n('entity.FormAttribute.faceType.help') }
                      placeholder={ this.i18n('entity.FormAttribute.faceType.placeholder') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField
                      ref="seq"
                      label={this.i18n('entity.FormAttribute.seq.label')}
                      helpBlock={this.i18n('entity.FormAttribute.seq.help')}
                      validation={Joi.number().required().integer().min(0).max(99999)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 12 }>
                    <Basic.TextArea
                      ref="description"
                      label={this.i18n('entity.FormAttribute.description')}
                      max={2000}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Checkbox
                  ref="required"
                  readOnly={this._isUnmodifiable()}
                  label={this.i18n('entity.FormAttribute.required')}/>
                <Basic.Checkbox
                  ref="readonly"
                  readOnly={this._isUnmodifiable()}
                  label={this.i18n('entity.FormAttribute.readonly')}/>
                <Basic.Checkbox
                  ref="confidential"
                  readOnly={this._isUnmodifiable()}
                  label={this.i18n('entity.FormAttribute.confidential')}/>
                <Basic.Checkbox
                  ref="multiple"
                  readOnly={this._isUnmodifiable()}
                  label={this.i18n('entity.FormAttribute.multiple')}/>
                <Basic.Checkbox
                  ref="unmodifiable"
                  readOnly
                  label={this.i18n('entity.FormAttribute.unmodifiable.label')}
                  helpBlock={this.i18n('entity.FormAttribute.unmodifiable.help')}/>
              </Basic.AbstractForm>
            </Basic.PanelBody>
            <Basic.PanelFooter showLoading={showLoading || _showLoading} >
              <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}
                rendered={ manager.canSave(entity, _permissions) }>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
      </div>
    );
  }
}

FormAttributeDetail.propTypes = {
  _permissions: PropTypes.arrayOf(PropTypes.string),
  isNew: PropTypes.bool,
  formDefinition: PropTypes.string,
};
FormAttributeDetail.defaultProps = {
  _permissions: null,
  isNew: false,
  formDefinition: null,
};

function select(state, component) {
  const { entityId } = component.params;

  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId),
    _permissions: manager.getPermissions(state, null, entityId)
  };
}

export default connect(select)(FormAttributeDetail);
