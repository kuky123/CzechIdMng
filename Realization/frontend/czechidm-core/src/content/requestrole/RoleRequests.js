import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { RoleRequestManager, IdentityManager} from '../../redux';
import RoleRequestTable from './RoleRequestTable';
import uuid from 'uuid';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';

const uiKey = 'role-request-table';
const manager = new RoleRequestManager();
const identityManager = new IdentityManager();

class RoleRequests extends Advanced.AbstractTableContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      detail: {
        show: false,
        entity: {}
      }
    };
  }

  getManager() {
    return manager;
  }

  getUiKey() {
    return uiKey;
  }

  getContentKey() {
    return 'content.roleRequests';
  }

  getNavigationKey() {
    return 'role-requests';
  }

  _showCreateDetail() {
    this.setState({
      detail: {
        ... this.state.detail,
        show: true
      }
    });
  }

  _createNewRequest(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.modalForm.isFormValid()) {
      return;
    }
    const applicantId = this.refs.applicant.getValue();
    const uuidId = uuid.v1();
    this.context.router.push(`/role-requests/${uuidId}/new?new=1&applicantId=${applicantId}`);
  }

  _startRequest(idRequest, event) {
    if (event) {
      event.preventDefault();
    }
    this.refs[`confirm-delete`].show(
      this.i18n(`action.startRequest.message`),
      this.i18n(`action.startRequest.header`)
    ).then(() => {
      this.setState({
        showLoading: true
      });
      const promise = this.getManager().getService().startRequest(idRequest);
      promise.then((json) => {
        this.setState({
          showLoading: false
        });
        if (this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
        if (json.state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.DUPLICATED)) {
          this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.duplicated', { created: moment(json._embedded.duplicatedToRequest.created).format(this.i18n('format.datetime'))}), level: 'warning'});
          return;
        }
        if (json.state === RoleRequestStateEnum.findKeyBySymbol(RoleRequestStateEnum.EXCEPTION)) {
          this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.exception'), level: 'error' });
          return;
        }
        this.addMessage({ message: this.i18n('content.roleRequests.action.startRequest.started') });
      }).catch(ex => {
        this.setState({
          showLoading: false
        });
        this.addError(ex);
        if (this.refs.table) {
          this.refs.table.getWrappedInstance().reload();
        }
      });
    }, () => {
      // Rejected
    });
    return;
  }

  /**
   * Close modal dialog
   */
  _closeDetail() {
    this.setState({
      detail: {
        ... this.state.detail,
        show: false
      }
    });
  }

  render() {
    const { _showLoading } = this.props;
    const { showLoading, detail } = this.state;
    const innerShowLoading = _showLoading || showLoading;
    return (
      <div>
        <Helmet title={this.i18n('title')} />
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.ContentHeader>
          <Basic.Icon type="fa" icon="universal-access"/>
          {' '}
          <span dangerouslySetInnerHTML={{ __html: this.i18n('header') }}/>
        </Basic.ContentHeader>
        <Basic.Panel>
          <RoleRequestTable
            ref="table"
            uiKey={uiKey}
            showLoading={innerShowLoading}
            manager={this.getManager()}
            createNewRequestFunc={this._showCreateDetail.bind(this)}
            startRequestFunc={this._startRequest.bind(this)}/>
        </Basic.Panel>
        <Basic.Modal
          bsSize="default"
          show={detail.show}
          onHide={this._closeDetail.bind(this)}
          backdrop="static"
          keyboard={!innerShowLoading}>

          <form onSubmit={this._createNewRequest.bind(this)}>
            <Basic.Modal.Header closeButton={!_showLoading} text={this.i18n('create.header')}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="modalForm" showLoading={_showLoading}>
                <Basic.SelectBox
                  ref="applicant"
                  manager={ identityManager }
                  label={ this.i18n('applicant') }
                  required/>

              </Basic.AbstractForm>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this._closeDetail.bind(this)}
                showLoading={_showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={_showLoading}
                showLoadingIcon>
                {this.i18n('button.createRequest')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

RoleRequests.propTypes = {
  _showLoading: PropTypes.bool
};
RoleRequests.defaultProps = {
  _showLoading: false
};

function select() {
  return {
  };
}

export default connect(select)(RoleRequests);
