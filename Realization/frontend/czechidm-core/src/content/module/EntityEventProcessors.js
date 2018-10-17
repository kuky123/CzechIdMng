import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import Immutable from 'immutable';
import _ from 'lodash';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { EntityEventProcessorManager, DataManager } from '../../redux';
import * as Utils from '../../utils';
import SearchParameters from '../../domain/SearchParameters';

const UIKEY = 'entity-event-processors';
const manager = new EntityEventProcessorManager();

/**
 * BE event precessors
 *
 * @author Radek Tomiška
 */
class EntityEventProcessors extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: true
    };
  }

  getContentKey() {
    return 'content.system.entity-event-processors';
  }

  getNavigationKey() {
    return 'entity-event-processors';
  }

  getManager() {
    return manager;
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.reload();
    this.context.store.dispatch(manager.fetchRegisteredProcessors());
    this.refs.text.focus();
  }

  reload() {
    const { _searchParameters } = this.props;
    //
    this.fetchEntities(_searchParameters);
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.fetchEntities(SearchParameters.getSearchParameters(SearchParameters.getFilterData(this.refs.filterForm), this.props._searchParameters));
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.fetchEntities(null);
    this.refs.filterForm.setData({});
  }

  fetchEntities(searchParameters) {
    this.context.store.dispatch(this.getManager().fetchEntities(searchParameters, UIKEY));
  }

  onEnable(entity, enable, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs[`confirm-${enable ? '' : 'de'}activate`].show(
      this.i18n(`action.${enable ? '' : 'de'}activate.message`, { count: 1, record: entity.name }),
      this.i18n(`action.${enable ? '' : 'de'}activate.header`, { count: 1 })
    ).then(() => {
      this.context.store.dispatch(this.getManager().setEnabled(entity.id, enable, (patchedEntity, error) => {
        if (!error) {
          this.addMessage({ message: this.i18n(`action.${enable ? '' : 'de'}activate.success`, { count: 1, record: entity.name }) });
          // refresh table with processors
          this.reload();
        } else {
          this.addError(error);
        }
      }));
    }, () => {
      // rejected
    });
  }

  render() {
    const { processors, registeredProcessors, showLoading, _searchParameters } = this.props;
    const { filterOpened } = this.state;
    //
    // all entity types and event types for select boxes
    let _registeredEntityTypes = new Immutable.OrderedSet();
    let _registeredEventTypes = new Immutable.OrderedSet();
    if (registeredProcessors) {
      registeredProcessors.forEach(processor => {
        _registeredEntityTypes = _registeredEntityTypes.add(processor.entityType);
        processor.eventTypes.forEach(e => {
          _registeredEventTypes = _registeredEventTypes.add(e);
        });
      });
      // sort _entityTypes
      _registeredEntityTypes = _registeredEntityTypes.sort((one, two) => {
        return one > two;
      });
    }
    //
    // filtered processors and entity types
    let _processors = new Immutable.OrderedMap();
    let _entityTypes = new Immutable.OrderedSet();
    if (processors) {
      processors.forEach(processor => {
        if (!_processors.has(processor.entityType)) {
          _processors = _processors.set(processor.entityType, []);
        }
        const entityProcessors = _processors.get(processor.entityType);
        entityProcessors.push(processor);
        _processors = _processors.set(processor.entityType, entityProcessors);
        _entityTypes = _entityTypes.add(processor.entityType);
      });
    }
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-deactivate" level="warning"/>
        <Basic.Confirm ref="confirm-activate" level="success"/>

        <Basic.Toolbar>
          <div>
            <div className="pull-right">
              <Advanced.Filter.ToogleButton
                filterOpen={ (open)=> this.setState({ filterOpened: open }) }
                filterOpened={ filterOpened }
                style={{ marginLeft: 3 }}
                searchParameters={ _searchParameters }/>
              <Advanced.RefreshButton
                onClick={ this.fetchEntities.bind(this, _searchParameters) }
                title={ this.i18n('button.refresh') }
                showLoading={ showLoading }/>
            </div>
            <div className="clearfix"></div>
          </div>
          <Basic.Collapse in={filterOpened}>
            <div>
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row>
                    <Basic.Col lg={ 8 }>
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.text.placeholder')}/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 } className="text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </Basic.Col>
                  </Basic.Row>
                  <Basic.Row className="last">
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.TextField
                        ref="module"
                        placeholder={this.i18n('filter.module.placeholder')}/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.EnumSelectBox
                        ref="entityType"
                        placeholder={ this.i18n('filter.entityType.placeholder') }
                        options={ _registeredEntityTypes.toArray().map(value => { return { value, niceLabel: value }; }) }
                        searchable/>
                    </Basic.Col>
                    <Basic.Col lg={ 4 }>
                      <Advanced.Filter.EnumSelectBox
                        ref="eventTypes"
                        placeholder={ this.i18n('filter.eventTypes.placeholder') }
                        options={ _registeredEventTypes.toArray().map(value => { return { value, niceLabel: value }; }) }
                        multiSelect
                        searchable/>
                    </Basic.Col>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            </div>
          </Basic.Collapse>
        </Basic.Toolbar>

        <Basic.Loading isStatic show={ showLoading }/>
        <Basic.Alert level="info" text={ this.i18n('component.basic.Table.noData') } style={{ margin: 15 }} rendered={ !showLoading && _processors.size === 0 } />

        {
          showLoading || _processors.size === 0
          ||
          <div>
            {
              _entityTypes.map((entityType) => {
                const entityProcessors = _processors.get(entityType);
                entityProcessors.sort((one, two) => {
                  return one.order > two.order;
                });
                return (
                  <div className="tab-pane-table-body" style={{ marginBottom: 15 }}>
                    <Basic.ContentHeader text={entityType}/>

                    <Basic.Table
                      data={ entityProcessors }
                      showLoading={ showLoading }
                      noData={ this.i18n('component.basic.Table.noData') }
                      rowClass={({ rowIndex, data }) => { return Utils.Ui.getRowClass(data[rowIndex]); }}>
                      <Basic.Column property="module" header={this.i18n('entity.EntityEventProcessor.module')} width={75} />
                      <Basic.Column property="name" header={this.i18n('entity.EntityEventProcessor.name')} width="30%"/>
                      <Basic.Column
                        property="description"
                        header={this.i18n('entity.EntityEventProcessor.description')}
                        cell={
                          ({ rowIndex, data, property }) => {
                            const values = [];
                            _.keys(data[rowIndex].configurationProperties).map(configurationProperty => {
                              const value = data[rowIndex].configurationProperties[configurationProperty];
                              if (value) {
                                values.push({ configurationProperty, value });
                              }
                            });
                            return (
                              <div>
                                <div>{data[rowIndex][property]}</div>
                                {
                                  values.length === 0
                                  ||
                                  <div>
                                    <div>Configuration:</div>
                                    {
                                      values.map(value => {
                                        return (<div>{ `- ${value.configurationProperty}: ${value.value}` }</div>);
                                      })
                                    }
                                  </div>
                                }
                              </div>
                            );
                          }
                        }/>
                      <Basic.Column
                        property="eventTypes"
                        header={this.i18n('entity.EntityEventProcessor.eventTypes')}
                        width={125}
                        cell={
                          ({ rowIndex, data, property }) => {
                            if (!data[rowIndex][property]) {
                              return null;
                            }
                            return data[rowIndex][property].join(', ');
                          }
                        }/>
                      <Basic.Column property="order" header={this.i18n('entity.EntityEventProcessor.order')} width={100}/>
                      <Basic.Column
                        property="disabled"
                        header={<Basic.Cell className="column-face-bool">{this.i18n('entity.EntityEventProcessor.disabled')}</Basic.Cell>}
                        cell={<Basic.BooleanCell className="column-face-bool"/>}
                        width={ 100 }/>
                      <Basic.Column
                        header={ this.i18n('entity.id.label') }
                        property="id"
                        rendered={ this.isDevelopment() }
                        className="text-center"
                        width={ 100 }
                        cell={
                          ({rowIndex, data, property}) => {
                            return (
                              <Advanced.UuidInfo value={data[rowIndex][property]}/>
                            );
                          }
                        }/>
                      <Basic.Column
                        header={this.i18n('label.action')}
                        className="action"
                        cell={
                          ( {rowIndex, data} ) => {
                            if (!data[rowIndex].disabled) {
                              return (
                                <Basic.Button
                                  level="warning"
                                  onClick={this.onEnable.bind(this, data[rowIndex], false)}
                                  className="btn-xs"
                                  title={this.i18n('button.deactivate')}
                                  titlePlacement="bottom"
                                  rendered={data[rowIndex].disableable}>
                                  {this.i18n('button.deactivate')}
                                </Basic.Button>
                              );
                            }
                            return (
                              <Basic.Button
                                level="success"
                                onClick={this.onEnable.bind(this, data[rowIndex], true)}
                                className="btn-xs"
                                title={this.i18n('button.activate')}
                                titlePlacement="bottom">
                                {this.i18n('button.activate')}
                              </Basic.Button>
                            );
                          }
                        }/>
                    </Basic.Table>
                  </div>
                );
              })
            }
            <Basic.Pagination total={ processors.length } />
          </div>
        }
      </div>
    );
  }
}

EntityEventProcessors.propTypes = {
  userContext: PropTypes.object,
  registeredProcessors: PropTypes.object, // immutable
  processors: PropTypes.arrayOf(PropTypes.object),
  showLoading: PropTypes.bool
};
EntityEventProcessors.defaultProps = {
  userContext: null,
  showLoading: true,
  registeredProcessors: null
};

function select(state) {
  return {
    userContext: state.security.userContext,
    registeredProcessors: DataManager.getData(state, EntityEventProcessorManager.UI_KEY_PROCESSORS),
    processors: manager.getEntities(state, UIKEY),
    showLoading: Utils.Ui.isShowLoading(state, UIKEY)
      || Utils.Ui.isShowLoading(state, EntityEventProcessorManager.UI_KEY_PROCESSORS),
    _searchParameters: Utils.Ui.getSearchParameters(state, UIKEY)
  };
}

export default connect(select)(EntityEventProcessors);
