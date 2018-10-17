import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { ContractSliceManager, IdentityManager, TreeNodeManager, TreeTypeManager, IdentityContractManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';
import ContractStateEnum from '../../../enums/ContractStateEnum';

const contractSliceManager = new ContractSliceManager();
const identityContractManager = new IdentityContractManager();

/**
 * Identity contract form
 *
 * @author Radek Tomiška
 */
class ContractSliceDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.identityManager = new IdentityManager();
    this.treeNodeManager = new TreeNodeManager();
    this.treeTypeManager = new TreeTypeManager();
    this.state = {
      _showLoading: false,
      treeTypeId: null,
      forceSearchParameters: new SearchParameters().setFilter('treeTypeId', SearchParameters.BLANK_UUID)
    };
  }

  getContentKey() {
    return 'content.contract-slice.detail';
  }

  getManager() {
    return contractSliceManager;
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
    const treeTypeId = entity._embedded && entity._embedded.workPosition ? entity._embedded.workPosition.treeType : null;
    const entityFormData = _.merge({}, entity, {
      treeTypeId
    });
    //
    this.setState({
      entityFormData,
      treeTypeId,
      forceSearchParameters: this.state.forceSearchParameters.setFilter('treeTypeId', treeTypeId || SearchParameters.BLANK_UUID)
    }, () => {
      // this.refs.form.setData(entityFormData);
      if (this.refs.treeTypeId) {
        this.refs.treeTypeId.focus();
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
        this.context.router.replace(`identity/${encodeURIComponent(identityId)}/contract-slice/${entity.id}/detail`);
      }
    });
  }

  onChangeTreeType(treeType) {
    const treeTypeId = treeType ? treeType.id : null;
    this.setState({
      treeTypeId,
      forceSearchParameters: this.state.forceSearchParameters.setFilter('treeTypeId', treeTypeId || SearchParameters.BLANK_UUID)
    }, () => {
      this.refs.workPosition.setValue(null);
      // focus automatically - maybe will be usefull?
      // this.refs.workPosition.focus();
    });
  }

  onChangeState(state) {
    this.refs.disabled.setValue(state ? ContractStateEnum.isContractDisabled(state.value) : null);
  }

  render() {
    const { uiKey, entity, showLoading, params, _permissions } = this.props;
    const { _showLoading, forceSearchParameters, treeTypeId, entityFormData } = this.state;
    const { identityId} = this.props.params;

    const parentForceSearchParameters = new SearchParameters()
      .setFilter('identity', identityId ? identityId : SearchParameters.BLANK_UUID);
    const canSave = contractSliceManager.canSave(entity, _permissions);

    return (
      <div>
        <Helmet title={Utils.Entity.isNew(entity) ? this.i18n('create.title') : this.i18n('edit.title')} />
        <Basic.Panel
          className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}
          style={Utils.Entity.isNew(entity) ? {paddingLeft: 15, paddingRight: 15 } : {}}>
          <form onSubmit={this.save.bind(this, 'CONTINUE')}>
              <Basic.AbstractForm
                ref="form"
                data={entityFormData}
                showLoading={ _showLoading || showLoading }
                uiKey={uiKey}
                readOnly={!canSave}>
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Basic.Panel className="no-border last">
                      <Basic.PanelHeader text={this.i18n('labelSlice')} style={{marginBottom: '10px'}}/>
                      <Basic.LabelWrapper readOnly ref="identity" label={this.i18n('entity.ContractSlice.identity')}>
                        <Advanced.IdentityInfo username={params.identityId}/>
                      </Basic.LabelWrapper>
                      <Basic.Checkbox
                        ref="usingAsContract"
                        label={this.i18n('entity.ContractSlice.usingAsContract.label')}
                        readOnly
                        helpBlock={this.i18n('entity.ContractSlice.usingAsContract.help')}/>
                      <Basic.SelectBox
                        ref="parentContract"
                        manager={identityContractManager}
                        forceSearchParameters={parentForceSearchParameters}
                        label={this.i18n('entity.ContractSlice.parentContract')}/>
                      <Basic.TextField
                        ref="contractCode"
                        label={this.i18n('entity.ContractSlice.contractCode')}/>
                      <Basic.DateTimePicker
                        mode="date"
                        ref="validFrom"
                        label={ this.i18n('entity.ContractSlice.validFrom') }/>
                      <Basic.DateTimePicker
                        mode="date"
                        readOnly
                        ref="validTill"
                        label={ this.i18n('entity.ContractSlice.validTill') }/>
                    </Basic.Panel>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Basic.Panel className="no-border last">
                      <Basic.PanelHeader text={this.i18n('labelContract')} style={{marginBottom: '10px'}}/>
                      <Basic.TextField
                        ref="position"
                        label={this.i18n('entity.ContractSlice.position')}/>
                      <Basic.SelectBox
                        ref="treeTypeId" useFirst={Utils.Entity.isNew(entity)}
                        manager={this.treeTypeManager}
                        label={this.i18n('entity.ContractSlice.treeType')}
                        onChange={this.onChangeTreeType.bind(this)}/>
                      <Basic.SelectBox
                        ref="workPosition"
                        manager={this.treeNodeManager}
                        label={this.i18n('entity.ContractSlice.workPosition')}
                        forceSearchParameters={forceSearchParameters}
                        hidden={treeTypeId === null}/>
                      <Basic.DateTimePicker
                        mode="date"
                        ref="contractValidFrom"
                        label={ this.i18n('entity.ContractSlice.contractValidFrom') }/>
                      <Basic.DateTimePicker
                        mode="date"
                        ref="contractValidTill"
                        label={ this.i18n('entity.ContractSlice.contractValidTill') }/>
                      <Basic.EnumSelectBox
                        ref="state"
                        enum={ ContractStateEnum }
                        useSymbol={ false }
                        label={ this.i18n('entity.ContractSlice.state.label') }
                        helpBlock={ this.i18n('entity.ContractSlice.state.help') }
                        onChange={ this.onChangeState.bind(this) }/>
                      <Basic.Checkbox
                        ref="main"
                        label={this.i18n('entity.ContractSlice.main.label')}
                        helpBlock={this.i18n('entity.ContractSlice.main.help')}/>
                      <Basic.Checkbox
                        ref="externe"
                        label={this.i18n('entity.ContractSlice.externe')}/>
                      <Basic.Checkbox
                        ref="disabled"
                        label={this.i18n('entity.ContractSlice.disabled.label')}
                        helpBlock={this.i18n('entity.ContractSlice.disabled.help')}
                        readOnly />
                      <Basic.TextArea
                        ref="description"
                        label={this.i18n('entity.ContractSlice.description')}
                        rows={4}
                        max={1000}/>
                    </Basic.Panel>
                  </Basic.Col>
                </Basic.Row>
              </Basic.AbstractForm>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" showLoading={_showLoading} onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
              <Basic.SplitButton
                level="success"
                title={ this.i18n('button.saveAndContinue') }
                onClick={ this.save.bind(this, 'CONTINUE') }
                showLoading={ _showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={canSave}
                pullRight
                dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'CLOSE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
          <input type="submit" className="hidden"/>
        </form>
        </Basic.Panel>
      </div>
    );
  }
}

ContractSliceDetail.propTypes = {
  entity: PropTypes.object,
  type: PropTypes.string,
  uiKey: PropTypes.string.isRequired,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
ContractSliceDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  return {
    userContext: state.security.userContext,
    _permissions: contractSliceManager.getPermissions(state, null, component.params.entityId)
  };
}
export default connect(select)(ContractSliceDetail);
