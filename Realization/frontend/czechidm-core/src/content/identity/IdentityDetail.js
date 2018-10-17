import React from 'react';
import PropTypes from 'prop-types';
import Joi from 'joi';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { IdentityManager, DataManager, ProfileManager } from '../../redux';
import ApiOperationTypeEnum from '../../enums/ApiOperationTypeEnum';
import IdentityStateEnum from '../../enums/IdentityStateEnum';

const identityManager = new IdentityManager();
const profileManager = new ProfileManager();

/**
 * Identity's detail form
 *
 * @author Radek Tomiška
 */
class IdentityDetail extends Basic.AbstractContent {

  constructor(props) {
    super(props);
    this.state = {
      showLoading: false,
      showLoadingIdentityTrimmed: false,
      setDataToForm: false,
      deleteButton: false,
      showCropper: false
    };
  }

  getContentKey() {
    return 'content.identity.profile';
  }

  componentDidMount() {
    const { identity } = this.props;
    this.refs.form.setData(identity);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.identity) {
      if (nextProps.identity._trimmed) {
        this.setState({showLoadingIdentityTrimmed: true});
      } else {
        this.setState({showLoadingIdentityTrimmed: false});
      }
      if (nextProps.identity !== this.props.identity) {
        // after receive new Identity we will hide showLoading on form
        this.setState({showLoading: false, setDataToForm: true});
        this.context.store.dispatch(identityManager.fetchProfilePermissions(nextProps.entityId));
      }
    }
  }

  componentDidUpdate() {
    if (this.props.identity && !this.props.identity._trimmed && this.state.setDataToForm) {
      // We have to set data to form after is rendered
      this.transformData(this.props.identity, null, ApiOperationTypeEnum.GET);
    }
  }

  onSave(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const json = this.refs.form.getData();
    this.saveIdentity(json);
  }

  saveIdentity(json) {
    this.setState({
      showLoading: true,
      setDataToForm: false // Form will not be set new data (we are waiting to saved data)
    }, () => {
      this.context.store.dispatch(identityManager.updateEntity(json, null, (updatedEntity, error) => {
        this._afterSave(updatedEntity, error);
      }));
    });
  }

  _afterSave(entity, error) {
    this.setState({
      showLoading: false
    }, () => {
      if (error) {
        this.addError(error);
        return;
      }
      this.addMessage({ level: 'success', key: 'form-success', message: this.i18n('messages.saved', { username: entity.username }) });
      //
      // when username was changed, then new url is replaced
      const { identity } = this.props;
      if (identity.username !== entity.username) {
        this.context.router.replace(`/identity/${encodeURIComponent(entity.username)}/profile`);
      }
    });
  }

  transformData(json, error, operationType) {
    this.refs.form.setData(json, error, operationType);
  }

  /**
   * Dropzone component function called after select file
   * @param file selected file (multiple is not allowed)
   */
  _onDrop(files) {
    if (this.refs.dropzone.state.isDragReject) {
      this.addMessage({
        message: this.i18n('fileRejected'),
        level: 'warning'
      });
      return;
    }
    files.forEach((file) => {
      // const { identity } = this.props;
      if (!file.name.endsWith('.jpg') && !file.name.endsWith('.jpeg') && !file.name.endsWith('.png') && !file.name.endsWith('.gif')) {
        this.addMessage({
          message: this.i18n('fileRejected', {name: file.name}),
          level: 'warning'
        });
        return;
      }
      const objectURL = URL.createObjectURL(file);
      this.setState({
        cropperSrc: objectURL,
        showCropper: true,
        fileName: file.name
      });
    });
  }

  deleteImage() {
    this.refs['confirm-delete'].show(
      this.i18n(`deleteImage.message`),
      this.i18n(`deleteImage.title`)
    ).then(() => {
      this.context.store.dispatch(identityManager.deleteProfileImage(this.props.entityId));
    }, () => {
      // Rejected
    });
  }

  _showCropper() {
    this.setState({
      showCropper: true
    });
  }

  _closeCropper() {
    this.setState({
      showCropper: false
    });
  }

  _crop() {
    this.refs.cropper.crop((formData) => {
      // append selected fileName
      formData.fileName = this.state.fileName;
      formData.name = this.state.fileName;
      formData.append( 'fileName', this.state.fileName);
      //
      this.context.store.dispatch(identityManager.uploadProfileImage(this.props.entityId, formData));
    });
    this._closeCropper();
  }

  render() {
    const { identity, readOnly, _permissions, _profilePermissions, _imageUrl, _imageLoading, userContext } = this.props;
    const { showLoading, showLoadingIdentityTrimmed, showCropper, cropperSrc } = this.state;
    //
    const blockLoginDate = identity && identity.blockLoginDate ? moment(identity.blockLoginDate).format(this.i18n('format.datetime')) : null;
    //
    return (
      <div className="identity-detail">
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Helmet title={ identity && identity.id === userContext.id ? this.i18n('title') : this.i18n('content.identity.profile.userDetail') } />
        <form onSubmit={this.onSave.bind(this)}>
          <Basic.Panel className="no-border last">
            <Basic.PanelHeader text={this.i18n('header')}/>
            <Basic.Alert
              ref="blockLoginDate"
              level="warning"
              rendered={blockLoginDate !== null}
              text={this.i18n('blockLoginDate', {date: blockLoginDate})} />

            <Basic.AbstractForm ref="form" readOnly={ !identityManager.canSave(identity, _permissions) || readOnly } showLoading={ showLoadingIdentityTrimmed || showLoading }>
              <div className="image-field-container">
                <div className="image-col">
                  <div className="image-wrapper">
                    <Advanced.ImageDropzone
                      ref="dropzone"
                      accept="image/*"
                      multiple={ false }
                      onDrop={ this._onDrop.bind(this) }
                      showLoading={ _imageLoading }
                      readOnly={ !profileManager.canSave(identity, _profilePermissions) }>
                      <img className="img-thumbnail" src={ _imageUrl } />
                    </Advanced.ImageDropzone>
                    <Basic.Button
                      type="button"
                      rendered={ cropperSrc && _imageUrl ? true : false }
                      titlePlacement="right"
                      onClick={ this._showCropper.bind(this) }
                      className="btn-xs btn-edit">
                      <Basic.Icon icon="edit"/>
                    </Basic.Button>
                    <Basic.Button
                      type="button"
                      level="danger"
                      rendered={ _imageUrl && profileManager.canSave(identity, _profilePermissions) ? true : false }
                      titlePlacement="left"
                      onClick={ this.deleteImage.bind(this) }
                      className="btn-xs btn-remove">
                      <Basic.Icon type="fa" icon="trash"/>
                    </Basic.Button>
                  </div>
                </div>
                <div className="field-col">
                  <Basic.TextField ref="username" label={this.i18n('username')} required min={3} max={255} />
                  <Basic.TextField ref="firstName" label={this.i18n('firstName')} max={255} />
                  <Basic.TextField ref="lastName" label={this.i18n('lastName')} max={255} />
                </div>
              </div>

              <Basic.TextField ref="externalCode" label={this.i18n('content.identity.profile.externalCode')} max={255}/>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField ref="titleBefore" label={this.i18n('entity.Identity.titleBefore')} max={100} />
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField ref="titleAfter" label={this.i18n('entity.Identity.titleAfter')} max={100} />
                </Basic.Col>
              </Basic.Row>

              <Basic.Row>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField
                    ref="email"
                    label={this.i18n('email.label')}
                    placeholder={this.i18n('email.placeholder')}
                    validation={Joi.string().allow(null).email()}/>
                </Basic.Col>
                <Basic.Col lg={ 6 }>
                  <Basic.TextField
                    ref="phone"
                    label={this.i18n('phone.label')}
                    placeholder={this.i18n('phone.placeholder')}
                    max={30} />
                </Basic.Col>
              </Basic.Row>

              <Basic.TextArea
                ref="description"
                label={this.i18n('description.label')}
                placeholder={this.i18n('description.placeholder')}
                rows={4}
                max={1000}/>

              <Basic.EnumSelectBox
                ref="state"
                enum={ IdentityStateEnum }
                useSymbol={ false }
                label={ this.i18n('entity.Identity.state.label') }
                helpBlock={ <span>{ this.i18n('entity.Identity.state.help') }</span> }
                readOnly/>

              <Basic.Checkbox
                ref="disabled"
                label={this.i18n('entity.Identity.disabledReadonly.label')}
                helpBlock={this.i18n('entity.Identity.disabledReadonly.help')}
                readOnly />

            </Basic.AbstractForm>

            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={this.context.router.goBack} showLoading={showLoading}>{this.i18n('button.back')}</Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={ showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ identityManager.canSave(identity, _permissions) }
                hidden={ readOnly }>
                { this.i18n('button.save') }
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>

        <Basic.Modal
          bsSize="default"
          show={ showCropper }
          onHide={ this._closeCropper.bind(this) }
          backdrop="static" >

          <Basic.Modal.Body>
            <Advanced.ImageCropper
              ref="cropper"
              src={ cropperSrc }/>
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={ this._closeCropper.bind(this) }
              showLoading={ showLoading }>
              { this.i18n('button.close') }
            </Basic.Button>
            <Basic.Button
              level="info"
              onClick={ this._crop.bind(this) }
              showLoading={ showLoading }>
              { this.i18n('button.crop') }
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </div>
    );
  }
}

IdentityDetail.propTypes = {
  identity: PropTypes.object,
  entityId: PropTypes.string.isRequired,
  readOnly: PropTypes.bool,
  userContext: PropTypes.object,
  _permissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ]),
  _profilePermissions: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.arrayOf(PropTypes.string)
  ]),
};
IdentityDetail.defaultProps = {
  userContext: null,
  _permissions: null,
  _profilePermissions: null,
  readOnly: false,
  _imageUrl: null
};

function select(state, component) {
  const identifier = component.entityId;
  const profileUiKey = identityManager.resolveProfileUiKey(identifier);
  const profile = DataManager.getData(state, profileUiKey);
  //
  return {
    userContext: state.security.userContext,
    _permissions: identityManager.getPermissions(state, null, identifier),
    _profilePermissions: profileManager.getPermissions(state, null, identifier),
    _imageLoading: DataManager.isShowLoading(state, profileUiKey),
    _imageUrl: profile ? profile.imageUrl : null,
  };
}
export default connect(select)(IdentityDetail);
