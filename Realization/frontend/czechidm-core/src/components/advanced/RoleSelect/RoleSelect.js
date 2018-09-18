import React, { PropTypes } from 'react';
import _ from 'lodash';
//
import { RoleManager, RoleCatalogueManager, SecurityManager } from '../../../redux';
import * as Basic from '../../basic';
import EntitySelectBox from '../EntitySelectBox/EntitySelectBox';
import Table from '../Table/Table';
import Column from '../Table/Column';
import Tree from '../Tree/Tree';
import * as Utils from '../../../utils';
import SearchParameters from '../../../domain/SearchParameters';

const TABLE_UIKEY = 'role-table-select';

const ROOTS_ROLE_CATALOGUE_UIKEY = 'roots-role-catalogue';

/**
* Component for select roles by role catalogue
*
* @author Ondrej Kopr
* @author Radek Tomiška
*/
class RoleSelect extends Basic.AbstractFormComponent {

  constructor(props, context) {
    super(props, context);
    //
    this.roleManager = new RoleManager();
    this.roleCatalogueManager = new RoleCatalogueManager();
    //
    this.state = {
      selectedRows: [],
      roleCatalogue: null,
      showLoading: true,
      showRoleCatalogue: false
    };
  }

  componentDidMount() {
    this._loadCatalogue();
    //
    this.cleanFilter();
  }

  componentWillReceiveProps() {
  }

  _loadCatalogue() {
    if (!SecurityManager.hasAuthority('ROLECATALOGUE_AUTOCOMPLETE')) {
      this.setState({
        showLoading: false
      });
      return;
    }
    //
    // find roots
    const searchParametersRoots = this.roleCatalogueManager.getRootSearchParameters();
    //
    this.context.store.dispatch(this.roleCatalogueManager.fetchEntities(searchParametersRoots, ROOTS_ROLE_CATALOGUE_UIKEY, (loadedRoots, error) => {
      if (error) {
        this.setState({
          showLoading: false
        });
      }
      if (loadedRoots) {
        const uiState = Utils.Ui.getUiState(this.context.store.getState(), ROOTS_ROLE_CATALOGUE_UIKEY);
        const rootNodes = loadedRoots._embedded[this.roleCatalogueManager.getCollectionType()];
        //
        this.setState({
          rootNodes,
          rootNodesCount: uiState.total,
          showLoading: false
        });
      }
    }));
  }

  getValue() {
    const { selectedRows } = this.state;
    return selectedRows;
  }

  setValue(value) {
    if (value) {
      const selectedRows = [];
      selectedRows.push(value.id);
      this.setState({
        selectedRows
      });
      this.refs.role.setValue(value);
    }
  }

