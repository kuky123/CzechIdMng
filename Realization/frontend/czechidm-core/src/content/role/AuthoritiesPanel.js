import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { DataManager, RoleManager } from '../../redux';

const roleManager = new RoleManager();

/**
* Panel of identities
*/
export class AuthoritiesPanel extends Basic.AbstractContextComponent {

  componentDidMount() {
    this.context.store.dispatch(roleManager.fetchAvailableAuthorities());
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      openedAuthorities: new Immutable.Set(),
      filledAuthorities: this.prepareFilledAuthorities(this.props.authorities, this.props.availableAuthorities)
    };
  }

  componentWillReceiveProps(nextProps) {
    const { authorities, availableAuthorities } = nextProps;
    // cursor is different
    if (availableAuthorities) {
      this.setState({
        filledAuthorities: this.prepareFilledAuthorities(authorities, availableAuthorities)
      });
    }
  }

  prepareFilledAuthorities(authorities, availableAuthorities) {
    let filledAuthorities = new Immutable.OrderedMap();
    if (availableAuthorities) {
      // sort authorities by name
      availableAuthorities = _.sortBy(availableAuthorities, function sort(authority) {
        return authority.name;
      });
      const authorityNames = !authorities ? [] : authorities.map(authority => {
        return authority.authority;
      });
      availableAuthorities.forEach(authorityGroup => {
        let permissions = new Immutable.OrderedMap();
        authorityGroup.permissions.forEach(permission => {
          permissions = permissions.set(permission.name, _.includes(authorityNames, `${authorityGroup.name}_${permission.name}`));
        });
        filledAuthorities = filledAuthorities.set(authorityGroup.name, permissions);
      });
    }
    return filledAuthorities;
  }

  onAuthorityGroupToogle(authorityGroup, event) {
    if (event) {
      event.preventDefault();
    }
    const { openedAuthorities } = this.state;
    this.setState({
      openedAuthorities: openedAuthorities.has(authorityGroup) ? openedAuthorities.delete(authorityGroup) : openedAuthorities.clear().add(authorityGroup)
    });
  }

  isAllAuthorityGroupSelected(authorityGroup) {
    const { filledAuthorities } = this.state;
    return filledAuthorities.get(authorityGroup).reduce((result, selected) => { return result && selected; }, true);
  }

  isSomeAuthorityGroupSelected(authorityGroup) {
    const { filledAuthorities } = this.state;
    return filledAuthorities.get(authorityGroup).reduce((result, selected) => { return result || selected; }, false);
  }

  /**
   * Resolve authority group localization by authority module
   * TODO: redesign authoritiy panel to structure: MODULE -> GROUP -> BASE
   * TODO: permission localization is not supported not (depends on structure)
   *
   * @param  {string} authorityGroupName
   * @return {string}
   */
  _authorityGroupI18n(authorityGroupName) {
    const { availableAuthorities } = this.props;
    const authorityGroup = availableAuthorities.find(a => {
      return a.name === authorityGroupName;
    });
    return this.i18n(`${authorityGroup.module ? authorityGroup.module : 'core'}:permission.group.${authorityGroup.name}`, { defaultValue: authorityGroupName});
  }

  render() {
    const { _showLoading, showLoading, rendered } = this.props;
    const { openedAuthorities, filledAuthorities } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    if (_showLoading || showLoading) {
      return (
        <Basic.Loading showLoading isStatic/>
      );
    }
    //
    return (
      <div>
        {
          !filledAuthorities
          ||
          filledAuthorities.map((permissions, authorityGroupName) => {
            return (
              <div>
                <Basic.Panel style={{ marginBottom: 2 }}>
                  <Basic.PanelHeader style={{ padding: '0 10px 0 0' }}>
                    <div className="pull-left">
                      <div style={{ padding: '8px 15px', cursor: 'pointer' }} onClick={this.onAuthorityGroupToogle.bind(this, authorityGroupName)}>
                        {/* TODO: create basic checkbox component */}
                        <Basic.Icon value="fa:check-square-o" rendered={ this.isAllAuthorityGroupSelected(authorityGroupName) }/>
                        <Basic.Icon value="fa:minus-square-o" rendered={ this.isSomeAuthorityGroupSelected(authorityGroupName) && !this.isAllAuthorityGroupSelected(authorityGroupName) }/>
                        <Basic.Icon value="fa:square-o" rendered={ !this.isSomeAuthorityGroupSelected(authorityGroupName) }/>
                        {' '}
                        { this._authorityGroupI18n(authorityGroupName)}
                      </div>
                    </div>
                    <div className="pull-right">
                      <Basic.Button
                        className="btn-xs"
                        onClick={this.onAuthorityGroupToogle.bind(this, authorityGroupName)}
                        style={{ display: 'inline-block', marginTop: 6 }}
                        title={openedAuthorities.has(authorityGroupName) ? this.i18n('content.roles.setting.authority.group.hide') : this.i18n('content.roles.setting.authority.group.show') }
                        titleDelayShow={ 500 }
                        titlePlacement="bottom">
                        <Basic.Icon value={openedAuthorities.has(authorityGroupName) ? 'fa:angle-double-up' : 'fa:angle-double-down'}/>
                      </Basic.Button>
                    </div>
                    <div className="clearfix"></div>
                  </Basic.PanelHeader>
                  <Basic.Collapse in={openedAuthorities.has(authorityGroupName)}>
                    <Basic.PanelBody style={{ paddingTop: 0, paddingBottom: 0 }}>
                      {
                        permissions.map((selected, permission) => {
                          return (
                            <div style={{ padding: '8px 0px'}}>
                              {/* TODO: create basic checkbox component */}
                              <Basic.Icon value="fa:check-square-o" rendered={ selected }/>
                              <Basic.Icon value="fa:square-o" rendered={ !selected }/>
                              {' '}
                              { this.i18n('permission.base.' + permission)}
                            </div>
                          );
                        })
                      }
                    </Basic.PanelBody>
                  </Basic.Collapse>
                </Basic.Panel>
              </div>
            );
          })
        }
      </div>
    );
  }
}

AuthoritiesPanel.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  authorities: PropTypes.arrayOf(PropTypes.object),
  availableAuthorities: PropTypes.arrayOf(PropTypes.object)
};

AuthoritiesPanel.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  authorities: [],
  availableAuthorities: []
};

function select(state) {
  return {
    availableAuthorities: DataManager.getData(state, RoleManager.UI_KEY_AVAILABLE_AUTHORITIES),
    _showLoading: Utils.Ui.isShowLoading(state, RoleManager.UI_KEY_AVAILABLE_AUTHORITIES_UIKEY)
  };
}

export default connect(select, null, null, { withRef: true })(AuthoritiesPanel);
