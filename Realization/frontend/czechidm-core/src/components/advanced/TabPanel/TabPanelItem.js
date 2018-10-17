import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';
import classnames from 'classnames';
//
import * as Basic from '../../basic';

export default class TabPanelItem extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const { className, to, active, icon, iconColor, showLoading, ...others } = this.props;
    const itemClassNames = classnames(
      { 'list-group-item': false },
      { 'active': active === true },
      className
    );
    let _icon = ( icon === undefined || icon === null ? 'fa:circle-o' : icon );
    if (showLoading) {
      _icon = 'refresh';
    }

    return (
      <li className={itemClassNames}>
        <Link to={to}>
          <Basic.Icon value={_icon} color={ iconColor }/>
          {this.props.children}
        </Link>
      </li>
    );
  }
}

TabPanelItem.propTypes = {
  to: PropTypes.string,
  title: PropTypes.string,
  active: PropTypes.bool
};
