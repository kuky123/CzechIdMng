import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import Joi from 'joi';
//
import * as Utils from '../../utils';
import * as Basic from '../../components/basic';
import * as Domain from '../../domain';
import * as Advanced from '../../components/advanced';
import { SecurityManager, DataManager, GenerateValueManager, FormAttributeManager } from '../../redux';

const MAX_DESCRIPTION_LENGTH = 60;

/**
 * Table with definitions of generate values
 *
 * @author Ondřej Kopr
 */
export class GenerateValueTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.formAttributeManager = new FormAttributeManager();
    // default filter status
    // true - open
    // false - close
    this.state = {
      filterOpened: props.filterOpened,
      generatorType: null,
      _generatorTypes: [],
      detail: {
        show: false
      }
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.context.store.dispatch(this.getManager().fetchAvailableGenerators());
    this.context.store.dispatch(this.getManager().fetchSupportedTypes());
  }

  getManager() {
    return this.props.manager;
  }

  getContentKey() {
    return 'content.generateValues';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  onChangeDtoType(dtoType) {
    this._setGeneratorTypeByDtoType(dtoType ? dtoType.value : null);
    this.setState({
      generatorType: null
    }, () => {
      this.refs.generatorType.setValue(null);
    });
  }

  onChangeGeneratorType(selectedGeneratorType) {
    this._setGeneratorPropertiesByGeneratorType(selectedGeneratorType ? selectedGeneratorType.value : null);
  }

  /**
   * Set generator types by entity type. Generator types is used in selecbox.
   */
  _setGeneratorTypeByDtoType(dtoType) {
    const { availableGenerators } = this.props;

    const generators = [];
    if (dtoType) {
      if (availableGenerators) {
        availableGenerators.forEach(generator => {
          if (generator.dtoType === dtoType) {
            generators.push({
              value: generator.generatorType,
              niceLabel: this.formAttributeManager.getLocalization(generator.formDefinition, null, 'label', generator.name)
            });
          }
        });
      }
    }
    this.setState({
      _generatorTypes: generators
    });
  }

  /**
   * Set generator properties by generator java type
   */
  _setGeneratorPropertiesByGeneratorType(generatorJavaType) {
    const { availableGenerators } = this.props;
    let generatorType = null;
    if (generatorJavaType) {
      availableGenerators.forEach(generator => {
        if (generator.generatorType === generatorJavaType) {
          generatorType = generator;
        }
      });
    }
    //
    this.setState({
      generatorType
    });
  }

  /**
   * Recive new form for create new type else show detail for existing org.
   */
  showDetail(entity, event) {
    const { uiKey } = this.props;
    //
    if (event) {
      event.preventDefault();
    }
    //
    if (!Utils.Entity.isNew(entity)) {
      this.context.store.dispatch(this.getManager().fetchPermissions(entity.id, `${uiKey}-detail`));
    }
    //
    this._setGeneratorPropertiesByGeneratorType(entity.generatorType);
    this._setGeneratorTypeByDtoType(entity.dtoType);
    this.setState({
      detail: {
        show: true,
        entity
      }
    }, () => {
      this.refs.form.setData(entity);
      this.refs.generatorType.focus();
    });
  }

  closeDetail() {
    this.setState({
      detail: {
        ...this.state.detail,
        show: false
      },
      generatorType: null,
      _generatorTypes: []
    });
  }

  save(entity, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    if (this.refs.formInstance) {
      if (!this.refs.formInstance.isValid()) {
        return;
      }
    }
    //
    const formEntity = this.refs.form.getData();
    // transform generator properties
    if (this.refs.formInstance) {
      formEntity.generatorProperties = this.refs.formInstance.getProperties();
    }
    if (formEntity.dtoType) {
      formEntity.dtoType = formEntity.dtoType.value;
    }
    //
    super.save(formEntity, event);
  }

  /**
   * Return niceLabel for supported entities
   */
  _getSupportedTypeNiceLabel(dtoType) {
    if (dtoType) {
      return Utils.Ui.getSimpleJavaType(dtoType);
    }
    return dtoType;
  }

  /**
   * Return localization key for generator name
   * @return {[type]} [description]
   */
  _getLocalizationKeyForGeneratorJavaType(generatorType) {
    const { availableGenerators } = this.props;
    if (generatorType && availableGenerators) {
      const generator = availableGenerators.get(generatorType);
      if (generator) {
        return 'eav.value-generator.' + generator.name + '.label';
      }
    }
    // generator type not found
    return generatorType;
  }

  _isUnmodifiable(entity) {
    return entity ? entity.unmodifiable : false;
  }

  render() {
    const { uiKey, manager, supportedTypes, _showLoading, _permissions } = this.props;
    const { filterOpened, detail, _generatorTypes, generatorType } = this.state;
    //
    let _supportedTypes = [];
    if (supportedTypes) {
      _supportedTypes = supportedTypes._embedded.classes.map(item => { return {value: item.content, niceLabel: this._getSupportedTypeNiceLabel(item.content) }; });
      _supportedTypes.sort((one, two) => {
        return one.niceLabel.localeCompare(two.niceLabel);
      });
    }
    //
    let formInstance = new Domain.FormInstance({});
    if (generatorType && generatorType.formDefinition && detail.entity) {
      formInstance = new Domain.FormInstance(generatorType.formDefinition).setProperties(detail.entity.generatorProperties);
    }
    const showProperties = generatorType && generatorType.formDefinition && generatorType.formDefinition.formAttributes.length > 0;
    const noProperties = generatorType && generatorType.formDefinition && generatorType.formDefinition.formAttributes.length === 0;
    let generatorDescription = null;
    if (showProperties) {
      generatorDescription = this.formAttributeManager.getLocalization(generatorType.formDefinition, null, 'label', generatorType.name);
    }
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={manager}
          showLoading={_showLoading}
          showRowSelection={SecurityManager.hasAuthority('GENERATEVALUE_DELETE')}
          rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
          filter={
            <Filter
              ref="filterForm"
              onSubmit={ this.useFilter.bind(this) }
              onCancel={ this.cancelFilter.bind(this) }
              _supportedTypes={ _supportedTypes }
              _showLoading={ _showLoading }/>
          }
          filterOpened={ filterOpened }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={this.showDetail.bind(this, {regenerateValue: false, seq: 0})}
                rendered={SecurityManager.hasAuthority('GENERATEVALUE_CREATE')}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }>
          <Advanced.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.showDetail.bind(this, data[rowIndex])}/>
                );
              }
            }
            sort={false}/>
          <Advanced.Column property="seq" sort />
          <Advanced.Column property="dtoType" sort cell={ ({ rowIndex, data }) => {
            return Utils.Ui.getSimpleJavaType(data[rowIndex].dtoType);
          }}/>
          <Advanced.Column property="generatorType" sort cell={ ({ rowIndex, data }) => {
            return this.i18n(this._getLocalizationKeyForGeneratorJavaType(data[rowIndex].generatorType));
          }}/>
          <Advanced.Column property="description" cell={ ({ rowIndex, data }) => {
            if (data[rowIndex] && data[rowIndex].description !== null) {
              const description = data[rowIndex].description.replace(/<(?:.|\n)*?>/gm, ''); // remove enters
              return Utils.Ui.substringByWord(description, MAX_DESCRIPTION_LENGTH, '...');
            }
            return null;
          }}/>
          <Advanced.Column property="unmodifiable" face="bool" sort />
        </Advanced.Table>
        <Basic.Modal
          bsSize="large"
          show={detail.show}
          onHide={this.closeDetail.bind(this)}
          backdrop="static"
          keyboard={!_showLoading}>

          <form onSubmit={this.save.bind(this, {})}>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('create.header')} rendered={Utils.Entity.isNew(detail.entity)}/>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('edit.header')} rendered={!Utils.Entity.isNew(detail.entity)}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm
                ref="form"
                readOnly={!manager.canSave(detail.entity, _permissions)}
                showLoading={_showLoading}>
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Basic.EnumSelectBox
                      ref="dtoType"
                      options={ _supportedTypes }
                      onChange={ this.onChangeDtoType.bind(this) }
                      label={ this.i18n('entity.GenerateValue.dtoType.label') }
                      palceholder={ this.i18n('entity.GenerateValue.dtoType.placeholder') }
                      helpBlock={ this.i18n('entity.GenerateValue.dtoType.help') }
                      searchable
                      required
                      useObject
                      readOnly={ this._isUnmodifiable(detail.entity) }/>
                    <Basic.Alert
                      rendered={ _generatorTypes.length < 0 }
                      text={ this.i18n('noGenerators') }/>
                    <Basic.EnumSelectBox
                      ref="generatorType"
                      options={ _generatorTypes }
                      onChange={ this.onChangeGeneratorType.bind(this) }
                      label={ this.i18n('entity.GenerateValue.generatorType.label') }
                      palceholder={ this.i18n('entity.GenerateValue.generatorType.placeholder') }
                      readOnly={ this._isUnmodifiable(detail.entity) || _generatorTypes.length === 0 }
                      searchable
                      required/>
                    <Basic.TextField
                      ref="seq"
                      validation={Joi.number().integer().min(0).max(9999)}
                      label={ this.i18n('entity.GenerateValue.seq.label') }
                      help={ this.i18n('entity.GenerateValue.seq.help') }
                      readOnly={ this._isUnmodifiable(detail.entity) }/>
                    <Basic.TextArea
                      ref="description"
                      label={this.i18n('entity.GenerateValue.description.label')}
                      max={2000}/>
                    <Basic.Checkbox
                      ref="regenerateValue"
                      label={ this.i18n('entity.GenerateValue.regenerateValue.label') }
                      helpBlock={ this.i18n('entity.GenerateValue.regenerateValue.help') }/>
                    <Basic.Checkbox
                      ref="unmodifiable"
                      readOnly
                      label={this.i18n('entity.GenerateValue.unmodifiable.label')}
                      helpBlock={this.i18n('entity.GenerateValue.unmodifiable.help')}/>
                    <Basic.Checkbox
                      ref="disabled"
                      label={ this.i18n('entity.GenerateValue.disabled.label') }
                      helpBlock={ this.i18n('entity.GenerateValue.disabled.help') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Basic.Alert
                      rendered={ noProperties }
                      text={ this.i18n('noProperties') }/>
                    <Basic.Alert
                      rendered={ !showProperties }
                      text={ this.i18n('selectGenerator') }/>

                    <Basic.Alert
                      rendered={ generatorDescription }
                      text={ generatorDescription }/>

                    <div style={ showProperties ? {} : { display: 'none' }}>
                      <Basic.ContentHeader text={ this.i18n('generatorProperties.title') }/>
                      <Advanced.EavForm
                        useDefaultValue
                        ref="formInstance"
                        readOnly={!manager.canSave(detail.entity, _permissions)}
                        formInstance={ formInstance }/>
                    </div>

                  </Basic.Col>
                </Basic.Row>

              </Basic.AbstractForm>
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
                readOnly={manager.canSave(detail.entity, _permissions)}
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}>
                {this.i18n('button.save')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

GenerateValueTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  manager: PropTypes.object.isRequired,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};

