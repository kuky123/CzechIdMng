import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { Basic, Advanced } from 'czechidm-core';
import { SystemManager } from '../../redux';

const manager = new SystemManager();

/**
 * System detail tabs - route
 *
 * @author Radek Tomiška
 */
class System extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId));
  }

  render() {
    const { entity, showLoading } = this.props;

    return (
      <div>
        <Helmet title={this.i18n('navigation.menu.profile')} />

        <Basic.PageHeader showLoading={!entity && showLoading}>
          <Basic.Icon value="link"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('acc:content.system.detail.edit.header', { name: manager.getNiceLabel(entity) }) }}/>
        </Basic.PageHeader>

        <Advanced.TabPanel parentId="sys-systems" params={this.props.params}>
          {this.props.children}
        </Advanced.TabPanel>
      </div>
    );
  }
}

System.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
System.defaultProps = {
  entity: null,
  showLoading: false
};

function select(state, component) {
  const { entityId } = component.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(System);
