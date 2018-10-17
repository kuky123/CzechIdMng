import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../../components/basic';
import { ConfidentialStorageValueManager } from '../../redux';
import ConfidentialStorageValueDetail from './ConfidentialStorageValueDetail';

const confidentialStorageValueManager = new ConfidentialStorageValueManager();

/**
 * Confidential storage value detail content
 *
 * @author Patrik Stloukal
 */
class ConfidentialStorageValueContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.confidentialStorage';
  }

  componentDidMount() {
    this.selectNavigationItem('confidential-storage');
    const { entityId } = this.props.params;

    this.getLogger().debug(`[ConfidentialStorageValueContent] loading entity detail [id:${entityId}]`);
    this.context.store.dispatch(confidentialStorageValueManager.fetchEntity(entityId));
  }

  render() {
    const { entity, showLoading } = this.props;
    //
    return (
      <div>
        <Helmet title={this.i18n('edit.title')} />
        <Basic.PageHeader>
          <Basic.Icon value="fa:lock"/>
          {' '}
          {
            <span>{this.i18n('edit.header')}</span>
          }
        </Basic.PageHeader>
        {
          showLoading
          ?
          <Basic.Loading isStatic show />
          :
          !entity
          ||
          <div>
            <ConfidentialStorageValueDetail entity={ entity } />
          </div>
        }
      </div>
    );
  }
}

ConfidentialStorageValueDetail.propTypes = {
  showLoading: PropTypes.bool
};
ConfidentialStorageValueDetail.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: confidentialStorageValueManager.getEntity(state, entityId),
    showLoading: confidentialStorageValueManager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(ConfidentialStorageValueContent);
