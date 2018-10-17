import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { BackendModuleManager, DataManager } from '../../redux';
import * as Utils from '../../utils';
import SearchParameters from '../../domain/SearchParameters';
import Immutable from 'immutable';

/**
 * Modal window for result codes
 *
 * @author Roman Kučera
 * @author Radek Tomiška
*/
class ResultCodesModal extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      detail: this.props.detail,
      filterOpened: true
    };
    this.backendModuleManager = new BackendModuleManager();
  }

  getContentKey() {
    return 'content.system.be-modules.result-codes';
  }

  componentDidMount() {
    super.componentDidMount();
  }

  _onEnter() {
    this.refs.text.focus();
    //
    return true;
  }

  _closeModal(event) {
    if (event) {
      event.preventDefault();
    }
    const { detail } = this.state;
    detail.show = false;
    this.setState({
      ...detail
    });
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.fetchResultCodes(SearchParameters.getSearchParameters(SearchParameters.getFilterData(this.refs.filterForm), this.props._searchParameters));
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.fetchResultCodes(null);
    this.refs.filterForm.setData({});
  }

  fetchResultCodes(searchParameters) {
    const { detail } = this.props;
    //
    this.context.store.dispatch(this.backendModuleManager.fetchResultCodes(detail.entity.id, searchParameters));
  }

  sortByStatusEnum(one, two) {
    if (one < two) {
      return -1;
    } else if (one > two) {
      return 1;
    }
    return 0;
  }

  render() {
    const { resultCodes, _searchParameters, showLoading } = this.props;
    const { detail, filterOpened } = this.state;
    if (!detail || !detail.entity) {
      return null;
    }
    //
    let _statusEnum = new Immutable.OrderedSet();
    const _resultCodes = [];
    if (resultCodes) {
      resultCodes.forEach(resultCode => {
        _resultCodes.push(resultCode);
        _statusEnum = _statusEnum.add(resultCode.statusEnum);
      });
    }
    _resultCodes.sort((one, two) => {
      // Sort by status code number
      if (parseInt(one.statusCode, 10) > parseInt(two.statusCode, 10)) {
        return 1;
      } else if (parseInt(one.statusCode, 10) < parseInt(two.statusCode, 10)) {
        return -1;
      }
      // Then sort by code
      return this.sortByStatusEnum(one.statusEnum, two.statusEnum);
    });

    _statusEnum = _statusEnum.sort((one, two) => {
      return this.sortByStatusEnum(one, two);
    });

    return (
      <Basic.Modal
        bsSize="large"
        show={ detail.show }
        onHide={ this._closeModal.bind(this) }
        onEnter={ this._onEnter.bind(this) }>
        <Basic.Modal.Header
          text={ this.i18n('title', { moduleName: detail.entity.name }) }
          closeButton/>

        <Basic.Toolbar>
          <div>
            <div className="pull-right">
              <Advanced.Filter.ToogleButton
                filterOpen={(open) => this.setState({ filterOpened: open })}
                filterOpened={filterOpened}
                style={{ marginLeft: 3 }}
                searchParameters={_searchParameters} />
              <Advanced.RefreshButton
                onClick={this.fetchResultCodes.bind(this, _searchParameters)}
                title={this.i18n('button.refresh')}
                showLoading={showLoading} />
            </div>
            <div className="clearfix"></div>
          </div>
          <Basic.Collapse in={filterOpened}>
            <div>
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <Basic.Col lg={ 6 }>
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.text.placeholder')}
                        options={_statusEnum.toArray().map(value => { return {value, niceLabel: value}; })}
                        searchable />
                    </Basic.Col>
                    <Basic.Col lg={ 6 } className="text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)} />
                    </Basic.Col>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            </div>
          </Basic.Collapse>
        </Basic.Toolbar>

        <Basic.Table
          ref="table"
          data={_resultCodes}
          noData={this.i18n('component.basic.Table.noData')}
          hover={ false }
          condensed
          rowClass={({ rowIndex, data }) => { return Utils.Ui.getRowClass(data[rowIndex]); }}
          showLoading={ showLoading }>
          <Basic.Column
            property="statusCode"
            header={this.i18n('status')} />
          <Basic.Column
            property="message"
            header={this.i18n('message')}
            cell={
              ({ rowIndex, data }) => {
                const resultModel = data[rowIndex];
                // TODO: get paramter name from localization service
                /* resultModel.parameters = {
                  system: 'system'
                };*/
                return (
                  <Advanced.OperationResult
                    key={ `flash-message-${resultModel.id}` }
                    header=""
                    value={{ model: resultModel, code: resultModel.statusEnum }}
                    face="full" />
                );
              }
            } />
        </Basic.Table>

        <Basic.Modal.Footer>
          <Basic.Button level="link" onClick={this._closeModal.bind(this)}>{this.i18n('button.cancel')}</Basic.Button>
        </Basic.Modal.Footer>
      </Basic.Modal>
    );
  }
}

ResultCodesModal.propTypes = {
  resultCodes: PropTypes.object,
  showLoading: PropTypes.bool,
  userContext: PropTypes.object
};

ResultCodesModal.defaultProps = {
  resultCodes: null,
  showLoading: true,
  userContext: null
};

function select(state) {
  return {
    resultCodes: DataManager.getData(state, BackendModuleManager.UI_KEY_RESULT_CODES),
    _searchParameters: Utils.Ui.getSearchParameters(state, BackendModuleManager.UI_KEY_RESULT_CODES),
    showLoading: Utils.Ui.isShowLoading(state, BackendModuleManager.UI_KEY_RESULT_CODES),
    userContext: state.security.userContext
  };
}

export default connect(select)(ResultCodesModal);
