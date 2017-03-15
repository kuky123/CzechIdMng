import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import _ from 'lodash';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { IdentityContractManager, IdentityManager, TreeNodeManager, TreeTypeManager, SecurityManager } from '../../../redux';
import SearchParameters from '../../../domain/SearchParameters';

/**
 * Identity contract form
 *
 * @author Radek Tomiška
 */
export default class IdentityContractDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.identityContractManager = new IdentityContractManager();
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
    return 'content.identity-contract.detail';
  }

  getManager() {
    return this.identityContractManager;
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
    const treeTypeId = entity._embedded && entity._embedded.workingPosition ? entity._embedded.workingPosition.treeType.id : null;
    const entityFormData = _.merge({}, entity, {
      guarantee: entity._embedded ? entity._embedded.guarantee : null,
      workingPosition: entity._embedded ? entity._embedded.workingPosition : null,
      treeTypeId
    });
    //
    this.setState({
      treeTypeId,
      forceSearchParameters: this.state.forceSearchParameters.setFilter('treeTypeId', treeTypeId || SearchParameters.BLANK_UUID)
    }, () => {
      this.refs.form.setData(entityFormData);
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
      entity.identity = this.identityManager.getSelfLink(identityId);
      entity.guarantee = this.identityManager.getSelfLink(entity.guarantee);
      entity.workingPosition = this.treeNodeManager.getSelfLink(entity.workingPosition);

      if (entity.id === undefined) {
        this.context.store.dispatch(this.getManager().createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
          if (!error) {
            this.addMessage({ message: this.i18n('create.success', { position: this.getManager().getNiceLabel(createdEntity), username: 'todo' }) });
          }
          this._afterSave(createdEntity, error, afterAction);
        }));
      } else {
        this.context.store.dispatch(this.getManager().patchEntity(entity, `${uiKey}-detail`, (patchedEntity, error) => {
          if (!error) {
            this.addMessage({ message: this.i18n('edit.success', { position: this.getManager().getNiceLabel(patchedEntity), username: 'todo' }) });
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
        this.context.router.replace(`identity/${identityId}/identity-contract/${entity.id}/detail`);
      }
    });
  }

  onChangeTreeType(treeType) {
    const treeTypeId = treeType ? treeType.id : null;
    this.setState({
      treeTypeId,
      forceSearchParameters: this.state.forceSearchParameters.setFilter('treeTypeId', treeTypeId || SearchParameters.BLANK_UUID)
    }, () => {
      // focus automatically - maybe will be usefull?
      // this.refs.workingPosition.focus();
    });
  }

  render() {
    const { uiKey, entity, showLoading, params } = this.props;
    const { _showLoading, forceSearchParameters, treeTypeId } = this.state;

    return (
      <div>
        <Helmet title={Utils.Entity.isNew(entity) ? this.i18n('create.title') : this.i18n('edit.title')} />

        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('label')} />

            <Basic.PanelBody style={Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 }}>
              <Basic.AbstractForm ref="form" showLoading={ _showLoading || showLoading } uiKey={uiKey} readOnly={!SecurityManager.isAdmin()}>
                <Basic.LabelWrapper readOnly ref="identity" label={this.i18n('entity.IdentityContract.identity')}>
                  <Advanced.IdentityInfo username={params.identityId}/>
                </Basic.LabelWrapper>
                <Basic.TextField
                  ref="position"
                  label={this.i18n('entity.IdentityContract.position')}/>
                <Basic.SelectBox
                  ref="treeTypeId"
                  manager={this.treeTypeManager}
                  label={this.i18n('entity.IdentityContract.treeType')}
                  onChange={this.onChangeTreeType.bind(this)}/>
                <Basic.SelectBox
                  ref="workingPosition"
                  manager={this.treeNodeManager}
                  label={this.i18n('entity.IdentityContract.workingPosition')}
                  forceSearchParameters={forceSearchParameters}
                  hidden={treeTypeId === null}/>
                <Basic.SelectBox
                  ref="guarantee"
                  manager={this.identityManager}
                  label={this.i18n('entity.IdentityContract.guarantee')}/>

                <Basic.DateTimePicker
                  mode="date"
                  ref="validFrom"
                  label={this.i18n('label.validFrom')}/>

                <Basic.DateTimePicker
                  mode="date"
                  ref="validTill"
                  label={this.i18n('label.validTill')}/>

                <Basic.Checkbox
                  ref="main"
                  label={this.i18n('entity.IdentityContract.main.label')}
                  helpBlock={this.i18n('entity.IdentityContract.main.help')}/>

                <Basic.Checkbox
                  ref="externe"
                  label={this.i18n('entity.IdentityContract.externe')}/>

                <Basic.Checkbox
                  ref="disabled"
                  label={this.i18n('entity.IdentityContract.disabled.label')}
                  helpBlock={this.i18n('entity.IdentityContract.disabled.help')}/>

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
                title={this.i18n('button.saveAndContinue')}
                onClick={this.save.bind(this, 'CONTINUE')}
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}
                rendered={SecurityManager.isAdmin()}
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
};