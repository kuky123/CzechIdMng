import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Advanced from '../../../components/advanced';
import * as Basic from '../../../components/basic';
import * as Utils from '../../../utils';
import { TreeTypeManager, DataManager, SecurityManager } from '../../../redux';

const UI_KEY = 'tree-type-configuration';

/**
 * Type configuration properties
 *
 * @author Radek Tomiška
 */
class TreeTypeConfiguration extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.treeTypeManager = new TreeTypeManager();
    this.state = {
      treeTypeId: props.treeTypeId
    };
  }

  getContentKey() {
    return 'content.tree.types';
  }

  componentDidMount() {
    this._load();
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.treeTypeId !== this.state.treeTypeId) {
      this.setState({
        treeTypeId: nextProps.treeTypeId
      }, () => {
        this._load();
      });
    }
  }

  _load() {
    const { treeTypeId } = this.state;
    //
    if (treeTypeId) {
      this.context.store.dispatch(this.treeTypeManager.fetchConfigurations(treeTypeId, `${UI_KEY}-${treeTypeId}`, () => {}));
    }
  }

  /**
   * Return false, if index is not valid
   *
   * @param  {immutable}  configurations
   * @return {Boolean}
   */
  _isValid(configurations) {
    if (!configurations) {
      return true;
    }
    if (configurations.has('valid')) {
      return configurations.get('valid').value === 'true';
    }
    return true;
  }

  /**
   * Returns long running task id, if index is in rebuild. False otherwise.
   *
   * @param  {immutable}  configurations
   * @return {oneOfType[bool, string]}
   */
  isRebuild(configurations) {
    if (!configurations) {
      return false;
    }
    if (configurations.has('rebuild')) {
      return configurations.get('rebuild').value;
    }
    return false;
  }

  /**
   * Rebuild Index
   */
  onRebuildIndex() {
    const { treeTypeId } = this.state;
    const treeType = this.treeTypeManager.getEntity(this.context.store.getState(), treeTypeId);
    //
    // show confirm message for deleting entity or entities
    this.refs['confirm-rebuild'].show(
      this.i18n('configuration.action.rebuild.message', { record: this.treeTypeManager.getNiceLabel(treeType) }),
      this.i18n('configuration.action.rebuild.header')
    ).then(() => {
      this.context.store.dispatch(this.treeTypeManager.rebuildIndex(treeTypeId, `${UI_KEY}-${treeTypeId}`));
    }, () => {
      //
    });
  }

  render() {
    const { rendered, _showLoading, _rebuildLoading, _configurations, className } = this.props;
    //
    if (!rendered || _showLoading) {
      return null;
    }
    //
    const isValid = this._isValid(_configurations);
    const isRebuild = this.isRebuild(_configurations);
    //
    if (isValid) {
      // TODO: valid index is not shown for now ... but maybe will be useful
      return null;
    }
    // action buttons
    const buttons = [];
    if (SecurityManager.hasAuthority('SCHEDULER_EXECUTE')) {
      buttons.push(
        <Basic.Button
          onClick={this.onRebuildIndex.bind(this)}>
          { this.i18n('configuration.button.rebuild') }
        </Basic.Button>
      );
    }
    //
    return (
      <div className={className}>
        <Basic.Confirm level="warning" ref="confirm-rebuild"/>

        <Basic.Alert
          level="success"
          icon="ok"
          text={ this.i18n('configuration.index.valid') }
          className="no-margin"
          rendered={isValid && !isRebuild}
          buttons={buttons}
          showLoading={_rebuildLoading}/>
        <Basic.Alert
          level="warning"
          icon="warning-sign"
          text={this.i18n('configuration.index.invalid')}
          className="no-margin"
          rendered={!isValid && !isRebuild}
          buttons={buttons}
          showLoading={_rebuildLoading}/>

        <Advanced.LongRunningTask
          entityIdentifier={ isRebuild }
          header={ this.i18n('configuration.index.rebuild', { escape: false } ) }
          showProperties={ false }/>
      </div>
    );
  }
}

TreeTypeConfiguration.propTypes = {
  rendered: PropTypes.bool,
  treeTypeId: PropTypes.string.isRequired,
  //
  _showLoading: PropTypes.bool,
  _rebuildLoading: PropTypes.bool,
  _configurations: PropTypes.object // immutable
};
TreeTypeConfiguration.defaultProps = {
  rendered: true,
  _showLoading: true,
  _rebuildLoading: false,
  _configurations: null
};

function select(state, component) {
  const treeTypeId = component.treeTypeId;
  //
  return {
    _showLoading: Utils.Ui.isShowLoading(state, `${UI_KEY}-${treeTypeId}`),
    _rebuildLoading: Utils.Ui.isShowLoading(state, `${UI_KEY}-${treeTypeId}-rebuild`),
    _configurations: DataManager.getData(state, `${UI_KEY}-${treeTypeId}`)
  };
}

export default connect(select)(TreeTypeConfiguration);
