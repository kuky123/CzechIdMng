import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { FormAttributeManager } from '../../redux';
import FormAttributeDetail from './FormAttributeDetail';

const manager = new FormAttributeManager();

/**
 * Form attribute detail -> detail with vertical menu
 *
 * @author Roman Kučera
 */
class FormAttributeRoutes extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.formAttributes';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entityId } = this.props.params;

    if (!this._getIsNew()) {
      this.getLogger().debug(`[FormContent] loading entity detail [id:${entityId}]`);
      this.context.store.dispatch(manager.fetchEntity(entityId));
    }
  }

  /**
   * Method check if exist params new
   */
  _getIsNew() {
    const { query } = this.props.location;
    if (query) {
      return query.new ? true : false;
    }
    return false;
  }

  _getFormDefinitionId() {
    const { query } = this.props.location;
    return (query) ? query.formDefinition : null;
  }

  render() {
    const { entity } = this.props;
    return (
      <div>
        {
          this._getIsNew()
          ?
          <Helmet title={this.i18n('create.title')} />
          :
          <Helmet title={this.i18n('edit.title')} />
        }
        {
          (this._getIsNew() || !entity)
          ||
          <Basic.PageHeader>
            <Basic.Icon value="fa:wpforms" />
            {' '}
            <span>{entity.name} <small>{this.i18n('edit')}</small></span>
          </Basic.PageHeader>
        }
        {
          this._getIsNew()
          ?
          <FormAttributeDetail isNew formDefinition={ this._getFormDefinitionId() } params={ this.props.params } />
          :
          <Advanced.TabPanel position="left" parentId="forms-attributes" params={this.props.params}>
            {this.props.children}
          </Advanced.TabPanel>
        }

      </div>
    );
  }
}

FormAttributeRoutes.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
FormAttributeRoutes.defaultProps = {
};

function select(state, component) {
  const { entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(FormAttributeRoutes);
