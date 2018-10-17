import React from 'react';
import PropTypes from 'prop-types';
import uuid from 'uuid';
import { connect } from 'react-redux';
//
import * as Utils from '../../utils';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { SecurityManager } from '../../redux';
import ScriptCategoryEnum from '../../enums/ScriptCategoryEnum';

const MAX_DESCRIPTION_LENGTH = 60;

/**
 * Table with definitions of scripts
 *
 * @author Ondřej Kopr
 */
export class ScriptTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    // default filter status
    // true - open
    // false - close
    this.state = {
      filterOpened: props.filterOpened
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.refs.text.focus();
  }

  getManager() {
    return this.props.scriptManager;
  }

  getContentKey() {
    return 'content.scripts';
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

  onRedeployOrBackup(bulkActionValue, selectedRows) {
    const { scriptManager, uiKey } = this.props;
    const selectedEntities = scriptManager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    // show confirm message for script operation redeploy/backup
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: scriptManager.getNiceLabel(selectedEntities[0]), records: scriptManager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: scriptManager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      this.context.store.dispatch(scriptManager.scriptBulkOperationForEntities(selectedEntities, bulkActionValue, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.${bulkActionValue}.error`, { record: scriptManager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          // refresh data in table
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      // nothing
    });
  }

  /**
   * Recive new form for create new type else show detail for existing org.
   */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    // when create new script is generate non existing id
    // and set parameter new to 1 (true)
    // this is necessary for ScriptDetail
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/scripts/${uuidId}/new?new=1`);
    } else {
      this.context.router.push(`/scripts/${entity.id}/detail`);
    }
  }

  render() {
    const { uiKey, scriptManager, disableAdd, forceSearchParameters } = this.props;
    const { filterOpened } = this.state;
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Confirm ref="confirm-backup" level="danger"/>
        <Basic.Confirm ref="confirm-redeploy" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={uiKey}
          manager={scriptManager}
          showRowSelection={SecurityManager.hasAuthority('SCRIPT_DELETE')}
          rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
          forceSearchParameters={forceSearchParameters}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="code"
                      placeholder={this.i18n('entity.Script.code')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.EnumSelectBox
                      ref="inCategory"
                      placeholder={ this.i18n('entity.Script.category') }
                      enum={ ScriptCategoryEnum }
                      multiSelect/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={ filterOpened }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false },
              { value: 'redeploy', niceLabel: this.i18n('action.redeploy.action'), action: this.onRedeployOrBackup.bind(this), disabled: false },
              { value: 'backup', niceLabel: this.i18n('action.backup.action'), action: this.onRedeployOrBackup.bind(this), disabled: false }
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                hidden={disableAdd}
                onClick={this.showDetail.bind(this, {})}
                rendered={SecurityManager.hasAuthority('SCRIPT_CREATE')}>
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
          <Advanced.ColumnLink to="scripts/:id/detail" property="code" sort />
          <Advanced.Column property="name" sort />
          <Advanced.Column property="category" sort face="enum" enumClass={ScriptCategoryEnum}/>
          <Advanced.Column property="description" cell={ ({ rowIndex, data }) => {
            if (data[rowIndex] && data[rowIndex].description !== null) {
              const description = data[rowIndex].description.replace(/<(?:.|\n)*?>/gm, '').substr(0, MAX_DESCRIPTION_LENGTH);
              return description.substr(0, Math.min(description.length, description.lastIndexOf(' ')));
            }
            return '';
          }}/>
        </Advanced.Table>
      </div>
    );
  }
}

ScriptTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  scriptManager: PropTypes.object.isRequired,
  disableAdd: PropTypes.boolean,
  forceSearchParameters: PropTypes.object
};

ScriptTable.defaultProps = {
  disableAdd: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(ScriptTable);
