import React from 'react';
import { Tab, Tabs } from 'react-bootstrap';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';

/**
 * Wrapped bootstrap Tabbs
 * - adds default styles
 *
 */
export default class BasicTabs extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, position, activeKey, onSelect, className, ...others } = this.props;
    if (!rendered) {
      return null;
    }

    const classNames = classnames(
      {'tab-horizontal': !position || position === 'top'},
      {'tab-vertical': position && position === 'left'}, // TODO: not implemened
      className
    );

    return (
      <Tabs position={position} onSelect={onSelect} activeKey={activeKey} className={classNames} {...others}>
        {this.props.children}
      </Tabs>
    );
  }
}

BasicTabs.Tab = Tab;