GenerateValueTable.defaultProps = {
  _permissions: null
};

function select(state, component) {
  const perm = Utils.Permission.getPermissions(state, `${component.uiKey}-detail`);
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${component.uiKey}-detail`)
                || Utils.Ui.isShowLoading(state, GenerateValueManager.UI_KEY_SUPPORTED_TYPES)
                || Utils.Ui.isShowLoading(state, GenerateValueManager.UI_KEY_AVAILABLE_GENERATORS),
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey),
    supportedTypes: DataManager.getData(state, GenerateValueManager.UI_KEY_SUPPORTED_TYPES),
    availableGenerators: DataManager.getData(state, GenerateValueManager.UI_KEY_AVAILABLE_GENERATORS),
    _permissions: perm
  };
}

export default connect(select)(GenerateValueTable);

/**
 * Table filter component
 *
 * @author Radek Tomiška
 */
class Filter extends Advanced.Filter {

  focus() {
    this.refs.text.focus();
  }

  render() {
    const { onSubmit, onCancel, _showLoading, _supportedTypes } = this.props;
    //
    return (
      <Advanced.Filter onSubmit={ onSubmit }>
        <Basic.AbstractForm ref="filterForm">
          <Basic.Row>
            <Basic.Col lg={ 6 }>
              <Advanced.Filter.EnumSelectBox
                ref="dtoType"
                searchable
                showLoading={ _showLoading }
                placeholder={ this.i18n('content.generateValues.filter.dtoType') }
                options={ _supportedTypes }/>
            </Basic.Col>
            <Basic.Col lg={ 6 } className="text-right">
              <Advanced.Filter.FilterButtons showLoading={ _showLoading } cancelFilter={ onCancel }/>
            </Basic.Col>
          </Basic.Row>
        </Basic.AbstractForm>
      </Advanced.Filter>
    );
  }
}
