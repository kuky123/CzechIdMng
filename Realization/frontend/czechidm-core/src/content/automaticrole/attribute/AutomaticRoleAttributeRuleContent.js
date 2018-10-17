import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import * as Basic from '../../../components/basic';
import { AutomaticRoleAttributeRuleManager, AutomaticRoleAttributeManager } from '../../../redux';
import AutomaticRoleAttributeRuleDetail from './AutomaticRoleAttributeRuleDetail';

const manager = new AutomaticRoleAttributeRuleManager();
const automaticRoleAttributeManager = new AutomaticRoleAttributeManager();

/**
 * Detail rule for automatic role by attribute
 *
 * @author Ondrej Kopr
 */
class AutomaticRoleAttributeRuleContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.automaticRoles.attribute.rule';
  }

  componentDidMount() {
    const { ruleId, entityId } = this.props.params;
    this.selectNavigationItems(['roles-menu', 'automatic-roles']);

    if (this._getIsNew()) {
      this.context.store.dispatch(manager.receiveEntity(ruleId, { }));
      this.context.store.dispatch(automaticRoleAttributeManager.fetchEntityIfNeeded(entityId));
    } else {
      this.getLogger().debug(`[TypeContent] loading entity detail [id:${ruleId}]`);
      this.context.store.dispatch(manager.fetchEntity(ruleId));
    }
  }

  /**
   * Function check if exist params new
   */
  _getIsNew() {
    const { query } = this.props.location;
    return (query) ? query.new : null;
  }

  render() {
    const { entity, showLoading, attribute } = this.props;
    const { entityId } = this.props.params;
    return (
      <div>
        {
          this._getIsNew()
          ?
          <Helmet title={this.i18n('create.title')} />
          :
          <Helmet title={this.i18n('edit.title')} />
        }
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        {
          !entity
          ||
          <Basic.PageHeader>
            <Basic.Icon value="fa:universal-access"/>
            {' '}
            {
              this._getIsNew()
              ?
              <span>{ attribute ? attribute.name : ''} <small>{ this.i18n('create.header') }</small></span>
              :
              <span>{ entity._embedded.automaticRoleAttribute ? entity._embedded.automaticRoleAttribute.name : null } <small>{this.i18n('edit.header')}</small></span>
            }
          </Basic.PageHeader>
        }
        <Basic.Panel>
          <Basic.Loading isStatic showLoading={showLoading} />
          {
            !entity
            ||
            <div style={{ padding: '15px 15px 0 15px' }}>
              <AutomaticRoleAttributeRuleDetail
                ref="detail"
                entity={entity}
                manager={manager}
                readOnly
                attributeId={entityId} />
              <Basic.PanelFooter showLoading={showLoading} >
                <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
              </Basic.PanelFooter>
            </div>
          }
        </Basic.Panel>
      </div>
    );
  }
}

AutomaticRoleAttributeRuleContent.propTypes = {
  showLoading: PropTypes.bool
};
AutomaticRoleAttributeRuleContent.defaultProps = {
};

function select(state, component) {
  const { ruleId, entityId } = component.params;
  //
  return {
    entity: manager.getEntity(state, ruleId),
    attribute: automaticRoleAttributeManager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, ruleId)
  };
}

export default connect(select)(AutomaticRoleAttributeRuleContent);
