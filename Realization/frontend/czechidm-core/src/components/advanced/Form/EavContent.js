import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import classnames from 'classnames';
//
import * as Basic from '../../../components/basic';
import * as Utils from '../../../utils';
import { DataManager, FormDefinitionManager } from '../../../redux';
import EavForm from './EavForm';

const formDefinitionManager = new FormDefinitionManager();

/**
 * Content with eav form
 *
 * @author Radek Tomiška
 */
class EavContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      error: null
    };
  }

  getComponentKey() {
    return this.props.contentKey;
  }

  componentDidMount() {
    super.componentDidMount();
    // load definition and values
    const { entityId, formableManager, uiKey } = this.props;
    //
    this.context.store.dispatch(formableManager.fetchFormInstances(entityId, `${uiKey}-${entityId}`, (formInstances, error) => {
      if (error) {
        this.addErrorMessage({ hidden: true, level: 'info' }, error);
        this.setState({ error });
      } else {
        this.getLogger().debug(`[EavForm]: Loaded [${formInstances.size}] form definitions`);
      }
    }));
  }

  _createFormRef(definitionCode) {
    return `eav-form-${definitionCode}`;
  }

  save(definitionCode, event) {
    if (event) {
      event.preventDefault();
    }
    const eavFormRef = this._createFormRef(definitionCode);
    if (!this.refs[eavFormRef].isValid()) {
      return;
    }
    //
    const { entityId, formableManager, uiKey } = this.props;
    //
    const filledFormValues = this.refs[eavFormRef].getValues();
    this.getLogger().debug(`[EavForm]: Saving form [${definitionCode}]`);
    // save values
    this.context.store.dispatch(formableManager.saveFormValues(entityId, definitionCode, filledFormValues, `${uiKey}-${entityId}`, (savedFormInstance, error) => {
      if (error) {
        this.addError(error);
      } else {
        const entity = formableManager.getEntity(this.context.store.getState(), entityId);
        this.addMessage({ message: this.i18n('save.success', { name: formableManager.getNiceLabel(entity) }) });
        this.getLogger().debug(`[EavForm]: Form [${definitionCode}] saved`);
      }
    }));
  }

  render() {
    const { _formInstances, _showLoading, showSaveButton } = this.props;
    const { error } = this.state;

    let content = null;
    if (error) {
      // loading eav definition failed
      content = (
        <div style={{ paddingTop: 15 }}>
          <Basic.Alert level="info" text={this.i18n('error.notFound')} className="no-margin"/>
        </div>
      );
    } else if (!_formInstances || _showLoading) {
      // connector eav form is loaded from BE
      content = (
        <Basic.Loading isStatic showLoading/>
      );
    } else if (_formInstances.size === 0) {
      content = (
        <div style={{ paddingTop: 15 }}>
          <Basic.Alert level="info" text={this.i18n('error.notFound')} className="no-margin"/>
        </div>
      );
    } else {
      // form instances are ready
      let index = 0;
      content = _formInstances.map(_formInstance => {
        let _showSaveButton = false; // some attribute is editable
        _formInstance.getAttributes().forEach(attribute => {
          if (!attribute.readonly) { // TODO: hidden
            _showSaveButton = true;
          }
        });

        return (
          <form className="abstract-form" onSubmit={ this.save.bind(this, _formInstance.getDefinition().code) }>
            <Basic.Panel className={
                classnames({
                  last: ++index === _formInstances.size
                })
              }>

              {/* RT: back compatibilty header */}
              <Basic.PanelHeader
                text={
                  _formInstance.getDefinition().name === 'default'
                  ?
                  this.i18n('header')
                  :
                  formDefinitionManager.getLocalization(_formInstance.getDefinition(), 'label', _formInstance.getDefinition().name) }/>

              <Basic.Alert
                icon="info-sign"
                text={ formDefinitionManager.getLocalization(_formInstance.getDefinition(), 'help', _formInstance.getDefinition().description) }
                style={{ marginBottom: 0 }}/>

              <Basic.PanelBody style={{ paddingTop: 15, paddingBottom: 0 }}>
                <EavForm
                  ref={ this._createFormRef(_formInstance.getDefinition().code) }
                  formInstance={ _formInstance }
                  readOnly={ !showSaveButton }/>
              </Basic.PanelBody>

              <Basic.PanelFooter rendered={ _showSaveButton && showSaveButton && _formInstance.getAttributes().size > 0 }>
                <Basic.Button
                  type="submit"
                  level="success"
                  rendered={ _formInstance.getAttributes().size > 0 }
                  showLoadingIcon
                  showLoadingText={ this.i18n('button.saving') }>
                  { this.i18n('button.save') }
                </Basic.Button>
              </Basic.PanelFooter>
            </Basic.Panel>
          </form>
        );
      });
    }

    return (
      <div>
        <Helmet title={this.i18n('title')} />

        { content }
      </div>
    );
  }
}

EavContent.propTypes = {
  /**
   * UI identifier - it's used as key in store (saving, loading ...)
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Parent entity identifier
   */
  entityId: PropTypes.string.isRequired,
  formableManager: PropTypes.object.isRequired,
  showSaveButton: PropTypes.bool,
  /**
   * Internal properties (loaded by redux)
   */
  _formInstances: PropTypes.object, // immutable map
  _showLoading: PropTypes.bool
};
EavContent.defaultProps = {
  showSaveButton: false,
  _formInstances: null,
  _showLoading: false
};

function select(state, component) {
  const entityId = component.entityId;
  const uiKey = component.uiKey;
  //
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${uiKey}-${entityId}`),
    _formInstances: DataManager.getData(state, `${uiKey}-${entityId}`)
  };
}

export default connect(select)(EavContent);
