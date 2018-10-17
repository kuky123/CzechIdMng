import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../../components/basic';
import { AutomaticRoleAttributeManager, AutomaticRoleAttributeRuleManager } from '../../../redux';
import AutomaticRoleAttributeRuleTable from './AutomaticRoleAttributeRuleTable';

const manager = new AutomaticRoleAttributeManager();

/**
 * Automatic role detail, update automatic role isn't currently allowed
 *
 * @author Ondrej Kopr
 */
class AutomaticRoleAttributeRules extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    //
    this.automaticRoleAttributeRuleManager = new AutomaticRoleAttributeRuleManager();
  }

  getContentKey() {
    return 'content.automaticRoles.attribute';
  }

  componentDidMount() {
    const { automaticRoleId, entityId } = this.props.params;
    if (entityId) {
      this.selectNavigationItems(['roles-menu', 'roles', 'role-automatic-roles', 'role-automatic-role-attribute', 'role-automatic-role-attribute-rules']);
    } else {
      this.selectNavigationItems(['roles-menu', 'automatic-roles', 'automatic-role-attribute-rules']);
    }

    this.getLogger().debug(`[TypeContent] loading entity detail [id:${automaticRoleId}]`);
    this.context.store.dispatch(manager.fetchEntity(automaticRoleId));
  }

  render() {
    const { entity} = this.props;
    return (
      <div className="tab-pane-table-body">
        <Basic.ContentHeader style={{ marginBottom: 0 }}>
          {this.i18n('content.automaticRoles.attribute.rules.header')}
        </Basic.ContentHeader>
        <Basic.Panel className="no-border last">
          <AutomaticRoleAttributeRuleTable
            manager={this.automaticRoleAttributeRuleManager}
            uiKey={entity ? entity.id : null}
            attributeId={entity ? entity.id : null} />
        </Basic.Panel>
      </div>
    );
  }
}

AutomaticRoleAttributeRules.propTypes = {
  showLoading: PropTypes.bool
};
AutomaticRoleAttributeRules.defaultProps = {
};

function select(state, component) {
  const { automaticRoleId } = component.params;
  //
  return {
    entity: manager.getEntity(state, automaticRoleId),
    showLoading: manager.isShowLoading(state, null, automaticRoleId)
  };
}

export default connect(select)(AutomaticRoleAttributeRules);
