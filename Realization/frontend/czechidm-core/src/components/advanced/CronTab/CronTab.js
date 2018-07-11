
import React, { PropTypes } from 'react';
import * as Basic from '../../basic';
//
import AbstractComponent from '../../basic/AbstractComponent/AbstractComponent';
import CronTabUi from 'crontab-ui';
// import Datetime from 'react-datetime';

class CronTab extends AbstractComponent {

  constructor(props) {
    super(props);
  }

    render() {
      const { style, showLoading, rendered } = this.props;
      if (!rendered) {
        return null;
      }
      if (showLoading) {
        return (
          <Basic.Loading isStatic showLoading/>
        );
      }
      return (
        <CronTabUi/>
      );
    }
}

// CronTabUi.PropTypes {
// }
// CronTabUi.defaultProps {
// }


export default CronTab;
