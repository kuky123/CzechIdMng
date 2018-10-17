import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { SecurityManager } from '../../redux';

/**
* Table of forms definitions (attributes is show in detail)
*
* @author Ondřej Kopr
* @author Radek Tomiška
*/
export class FormDefinitionTable extends Advanced.AbstractTableContent {

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
    this.refs.text.focus();
  }

  getContentKey() {
    return 'content.formDefinitions';
  }

  getManager() {
    return this.props.definitionManager;
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
      this.context.router.push(`/forms/${uuidId}/detail?new=1`);
    } else {
      this.context.router.push('/forms/' + entity.id + '/detail');
    }
  }

  render() {
    const { uiKey, definitionManager } = this.props;
    const { filterOpened } = this.state;
    return (
      <Basic.Row>
        <div className="col-lg-12">
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={definitionManager}
            showRowSelection={SecurityManager.hasAuthority('FORMDEFINITION_DELETE')}
            rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
            actions={
              [
                { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
              ]
            }
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row className="last">
                    <Basic.Col lg={ 6 }>
                      <Advanced.Filter.TextField
                        ref="text"
                        placeholder={this.i18n('filter.text')}/>
                    </Basic.Col>
                    <Basic.Col lg={ 6 } className="text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </Basic.Col>
                  </Basic.Row>
                </Basic.AbstractForm>
              </Advanced.Filter>
            }
            buttons={
              [
                <Basic.Button
                  level="success"
                  key="add_button"
                  className="btn-xs"
                  onClick={this.showDetail.bind(this, { })}
                  rendered={SecurityManager.hasAuthority('FORMDEFINITION_CREATE')}
                  icon="fa:plus">
                  { this.i18n('button.add') }
                </Basic.Button>
              ]
            }
            filterOpened={ filterOpened }
            _searchParameters={ this.getSearchParameters() }>
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
            <Advanced.Column property="type" sort
              face="text" width="75px"
              cell={
                ({ rowIndex, data, property }) => {
                  return Utils.Ui.getSimpleJavaType(data[rowIndex][property]);
                }}/>
            <Advanced.Column property="main" header={this.i18n('entity.FormDefinition.main.label')} face="bool" sort />
            <Advanced.Column property="code" sort/>
            <Advanced.Column property="name" sort/>
            <Advanced.Column property="unmodifiable" header={this.i18n('entity.FormDefinition.unmodifiable.label')} face="bool" sort />
          </Advanced.Table>
        </div>
      </Basic.Row>
      );
  }
}

FormDefinitionTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  definitionManager: PropTypes.object.isRequired,
  filterOpened: PropTypes.bool
};

FormDefinitionTable.defaultProps = {
  filterOpened: true
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select, null, null, { withRef: true })(FormDefinitionTable);
