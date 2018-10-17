import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
//
import { SecurityManager } from '../../redux';
import uuid from 'uuid';

// Root nodes  key for tree
const rootRoleCatalogueKey = 'tree-role-catalogue-table-roots';

/**
* Table of roles catalogues
*
* @author Ondřej Kopr
* @author Radek Tomiška
*/
class RoleCatalogueTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: props.filterOpened,
      showLoading: true
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { roleCatalogueManager } = this.props;
    const searchParametersRoots = roleCatalogueManager.getRootSearchParameters();
    this.context.store.dispatch(roleCatalogueManager.fetchEntities(searchParametersRoots, rootRoleCatalogueKey, (loadedRoots) => {
      const rootNodes = loadedRoots._embedded[roleCatalogueManager.getCollectionType()];
      this.setState({
        rootNodes
      });
    }));
  }

  getContentKey() {
    return 'content.roleCatalogues';
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  showDetail(entity) {
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/role-catalogue/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`/role-catalogue/${entity.id}/detail`);
    }
  }

  /**
  * Decorator for roles catalogue. Add custom icons and allow filtering after click on node
  */
  _orgTreeHeaderDecorator(props) {
    const style = props.style;
    const icon = props.node.isLeaf ? 'file-text' : 'folder';
    return (
      <div style={style.base}>
        <div style={style.title}>
          <Basic.Icon type="fa" icon={icon} style={{ marginRight: '5px' }}/>
          <Basic.Button level="link" onClick={this._useFilterByTree.bind(this, props.node.id)} style={{padding: '0px 0px 0px 0px'}}>
            { props.node.name }
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

  _useFilterByTree(nodeId, event) {
    if (event) {
      event.preventDefault();
      // Stop propagation is important for prohibition of node tree expand.
      // After click on link node, we want only filtering ... not node expand.
      event.stopPropagation();
    }
    if (!nodeId) {
      return;
    }
    const data = {
      ... this.refs.filterForm.getData(),
      parent: nodeId
    };
    this.refs.parent.setValue(nodeId);
    this.refs.table.getWrappedInstance().useFilterData(data);
  }

  /**
  * Copy from roles
  */
  onDelete(bulkActionValue, selectedRows) {
    const { roleCatalogueManager, uiKey } = this.props;
    const selectedEntities = roleCatalogueManager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: roleCatalogueManager.getNiceLabel(selectedEntities[0]), records: roleCatalogueManager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: roleCatalogueManager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(roleCatalogueManager.deleteEntities(selectedEntities, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: roleCatalogueManager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          this.refs.table.getWrappedInstance().reload();
          this.setState({
            rootNodes: undefined
          }, () => {
            this.context.store.dispatch(roleCatalogueManager.clearEntities());
            const searchParametersRoots = roleCatalogueManager.getRootSearchParameters();
            this.context.store.dispatch(roleCatalogueManager.fetchEntities(searchParametersRoots, rootRoleCatalogueKey, (loadedRoots) => {
              const rootNodes = loadedRoots._embedded[roleCatalogueManager.getCollectionType()];
              this.setState({
                rootNodes
              });
            }));
          });
        }
      }));
    }, () => {
      // nothing
    });
  }

  render() {
    const { uiKey, roleCatalogueManager } = this.props;
    const { filterOpened, rootNodes } = this.state;
    return (
      <Basic.Row>
        <div className="col-lg-3" style={{ paddingRight: 0, paddingLeft: 0, marginLeft: 15, marginRight: -15 }}>
          <div className="basic-toolbar">
            <div className="clearfix"></div>
          </div>
          {
            !(rootNodes !== undefined && rootNodes.length !== 0)
            ||
            <div style={{ paddingLeft: 15, paddingRight: 15, paddingTop: 15 }}>
              <Advanced.Tree
                ref="roleCatalogueTree"
                rootNodes={ rootNodes }
                headerDecorator={this._orgTreeHeaderDecorator.bind(this)}
                uiKey="roleCatalogueTree"
                manager={roleCatalogueManager}/>
            </div>
          }
        </div>

        <div className="col-lg-9">
          <Basic.Confirm ref="confirm-delete" level="danger"/>

          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={roleCatalogueManager}
            rowClass={({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
            filterOpened={filterOpened}
            showRowSelection={SecurityManager.hasAuthority('ROLE_DELETE')}
            style={{ borderLeft: '1px solid #ddd' }}
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <div className="col-lg-4">
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.text')}/>
                    </div>
                    <div className="col-lg-4">
                      <Advanced.Filter.SelectBox
                        ref="parent"
                        placeholder={this.i18n('filter.parentPlaceHolder')}
                        manager={roleCatalogueManager}/>
                    </div>
                    <div className="col-lg-4 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            actions={
              [
                { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
              ]
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { })}
                  rendered={SecurityManager.hasAuthority('ROLECATALOGUE_CREATE')}>
                  <Basic.Icon type="fa" icon="plus"/>
                  {' '}
                  {this.i18n('button.add')}
                </Basic.Button>
              ]
            }
            _searchParameters={ this.getSearchParameters() }
            >

            <Advanced.Column
              header=""
              className="detail-button"
              cell={
                ({ rowIndex, data }) => {
                  return (
                    <Advanced.DetailButton
                      title={this.i18n('button.detail')}
                      onClick={this.showDetail.bind(this, data[rowIndex])}/>
                  );
                }
              }
              sort={false}/>
            <Advanced.ColumnLink to="role-catalogue/:id/detail"
              header={this.i18n('entity.RoleCatalogue.code.name')}
              property="code" width="15%" sort face="text"/>
            <Advanced.Column property="name" header={this.i18n('entity.RoleCatalogue.name.name')} sort face="text"/>
            <Advanced.Column property="parent.name" cell={
                ({rowIndex, data}) => {
                  // get parent name from _embedded
                  const parentName = (data[rowIndex]._embedded && data[rowIndex]._embedded.parent) ? data[rowIndex]._embedded.parent.name : null;
                  return parentName;
                }
              }/>
            <Advanced.Column header={this.i18n('entity.RoleCatalogue.url')}
              cell={
                ({ rowIndex, data }) => {
                  return (<Basic.Link href={data[rowIndex].url} text={data[rowIndex].urlTitle} />);
                }
              }/>
              <Advanced.Column property="description" sort face="text"/>
            </Advanced.Table>
          </div>
        </Basic.Row>
      );
  }
}

RoleCatalogueTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  roleManager: PropTypes.object.isRequired,
  filterOpened: PropTypes.bool
};

RoleCatalogueTable.defaultProps = {
  filterOpened: true,
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(RoleCatalogueTable);
