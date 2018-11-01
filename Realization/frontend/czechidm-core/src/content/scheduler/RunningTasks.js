import React, { PropTypes } from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';
import SearchParameters from '../../domain/SearchParameters';
import { LongRunningTaskManager } from '../../redux';

const UIKEY = 'active-long-running-task-table';
const manager = new LongRunningTaskManager();

/**
 * Running tasks overview
 *
 * @author Radek Tomiška
 */
class RunningTasks extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.scheduler.running-tasks';
  }

  getNavigationKey() {
    const { creatorId } = this.props;
    if (creatorId) {
      // FIXME: this content is reused on dashboard - refactor to two route contents
      return null;
    }
    return 'scheduler-running-tasks';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    this._fetchRunningTasks();
  }

  _fetchRunningTasks() {
    const { creatorId } = this.props;
    const forceSearchParameters = new SearchParameters().setFilter('running', true).setFilter('stateful', true).setFilter('creatorId', creatorId).setSort('created', 'desc');
    this.context.store.dispatch(manager.fetchEntities(forceSearchParameters, UIKEY));
  }

  render() {
    const { _entities, _showLoading } = this.props;
    //
    return (
      <div>
        <Helmet title={ this.i18n('title') } />
        <Basic.Toolbar>
          <div className="pull-right">
            <Advanced.RefreshButton
              onClick={ this._fetchRunningTasks.bind(this) }
              title={ this.i18n('refresh') }
              showLoading={ _showLoading }/>
          </div>
        </Basic.Toolbar>
        {
          _showLoading
          ?
          <Basic.Loading show isStatic />
          :
          <div style={{ padding: '15px 15px 0' }}>
            {
              (!_entities || _entities.length === 0)
              ?
              <Basic.Alert className="no-margin" text={ this.i18n('empty') }/>
              :
              <div>
                {
                  _entities.map(entity => {
                    return (
                      <Advanced.LongRunningTask entity={ entity } />
                    );
                  })
                }
              </div>
            }
          </div>
        }
      </div>
    );
  }
}

RunningTasks.propTypes = {
  creatorId: PropTypes.string,
  _showLoading: PropTypes.bool,
  _entities: PropTypes.arrayOf(React.PropTypes.object)
};
RunningTasks.defaultProps = {
  creatorId: null,
  _showLoading: true,
  _entities: []
};

function select(state) {
  return {
    _showLoading: Utils.Ui.isShowLoading(state, UIKEY),
    _entities: Utils.Ui.getEntities(state, UIKEY)
  };
}

export default connect(select)(RunningTasks);
