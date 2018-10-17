import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { RequestManager} from '../../redux';
import RequestTable from './RequestTable';
import RoleRequestStateEnum from '../../enums/RoleRequestStateEnum';

const uiKey = 'universal-request-table';
const manager = new RequestManager();

/**
 * Universal request agenda
 *
 * @author Vít Švanda
 */
class Requests extends Advanced.AbstractTableContent {

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
    return 'content.requests';
  }

  getNavigationKey() {
    return 'requests';
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

  render() {
    const { _showLoading } = this.props;
    const { showLoading } = this.state;
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
          <RequestTable
            ref="table"
            uiKey={uiKey}
            showLoading={innerShowLoading}
            manager={this.getManager()}
            startRequestFunc={this._startRequest.bind(this)}/>
        </Basic.Panel>
      </div>
    );
  }
}

Requests.propTypes = {
  _showLoading: PropTypes.bool
};
Requests.defaultProps = {
  _showLoading: false
};

function select() {
  return {
  };
}

export default connect(select)(Requests);
