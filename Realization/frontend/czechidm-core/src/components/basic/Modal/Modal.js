import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'react-bootstrap';
import _ from 'lodash';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Loading from '../Loading/Loading';
import HelpIcon from '../HelpIcon/HelpIcon';
import Icon from '../Icon/Icon';

const SUPPORTED_SIZES = ['lg', 'large', 'sm', 'small'];

/**
 * Wrapped bootstrap modal
 * - adds backdrop fix
 *
 * https://github.com/react-bootstrap/react-bootstrap/blob/v0.28.3/src/Modal.js
 * https://react-bootstrap.github.io/components.html#modals
 *
 * @author Radek Tomiška
 */
export default class BasicModal extends AbstractComponent {

  /**
   * Fix modal backdrop size
   */
  _onEnter(...args) {
    const { onEnter } = this.props;
    // find modal-backdrop
    if (typeof $ !== 'undefined') {
      $('.modal-backdrop').css({
        bottom: 0 - $(window).scrollTop()
      });
    }
    // original
    if (onEnter) {
      onEnter(...args);
    }
  }

  render() {
    const { rendered, bsSize, showLoading, onEnter, ...others } = this.props;
    if (!rendered) {
      return null;
    }
    if (bsSize && SUPPORTED_SIZES.indexOf(bsSize) > -1) { // workaround for ugly Modal warning, when bsSize lack default
      others.bsSize = bsSize;
    }
    return (
      <Modal onEnter={this._onEnter.bind(this)} {...others}>
        {
          showLoading
          ?
          <Modal.Body>
            <Loading isStatic showLoading/>
          </Modal.Body>
          :
          null
        }
        {/* prevent exception, when parent component is touching to childerns ref etc.*/}
        <div className={showLoading ? 'hidden' : ''}>
          {this.props.children}
        </div>
      </Modal>
    );
  }
}

BasicModal.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Callback fired before the Modal transitions in
   */
  onEnter: PropTypes.func,
  /**
   * Component size variations.
   */
  bsSize: PropTypes.oneOf(_.concat(SUPPORTED_SIZES, 'default'))

  /**
   * ... and other react bootstap modal props
   */
};

BasicModal.defaultProps = {
  ...AbstractComponent.defaultProps,
  bsSize: 'default'
};

/**
 * FIXME: show loading icon has to be in <h2> tag
 */
class BasicModalHeader extends AbstractComponent {
  render() {
    const { rendered, text, children, help, showLoading, icon, ...others } = this.props;
    if (!rendered) {
      return null;
    }
    return (
      <Modal.Header {...others}>
        <div className="pull-left">
          <Icon type="fa" icon="refresh" showLoading rendered={ showLoading } />
          {
            showLoading
            ||
            text
            ?
            <h2>
              <Icon value={ icon } showLoading={ showLoading } style={{ marginRight: 5 }}/>
              <span dangerouslySetInnerHTML={{__html: text}}/>
            </h2>
            :
            null
          }
          { children }
        </div>
        {
          help
          ?
          <div className="pull-right">
            <HelpIcon content={help}/>
          </div>
          :
          null
        }
        <div className="clearfix"></div>
      </Modal.Header>
    );
  }
}

BasicModalHeader.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Header text
   */
  text: PropTypes.any
};

BasicModalHeader.defaultProps = {
  ...AbstractComponent.defaultProps
};

BasicModal.Header = BasicModalHeader;
BasicModal.Body = Modal.Body;
BasicModal.Footer = Modal.Footer;
