import React from 'react';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { GenerateValueManager } from '../../redux';
import GenerateValueTable from './GenerateValueTable';

const TABLE_UIKEY = 'generate-values-table-ui-key';

/**
* Content with all generate values
*/
class GenerateValues extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.manager = new GenerateValueManager();
  }

  getManager() {
    return this.manager;
  }

  getContentKey() {
    return 'content.generateValues';
  }

  getNavigationKey() {
    return 'generate-values';
  }

  render() {
    return (
      <div>
        {this.renderPageHeader()}

        <Basic.Panel>
          <GenerateValueTable manager={this.manager} uiKey={TABLE_UIKEY} params={ this.props.params } filterOpened/>
        </Basic.Panel>
      </div>
    );
  }
}

GenerateValues.propTypes = {
};
GenerateValues.defaultProps = {
};

export default connect()(GenerateValues);
