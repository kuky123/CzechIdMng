import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import Joi from 'joi';
//
import { Basic, Utils } from 'czechidm-core';
import { ExampleProductManager } from '../../redux';

const manager = new ExampleProductManager();

/**
 * Example product form
 *
 * @author Radek Tomiška
 */
class ExampleProductDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    //
    this.state = {
      showLoading: false
    };
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'example:content.example-product.detail';
  }

  componentDidMount() {
    const { entity } = this.props;
    // set loaded entity to form
    this.refs.form.setData(entity);
    this.refs.code.focus();
  }

  /**
   * Component will receive new props, try to compare with actual,
   * then init form
   */
  componentWillReceiveProps(nextProps) {
    const { entity } = this.props;
    if (entity && entity.id !== nextProps.entity.id) {
      this.refs.form.setData(nextProps.entity);
    }
  }

  /**
   * Save entity to BE
   */
  save(afterAction, event) {
    if (event) {
      event.preventDefault();
    }
    // get entity from form
    const entity = this.refs.form.getData();
    // check form validity
    if (!this.refs.form.isFormValid()) {
      return;
    }
    // ui redux store identifier
    const { uiKey } = this.props;
    //
    this.setState({
      showLoading: true
    }, () => {
      const saveEntity = {
        ...entity,
      };

      if (Utils.Entity.isNew(saveEntity)) {
        this.context.store.dispatch(manager.createEntity(saveEntity, `${uiKey}-detail`, (createdEntity, newError) => {
          this._afterSave(createdEntity, newError, afterAction);
        }));
      } else {
        this.context.store.dispatch(manager.updateEntity(saveEntity, `${uiKey}-detail`, (patchedEntity, newError) => {
          this._afterSave(patchedEntity, newError, afterAction);
        }));
      }
    });
  }

  /**
   * `Callback` after save action ends
   */
  _afterSave(entity, error, afterAction = 'CLOSE') {
    this.setState({
      showLoading: false
    }, () => {
      if (error) {
        this.addError(error);
        return;
      }

      this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
      //
      if (afterAction === 'CLOSE') {
        // reload options with remote connectors
        this.context.router.replace(`example/products`);
      } else {
        this.context.router.replace(`example/product/${entity.id}/detail`);
      }
    });
  }

  render() {
    const { uiKey, entity, _permissions } = this.props;
    const { showLoading } = this.state;
    //
    return (
      <div>
        <Helmet title={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('edit.title') } />

        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('basic') } />

            <Basic.PanelBody
              style={ Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 } }
              showLoading={ showLoading } >
              <Basic.AbstractForm
                ref="form"
                uiKey={ uiKey }
                readOnly={ !manager.canSave(entity, _permissions) } >

                <Basic.Row>
                  <Basic.Col lg={ 2 }>
                    <Basic.TextField
                      ref="code"
                      label={ this.i18n('example:entity.ExampleProduct.code.label') }
                      required
                      max={ 255 }/>
                  </Basic.Col>
                  <Basic.Col lg={ 10 }>
                    <Basic.TextField
                      ref="name"
                      label={this.i18n('example:entity.ExampleProduct.name.label')}
                      required
                      max={ 255 }/>
                  </Basic.Col>
                </Basic.Row>

                <Basic.TextField
                  ref="price"
                  label={ this.i18n('example:entity.ExampleProduct.price.label') }
                  placeholder={ this.i18n('example:entity.ExampleProduct.price.placeholder') }
                  helpBlock={ this.i18n('example:entity.ExampleProduct.price.help') }
                  validation={ Joi.number().min(-Math.pow(10, 33)).max(Math.pow(10, 33)).concat(Joi.number().allow(null)) }/>

                <Basic.TextArea
                  ref="description"
                  label={ this.i18n('example:entity.ExampleProduct.description.label') }
                  max={ 2000 }/>

                <Basic.Checkbox
                  ref="disabled"
                  label={ this.i18n('example:entity.ExampleProduct.disabled.label') }
                  helpBlock={ this.i18n('example:entity.ExampleProduct.disabled.help') }/>
              </Basic.AbstractForm>
            </Basic.PanelBody>

            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={ this.context.router.goBack }>{ this.i18n('button.back') }</Basic.Button>

              <Basic.SplitButton
                level="success"
                title={ this.i18n('button.saveAndContinue') }
                onClick={ this.save.bind(this, 'CONTINUE') }
                showLoading={ showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ manager.canSave(entity, _permissions) }
                pullRight
                dropup>
                <Basic.MenuItem eventKey="1" onClick={ this.save.bind(this, 'CLOSE')}>{this.i18n('button.saveAndClose') }</Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          </Basic.Panel>
          {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
          <input type="submit" className="hidden"/>
        </form>
      </div>
    );
  }
}

ExampleProductDetail.propTypes = {
  /**
   * Loaded entity
   */
  entity: PropTypes.object,
  /**
   * Entity, permissions etc. fro this content are stored in redux under given key
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Logged identity permissions - what can do with currently loaded entity
   */
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
ExampleProductDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  if (!component.entity) {
    return {};
  }
  return {
    _permissions: manager.getPermissions(state, null, component.entity.id)
  };
}

export default connect(select)(ExampleProductDetail);
