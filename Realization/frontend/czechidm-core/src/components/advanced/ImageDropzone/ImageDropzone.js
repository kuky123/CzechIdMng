import React from 'react';
import Well from '../../basic/Well/Well';
//
import * as Basic from '../../basic';

/**
* Advanced Dropzone component
* it will shows dropzone if there is no uploaded image yet
* otherwise it will show an image
*
* @author Petr Hanák
*/
class ImageDropzone extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {};
  }

  render() {
    const { rendered, onDrop, children, showLoading, accept, ...others } = this.props;
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
        <Basic.Dropzone
          accept={ accept }
          onDrop={ onDrop }
          children={ children && children.props.src ? children : undefined }
          style={ children && children.props.src ? { margin: 0, padding: 0, border: 'none' } : ImageDropzone.defaultProps.style }
          {...others}/>
      </div>
    );
  }
}

ImageDropzone.propTypes = {
  ...Basic.Dropzone.propTypes
};

ImageDropzone.defaultProps = {
  ...Basic.Dropzone.defaultProps
};

export default ImageDropzone;
