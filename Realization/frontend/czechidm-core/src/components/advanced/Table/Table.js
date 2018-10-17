import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import invariant from 'invariant';
import _ from 'lodash';
import Immutable from 'immutable';
import moment from 'moment';
import classnames from 'classnames';
//
import * as Basic from '../../basic';
import * as Utils from '../../../utils';
import Filter from '../Filter/Filter';
import SearchParameters from '../../../domain/SearchParameters';
import UuidInfo from '../UuidInfo/UuidInfo';
import RefreshButton from './RefreshButton';
import { DataManager, FormAttributeManager, LongRunningTaskManager, SecurityManager } from '../../../redux';
import EavAttributeForm from '../Form/EavAttributeForm';
import LongRunningTask from '../LongRunningTask/LongRunningTask';

/**
 * Table component with header and columns.
 *
 * @author Radek Tomiška
 */
class AdvancedTable extends Basic.AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    // resolve static frontend actions (backend actions are loaded asynchronously ... check componentDidMount)
    const { manager, actions } = this.props;
    let _actions = [];
    if (!manager.supportsBulkAction() && actions !== null && actions.length > 0) {
      _actions = actions.filter(action => {
        return action.rendered === undefined || action.rendered === true || action.rendered === null;
      });
    }
    //
    this.state = {
      filterOpened: this.props.filterOpened,
      selectedRows: this.props.selectedRows,
      removedRows: new Immutable.Set(),
      showBulkActionDetail: false,
      bulkActionShowLoading: false,
      _actions
    };
    this.attributeManager = new FormAttributeManager();
    this.longRunningTaskManager = new LongRunningTaskManager();
  }

  /**
   * Return component identifier, with can be used in localization etc.
   *
   * @return {string} component identifier
   */
  getComponentKey() {
    return 'component.advanced.Table';
  }

  componentDidMount() {
    const { manager } = this.props;
    this.reload();
    //
    if (manager.supportsBulkAction() && manager.canRead()) {
      this.context.store.dispatch(manager.fetchAvailableBulkActions((actions, error) => {
        if (error) {
          if (error.statusCode === 403) {
            // user doesn't have permissions for work with entities in the table
          } else {
            this.addErrorMessage({}, error);
          }
        } else {
          const _actions = [];
          // TODO: react elements are stored in state ... redesign raw data and move cached actions into reducer
          for (const index in actions) {
            if (actions.hasOwnProperty(index)) {
              const backendBulkAction = actions[index];
              const iconKey = backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.icon';
              const icon = this.i18n(iconKey);
              _actions.push({
                value: backendBulkAction.name,
                niceLabel: (
                  <span key={ `b-a-${backendBulkAction.name}` }>
                    <Basic.Icon value={ icon } rendered={ backendBulkAction.module + icon !== iconKey } style={{ marginRight: 5 }}/>
                    { this.i18n(backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.label') }
                  </span>),
                action: this.showBulkActionDetail.bind(this, backendBulkAction),
                disabled: !SecurityManager.hasAllAuthorities(backendBulkAction.authorities)
              });
            }
          }
          //
          this.setState({
            _actions
          });
        }
      }));
    }
  }

  componentWillReceiveProps(newProps) {
    if (!SearchParameters.is(newProps.forceSearchParameters, this.props.forceSearchParameters)) {
      this.reload(newProps);
    } else if (!SearchParameters.is(newProps.defaultSearchParameters, this.props.defaultSearchParameters)) {
      this.reload(newProps);
    } else if (newProps.rendered !== this.props.rendered) {
      this.reload(newProps);
    }
  }

  reload(props = null) {
    const _props = props || this.props;
    const { rendered, _searchParameters } = _props;
    if (!rendered) {
      return;
    }
    this.fetchEntities(_searchParameters, _props);
  }

  /**
   * Clears row selection
   */
  clearSelectedRows() {
    this.setState({
      selectedRows: []
    });
  }

  /**
   * Process select row in table. The method is ised only for bulk action.
   * Methot has own behavior for select all.
   *
   * @param  rowIndex
   * @param  selected
   * @return
   */
  selectRowForBulkAction(rowIndex, selected) {
    const { selectedRows, removedRows } = this.state;
    let newRemovedRows = new Immutable.Set(removedRows);
    let newSelectedRows = new Immutable.Set(selectedRows);
    if (rowIndex === -1) {
      // de/select all rows
      // reset selected rows
      newSelectedRows = new Immutable.Set();
      if (selected) {
        newSelectedRows = newSelectedRows.add(Basic.Table.SELECT_ALL);
      } else {
        newSelectedRows = newSelectedRows.remove(Basic.Table.SELECT_ALL);
      }
      // reset removed rows
      newRemovedRows = new Immutable.Set();
    } else {
      const recordId = this.refs.table.getIdentifier(rowIndex);
      // de/select one row
      if (_.includes(selectedRows, Basic.Table.SELECT_ALL)) {
        if (selected) {
          newRemovedRows = newRemovedRows.remove(recordId);
        } else {
          newRemovedRows = newRemovedRows.add(recordId);
        }
      } else {
        newSelectedRows = (selected ? newSelectedRows.add(recordId) : newSelectedRows.remove(recordId));
      }
    }
    this.setState({
      selectedRows: newSelectedRows.toArray(),
      removedRows: newRemovedRows
    });
    return newSelectedRows;
  }

  isAllRowSelected() {
    const { selectedRows } = this.state;
    // if selected rows contains SELECT ALL return true
    return _.includes(selectedRows, Basic.Table.SELECT_ALL);
  }

  isRowSelected(identifier) {
    const { selectedRows, removedRows } = this.state;
    if (_.includes(selectedRows, Basic.Table.SELECT_ALL)) {
      return !removedRows.has(identifier);
    }
    return _.includes(selectedRows, identifier);
  }

  processBulkAction(bulkAction, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.bulkActionAttributes.isValid()) {
      return;
    }
    const _searchParameters = this._mergeSearchParameters(this.props._searchParameters);

    const { selectedRows, removedRows } = this.state;

    if (bulkAction) {
      const bulkActionToProcess = {
        ...bulkAction
      };
      const { manager } = this.props;
      // remove unnecessary attributes
      delete bulkActionToProcess.formAttributes;
      delete bulkActionToProcess.longRunningTaskId;
      delete bulkActionToProcess.permissions;
      //
      bulkActionToProcess.properties = this.refs.bulkActionAttributes.getValues();
      if (_.includes(selectedRows, Basic.Table.SELECT_ALL)) {
        bulkActionToProcess.filter = _searchParameters.getFilters().toJSON();
        bulkActionToProcess.removeIdentifiers = removedRows.toArray();
      } else {
        bulkActionToProcess.identifiers = selectedRows;
      }
      //
      this.setState({
        bulkActionShowLoading: true
      });
      this.context.store.dispatch(manager.processBulkAction(bulkActionToProcess, (processBulkAction, error) => {
        if (error) {
          this.addErrorMessage({}, error);
          this.setState({
            bulkActionShowLoading: false
          });
        } else {
          this.addMessage({ level: 'info', message: this.i18n('bulkAction.created', { longRunningTaskId: processBulkAction.longRunningTaskId, name: this.i18n(processBulkAction.module + ':eav.bulk-action.' + processBulkAction.name + '.label')})});
          this.setState({
            selectedRows: [],
            removedRows: new Immutable.Set(),
            bulkActionShowLoading: false,
            backendBulkAction: processBulkAction
          });
        }
      }));
    }
  }

  prevalidateBulkAction(bulkAction, event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.bulkActionAttributes.isValid()) {
      return;
    }
    const _searchParameters = this._mergeSearchParameters(this.props._searchParameters);

    const { selectedRows, removedRows } = this.state;

    if (bulkAction) {
      const bulkActionToProcess = {
        ...bulkAction
      };
      const { manager } = this.props;
      // remove unnecessary attributes
      delete bulkActionToProcess.formAttributes;
      delete bulkActionToProcess.longRunningTaskId;
      delete bulkActionToProcess.permissions;
      //
      bulkActionToProcess.properties = this.refs.bulkActionAttributes.getValues();
      if (_.includes(selectedRows, Basic.Table.SELECT_ALL)) {
        bulkActionToProcess.filter = _searchParameters.getFilters().toJSON();
        bulkActionToProcess.removeIdentifiers = removedRows.toArray();
      } else {
        bulkActionToProcess.identifiers = selectedRows;
      }
      //
      this.setState({
        bulkActionShowLoading: true
      });
      this.context.store.dispatch(manager.prevalidateBulkAction(bulkActionToProcess, (resultModel, error) => {
        if (error) {
          this.addErrorMessage({}, error);
          this.setState({
            bulkActionShowLoading: false
          });
        } else if (resultModel) {
          const backendBulkAction = this.state.backendBulkAction;
          backendBulkAction.prevalidateResult = resultModel;
          this.setState({
            bulkActionShowLoading: false,
            backendBulkAction
          });
        } else {
          this.setState({
            bulkActionShowLoading: false
          });
        }
      }));
    }
  }

  /**
   * Merge hard, default and user deffined search parameters
   */
  _mergeSearchParameters(searchParameters, props = null) {
    const _props = props || this.props;
    const { defaultSearchParameters, forceSearchParameters, manager } = _props;
    //
    let _forceSearchParameters = null;
    if (forceSearchParameters) {
      _forceSearchParameters = forceSearchParameters.setSize(null).setPage(null); // we dont want override setted pagination
    }
    return manager.mergeSearchParameters(searchParameters || defaultSearchParameters || manager.getDefaultSearchParameters(), _forceSearchParameters);
  }

  fetchEntities(searchParameters, props = null) {
    const _props = props || this.props;
    const { uiKey, manager } = _props;
    searchParameters = this._mergeSearchParameters(searchParameters, _props);
    this.context.store.dispatch(manager.fetchEntities(searchParameters, uiKey, (json, error) => {
      if (error) {
        this.addErrorMessage({
          key: 'error-' + manager.getEntityType() + '-load'
        }, error);
      // remove selection for unpresent records
      } else if (json && json._embedded) {
        const { selectedRows } = this.state;
        const newSelectedRows = [];
        if (json._embedded[manager.getCollectionType()]) {
          json._embedded[manager.getCollectionType()].forEach(entity => {
            if (_.includes(selectedRows, entity.id)) { // TODO: custom identifier - move to manager
              newSelectedRows.push(entity.id);
            }
          });
        }
        this.setState({
          selectedRows: newSelectedRows
        });
      }
    }));
  }

  _handlePagination(page, size) {
    const { uiKey, manager } = this.props;
    this.context.store.dispatch(manager.handlePagination(page, size, uiKey));
  }

  _handleSort(property, order) {
    const { uiKey, manager } = this.props;
    this.context.store.dispatch(manager.handleSort(property, order, uiKey));
  }

  _resolveColumns() {
    const children = [];
    //
    React.Children.forEach(this.props.children, (child) => {
      if (child == null) {
        return;
      }
      invariant(
        // child.type.__TableColumnGroup__ ||
        child.type.__TableColumn__ ||
        child.type.__AdvancedColumn__ || child.type.__AdvancedColumnLink__,
        'child type should be <TableColumn /> or ' +
        'child type should be <AdvancedColumn /> or ' +
        '<AdvancedColumnGroup />'
      );
      children.push(child);
    });
    return children;
  }

  useFilterForm(filterForm) {
    this.useFilterData(SearchParameters.getFilterData(filterForm));
  }

  useFilterData(formData) {
    this.fetchEntities(this._getSearchParameters(formData));
  }

  /**
   * Returns search parameters filled from given filter form data
   *
   * @param  {object} formData
   * @return {SearchParameters}
   */
  _getSearchParameters(formData) {
    const { _searchParameters } = this.props;
    //
    return SearchParameters.getSearchParameters(formData, _searchParameters);
  }

  /**
   * Returns search parameters filled from given filter form (referernce)
   *
   * @param  {ref} filterForm
   * @return {SearchParameters}
   */
  getSearchParameters(filterForm) {
    return this._getSearchParameters(SearchParameters.getFilterData(filterForm));
  }

  cancelFilter(filterForm) {
    const { manager, _searchParameters } = this.props;
    //
    const filterValues = filterForm.getData();
    for (const property in filterValues) {
      if (!filterValues[property]) {
        continue;
      }
      const filterComponent = filterForm.getComponent(property);
      if (!filterComponent) {
        // filter is not rendered
        continue;
      }
      filterComponent.setValue(null);
    }
    // prevent sort and pagination
    let userSearchParameters = _searchParameters.setFilters(manager.getDefaultSearchParameters().getFilters());
    userSearchParameters = userSearchParameters.setPage(0);
    //
    this.fetchEntities(userSearchParameters);
  }

  _onRowSelect(rowIndex, selected, selection) {
    const { onRowSelect } = this.props;
    //
    this.setState({
      selectedRows: selection
    }, () => {
      if (onRowSelect) {
        onRowSelect(rowIndex, selected, selection);
      }
    });
  }

  onBulkAction(actionItem) {
    if (actionItem.action) {
      actionItem.action(actionItem.value, this.state.selectedRows, actionItem);
    } else {
      this.addMessage({ level: 'info', message: this.i18n('bulk-action.notImplemented') });
    }
    return false;
  }

  getNoData(noData) {
    if (noData !== null && noData !== undefined) {
      return noData;
    }
    // default noData
    return this.i18n('noData', { defaultValue: 'No record found' });
  }

  _showId() {
    const { showId } = this.props;
    //
    if (showId === null || showId === undefined) {
      return this.isDevelopment();
    }
    return showId;
  }

  _filterOpen(open) {
    const { filterOpen } = this.props;
    let result = true;
    if (filterOpen) {
      result = filterOpen(open);
    }
    //
    if (result !== false) {
      this.setState({
        filterOpened: open
      });
    }
  }

  showBulkActionDetail(backendBulkAction) {
    const { showBulkActionDetail } = this.state;
    //
    if (showBulkActionDetail) {
      this.setState({
        showBulkActionDetail: !showBulkActionDetail
      });
    } else {
      this.setState({
        showBulkActionDetail: !showBulkActionDetail,
        backendBulkAction,
        now: moment(new Date()).format(this.i18n('format.datetime'))
      }, () => {
        this.prevalidateBulkAction(backendBulkAction);
      });
    }
  }

  _renderPrevalidateMessages(backendBulkAction) {
    if (!backendBulkAction.prevalidateResult) {
      return null;
    }
    if (!backendBulkAction.prevalidateResult._infos) {
      return null;
    }

    const result = [];
    for (const model of backendBulkAction.prevalidateResult._infos) {
      result.push(
        <Basic.FlashMessage showHtmlText message={ this.getFlashManager().convertFromResultModel(model) }/>
      );
    }
    return result;
  }

  _renderBulkActionDetail() {
    const {
      backendBulkAction,
      showBulkActionDetail,
      bulkActionShowLoading,
      selectedRows,
      now,
      removedRows } = this.state;
    const { _total, manager } = this.props;
    const count = _total - removedRows.size;

    const isSelectedAll = _.includes(selectedRows, Basic.Table.SELECT_ALL);
    // get entities for currently selected
    let selectedEntities = [];
    if (!isSelectedAll) {
      selectedEntities = manager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    }
    //
    // get entitties for currently deselected
    let removedEnties = [];
    if (removedRows.size > 0) {
      removedEnties = manager.getEntitiesByIds(this.context.store.getState(), removedRows.toArray());
    }
    let modalContent = null;
    if (backendBulkAction && backendBulkAction.longRunningTaskId) {
      if (SecurityManager.hasAuthority('SCHEDULER_READ')) {
        modalContent = (
          <Basic.Modal.Body style={ {padding: 0, marginBottom: -20} }>
            <LongRunningTask
              entityIdentifier={ backendBulkAction.longRunningTaskId }
              header={ this.i18n(backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.label')}
              showProperties={ false }
              onComplete={ () => this.reload() }
              footerButtons={
                <Basic.Button
                  level="link"
                  onClick={this.showBulkActionDetail.bind(this)}>
                  {this.i18n('button.close')}
                </Basic.Button>
              }/>
            </Basic.Modal.Body>
          );
      } else {
        modalContent = (
          <div>
            <Basic.Modal.Header text={ this.i18n(backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.label') }/>
            <Basic.Modal.Body>
              <Basic.Alert
                level="info"
                text={this.i18n('bulkAction.insufficientReadPermission')}/>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.showBulkActionDetail.bind(this)}>
                {this.i18n('button.close')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </div>
        );
      }
    } else if (backendBulkAction) {
      const helpKey = backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.help';
      const help = this.i18n(helpKey);
      modalContent = (
        <form onSubmit={this.processBulkAction.bind(this, backendBulkAction)}>
          <Basic.Modal.Header text={ this.i18n(backendBulkAction.module + ':eav.bulk-action.' + backendBulkAction.name + '.label') }/>
          <Basic.Modal.Body>
            <Basic.AbstractForm ref="bulkActionForm" showLoading={bulkActionShowLoading}>
              <Basic.Alert
                level="warning"
                text={this.i18n('bulkAction.selectAllRecordsWarning',
                  {
                    count,
                    action: this.i18n(`${backendBulkAction.module}:eav.bulk-action.${backendBulkAction.name}.label`),
                    date: now,
                    escape: false
                  })}
                rendered={isSelectedAll} />
              <Basic.Row rendered={!isSelectedAll} style={ { marginLeft: 0, marginRight: 0, marginBottom: 15 } }>
                <span dangerouslySetInnerHTML={{ __html: this.i18n('bulkAction.message', {
                  count: selectedRows.length,
                  entities: manager.getNiceLabels(selectedEntities).join(', '),
                  name: this.i18n(`${backendBulkAction.module}:eav.bulk-action.${backendBulkAction.name}.label`) }) }}/>
              </Basic.Row>
              <Basic.Row rendered={removedEnties.length > 0} style={ { marginLeft: 0, marginRight: 0, marginBottom: 15 } }>
                <span dangerouslySetInnerHTML={{ __html: this.i18n('bulkAction.removedRecord', {
                  count: removedEnties.length,
                  entities: manager.getNiceLabels(removedEnties).join(', ') }) }}/>
              </Basic.Row>
              <Basic.Alert
                level="info"
                showHtmlText
                text={ help }
                rendered={ (`${backendBulkAction.module}:${help}`) !== helpKey } />

              { this._renderPrevalidateMessages(backendBulkAction) }

              <EavAttributeForm
                ref="bulkActionAttributes"
                localizationKey={ backendBulkAction.name }
                localizationModule={ backendBulkAction.module }
                formAttributes={ backendBulkAction.formAttributes }
                localizationType="bulk-action"/>
            </Basic.AbstractForm>
          </Basic.Modal.Body>
          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={ this.showBulkActionDetail.bind(this) }>
              { this.i18n('button.close') }
            </Basic.Button>
            <Basic.Button
              type="submit"
              level="success"
              showLoading={ bulkActionShowLoading }
              showLoadingIcon
              showLoadingText={ this.i18n('button.saving') }>
              { this.i18n('bulkAction.button.execute') }
            </Basic.Button>
          </Basic.Modal.Footer>
        </form>
      );
    }

    return (
      <div>
        <Basic.Modal
          show={ showBulkActionDetail }
          onHide={ this.showBulkActionDetail.bind(this) }
          backdrop="static">
          { modalContent }
        </Basic.Modal>
      </div>
    );
  }

  render() {
    const {
      _entities,
      _total,
      _showLoading,
      _error,
      manager,
      pagination,
      onRowClick,
      onRowDoubleClick,
      rowClass,
      rendered,
      filter,
      filterCollapsible,
      filterViewportOffsetTop,
      buttons,
      noData,
      style,
      showRowSelection,
      showLoading,
      showFilter,
      showPageSize,
      showToolbar,
      showRefreshButton,
      condensed,
      header,
      forceSearchParameters,
      className
    } = this.props;
    const {
      filterOpened,
      selectedRows,
      removedRows,
      _actions
    } = this.state;
    //
    if (!rendered) {
      return null;
    }
    //
    const columns = this._resolveColumns();
    let range = null;
    const _searchParameters = this._mergeSearchParameters(this.props._searchParameters);
    if (_searchParameters) {
      range = {
        page: _searchParameters.getPage(),
        size: _searchParameters.getSize()
      };
    }
    const renderedColumns = [];
    for (let i = 0; i < columns.length; i++) {
      const column = columns[i];
      // basic column support
      if (column.type.__TableColumn__) {
        // add cloned elemet with data provided
        renderedColumns.push(column);
        continue;
      }
      // common props to all column faces
      const commonProps = {
        // title: column.props.title,
        className: `column-face-${column.props.face}`
      };
      // construct basic column from advanced column definition
      let columnHeader = column.props.header;
      let columnTitle = column.props.title;
      if (column.props.property) {
        if (!columnHeader) {
          columnHeader = this.i18n(
            `${manager.getModule()}:entity.${manager.getEntityType()}.${column.props.property}.label`, // label has higher priority
            { defaultValue: this.i18n(`${manager.getModule()}:entity.${manager.getEntityType()}.${column.props.property}`)}
          );
        }
        if (!columnTitle) {
          columnTitle = this.i18n(`${manager.getModule()}:entity.${manager.getEntityType()}.${column.props.property}.title`, { defaultValue: '' });
        }
      }
      if (column.props.sort) {
        columnHeader = (
          <Basic.BasicTable.SortHeaderCell
            header={ columnHeader }
            sortHandler={ this._handleSort.bind(this) }
            sortProperty={ column.props.sortProperty || column.props.property }
            searchParameters={ _searchParameters }
            className={ commonProps.className }
            title={ columnTitle }/>
        );
      } else if (columnTitle) {
        columnHeader = (
          <span title={ columnTitle }>
            { columnHeader }
          </span>
        );
      }
      //
      const key = 'column_' + i;
      let cell = null;
      if (column.props.cell) {
        cell = column.props.cell;
      } else if (column.type.__AdvancedColumnLink__) {
        cell = (
          <Basic.BasicTable.LinkCell to={column.props.to} target={column.props.target} access={column.props.access} {...commonProps}/>
        );
      } else {
        switch (column.props.face) {
          case 'text': {
            cell = (
              <Basic.BasicTable.TextCell {...commonProps} maxLength={ column.props.maxLength }/>
            );
            break;
          }
          case 'date': {
            cell = (
              <Basic.BasicTable.DateCell format={this.i18n('format.date')} {...commonProps}/>
            );
            break;
          }
          case 'datetime': {
            cell = (
              <Basic.BasicTable.DateCell format={this.i18n('format.datetime')} {...commonProps}/>
            );
            break;
          }
          case 'bool':
          case 'boolean': {
            cell = (
              <Basic.BasicTable.BooleanCell {...commonProps}/>
            );
            break;
          }
          case 'enum': {
            cell = (
              <Basic.BasicTable.EnumCell enumClass={column.props.enumClass} {...commonProps}/>
            );
            break;
          }
          default: {
            this.getLogger().trace('[AdvancedTable] usind default for column face [' + column.props.face + ']');
          }
        }
      }
      // add target column with cell by data type
      renderedColumns.push(
        <Basic.BasicTable.Column
          key={key}
          property={column.props.property}
          rendered={column.props.rendered}
          className={column.props.className}
          width={column.props.width}
          header={ columnHeader }
          title={ columnTitle }
          cell={cell}/>
      );
    }
    //
    let _rowClass = rowClass;
    if (!_rowClass) {
      // automatic rowClass by entity's "disabled" attribute
      _rowClass = ({rowIndex, data}) => { return Utils.Ui.getDisabledRowClass(data[rowIndex]); };
    }
    // If is manager in the universal request mode, then row class by requests will be used (REMOVE / ADD / CHANGE).
    if (manager.isRequestModeEnabled && manager.isRequestModeEnabled()) {
      _rowClass = ({rowIndex, data}) => { return Utils.Ui.getRequestRowClass(data[rowIndex]); };
    }
    //
    let count = 0;
    if (_.includes(selectedRows, Basic.Table.SELECT_ALL)) {
      count = _total - removedRows.size;
    } else {
      count = selectedRows.length;
    }
    //
    return (
      <div className={ classnames('advanced-table', className) } style={ style }>
        {
          !filter && (_actions.length === 0 || !showRowSelection) && (buttons === null || buttons.length === 0)
          ||
          <Basic.Toolbar container={this} viewportOffsetTop={filterViewportOffsetTop} rendered={showToolbar}>
            <div className="advanced-table-heading">
              <div className="pull-left">
                <Basic.EnumSelectBox
                  onChange={this.onBulkAction.bind(this)}
                  ref="bulkActionSelect"
                  componentSpan=""
                  className={selectedRows.length <= 0 ? 'hidden' : 'bulk-action'}
                  multiSelect={false}
                  options={ _actions }
                  placeholder={this.i18n('bulk-action.selection' + (selectedRows.length === 0 ? '_empty' : ''), { count })}
                  rendered={ _actions.length > 0 && showRowSelection}
                  searchable={false}/>
              </div>
              <div className="pull-right">
                { buttons }

                <Filter.ToogleButton
                  filterOpen={ this._filterOpen.bind(this) }
                  filterOpened={ filterOpened }
                  rendered={ showFilter && filter !== undefined && filterCollapsible }
                  style={{ marginLeft: 3 }}
                  searchParameters={ _searchParameters }
                  forceSearchParameters={ forceSearchParameters }/>

                <RefreshButton
                  onClick={ this.fetchEntities.bind(this, _searchParameters, this.props) }
                  title={ this.i18n('button.refresh') }
                  showLoading={ _showLoading }
                  rendered={ showRefreshButton }/>
              </div>
              <div className="clearfix"></div>
            </div>
            <Basic.Collapse in={filterOpened} rendered={ showFilter }>
              <div>
                { filter }
              </div>
            </Basic.Collapse>
          </Basic.Toolbar>
        }
        {
          _error
          ?
          <Basic.Alert level="warning">
            <div style={{ textAlign: 'center', display: 'none'}}>
              <Basic.Icon value="fa:warning"/>
              {' '}
              { this.i18n('error.load') }
            </div>
            <div style={{ textAlign: 'center' }}>
              <Basic.Button onClick={this.reload.bind(this, this.props)}>
                <Basic.Icon value="fa:refresh"/>
                {' '}
                { this.i18n('button.refresh') }
              </Basic.Button>
            </div>
          </Basic.Alert>
          :
          <div>
            <Basic.BasicTable.Table
              ref="table"
              header={ header }
              data={ _entities }
              showLoading={ _showLoading || showLoading }
              onRowClick={ onRowClick }
              onRowDoubleClick={ onRowDoubleClick }
              showRowSelection={ _actions.length > 0 && showRowSelection }
              selectedRows={ selectedRows }
              onRowSelect={ this._onRowSelect.bind(this) }
              rowClass={ _rowClass }
              condensed={ condensed }
              noData={ this.getNoData(noData) }
              selectRowCb={ manager.supportsBulkAction() ? this.selectRowForBulkAction.bind(this) : null }
              isRowSelectedCb={ manager.supportsBulkAction() ? this.isRowSelected.bind(this) : null }
              isAllRowsSelectedCb={ manager.supportsBulkAction() ? this.isAllRowSelected.bind(this) : null }>

              { renderedColumns }

              <Basic.Column
                header={ this.i18n('entity.id.label') }
                property="id"
                rendered={ this._showId() }
                className="text-center"
                width={ 100 }
                cell={
                  ({rowIndex, data, property}) => {
                    return (
                      <UuidInfo value={ data[rowIndex][property] }/>
                    );
                  }
                }/>
            </Basic.BasicTable.Table>
            <Basic.BasicTable.Pagination
              ref="pagination"
              showPageSize={ showPageSize }
              paginationHandler={ pagination ? this._handlePagination.bind(this) : null }
              total={ pagination ? _total : _entities.length } {...range} />
          </div>
        }
        { this._renderBulkActionDetail() }
      </div>
    );
  }
}

AdvancedTable.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Table identifier - it's used as key in store
   */
  uiKey: PropTypes.string,
  /**
   * Table Header
   */
  header: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  /**
   * EntityManager subclass, which provides data fetching
   */
  manager: PropTypes.object.isRequired,
  /**
   * If pagination is shown
   */
  pagination: PropTypes.bool, // enable paginator action
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * "Default filters"
   */
  defaultSearchParameters: PropTypes.object,
  /**
   * Callback that is called when a row is clicked.
   */
  onRowClick: PropTypes.func,
  /**
   * Callback that is called when a row is double clicked.
   */
  onRowDoubleClick: PropTypes.func,
  /**
   * Callback that is called when a row is selected.
   */
  onRowSelect: PropTypes.func,
  /**
   * Enable row selection - checkbox in first cell
   */
  showRowSelection: PropTypes.oneOfType([PropTypes.bool, PropTypes.func]),
  /**
   * Shows column with id. Default is id shown in Development stage.
   */
  showId: PropTypes.bool,
  /**
   * selected row indexes as immutable set
   */
  selectedRows: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.number])),
  /**
   * css added to row
   */
  rowClass: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func
  ]),
  /**
   * Filter definition
   * @type {Filter}
   */
  filter: PropTypes.element,
  /**
   * Show filter
   */
  showFilter: PropTypes.bool,
  /**
   * If filter is opened by default
   */
  filterOpened: PropTypes.bool,
  /**
   * External filter open function. If false is returned, internal filterOpened is not set.
   */
  filterOpen: PropTypes.func,
  /**
   * If filter can be collapsed
   */
  filterCollapsible: PropTypes.bool,
  /**
   * When affixed, pixels to offset from top of viewport
   */
  filterViewportOffsetTop: PropTypes.number,
  /**
   * Bulk actions e.g. { value: 'activate', niceLabel: this.i18n('content.identities.action.activate.action'), action: this.onActivate.bind(this) }
   * This prop is ignored, if backend actions are used (see manager.supportsBulkAction())
   */
  actions: PropTypes.arrayOf(PropTypes.object),
  /**
   * Buttons are shown on the right of toogle filter button
   */
  buttons: PropTypes.arrayOf(PropTypes.element),
  /**
   * If table data is empty, then this text will be shown
   *
   * @type {string}
   */
  noData: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  /**
   * Shows page size
   */
  showPageSize: PropTypes.bool,
  /**
   * Shows toolbar.
   */
  showToolbar: PropTypes.bool,
  /**
   * Shows refresh button.
   */
  showRefreshButton: PropTypes.bool,
  /**
   * Css
   */
  className: PropTypes.string,
  //
  // Private properties, which are used internally for async data fetching
  //

  /**
   * loadinig indicator
   */
  _showLoading: PropTypes.bool,
  _entities: PropTypes.arrayOf(PropTypes.object),
  _total: PropTypes.number,
  /**
   * Persisted / used search parameters in redux
   */
  _searchParameters: PropTypes.object,
  _backendBulkActions: PropTypes.arrayOf(PropTypes.object),
};
AdvancedTable.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  _showLoading: true,
  _entities: [],
  _total: null,
  _searchParameters: null,
  _error: null,
  _backendBulkActions: null,
  pagination: true,
  showRowSelection: false,
  showFilter: true,
  showId: null,
  selectedRows: [],
  filterCollapsible: true,
  actions: [],
  buttons: [],
  showPageSize: true,
  showToolbar: true,
  showRefreshButton: true,
};

function select(state, component) {
  const uiKey = component.manager.resolveUiKey(component.uiKey);
  const ui = state.data.ui[uiKey];
  if (!ui) {
    return {};
  }
  return {
    _showLoading: ui.showLoading,
    _entities: component.manager.getEntities(state, uiKey),
    _total: ui.total,
    _searchParameters: ui.searchParameters,
    _error: ui.error,
    _backendBulkActions: component.manager.supportsBulkAction() ? DataManager.getData(state, component.manager.getUiKeyForBulkActions()) : null
  };
}

export default connect(select, null, null, { withRef: true})(AdvancedTable);
