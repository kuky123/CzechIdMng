
import React from 'react';
//
import { Basic, Managers} from 'czechidm-core';
import { SystemTable} from 'czechidm-acc';
import VsSystemService from '../../services/VsSystemService';
const {SystemManager} = require('czechidm-acc').Managers;

const identityManager = new Managers.IdentityManager();
const systemManager = new SystemManager();
const roleManager = new Managers.RoleManager();
const vsSystemService = new VsSystemService();

/**
 * Virtual system table
 *
 * @author Vít Švanda
 */
export default class VsSystemTable extends SystemTable {

  constructor(props, context) {
    super(props, context);
    this.state = {
      detail: {
        createDefaultRole: true
      }
    };
  }

  _createNewSystem(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs['create-form'].isFormValid()) {
      return;
    }
    this.setState({showLoading: true});
    const detail = this.refs['create-form'].getData();
    vsSystemService.createVirtualSystem(detail)
    .then(json => {
      this.addMessage({ message: this.i18n('vs:content.vs-system.action.create.success', { system: json.name }) });
      this.setState({showLoading: false, show: false});
      this.context.store.dispatch(systemManager.receiveEntity(json.id, json, null, ()=> {
        this.context.router.push(`/system/${json.id}/detail`);
      }));
    })
    .catch(error => {
      this.setState({showLoading: false, show: false});
      this.addError(error);
    });
  }

  _showModal() {
    this.setState({show: true}, () => {
      if (this.refs.name) {
        this.refs.name.focus();
      }
    });
  }
  _closeModal() {
    this.setState({show: false});
  }

  _validateCreateDialog(result) {
    if (result === 'reject') {
      return true;
    }
    if (result === 'confirm' && this.refs['create-form'].isFormValid()) {
      return true;
    }
    return false;
  }

  getTableButtons(showAddButton) {
    return (
      [
        <Basic.Button
          level="success"
          key="add_button"
          className="btn-xs"
          onClick={this._showModal.bind(this)}
          rendered={Managers.SecurityManager.hasAuthority('SYSTEM_CREATE') && showAddButton}>
          <Basic.Icon type="fa" icon="plus"/>
          {' '}
          {this.i18n('button.add')}
        </Basic.Button>
      ]
    );
  }

  _onChangeName(event) {
    const roleName = this.refs.roleName.getValue();
    const oldName = this.refs.name.getValue();
    const name = event.currentTarget.value;
    const newRoleName = name + '-users';

    if (!oldName || oldName + '-users' === roleName) {
      this.refs.roleName.setValue(newRoleName);
    }
  }

  _onChangeDefaultRole(event) {
    const checked = event.currentTarget.checked;
    const formData = this.refs['create-form'].getData();
    formData.createDefaultRole = checked;

    this.setState({
      detail: formData
    });
  }

  render() {
    const {show, showLoading, detail} = this.state;
    const parentRender = super.render();
    const title = this.i18n(`vs:content.vs-system.action.create.header`);
    return (
      <div>
        {parentRender}
        <Basic.Modal show={show} onHide={this._closeModal.bind(this)}>
          <Basic.Loading showLoading={showLoading} showAnimation>
            <form onSubmit={this._createNewSystem.bind(this)}>
              <Basic.Modal.Header text={title} rendered={title !== undefined && title !== null} />
              <Basic.Modal.Body>
                <span dangerouslySetInnerHTML={{ __html: this.i18n(`vs:content.vs-system.action.create.message`) }}/>
                <div style={{marginTop: '20px'}}>
                  <Basic.AbstractForm data={detail} ref="create-form">
                    <Basic.TextField
                      ref="name"
                      label={this.i18n('vs:content.vs-system.name.label')}
                      placeholder={this.i18n('vs:content.vs-system.name.placeholder')}
                      onChange={ this._onChangeName.bind(this) }
                      required/>
                    <Basic.SelectBox
                      ref="implementers"
                      label={this.i18n('vs:content.vs-system.implementers.label')}
                      placeholder={this.i18n('vs:content.vs-system.implementers.placeholder')}
                      manager={identityManager }
                      value={null}
                      multiSelect/>
                    <Basic.SelectBox
                      ref="implementerRoles"
                      label={this.i18n('vs:content.vs-system.implementerRoles.label')}
                      placeholder={this.i18n('vs:content.vs-system.implementerRoles.placeholder')}
                      manager={roleManager }
                      value={null}
                      multiSelect/>
                    <Basic.Checkbox
                      ref="createDefaultRole"
                      label={this.i18n('vs:content.vs-system.createDefaultRole.label')}
                      onChange={ this._onChangeDefaultRole.bind(this) }
                      helpBlock={this.i18n('vs:content.vs-system.createDefaultRole.placeholder')}/>
                    <Basic.TextField
                      ref="roleName"
                      label={this.i18n('vs:content.vs-system.roleName.label')}
                      helpBlock={this.i18n('vs:content.vs-system.roleName.placeholder')}
                      rendered={detail.createDefaultRole}
                      required={detail.createDefaultRole}/>
                  </Basic.AbstractForm>
                  {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
                  <input type="submit" className="hidden"/>
                </div>
              </Basic.Modal.Body>
              <Basic.Modal.Footer>
                <Basic.Button level="link" onClick={this._closeModal.bind(this)}>{this.i18n('vs:content.vs-system.button.cancel')}</Basic.Button>
                <Basic.Button ref="yesButton" level="success" onClick={this._createNewSystem.bind(this)}>{this.i18n('vs:content.vs-system.button.create')}</Basic.Button>
              </Basic.Modal.Footer>
            </form>
          </Basic.Loading>
      </Basic.Modal>
      </div>
    );
  }
}
