import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import uuid from 'uuid';
//
import { SecurityManager } from '../../redux';
import PasswordPolicyTypeEnum from '../../enums/PasswordPolicyTypeEnum';
import PasswordPolicyGenerateTypeEnum from '../../enums/PasswordPolicyGenerateTypeEnum';
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';

/**
 * Table with definitions of password policies
 *
 * @author Ondřej Kopr
 */
export class PasswordPolicyTable extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      filterOpened: true
    };
  }

  getContentKey() {
    return 'content.passwordPolicies';
  }

  componentDidMount() {
    this.selectNavigationItems(['system', 'password-policies']);
    const { passwordPolicyManager, uiKey } = this.props;
    const searchParameters = passwordPolicyManager.getService().getDefaultSearchParameters();
    this.context.store.dispatch(passwordPolicyManager.fetchEntities(searchParameters, uiKey));
  }

  componentWillUnmount() {
    this.cancelFilter();
  }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }

    this.refs.table.getWrappedInstance().useFilterData(this.refs.filterForm.getData());
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
      <Basic.Row>
        <div className="col-lg-12">
          <Basic.Confirm ref="confirm-delete" level="danger"/>
          <Advanced.Table
            ref="table"
            uiKey={uiKey}
            manager={passwordPolicyManager}
            showRowSelection={SecurityManager.hasAuthority('PASSWORDPOLICY_DELETE')}
            rowClass={({rowIndex, data}) => { return data[rowIndex].disabled ? 'disabled' : ''; }}
            filter={
              <Advanced.Filter onSubmit={this.useFilter.bind(this)}>
                <Basic.AbstractForm ref="filterForm">
                  <Basic.Row>
                    <div className="col-lg-6">
                      <Advanced.Filter.TextField
                        ref="name"
                        placeholder={this.i18n('entity.PasswordPolicy.name')}/>
                    </div>
                    <div className="col-lg-6 text-right">
                      <Advanced.Filter.FilterButtons cancelFilter={this.cancelFilter.bind(this)}/>
                    </div>
                  </Basic.Row>
                  <Basic.Row>
                    <div className="col-lg-6">
                      <Basic.EnumSelectBox
                        ref="category"
                        placeholder={this.i18n('entity.PasswordPolicy.type')}
                        enum={PasswordPolicyTypeEnum}/>
                    </div>
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
            }>
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
      </Basic.Row>
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

export default connect()(PasswordPolicyTable);
