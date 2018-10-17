import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
//
import AbstractContextComponent from '../AbstractContextComponent/AbstractContextComponent';
import Icon from '../Icon/Icon';
import Button from '../Button/Button';
import Modal from '../Modal/Modal';
import Tooltip from '../Tooltip/Tooltip';
import HelpContent from '../../../domain/HelpContent';

/**
 * Help icon opens modal window with user documentation.
 *
 * @author Radek Tomiška
 */
export default class HelpIcon extends AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showModal: false
    };
  }

  close() {
    this.setState({ showModal: false });
  }

  open(event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({ showModal: true });
  }

  getHeaderText(content) {
    const lines = content.split('\n');
    if (lines[0].lastIndexOf('# ', 0) === 0) { // startsWith - this work even in ie
      return lines[0].substr('# '.length);
    }
    return this.i18n('component.basic.HelpIcon.title');
  }

  getContentText(content) {
    const lines = content.split('\n');
    if (lines[0].lastIndexOf('# ', 0) === 0) { // startsWith - this work even in ie
      lines.shift();
      return lines.join('\n');
    }
    return content;
  }

  render() {
    const { content, rendered, showLoading, titlePlacement, ...others } = this.props;
    if (!rendered || !content) {
      return null;
    }
    let _header = null;
    let _body = null;
    //
    if (content instanceof HelpContent) {
      _header = content.getHeader();
      _body = content.getBody();
    } else if (_.isObject(content) && content.body) {
      _header = content.header;
      _body = content.body;
    } else {
      _body = (<span dangerouslySetInnerHTML={{ __html: content }}/>);
    }
    if (!_header) {
      _header = this.i18n('component.basic.HelpIcon.title');
    }

    return (
      <span className="help-icon-container" {...others}>
        <Tooltip placement={titlePlacement} id={`button-tooltip`} value={this.i18n('component.basic.HelpIcon.title')}>
          <a href="#" onClick={this.open.bind(this)} className="help-icon">
            <Icon icon="question-sign"/>
          </a>
        </Tooltip>
        <Modal show={this.state.showModal} onHide={this.close.bind(this)} bsSize="large" className="help-icon-modal">
          <Modal.Header closeButton>
            <h2>{ _header }</h2>
          </Modal.Header>
          <Modal.Body className="markdown-body">
            { _body }
          </Modal.Body>
          <Modal.Footer>
            <Button level="link" onClick={this.close.bind(this)}>Zavřít</Button>
          </Modal.Footer>
        </Modal>
      </span>
    );
  }
}

HelpIcon.propTypes = {
  ...AbstractContextComponent.propTypes,
  content: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.object
  ]),
  /**
   * Help icon title position
   */
  titlePlacement: PropTypes.oneOf(['top', 'bottom', 'right', 'left'])
};

HelpIcon.defaultProps = {
  ...AbstractContextComponent.defaultProps,
  titlePlacement: 'bottom'
};
