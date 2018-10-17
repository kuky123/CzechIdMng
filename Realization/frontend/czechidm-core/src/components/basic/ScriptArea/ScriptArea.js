import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import Joi from 'joi';
//
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import Tooltip from '../Tooltip/Tooltip';
import Button from '../Button/Button';
import Icon from '../Icon/Icon';
import Modal from '../Modal/Modal';

/**
 * @author Vít Švanda
 */
class ScriptArea extends AbstractFormComponent {

  constructor(props) {
    super(props);
  }

  getRequiredValidationSchema() {
    return Joi.string().required();
  }

  /**
   * Focus input field
   */
  focus() {
    const { showModalEditor } = this.state;
    if (showModalEditor) {
      this.refs.inputModal.editor.focus();
    } else {
      this.refs.input.editor.focus();
    }
  }

  onChange(newValue) {
    if (this.props.onChange) {
      this.props.onChange(newValue);
    } else {
      this.setState({
        value: newValue
      }, () => {
        this.validate();
      });
    }
  }

  _closeModalEditor() {
    this.setState({showModalEditor: false});
  }

  _showModalEditor() {
    this.setState({showModalEditor: true}, ()=>{
      // this.refs.inputModal.focus();
    });
  }

  _getAceEditor(AceEditor, mode, className, height, modal = false) {
    return (
      <AceEditor
      ref={modal ? 'inputModal' : 'input'}
      mode={mode}
      width={null}
      height={modal ? '40em' : height}
      className={className}
      title={this.getValidationResult() != null ? this.getValidationResult().message : ''}
      readOnly={this.state.readOnly}
      enableBasicAutocompletion
      enableLiveAutocompletion
      theme="github"
      onChange={this.onChange}
      value={this.state.value || ''}
      tabSize={4}
      fontSize={14}
      spellcheck
      showGutter
      editorProps={{$blockScrolling: true}}
    />);
  }

  _getMaximalizationButton(showMaximalizationBtn) {
    return (
      <Button
        type="button"
        className="btn-xs"
        level="success"
        rendered={showMaximalizationBtn}
        onClick={this._showModalEditor.bind(this)}>
        <Icon icon="fullscreen"/>
      </Button>
    );
  }

  getOptionsButton() {
    const { showMaximalizationBtn } = this.props;
    return (
      <div className="pull-right script-area-btn-max">
        { this._getMaximalizationButton(showMaximalizationBtn) }
      </div>
    );
  }

  _getComponent(feedback) {
    const { labelSpan, label, componentSpan, required, mode, height } = this.props;
    const {showModalEditor} = this.state;
    //
    const className = classNames('form-control');
    const labelClassName = classNames(labelSpan, 'control-label');
    let showAsterix = false;
    if (required && !this.state.value) {
      showAsterix = true;
    }

    // Workaround - Import for AceEditor must be here. When was on start, then not working tests (error is in AceEditor);
    let AceEditor;
    AceEditor = require('react-ace').default;
    require('brace/mode/groovy');
    require('brace/mode/json');
    require('brace/theme/github');
    const AceEditorInstance = this._getAceEditor(AceEditor, mode, className, height, showModalEditor);
    return (
      <div className={ showAsterix ? 'has-feedback' : ''}>
        {
          !label
          ||
          <label
            className={labelClassName}>
            {label}
            { this.renderHelpIcon() }
          </label>
        }

        <div className={componentSpan}>
          <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={ this.getTitle() }>
            <span>
              { this.getOptionsButton() }
              {!showModalEditor ? AceEditorInstance : null }
              {
                feedback
                ||
                !showAsterix
                ||
                <span className="form-control-feedback" style={{color: 'red', zIndex: 0}}>*</span>
              }
            <Modal
               show={showModalEditor}
               dialogClassName="modal-large"
               onHide={this._closeModalEditor.bind(this)}>
              <Modal.Header text={label}/>
              <Modal.Body style={{overflow: 'scroll'}}>
                {AceEditorInstance}
              </Modal.Body>
              <Modal.Footer>
                <Button level="link" onClick={this._closeModalEditor.bind(this)}>{this.i18n('button.close')}</Button>
              </Modal.Footer>
            </Modal>
            </span>
          </Tooltip>
          { !label ? this.renderHelpIcon() : null }
          { this.renderHelpBlock() }
        </div>
      </div>
    );
  }

  getBody(feedback) {
    return this._getComponent(feedback);
  }
}

ScriptArea.propTypes = {
  ...AbstractFormComponent.propTypes,
  helpBlock: PropTypes.string,
  mode: PropTypes.string,
  height: PropTypes.string
};

ScriptArea.defaultProps = {
  ...AbstractFormComponent.defaultProps,
  mode: 'groovy',
  height: '10em',
  showMaximalizationBtn: true
};

export default ScriptArea;
