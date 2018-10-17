import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import { LoggingEventExceptionManager } from '../../../redux';

const EU_BCVSOLUTIONS_PREFIX = 'eu.bcvsolutions.idm.';
const uiKey = 'loggingEventExceptionUiKey';
/**
 * Logging event exception detail
 *
 * TODO:? there is two redundant get for exception entities (rows),
 * because advanced table not supported data props and basic table not supported pageable,
 * is not possible get entities in one request.
 *
 * @author Ondřej Kopr
 */

const manager = new LoggingEventExceptionManager();

class LoggingEventExceptionDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showRawError: false
    };
  }

  componentDidMount() {
    const { eventId } = this.props;
    this.context.store.dispatch(
      manager.fetchEntities(
        manager.getDefaultSearchParameters()
        .setSize(1000) // setSize(null) not working for now
        .setFilter('event', eventId), `${uiKey}-${eventId}`)
      );
  }

  _transformEntitiesToText(entities) {
    let rawEntities = '';
    if (entities) {
      for (const entity in entities) {
        if (entities.hasOwnProperty(entity)) {
          rawEntities += entities[entity].traceLine + '\n';
        }
      }
    }
    return rawEntities;
  }

  getContentKey() {
    return 'content.audit.logging-event';
  }

  _showRawError(show) {
    this.setState({
      showRawError: show
    });
  }

  render() {
    const { eventId, showLoading, entities } = this.props;
    const { showRawError } = this.state;

    const rawEntities = this._transformEntitiesToText(entities);

    return (
      <div>
        <Advanced.Table
          ref="table"
          buttons={
            <Basic.Button
              showLoading={showLoading}
              className="btn-xs"
              hidden={!entities && entities.size === 0}
              onClick={this._showRawError.bind(this, true)}>
              <Basic.Icon icon="fa:file-code-o" />
              {' '}
              { this.i18n('rawSource') }
            </Basic.Button>
          }
          manager={manager}
          forceSearchParameters={manager.getDefaultSearchParameters().setFilter('event', eventId)}
          showId={false}
          data={entities}
          showLoading={showLoading}
          rowClass={({ rowIndex, data }) => {
            if (data[rowIndex].traceLine.indexOf(EU_BCVSOLUTIONS_PREFIX) + 1) {
              return 'warning';
            }
          }}>
          <Advanced.Column property="id" width={ 50 } />
          <Advanced.Column
            property="traceLine"
            className="pre"/>
        </Advanced.Table>
        <Basic.Modal
          bsSize="large"
          show={showRawError}
          onHide={this._showRawError.bind(this, false)}
          backdrop="static"
          keyboard={!showLoading}>

          <Basic.Modal.Header closeButton={!showLoading} text={this.i18n('exception.header')}/>
          <Basic.Modal.Body>
            <Basic.ScriptArea
              showMaximalizationBtn={false}
              height="50em"
              value={rawEntities}/>
          </Basic.Modal.Body>

          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={this._showRawError.bind(this, false)}
              showLoading={showLoading}>
              {this.i18n('button.close')}
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </div>
    );
  }
}

LoggingEventExceptionDetail.propTypes = {
  eventId: PropTypes.string
};

LoggingEventExceptionDetail.defaultProps = {

};

function select(state, component) {
  const { eventId } = component;
  //
  return {
    entities: manager.getEntities(state, `${uiKey}-${eventId}`),
    showLoading: manager.isShowLoading(state, `${uiKey}-${eventId}`)
  };
}

export default connect(select)(LoggingEventExceptionDetail);
