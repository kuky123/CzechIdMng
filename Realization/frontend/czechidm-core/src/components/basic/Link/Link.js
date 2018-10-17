import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
//
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Icon from '../Icon/Icon';

/**
 * Link (external)
 *
 * TODO: use even for internal links with "to" parameter and context router?
 *
 * @author Radek Tomiška
 */
export default class Link extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { rendered, showLoading, isExternal, style, className, text, children, href } = this.props;
    if (!rendered || !href || (!text && !children)) {
      return null;
    }
    //
    const classNames = classnames(
      'basic-link',
      className
    );
    return (
      <a href={href} className={classNames} style={style} target={isExternal ? '_blank' : null}>
        <Icon value="fa:globe" showLoading={showLoading} style={{ marginRight: 2 }} rendered={isExternal || showLoading}/>
        { text }
        { children }
      </a>
    );
  }
}

Link.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * Standard "href" link parameter
   */
  href: PropTypes.string,
  /**
   * link text (or children can be used)
   */
  text: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node
  ]),
  /**
   * Will be opened in new window
   */
  isExternal: PropTypes.bool
};

Link.defaultProps = {
  ...AbstractComponent.defaultProps,
  isExternal: true
};
