import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { ScriptManager, SecurityManager, ScriptAuthorityManager } from '../../redux';
import ScriptCategoryEnum from '../../enums/ScriptCategoryEnum';
import EntityUtils from '../../utils/EntityUtils';
import AbstractEnum from '../../enums/AbstractEnum';
/**
 * Detail for sript
 * * name
 * * description
 * * script area
 * * category
 * * script authorities (table)
 *
 */
export default class ScriptDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.scriptManager = new ScriptManager();
    this.scriptAuthorityManager = new ScriptAuthorityManager();
    this.state = {
      showLoading: false
    };
  }

  getContentKey() {
    return 'content.scripts';
  }

  componentDidMount() {
    const { entity } = this.props;
    this._initForm(entity);
  }

  /**
   * Method check if props in this component is'nt different from new props.
   */
  componentWillReceiveProps(nextProps) {
    // check id of old and new entity
    if (nextProps.entity.id !== this.props.entity.id) {
      this._initForm(nextProps.entity);
    }
  }

  /**
   * Method for basic initial form
   */
  _initForm(entity) {
    if (entity !== undefined) {
      if (EntityUtils.isNew(entity)) {
        entity.description = '';
        entity.category = AbstractEnum.findKeyBySymbol(ScriptCategoryEnum, ScriptCategoryEnum.DEFAULT);
      }
      this.refs.code.focus();
      this.refs.form.setData(entity);
    }
  }

  /**
   * Default save method that catch save event from form.
   */
  save(afterAction = 'CONTINUE', event) {
    const { uiKey } = this.props;

    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      showLoading: true
    }, this.refs.form.processStarted());

    const entity = this.refs.form.getData();
    // entity.category = AbstractEnum.findKeyBySymbol(ScriptCategoryEnum, entity.category);
    if (entity.id === undefined) {
      this.context.store.dispatch(this.scriptManager.createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
        this._afterSave(createdEntity, error, afterAction);
      }));
    } else {
      this.context.store.dispatch(this.scriptManager.updateEntity(entity, `${uiKey}-detail`, (updateEntity, error) => {
        this._afterSave(updateEntity, error, afterAction);
      }));
    }
  }

  /**
   * Method set showLoading to false and if is'nt error then show success message
   */
  _afterSave(entity, error, afterAction) {
    if (error) {
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      this.addError(error);
      return;
    }
    this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    if (afterAction !== 'CONTINUE') {
      this.context.router.goBack();
    } else {
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      //
      this.context.router.replace('/scripts/' + entity.id + '/detail');
    }
  }

  render() {
    const { uiKey, entity } = this.props;
    const { showLoading } = this.state;
    //
    return (
      <div>

        <Helmet title={Utils.Entity.isNew(entity) ? this.i18n('create.title') : this.i18n('edit.title')} />
        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('detail.header')} />

            <Basic.PanelBody style={Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
              <Basic.AbstractForm
                ref="form"
                uiKey={uiKey}
                readOnly={!SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'SCRIPT_CREATE' : 'SCRIPT_UPDATE')}>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.TextField
                      ref="code"
                      label={this.i18n('entity.Script.code')}
                      required
                      max={255}/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField
                      ref="name"
                      label={this.i18n('entity.Script.name')}
                      required
                      max={255}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.EnumSelectBox
                  ref="category"
                  label={this.i18n('entity.Script.category')}
                  enum={ScriptCategoryEnum}
                  max={255}
                  required/>
                <Advanced.RichTextArea ref="description" label={this.i18n('entity.Script.description')} />
                <Basic.ScriptArea
                  ref="script"
                  mode="groovy"
                  height="25em"
                  helpBlock={this.i18n('entity.Script.script.help')}
                  label={this.i18n('entity.Script.script.label')}/>
                </Basic.AbstractForm>
              </Basic.PanelBody>

              <Basic.PanelFooter showLoading={showLoading} >
                <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
                <Basic.SplitButton
                  level="success"
                  title={ this.i18n('button.saveAndContinue') }
                  onClick={ this.save.bind(this, 'CONTINUE') }
                  showLoading={ showLoading }
                  showLoadingIcon
                  showLoadingText={ this.i18n('button.saving') }
                  rendered={SecurityManager.hasAuthority(Utils.Entity.isNew(entity) ? 'SCRIPT_CREATE' : 'SCRIPT_UPDATE')}
                  dropup>
                  <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'CLOSE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
                </Basic.SplitButton>
              </Basic.PanelFooter>
              {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
              <input type="submit" className="hidden"/>
            </Basic.Panel>
          </form>
        </div>
      );
  }
}

ScriptDetail.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
};
ScriptDetail.defaultProps = {
};
