import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { IdentityContractManager, IdentityManager } from '../../../redux';
import ContractStateEnum from '../../../enums/ContractStateEnum';

const identityContractManager = new IdentityContractManager();

/**
 * Identity contract form
 *
 * @author Radek Tomiška
 */
class IdentityContractDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
    this.state = {
      _showLoading: false
    };
  }

  getContentKey() {
    return 'content.identity-contract.detail';
  }

  getManager() {
    return identityContractManager;
  }

  componentDidMount() {
    const { entity } = this.props;
    if (entity !== undefined) {
      this._setSelectedEntity(entity);
    }
  }

  /**
   * TODO: prevent set state in did mount
   */
  _setSelectedEntity(entity) {
    const treeType = entity._embedded && entity._embedded.workPosition && entity._embedded.workPosition._embedded ? entity._embedded.workPosition._embedded.treeType : null;
    const entityFormData = _.merge({}, entity);
    //
    this.setState({
      entityFormData
    }, () => {
      if (this.refs.workPosition) {
        if (treeType) {
          this.refs.workPosition.setTreeType(treeType);
          this.refs.workPosition.focus();
        }
      }
    });
  }

  save(afterAction, event) {
    if (event) {
      event.preventDefault();
    }
    //
    const { uiKey } = this.props;
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      _showLoading: true
    }, () => {
      const entity = this.refs.form.getData();
      const { identityId } = this.props.params;
      //
      const state = this.context.store.getState();
      const identity = this.identityManager.getEntity(state, identityId);
      entity.identity = identity.id;
      //
      if (entity.id === undefined) {
        this.context.store.dispatch(this.getManager().createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
          if (!error) {
            this.addMessage({ message: this.i18n('create.success', { position: this.getManager().getNiceLabel(createdEntity), username: this.identityManager.getNiceLabel(identity) }) });
          }
          this._afterSave(createdEntity, error, afterAction);
        }));
      } else {
        this.context.store.dispatch(this.getManager().updateEntity(entity, `${uiKey}-detail`, (patchedEntity, error) => {
          if (!error) {
            this.addMessage({ message: this.i18n('edit.success', { position: this.getManager().getNiceLabel(patchedEntity), username: this.identityManager.getNiceLabel(identity) }) });
          }
          this._afterSave(patchedEntity, error, afterAction);
        }));
      }
    });
  }

  _afterSave(entity, error, afterAction = 'CLOSE') {
    this.setState({
      _showLoading: false
    }, () => {
      if (error) {
        this.addError(error);
        return;
      }
      //
      if (afterAction === 'CLOSE') {
        // go back to tree types or organizations
        this.context.router.goBack();
      } else {
        const { identityId } = this.props.params;
        this.context.router.replace(`identity/${encodeURIComponent(identityId)}/identity-contract/${entity.id}/detail`);
      }
    });
  }

  onChangeState(state) {
    this.refs.disabled.setValue(state ? ContractStateEnum.isContractDisabled(state.value) : null);
  }

  render() {
    const { uiKey, entity, showLoading, params, _permissions } = this.props;
    const { _showLoading, entityFormData } = this.state;

    return (
      <div>
        <Helmet title={Utils.Entity.isNew(entity) ? this.i18n('create.title') : this.i18n('edit.title')} />

        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('label')} />

            <Basic.PanelBody style={Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
              <Basic.AbstractForm
                ref="form"
                data={entityFormData}
                showLoading={ _showLoading || showLoading }
                uiKey={uiKey}
                readOnly={ !identityContractManager.canSave(entity, _permissions) }>
                <Basic.LabelWrapper readOnly ref="identity" label={this.i18n('entity.IdentityContract.identity')}>
                  <Advanced.IdentityInfo username={params.identityId}/>
                </Basic.LabelWrapper>

                <Basic.TextField
                  ref="position"
                  label={ this.i18n('entity.IdentityContract.position') }
                  max={ 255 }/>

                <Advanced.TreeNodeSelect
                  ref="workPosition"
                  header={ this.i18n('entity.IdentityContract.workPosition') }
                  treeNodeLabel={ this.i18n('entity.IdentityContract.workPosition') }
                  useFirstType />

                <Basic.DateTimePicker
                  mode="date"
                  ref="validFrom"
                  label={ this.i18n('label.validFrom') }/>

                <Basic.DateTimePicker
                  mode="date"
                  ref="validTill"
                  label={ this.i18n('label.validTill') }/>

                <Basic.Checkbox
                  ref="main"
                  label={this.i18n('entity.IdentityContract.main.label')}
                  helpBlock={this.i18n('entity.IdentityContract.main.help')}/>

                <Basic.Checkbox
                  ref="externe"
                  label={this.i18n('entity.IdentityContract.externe')}/>

                <Basic.EnumSelectBox
                  ref="state"
                  enum={ ContractStateEnum }
                  useSymbol={ false }
                  label={ this.i18n('entity.IdentityContract.state.label') }
                  helpBlock={ this.i18n('entity.IdentityContract.state.help') }
                  onChange={ this.onChangeState.bind(this) }/>

                <Basic.Checkbox
                  ref="disabled"
                  label={this.i18n('entity.IdentityContract.disabled.label')}
                  helpBlock={this.i18n('entity.IdentityContract.disabled.help')}
                  readOnly />

                <Basic.TextArea
                  ref="description"
                  label={this.i18n('entity.IdentityContract.description')}
                  rows={4}
                  max={1000}/>
              </Basic.AbstractForm>
            </Basic.PanelBody>

            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" showLoading={_showLoading} onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>

              <Basic.SplitButton
                level="success"
                title={ this.i18n('button.saveAndContinue') }
                onClick={ this.save.bind(this, 'CONTINUE') }
                showLoading={ _showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ identityContractManager.canSave(entity, _permissions) }
                pullRight
                dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'CLOSE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          </Basic.Panel>
          {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
          <input type="submit" className="hidden"/>
        </form>
      </div>
    );
  }
}

IdentityContractDetail.propTypes = {
  entity: PropTypes.object,
  type: PropTypes.string,
  uiKey: PropTypes.string.isRequired,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
IdentityContractDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  return {
    userContext: state.security.userContext,
    _permissions: identityContractManager.getPermissions(state, null, component.params.entityId)
  };
}
export default connect(select)(IdentityContractDetail);