  /**
  * Method clean filter from role table
  */
  cleanFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      roleCatalogue: null
    });
  }

  validate() {
    const {
      readOnly
    } = this.props;
    if (readOnly) {
      return true;
    }
    return this.refs.role.validate();
  }

  /**
   * Focus input field
   */
  focus() {
    this.refs.role.focus();
  }

  _addRole(index, value, event) {
    //
    if (event) {
      event.preventDefault();
    }
    let { selectedRows } = this.state;
    const { multiSelect } = this.props;
    //
    if (multiSelect) {
      selectedRows.push(value.id);
      this.setState({
        selectedRows
      });
      this.refs.role.setValue(selectedRows);
    } else {
      selectedRows = [];
      selectedRows.push(value.id);
      this.setState({
        selectedRows
      });
      this.refs.role.setValue(value);
    }
  }

  _removeRole(index, value, event) {
    if (event) {
      event.preventDefault();
    }
    //
    let { selectedRows } = this.state;
    //
    selectedRows = _.pull(selectedRows, value.id);
    this.setState({
      selectedRows
    });
    //
    this.refs.role.setValue(selectedRows);
  }

  /**
  * Method filtering by roleCatalogue. Filtr si applied to role table
  */
  _filterByRoleCatalogue(roleCatalogue, event) {
    if (event) {
      event.preventDefault();
      // Stop propagation is important for prohibition of node tree expand.
      // After click on link node, we want only filtering ... not node expand.
      event.stopPropagation();
    }
    if (!roleCatalogue) {
      return;
    }
    //
    this.setState({
      roleCatalogue: roleCatalogue.id
    });
  }

  /**
  * Decorator and filter method for tree
  */
  _roleCatalogueDecorator(props) {
    const style = props.style;
    const icon = props.node.isLeaf ? 'file-text' : 'folder';
    return (
      <div style={style.base}>
        <div style={style.title}>
          <Basic.Button level="link" title={props.node.name} onClick={this._filterByRoleCatalogue.bind(this, props.node)} style={{padding: '0px 0px 0px 0px'}}>
            <Basic.Icon type="fa" icon={icon} style={{ marginRight: '5px' }}/>
            { Utils.Ui.substringByWord(props.node.name, 25)}
            {
              !props.node.childrenCount
              ||
              <small style={{ color: '#aaa' }}>{' '}({props.node.childrenCount})</small>
            }
          </Basic.Button>
        </div>
      </div>
    );
  }

  _onRowClick(event, rowIndex, data) {
    if (event) {
      event.preventDefault();
    }
    //
    const { selectedRows } = this.state;
    const role = data[rowIndex];
    const isSelected = _.includes(selectedRows, role.id);
    //
    if (isSelected) {
      this._removeRole(rowIndex, role);
    } else {
      this._addRole(rowIndex, role);
    }
  }

  _addAll(event) {
    if (event) {
      event.preventDefault();
    }
    // get data from filter
    const filterData = this.refs.filterForm.getData();
    //
    const searchParameters = new SearchParameters(SearchParameters.NAME_QUICK)
    // TODO: null in setSize not working
    .setSize(250).setPage(null)
    .setFilter('roleCatalogue', filterData.roleCatalogue)
    .setFilter('text', filterData.text);
    this.context.store.dispatch(this.roleManager.fetchEntities(searchParameters, null, (entities) => {
      if (entities) {
        const selectedRows = [];
        const roles = entities._embedded[this.roleManager.getCollectionType()];
        for (const index in roles) {
          if (roles.hasOwnProperty(index)) {
            selectedRows.push(roles[index].id);
          }
        }
        this.refs.role.setValue(selectedRows);
        //
        this.setState({
          selectedRows,
          showLoading: false
        });
      }
    }));
  }

  _removeAll(event) {
    if (event) {
      event.preventDefault();
    }
    //
    this.setState({
      selectedRows: []
    });
    this.refs.role.setValue([]);
  }

  hideRoleCatalogueTable() {
    this.setState({
      showRoleCatalogue: false
    });
  }

  showRoleCatalogueTable() {
    this.setState({
      showRoleCatalogue: true
    });
  }

  _showOrHideRoleCatalogueTable(event) {
    if (event) {
      event.preventDefault();
    }
    const { showRoleCatalogue } = this.state;
    const { onCatalogueShow } = this.props;
    //
    this.setState({
      showRoleCatalogue: !showRoleCatalogue
    });
    //
    if (onCatalogueShow) {
      onCatalogueShow(!showRoleCatalogue);
    }
  }

  _selectChange(value, event) {
    if (event) {
      event.preventDefault();
    }
    //
    const selectedRows = [];
    for (const index in value) {
      if (value.hasOwnProperty(index)) {
        if (value[index] && value[index].id) {
          selectedRows.push(value[index].id);
        }
      }
    }
    this.setState({
      selectedRows
    });
  }

  render() {
    const {
      columns,
      multiSelect,
      readOnly,
      showActionButtons,
      showBulkAction,
      selectRowClass
    } = this.props;
    //
    const {
      rootNodes,
      rootNodesCount,
      selectedRows,
      showLoading,
      roleCatalogue,
      showRoleCatalogue
    } = this.state;
    //
    const showTree = rootNodes && rootNodes.length !== 0;
    //
    // TODO: add onRowClick={this._onRowClick.bind(this)}
    return (
      <div>
        <Basic.Row className={ showRoleCatalogue ? 'hidden' : null }>
          <Basic.Col lg={ showTree ? 9 : 12 }>
            <EntitySelectBox
              ref="role"
              manager={this.roleManager}
              label={this.i18n('entity.IdentityRole.role')}
              multiSelect={multiSelect}
              onChange={this._selectChange.bind(this)}
              readOnly={readOnly}
              entityType="role"
              required/>
          </Basic.Col>
          <Basic.Col lg={ 3 } rendered={ showTree === true }>
            <Basic.Button
              style={{marginTop: '25px', width: '100%', display: 'block' }}
              level="primary"
              title={this.i18n('content.roles.select.showRoleCatalogue')}
              titlePlacement="bottom"
              disabled={readOnly}
              onClick={this._showOrHideRoleCatalogueTable.bind(this)}>
              {' '}
              {this.i18n('content.roles.select.showRoleCatalogue')}
            </Basic.Button>
          </Basic.Col>
        </Basic.Row>

        <Basic.Panel className="no-border last">
          <Basic.Row rendered={ showRoleCatalogue }>
            <Basic.Col lg={ 3 } style={{ paddingRight: 0, paddingLeft: 0, marginLeft: 15, marginRight: -15 }} rendered={ showTree === true }>
              <div className="basic-toolbar" style={{ padding: '3px 15px 5px 0px', minHeight: 'auto' }}>
                <div className="pull-left">
                  <h3 style={{ margin: 0 }}>
                    { this.i18n('content.roles.select.chooseFolder') }
                  </h3>
                </div>
                <div className="pull-right">
                  <Basic.Button
                    level="primary"
                    title={this.i18n('content.tree.nodes.reloadTree')}
                    titlePlacement="bottom"
                    className="btn-xs"
                    onClick={this.cleanFilter.bind(this)}
                    rendered={ false }>
                    <Basic.Icon value="fa:refresh"/>
                  </Basic.Button>
                </div>
                <div className="clearfix"></div>
              </div>
              <div style={{ paddingTop: 5 }}>
                {
                  !showTree
                  ||
                  <div>
                    <Basic.Button
                        level="link"
                        className="btn-xs"
                        onClick={ this.cleanFilter.bind(this) }
                        disabled={ roleCatalogue === null }>
                      { this.i18n('content.roles.button.allRoles') }
                    </Basic.Button>
                    <Tree
                      showLoading={showLoading}
                      rootNodes={rootNodes}
                      rootNodesCount={ rootNodesCount }
                      uiKey="roleCatalogueTree"
                      headerDecorator={this._roleCatalogueDecorator.bind(this)}
                      manager={this.roleCatalogueManager}/>
                  </div>
                }
              </div>
            </Basic.Col>
            <Basic.Col lg={ showTree ? 9 : 12 }>
              <Table
                showLoading={showLoading}
                ref="table"
                condensed
                className={ showTree ? '' : 'marginable' }
                style={ showTree ? { borderLeft: '1px solid #ddd' } : {} }
                forceSearchParameters={
                  this.roleManager.getDefaultSearchParameters()
                    .setName(SearchParameters.NAME_AUTOCOMPLETE)
                    .setFilter('roleCatalogue', roleCatalogue)
                }
                showToolbar={false}
                uiKey={TABLE_UIKEY}
                manager={this.roleManager}
                showPageSize={false}
                rowClass={({rowIndex, data}) => {
                  return _.includes(selectedRows, data[rowIndex].id) ? selectRowClass : '';
                }}
                buttons={
                  [
                    !showBulkAction
                    ||
                    <div>
                      <Basic.Button type="submit" className="btn-xs"
                        readOnly={readOnly}
                        hidden={!multiSelect}
                        onClick={this._addAll.bind(this)}>
                        <Basic.Icon type="fa" icon="plus"/>
                        {' '}
                        {this.i18n('button.addAll')}
                      </Basic.Button>
                      <Basic.Button type="submit" className="btn-xs"
                        readOnly={readOnly}
                        hidden={!multiSelect}
                        onClick={this._removeAll.bind(this)}>
                        <Basic.Icon type="fa" icon="minus"/>
                        {' '}
                        {this.i18n('button.removeAll')}
                      </Basic.Button>
                    </div>
                  ]
                }>
                <Column
                  property=""
                  header=""
                  width="5px"
                  rendered={showActionButtons}
                  cell={
                    ({ rowIndex, data }) => {
                      const isSelected = _.includes(selectedRows, data[rowIndex].id);
                      if (data[rowIndex].disabled) {
                        return (
                          null
                        );
                      }
                      return (
                        <span>
                          {
                            !isSelected
                            ?
                            <input
                              readOnly={readOnly}
                              type="checkbox"
                              checked={false}
                              onMouseDown={this._addRole.bind(this, rowIndex, data[rowIndex])}/>
                            :
                            <input
                              readOnly={readOnly}
                              type="checkbox"
                              checked
                              onMouseDown={this._removeRole.bind(this, rowIndex, data[rowIndex])}/>
                          }
                        </span>
                      );
                    }
                  }/>
                  <Column property="code" sort={false} face="text" rendered={_.includes(columns, 'code')}/>
                  <Column property="name" sort={false} face="text" rendered={_.includes(columns, 'name')}/>
                </Table>
              </Basic.Col>
            </Basic.Row>
          </Basic.Panel>
        </div>
      );
  }
}

RoleSelect.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.string),
  multiSelect: PropTypes.bool,
  showActionButtons: PropTypes.bool,
  showBulkAction: PropTypes.bool,
  selectRowClass: PropTypes.string,
  onCatalogueShow: PropTypes.func
};
RoleSelect.defaultProps = {
  columns: ['name'],
  multiSelect: true,
  showActionButtons: true,
  showBulkAction: false,
  selectRowClass: 'success',
  onCatalogueShow: null
};


export default RoleSelect;
