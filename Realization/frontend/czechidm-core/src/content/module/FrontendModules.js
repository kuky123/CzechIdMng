import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import ConfigLoader from '../../utils/ConfigLoader';
import ComponentLoader from '../../utils/ComponentLoader';
import { BackendModuleManager, ConfigurationManager } from '../../redux';
import * as Utils from '../../utils';

/**
 * FE modules administration
 *
 * @author Radek Tomiška
 */
class FrontendModules extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      moduleDescriptors: [],
      components: [],
      _showLoading: false
    };
    this.backendModuleManager = new BackendModuleManager();
  }

  getContentKey() {
    return 'content.system.fe-modules';
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'modules', 'fe-modules']);
    this.context.store.dispatch(this.backendModuleManager.fetchInstalledModules());
  }

  onEnable(entity, enable, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs[`confirm-${enable ? '' : 'de'}activate`].show(
      this.i18n(`action.${enable ? '' : 'de'}activate.message`, { count: 1, record: entity.name }),
      this.i18n(`action.${enable ? '' : 'de'}activate.header`, { count: 1 })
    ).then(() => {
      this.context.store.dispatch(this.backendModuleManager.setEnabled(entity.id, enable, (patchedEntity, error) => {
        if (!error) {
          this.addMessage({ message: this.i18n(`action.${enable ? '' : 'de'}activate.success`, { count: 1, record: patchedEntity.id }) });
          // reload is needed - react routes has to be reloaded
          window.location.reload();
        } else {
          this.addError(error);
        }
        this.setState({
          _showLoading: false
        });
      }));
    }, () => {
      // Rejected
    });
  }

  render() {
    const { showLoading } = this.props;
    const { _showLoading } = this.state;
    //
    return (
      <div className="tab-pane-panel-body">
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-deactivate" level="warning"/>
        <Basic.Confirm ref="confirm-activate" level="success"/>

        {
          showLoading || _showLoading
          ?
          <Basic.Loading isStatic showLoading/>
          :
          ConfigLoader.getModuleDescriptors()
            .sort((one, two) => {
              return one.id > two.id;
            }).map(moduleDescriptor => {
              const componentDescriptor = ComponentLoader.getComponentDescriptor(moduleDescriptor.id);
              const enableable = !moduleDescriptor.backendId || moduleDescriptor.backendId === moduleDescriptor.id || ConfigurationManager.isModuleEnabled(this.context.store.getState(), moduleDescriptor.backendId);
              //
              return (
                <Basic.Panel>
                  <Basic.PanelHeader>
                    <div className="pull-left">
                      <h2 className={ConfigLoader.isEnabledModule(moduleDescriptor.id) ? '' : 'disabled'} style={{ display: 'inline-block' }}>
                        <span>{moduleDescriptor.name} <small>{moduleDescriptor.id}</small></span>
                      </h2>
                      <Basic.Label text={this.i18n('label.disabled')} level="default" rendered={!ConfigLoader.isEnabledModule(moduleDescriptor.id)} style={{ marginLeft: 5 }}/>
                    </div>

                    <div className="pull-right" style={{ marginTop: 4}}>
                      {
                        ConfigLoader.isEnabledModule(moduleDescriptor.id)
                        ?
                        <Basic.Button
                          level="warning"
                          onClick={this.onEnable.bind(this, moduleDescriptor, false)}
                          className="btn-xs"
                          title={this.i18n('button.deactivate')}
                          titlePlacement="bottom"
                          rendered={moduleDescriptor.disableable !== false}>
                          {this.i18n('button.deactivate')}
                        </Basic.Button>
                        :
                        <Basic.Button
                          level="success"
                          onClick={this.onEnable.bind(this, moduleDescriptor, true)}
                          className="btn-xs"
                          title={ enableable ? this.i18n('button.activate') : this.i18n('activate.disabled', { backendId: moduleDescriptor.backendId })}
                          titlePlacement="bottom"
                          disabled={!enableable}>
                          {this.i18n('button.activate')}
                        </Basic.Button>
                      }
                    </div>
                    <div className="clearfix"></div>
                  </Basic.PanelHeader>

                  <Basic.PanelBody>
                    {moduleDescriptor.description}
                  </Basic.PanelBody>

                  <Basic.PanelHeader>
                    <h2>{this.i18n('components.header')}</h2>
                  </Basic.PanelHeader>

                  <Basic.Table
                    data={componentDescriptor ? componentDescriptor.components : null}
                    rowClass={({rowIndex, data}) => {
                      const componentDefinition = ComponentLoader.getComponentDefinition(data[rowIndex].id);
                      return (!componentDefinition || data[rowIndex].module !== componentDefinition.module) ? 'disabled' : '';
                    }}>
                    <Basic.Column
                      property="id"
                      header={this.i18n('label.id')}
                      cell={({rowIndex, data}) => {
                        const componentDefinition = ComponentLoader.getComponentDefinition(data[rowIndex].id);
                        const overridedInModule = componentDefinition ? componentDefinition.module : null;
                        const overrided = overridedInModule && data[rowIndex].module !== overridedInModule;
                        const textStyle = {};
                        if (overrided) {
                          textStyle.textDecoration = 'line-through';
                        }
                        return (
                          <span
                            title={overrided ? this.i18n('', { moduleId: overridedInModule }) : ''}>
                            <span style={textStyle}>{data[rowIndex].id}</span>
                            {
                              !overrided
                              ||
                              <span> ({overridedInModule})</span>
                            }
                          </span>
                        );
                      }}/>
                    <Basic.Column property="priority" header={this.i18n('components.priority')} width="100px"/>
                    <Basic.Column property="type" header={this.i18n('components.type')} width="100px"/>
                    <Basic.Column property="order" header={this.i18n('components.order')} width="100px"/>
                    <Basic.Column property="span" header={<span>Span <small>col-lg</small></span>} width="100px"/>
                  </Basic.Table>
                </Basic.Panel>
              );
            })
        }

      </div>
    );
  }
}

FrontendModules.propTypes = {
  userContext: PropTypes.object,
  showLoading: PropTypes.bool
};
FrontendModules.defaultProps = {
  userContext: null,
  showLoading: true
};

function select(state) {
  return {
    userContext: state.security.userContext,
    showLoading: Utils.Ui.isShowLoading(state, BackendModuleManager.UI_KEY_MODULES)
  };
}

export default connect(select)(FrontendModules);
