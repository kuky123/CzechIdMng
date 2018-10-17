import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import _ from 'lodash';
import moment from 'moment';
//
import { Basic, Advanced, Utils, Managers, Services, Domain, Enums } from 'czechidm-core';
import { ReportManager } from '../../redux';

const manager = new ReportManager();
const longRunningTaskManager = new Managers.LongRunningTaskManager();

/**
* Table of reports
*
* @author Radek Tomiška
*
*/
export class ReportTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      filterOpened: this.props.filterOpened,
      report: null,
      longRunningTask: null
    };
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'rpt:content.reports';
  }

  /**
   * Base manager for this agenda (used in `AbstractTableContent`)
   */
  getManager() {
    return manager;
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this.context.store.dispatch(manager.fetchSupportedReports( (data, error) => {
      if (error) {
        this.addError(error);
      } else {
        // open modal automatically
        if (this.props.location) {
          const { query } = this.props.location;
          if (query && query.id) {
            this.loadDetail({
              id: query.id
            });
          }
        }
      }
    }));
  }

  /**
   * Submit filter action
   */
  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  /**
   * Cancel filter action
   */
  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  /**
   * Open modal with report's select
   */
  loadDetail(entity) {
    // reload Entity
    if (!Utils.Entity.isNew(entity) && (!entity.result
      || Enums.OperationStateEnum.findSymbolByKey(entity.result.state) === Enums.OperationStateEnum.CREATED
      || Enums.OperationStateEnum.findSymbolByKey(entity.result.state) === Enums.OperationStateEnum.RUNNING)) {
      this.setState({
        detail: {
          show: true,
          entity
        }
      }, () => {
        this.context.store.dispatch(manager.fetchEntity(entity.id, `${this.getUiKey()}-detail`, (loadedEntity, error) => {
          if (error) {
            this.addError(error);
          } else {
            this.showDetail(loadedEntity);
          }
        }));
      });
    } else {
      this.showDetail(entity);
    }
  }

  showDetail(entity) {
    const { supportedReports } = this.props;
    let report = null;
    if (entity.executorName && supportedReports && supportedReports.has(entity.executorName)) {
      report = supportedReports.get(entity.executorName);
    }
    //
    this.setState({
      report,
      longRunningTask: entity._embedded && entity._embedded.longRunningTask ? entity._embedded.longRunningTask : null
    }, () => {
      super.showDetail(entity, () => {
        if (this.refs.executorName) {
          this.refs.executorName.focus();
        }
      });
    });
  }

  /**
   * Close modal task detail
   */
  closeDetail() {
    this.setState({
      detail: {
        show: false,
        entity: {}
      },
      report: null,
      longRunningTask: null
    });
  }

  onChangeReport(report) {
    this.setState({
      report
    });
  }

  /**
   * Saves give entity
   */
  createReport(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid() || (this.refs.reportFilter && !this.refs.reportFilter.isValid())) {
      return;
    }
    const entity = this.refs.form.getData();
    //
    if (this.refs.reportFilter) {
      entity.filter = {
        formDefinition: this.refs.reportFilter.getFormDefinition().id,
        values: this.refs.reportFilter.getValues()
      };
    }
    //
    this.context.store.dispatch(this.getManager().createEntity(entity, `${this.getUiKey()}-detail`, this.afterSave.bind(this)));
  }

  /**
   * Callback after save
   */
  afterSave(entity, error) {
    if (error) {
      this.addError(error);
      this.refs.form.processEnded();
      return;
    }
    this.addMessage({ level: 'info', message: this.i18n('action.report-create.success', { record: this.getManager().getNiceLabel(entity) }) });
    this.refs.table.getWrappedInstance().reload();
    this.closeDetail();
  }

  renderDownloadButtons(entity, className = '') {
    const { supportedReports } = this.props;
    //
    if (!supportedReports || !supportedReports.has(entity.executorName)) {
      return null;
    }
    // successfully executed reports only
    if (Enums.OperationStateEnum.findSymbolByKey(entity.result.state) !== Enums.OperationStateEnum.EXECUTED) {
      return null;
    }
    //
    const enabledRenderers = supportedReports.get(entity.executorName).renderers
      .filter(renderer => {
        return renderer.disabled !== true;
      })
      .sort((one, two) => {
        // sort by description - description will be in button label
        return one.description > two.description;
      });
    //
    // no enabled renderers found
    if (enabledRenderers.length === 0) {
      return (
        <Basic.Label
          level="default"
          text={ this.i18n('label.renderer.notFound.label') }
          title={ this.i18n('label.renderer.notFound.title') }
          className="disabled"/>
      );
    }
    return (
      <span>
        {
          enabledRenderers.map(renderer => {
            return (
              <a
                key={ `rep-${renderer.id}-${entity.id}` }
                href={ this.getManager().getService().getDownloadUrl(entity.id, renderer.name) }
                title={ this.i18n('action.download.title', { report: entity.name, renderer: renderer.description } )}
                className={ `btn btn-primary ${className}` }
                style={{ color: 'white', marginLeft: 3 }}>
                <Basic.Icon value="fa:download" />
                {' '}
                { renderer.description }
              </a>
            );
          })
        }
      </span>
    );
  }

  render() {
    const {
      uiKey,
      columns,
      forceSearchParameters,
      showRowSelection,
      showAddButton,
      supportedReports,
      showLoading,
      showLoadingDetail
    } = this.props;
    const {
      filterOpened,
      detail,
      report,
      longRunningTask
    } = this.state;

    const _supportedReports = [];
    if (supportedReports) {
      supportedReports.forEach(executor => {
        _supportedReports.push({
          niceLabel: `${executor.description} (${executor.name})`,
          value: executor.id,
          description: executor.description,
          formDefinition: executor.formDefinition,
          disabled: executor.disabled
        });
      });
    }
    _supportedReports.sort((one, two) => {
      return one.niceLabel > two.niceLabel;
    });

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>

        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          manager={ manager }
          rowClass={ ({rowIndex, data}) => { return Utils.Ui.getRowClass(data[rowIndex]); } }
          filterOpened={filterOpened}
          forceSearchParameters={ forceSearchParameters }
          showRowSelection={ Managers.SecurityManager.hasAuthority('REPORT_DELETE') && showRowSelection }
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 8 }>
                    <Advanced.Filter.FilterDate ref="fromTill"/>
                  </Basic.Col>
                  <Basic.Col lg={ 4 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row className="last">
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={this.i18n('filter.text.placeholder')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false },
            ]
          }
          buttons={
            [
              <Basic.Button
                level="success"
                key="add_button"
                className="btn-xs"
                onClick={ this.showDetail.bind(this, { }) }
                rendered={ Managers.SecurityManager.hasAuthority('REPORT_CREATE') && showAddButton}
                disabled={ !_supportedReports || !_supportedReports.length }
                showLoading={ showLoading }>
                <Basic.Icon type="fa" icon="plus"/>
                { ' ' }
                { this.i18n('button.add') }
              </Basic.Button>
            ]
          }
          _searchParameters={ this.getSearchParameters() }
          >
          <Advanced.Column
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this.loadDetail.bind(this, data[rowIndex])}/>
                );
              }
            }/>
          <Advanced.Column
            width={75}
            header={this.i18n('entity.LongRunningTask.result.state')}
            sort
            sortProperty="longRunningTask.result.state"
            rendered={ _.includes(columns, 'state') }
            cell={
              ({ data, rowIndex }) => {
                const entity = data[rowIndex];
                if (!entity.result || !entity.result.state) {
                  return null;
                }
                const lrt = entity._embedded && entity._embedded.longRunningTask ? entity._embedded.longRunningTask : null;
                const label = !lrt || Enums.OperationStateEnum.findSymbolByKey(entity.result.state) !== Enums.OperationStateEnum.RUNNING ? null : longRunningTaskManager.getProcessedCount(lrt);
                //
                return (
                  <Advanced.OperationResult value={ entity.result } stateLabel={ label } detailLink={ () => this.loadDetail(data[rowIndex]) }/>
                );
              }
            }/>
          <Advanced.Column property="name" sort face="text" rendered={ _.includes(columns, 'name') }/>
          <Advanced.Column
            property="executorName"
            width="20%"
            sort
            rendered={_.includes(columns, 'executorName')}
            cell={
              ({ data, rowIndex, property }) => {
                const entity = data[rowIndex];
                if (!supportedReports || !supportedReports.has(entity.executorName)) {
                  return (
                    <span>{ entity[property] }</span>
                  );
                }
                //
                return (
                  <div>
                    <div>{ supportedReports.get(entity.executorName).description }</div>
                    <div><small>{ `(${entity[property]})` }</small></div>
                  </div>
                );
              }
            }
            />
          <Advanced.Column property="created" sort face="datetime" rendered={ _.includes(columns, 'created') }/>
          <Advanced.Column
            property="creator"
            rendered={_.includes(columns, 'creator')}
            sort
            cell={
              ({ data, rowIndex, property }) => {
                return (
                  <Advanced.EntityInfo
                    entityType="identity"
                    entityIdentifier={ data[rowIndex][property] }
                    face="popover" />
                );
              }
            }
            />
          <Advanced.Column
            header={ this.i18n('label.download') }
            cell={
              ({ data, rowIndex }) => {
                return this.renderDownloadButtons(data[rowIndex], 'btn-xs');
              }
            }/>
        </Advanced.Table>

        <Basic.Modal
          show={ detail.show }
          onHide={ this.closeDetail.bind(this) }
          backdrop="static"
          keyboard={ !showLoadingDetail }>

          <form onSubmit={this.createReport.bind(this)}>
            <Basic.Modal.Header closeButton={ !showLoadingDetail } text={ Utils.Entity.isNew(detail.entity) ? this.i18n('action.report-create.header') : this.i18n('action.report-detail.header') }/>
            <Basic.Modal.Body>
              <Basic.Loading isStatic show={ showLoadingDetail }/>
              <Basic.AbstractForm ref="form" readOnly={ !Utils.Entity.isNew(detail.entity) } className={ showLoadingDetail ? 'hidden' : '' }>
                <Basic.EnumSelectBox
                  ref="executorName"
                  label={ this.i18n('rpt:entity.Report._type') }
                  options={ _supportedReports }
                  onChange={ this.onChangeReport.bind(this) }
                  required
                  searchable/>
                {
                  !report || !report.formDefinition
                  ||
                  <Advanced.EavForm
                    ref="reportFilter"
                    formInstance={ new Domain.FormInstance(report.formDefinition, detail.entity && detail.entity.filter ? detail.entity.filter.values : null ) }
                    readOnly={ !Utils.Entity.isNew(detail.entity) }
                    useDefaultValue/>
                }
                {
                  !longRunningTask
                  ||
                  <div>
                    <Basic.Row>
                      <Basic.Col lg={ 6 }>
                        <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.counter')}>
                          { longRunningTaskManager.getProcessedCount(longRunningTask) }
                        </Basic.LabelWrapper>
                      </Basic.Col>
                      <Basic.Col lg={ 6 }>
                        {
                          !longRunningTask.taskStarted
                          ||
                          <Basic.LabelWrapper label={this.i18n('entity.LongRunningTask.duration')}>
                            <Basic.Tooltip
                              ref="popover"
                              placement="bottom"
                              value={ moment.utc(moment.duration(moment(longRunningTask.modified).diff(moment(longRunningTask.taskStarted))).asMilliseconds()).format(this.i18n('format.times'))}>
                              <span>
                                { moment.duration(moment(longRunningTask.taskStarted).diff(moment(longRunningTask.modified))).locale(Services.LocalizationService.getCurrentLanguage()).humanize() }
                              </span>
                            </Basic.Tooltip>
                          </Basic.LabelWrapper>
                        }
                      </Basic.Col>
                    </Basic.Row>

                    <Advanced.OperationResult value={ longRunningTask.result } face="full"/>
                  </div>
                }
              </Basic.AbstractForm>
            </Basic.Modal.Body>

            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this.closeDetail.bind(this)}
                showLoading={ showLoadingDetail }>
                {this.i18n('button.close')}
              </Basic.Button>

              {
                Utils.Entity.isNew(detail.entity)
                ?
                <Basic.Button
                  type="submit"
                  level="success"
                  showLoading={ showLoadingDetail }
                  showLoadingIcon
                  showLoadingText={ this.i18n('button.saving') }
                  rendered={ detail.entity.id === undefined && Managers.SecurityManager.hasAnyAuthority(['REPORT_CREATE']) }>
                  {this.i18n('button.generate.label')}
                </Basic.Button>
                :
                this.renderDownloadButtons(detail.entity)
              }
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

