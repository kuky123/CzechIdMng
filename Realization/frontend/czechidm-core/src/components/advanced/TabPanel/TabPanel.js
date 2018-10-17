import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import TabPanelItem from './TabPanelItem';
import { getNavigationItems, resolveNavigationParameters } from '../../../redux/config/actions';
import * as Basic from '../../basic';

const ITEM_HEIGTH = 45; // item heigth for dynamic content resize

/**
 * Sidebar renders tabs by given navigation parent (parentId)
 *
 * @author Radek Tomiška
 */
class TabPanel extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
  }

  getNavigationItems() {
    const { navigation, userContext, parentId, selectedNavigationItems } = this.props;
    const { params } = this.props;
    //
    return getNavigationItems(navigation, parentId, null, userContext, params).map(item => {
      // reslve label
      const labelParams = resolveNavigationParameters(userContext, params);
      labelParams.defaultValue = item.label;
      let label = item.label;
      if (item.labelKey) {
        label = (<span>{this.i18n(item.labelKey, labelParams)}</span>);
      } else if (item.titleKey) {
        // label from title
        label = (<span>{this.i18n(item.titleKey, { defaultValue: item.title })}</span>);
      }
      //
      switch (item.type) {
        case 'TAB':
        case 'DYNAMIC': {
          return (
            <TabPanelItem
              id={`nav-item-${item.id}`}
              key={`nav-item-${item.id}`}
              to={item.to}
              icon={item.icon}
              iconColor={item.iconColor}
              title={this.i18n(item.titleKey, { defaultValue: item.title })}
              active={_.includes(selectedNavigationItems, item.id)}>
              { label }
            </TabPanelItem>
          );
        }
        default: {
          this.getLogger().error('WARNING: navigation: ' + item.type + ' type not implemeted for item id [' + item.id + ']');
        }
      }
    });
  }

  render() {
    const { position } = this.props;
    const navigationItems = this.getNavigationItems();
    //
    if (position === 'top') {
      return (
        <div className="tab-horizontal">
          <ul className="nav nav-tabs">
            { navigationItems }
          </ul>
          <div className="tab-content">
            <div className="tab-pane active">
              { this.props.children }
            </div>
          </div>
        </div>
      );
    }
    //
    // left
    return (
      <Basic.Panel className="clearfix">
        <div ref="tabPanel" className="tab-panel tab-vertical clearfix">
          <ul ref="tabPanelSidebar" className="tab-panel-sidebar nav nav-pills nav-stacked">
            { navigationItems }
          </ul>
          <div ref="tabPanelContent" className="tab-panel-content tab-content" style={{ minHeight: navigationItems.length * ITEM_HEIGTH }}>
            { this.props.children }
          </div>
        </div>
      </Basic.Panel>
    );
  }
}

TabPanel.propTypes = {
  navigation: PropTypes.object,
  userContext: PropTypes.object,
  /**
   * which navigation parent wil be rendered - sub menus to render
   */
  parentId: PropTypes.string,
  /**
   * Tabs position
   *
   * @param  {[type]} ['left' [description]
   * @param  {[type]} 'top']  [description]
   * @return {[type]}         [description]
   */
  position: PropTypes.oneOf(['left', 'top'])
};
TabPanel.defaultProps = {
  navigation: null,
  userContext: null,
  position: 'left'
};

function select(state) {
  const selectedNavigationItems = state.config.get('selectedNavigationItems');
  return {
    navigation: state.config.get('navigation'),
    selectedNavigationItems,
    userContext: state.security.userContext
  };
}

export default connect(select)(TabPanel);
