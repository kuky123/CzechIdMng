import React from 'react';
import PropTypes from 'prop-types';
import { Tab, Tabs } from 'react-bootstrap';
import classnames from 'classnames';
import _ from 'lodash';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

/**
 * Wrapped bootstrap Tabbs
 * - adds default styles
 * - adds rendered supported
 *
 * @author Radek Tomiška
 */
export default class BasicTabs extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  _getRenderedChildren(children) {
    return children.filter(child => {
      return child.props.rendered;
    });
  }

  render() {
    const { id, rendered, position, activeKey, onSelect, className, style } = this.props;
    if (!rendered) {
      return null;
    }

    const classNames = classnames(
      {'tab-horizontal': !position || position === 'top'},
      {'tab-vertical': position && position === 'left'}, // TODO: not implemened
      className
    );
    //
    let _id = id;
    if (!_id) {
      _id = _.uniqueId('tooltip_');
    }
    //
    return (
      <Tabs
        id={ _id }
        onSelect={ onSelect }
        activeKey={ activeKey }
        className={ classNames }
        style={ style }>
        { this._getRenderedChildren(this.props.children) }
      </Tabs>
    );
  }
}

BasicTabs.propTypes = {
  rendered: PropTypes.bool
};

BasicTabs.defaultProps = {
  rendered: true
};

/**
 * Adds rendered to react bootstrap Tab.
 */
export class BasicTab extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, ...others } = this.props;
    if (!rendered) {
      return null;
    }

    return (
      <Tab {...others} />
    );
  }
}

BasicTab.propTypes = {
  rendered: PropTypes.bool
};

BasicTab.defaultProps = {
  rendered: true
};

BasicTabs.Tab = BasicTab;
