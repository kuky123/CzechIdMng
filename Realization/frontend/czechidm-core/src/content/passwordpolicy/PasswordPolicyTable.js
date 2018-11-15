import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import { SecurityManager } from '../../redux';
import PasswordPolicyTypeEnum from '../../enums/PasswordPolicyTypeEnum';
import PasswordPolicyGenerateTypeEnum from '../../enums/PasswordPolicyGenerateTypeEnum';

/**
 * Table with definitions of password policies
 *
 * @author Ondřej Kopr
 * @author Radek Tomiška
 */
export class PasswordPolicyTable extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: true
    };
  }

  getContentKey() {
    return 'content.passwordPolicies';
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

  onDelete(bulkActionValue, selectedRows) {
    const { uiKey, passwordPolicyManager } = this.props;
    const selectedEntities = passwordPolicyManager.getEntitiesByIds(this.context.store.getState(), selectedRows);
    //
    // show confirm message for deleting entity or entities
    this.refs['confirm-' + bulkActionValue].show(
      this.i18n(`action.${bulkActionValue}.message`, { count: selectedEntities.length, record: passwordPolicyManager.getNiceLabel(selectedEntities[0]), records: passwordPolicyManager.getNiceLabels(selectedEntities).join(', ') }),
      this.i18n(`action.${bulkActionValue}.header`, { count: selectedEntities.length, records: passwordPolicyManager.getNiceLabels(selectedEntities).join(', ') })
    ).then(() => {
      // try delete
      this.context.store.dispatch(passwordPolicyManager.deleteEntities(selectedEntities, uiKey, (entity, error, successEntities) => {
        if (entity && error) {
          this.addErrorMessage({ title: this.i18n(`action.delete.error`, { record: passwordPolicyManager.getNiceLabel(entity) }) }, error);
        }
        if (!error && successEntities) {
          // refresh data in table
          this.refs.table.getWrappedInstance().reload();
        }
      }));
    }, () => {
      //
    });
  }

  /**
   * Recive new form for create new type else show detail for existing org.
   */
  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/password-policies/${uuidId}?new=1`);
    } else {
      this.context.router.push('/password-policies/' + entity.id);
    }
  }

  render() {
    const { uiKey, passwordPolicyManager } = this.props;
    const { filterOpened } = this.state;

    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Advanced.Table
          ref="table"
          uiKey={ uiKey }
          manager={passwordPolicyManager}
          showRowSelection={SecurityManager.hasAuthority('PASSWORDPOLICY_DELETE')}
          rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
          filter={
            <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
              <Basic.AbstractForm ref="filterForm">
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Advanced.Filter.TextField
                      ref="text"
                      placeholder={ this.i18n('entity.PasswordPolicy.name.label') }/>
                  </Basic.Col>
                  <Basic.Col lg={ 6 } className="text-right">
                    <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 6 } className="last">
                    <Advanced.Filter.EnumSelectBox
                      ref="type"
                      placeholder={this.i18n('entity.PasswordPolicy.type.label')}
                      enum={ PasswordPolicyTypeEnum }/>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            </Advanced.Filter>
          }
          filterOpened={!filterOpened}
          actions={
            [
              { value: 'delete', niceLabel: this.i18n('action.delete.action'), action: this.onDelete.bind(this), disabled: false }
            ]
          }
          buttons={
            [
              <Basic.Button level="success" key="add_button" className="btn-xs"
                      onClick={this.showDetail.bind(this, {})}
                      rendered={SecurityManager.hasAuthority('PASSWORDPOLICY_CREATE')}>
                <Basic.Icon type="fa" icon="plus"/>
                {' '}
                {this.i18n('button.add')}
              </Basic.Button>
            ]
          }
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
            face="enum" width="75px"
            enumClass={PasswordPolicyTypeEnum}/>
          <Advanced.Column property="name" sort
            cell={
              ({ rowIndex, data }) => {
                let name = data[rowIndex].name;
                if (data[rowIndex].defaultPolicy) {
                  name = (
                    <span>
                      { name }
                      <small> ({ this.i18n('entity.PasswordPolicy.defaultPolicy.label') })</small>
                    </span>
                  );
                }
                return name;
              }
            }/>
          <Advanced.Column
            property="generateType"
            sort
            face="enum"
            width={ 75 }
            enumClass={ PasswordPolicyGenerateTypeEnum }/>
          <Advanced.Column property="enchancedControl" face="bool" sort />
          <Advanced.Column property="minPasswordLength" sort />
          <Advanced.Column property="maxPasswordLength" sort />
        </Advanced.Table>
      </div>
    );
  }
}

PasswordPolicyTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  passwordPolicyManager: PropTypes.object.isRequired
};

PasswordPolicyTable.defaultProps = {
  _showLoading: false
};

function select(state, component) {
  return {
    _searchParameters: Utils.Ui.getSearchParameters(state, component.uiKey)
  };
}

export default connect(select)(PasswordPolicyTable);
