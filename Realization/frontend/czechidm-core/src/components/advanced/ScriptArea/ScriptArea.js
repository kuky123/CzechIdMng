import React, { PropTypes } from 'react';
//
import * as Basic from '../../basic';

/**
 * ScriptArea + select script by category.
 * Component with select from available script (select from script category).
 * Select script from agenda is realized as new modal window.
 *
 * @author Ondřej Kopr
 */
class ScriptArea extends Basic.ScriptArea {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showModal: false,
      script: null
    };
  }

  _closeDetail(value, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      showModal: false
    });
  }

  _selectScript(value, event) {
    const { scriptManager } = this.props;
    if (event) {
      event.preventDefault();
    }
    if (value === null) {
      return;
    }
    const selectedScriptId = this.refs.form.getData().selectedScript;
    this.context.store.dispatch(scriptManager.fetchEntity(selectedScriptId, selectedScriptId, (entity) => {
      if (entity !== null) {
        const currentValue = this.getValue();
        if (currentValue) {
          this.setValue(this.getValue() + entity.template);
        } else {
          this.setValue(entity.template);
        }
      }
    }));
    //
    this.setState({
      showModal: false
    });
    //
    // TODO: set into cursor?
    // TODO: script area focus not working
  }

  _showModal(value, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      showModal: true
    });
  }

  _chageScript(script, event) {
    if (event) {
      event.preventDefault();
    }
    if (script) {
      this.setState({
        script
      });
    } else {
      this.setState({
        script: null
      });
    }
  }

  _getAddButton() {
    return (
      <Basic.Button
        type="button"
        className="btn-xs"
        level="success"
        onClick={this._showModal.bind(this)}>
        {this.i18n('entity.Script.select.button')}
        {' '}
        <Basic.Icon icon="arrow-down"/>
      </Basic.Button>
    );
  }

  getOptionsButton() {
    const { showMaximalizationBtn, showScriptSelection } = this.props;
    if (showScriptSelection === false) {
      return super.getOptionsButton();
    }
    return (
      <div className="pull-right script-area-btn-max">
        { this._getAddButton() }
        {' '}
        { this._getMaximalizationButton(showMaximalizationBtn) }
      </div>
     );
  }

  render() {
    const { scriptCategory, scriptManager, headerText, rendered, showScriptSelection } = this.props;
    const { showModal, script } = this.state;
    if (rendered === false) {
      return <div/>;
    }
    if (showScriptSelection === false) {
      return this._getComponent();
    }
    return (
      <div>
        { this._getComponent() }
        <Basic.Modal
          bsSize="default"
          show={showModal}
          onHide={this._closeDetail.bind(this)}
          backdrop="static">

          <form onSubmit={this._selectScript.bind(this)}>
            <Basic.Modal.Header closeButton text={headerText} rendered/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="form" >
                <Basic.SelectBox
                  ref="selectedScript"
                  manager={scriptManager}
                  useFirst clearable={false}
                  onChange={this._chageScript.bind(this)}
                  forceSearchParameters={
                    scriptManager.getDefaultSearchParameters().setFilter('inCategory', scriptCategory)}
                  label={this.i18n('entity.Script.select.label')}/>
              </Basic.AbstractForm>
              <Basic.AbstractForm ref="scriptDetail" data={script} >
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.TextField ref="code" readOnly
                      label={this.i18n('entity.Script.code')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField ref="name" readOnly
                      label={this.i18n('entity.Script.name')}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.TextArea ref="description" readOnly
                  label={this.i18n('entity.Script.description')}/>
                <Basic.ScriptArea ref="script" readOnly
                  label={this.i18n('entity.Script.script.label')}/>
              </Basic.AbstractForm>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this._closeDetail.bind(this)}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                level="success"
                onClick={this._selectScript.bind(this)}>
                {this.i18n('button.select')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

ScriptArea.propTypes = {
  scriptCategory: PropTypes.array,
  scriptManager: PropTypes.object,
  headerText: PropTypes.string.isRequired,
  mode: PropTypes.string,
  height: PropTypes.string,
  showScriptSelection: PropTypes.bool
};

ScriptArea.defaultProps = {
  scriptCategory: null,
  height: '10em',
  mode: 'groovy',
  showScriptSelection: true
};


export default ScriptArea;
