import React from 'react';
import PropTypes from 'prop-types';
import Well from '../../basic/Well/Well';
import Cropper from 'react-cropper';
//
import * as Basic from '../../basic';

/**
* Component for image crop
*
* @author Petr Hanák
*/
class ImageCropper extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      src: null
    };
  }

  setDragMode(option) {
    this.refs.cropper.setDragMode(option);
  }

  reset() {
    this.refs.cropper.reset();
  }

  clear() {
    this.refs.cropper.clear();
  }

  rotateLeft() {
    this.refs.cropper.rotate(-90);
  }

  rotateRight() {
    this.refs.cropper.rotate(90);
  }

  zoomIn() {
    this.refs.cropper.zoom(0.1);
  }

  zoomOut() {
    this.refs.cropper.zoom(-0.1);
  }

  crop(cb) {
    this.refs.cropper.getCroppedCanvas({
      width: 300, height: 300
    })
    .toBlob((blob) => {
      const formData = new FormData();
      formData.append('data', blob);
      cb(formData);
    });
  }

  render() {
    const { showLoading, rendered, src } = this.props;
    //
    if (!rendered) {
      return null;
    }
    if (showLoading) {
      return <Well showLoading/>;
    }
    //
    return (
      <div>
        <Cropper
          ref="cropper"
          src={src}
          viewMode={3}
          dragMode="move"
          style={{maxHeight: '568px'}}
          autoCropArea={0.6}
          aspectRatio={1 / 1} />

        <div
          className="btn-group"
          role="group"
          style={{
            padding: '10px',
            position: 'absolute',
            bottom: '20px',
            left: '50%',
            transform: 'translateX(-50%)'
          }} >
          <Basic.Button
            type="button"
            level="info"
            onClick={this.setDragMode.bind(this, 'move')}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="arrows" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.setDragMode.bind(this, 'crop')}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="crop" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.zoomIn.bind(this)}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="search-plus" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.zoomOut.bind(this)}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="search-minus" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.rotateLeft.bind(this)}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="rotate-left" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.rotateRight.bind(this)}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="rotate-right" />
          </Basic.Button>
          <Basic.Button
            type="button"
            level="info"
            onClick={this.reset.bind(this)}
            className="btn-sm" >
            <Basic.Icon type="fa" icon="reply-all" />
          </Basic.Button>
        </div>
      </div>
    );
  }
}

ImageCropper.PropTypes = {
  /**
  * Rendered component
  */
  rendered: PropTypes.bool,
  /**
  * Show loading in component
  */
  showLoading: PropTypes.bool,
};

ImageCropper.defaultProps = {
  rendered: true,
  showLoading: false,
};

export default ImageCropper;