ReportTable.propTypes = {
  /**
   * Entities, permissions etc. fro this content are stored in redux under given key
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Rendered columns (all by default)
   */
  columns: PropTypes.arrayOf(PropTypes.string),
  /**
   * Show filter or collapse
   */
  filterOpened: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
   * Show add button to create new product
   */
  showAddButton: PropTypes.bool,
  /**
   * Show row selection for bulk actions
   */
  showRowSelection: PropTypes.bool,
  /**
   * Supported reports - immutable map
   */
  supportedTasks: PropTypes.object
};

ReportTable.defaultProps = {
  columns: ['state', 'executorName', 'created', 'creator'],
  filterOpened: false,
  forceSearchParameters: new Domain.SearchParameters(),
  showAddButton: true,
  showRowSelection: true,
  supportedReports: null,
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey), // persisted filter state in redux
    supportedReports: Managers.DataManager.getData(state, ReportManager.UI_KEY_SUPPORTED_REPORTS),
    showLoading: Utils.Ui.isShowLoading(state, ReportManager.UI_KEY_SUPPORTED_REPORTS)
                  || Utils.Ui.isShowLoading(state, component.uiKey),
    showLoadingDetail: Utils.Ui.isShowLoading(state, `${component.uiKey}-detail`)
  };
}

export default connect(select, null, null, { withRef: true })(ReportTable);
