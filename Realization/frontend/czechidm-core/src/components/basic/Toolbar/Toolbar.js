import React from 'react';
import PropTypes from 'prop-types';
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Loading from '../Loading/Loading';
import classnames from 'classnames';
import { AutoAffix } from 'react-overlays';

export default class Toolbar extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { className, rendered, showLoading, viewportOffsetTop, container, ...other } = this.props;
    if (!rendered) {
      return null;
    }
    const classNames = classnames(
      'basic-toolbar',
      'form-inline',
      className
    );
    let render = (
      <div className={classNames} {...other}>
        <Loading className="simple" showLoading={showLoading} showAnimation={false}>
          {this.props.children}
        </Loading>
      </div>
    );
    if (viewportOffsetTop !== undefined) { // affix decorator, when viewportOffsetTop is defined
      render = (
        <AutoAffix viewportOffsetTop={viewportOffsetTop} container={container}>
          {render}
        </AutoAffix>
      );
    }
    return render;
  }
}

Toolbar.propTypes = {
  ...AbstractComponent.propTypes,
  /**
   * When affixed, pixels to offset from top of viewport
   */
  viewportOffsetTop: PropTypes.number,
  /**
   * The logical container node or component for determining offset from bottom of viewport, or a function that returns it
   */
  container: PropTypes.object
};

Toolbar.defaultProps = {
  ...AbstractComponent.defaultProps
};
