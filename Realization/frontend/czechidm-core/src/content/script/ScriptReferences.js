import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { ScriptManager } from '../../redux';
import uuid from 'uuid';
import Helmet from 'react-helmet';
//
import ScriptTable from './ScriptTable';

// const uiKey = 'script-authorities';
const scriptManager = new ScriptManager();

/**
 * Script usage in other scripts
 *
 * @author Patrik Stloukal
 */
class ScriptReferences extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
  }

  getContentKey() {
    return 'content.scripts.references';
  }

    getNavigationKey() {
      return 'script-references';
    }

  useFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().useFilterForm(this.refs.filterForm);
  }

  cancelFilter(event) {
    if (event) {
      event.preventDefault();
    }
    this.refs.table.getWrappedInstance().cancelFilter(this.refs.filterForm);
  }

  showDetail(entity, event) {
    if (event) {
      event.preventDefault();
    }
    // when create new script is generate non existing id
    // and set parameter new to 1 (true)
    // this is necessary for ScriptDetail
    if (entity.id === undefined) {
      const uuidId = uuid.v1();
      this.context.router.push(`/scripts/${uuidId}?new=1`);
    } else {
      this.context.router.push(`/scripts/${entity.id}/detail`);
    }
  }

  render() {
    const { uiKey, _entity } = this.props;
    if (this.props._entity == null) {
      return null;
    }
    //
    return (
      <div>
        <Helmet title={ this.i18n('title') } />
        <Basic.Panel className={ 'no-border last' }>
          <Basic.PanelHeader text={ this.i18n('header') } />

          <ScriptTable
            uiKey={uiKey}
            scriptManager={scriptManager}
            forceSearchParameters={scriptManager.getDefaultSearchParameters().setFilter('usedIn', _entity.code)}
            disableAdd
            filterOpened={ false }/>

        </Basic.Panel>
      </div>
    );
  }
  }

ScriptReferences.propTypes = {
  _entity: PropTypes.object,
  _permissions: PropTypes.arrayOf(PropTypes.string),
  uiKey: PropTypes.string.isRequired,
  script: PropTypes.object.isRequired,
  rendered: PropTypes.bool.isRequired
};
ScriptReferences.defaultProps = {
  _entity: null,
  _permissions: null,
  rendered: true
};

function select(state, component) {
  return {
    _entity: scriptManager.getEntity(state, component.params.entityId),
    _permissions: scriptManager.getPermissions(state, null, component.params.entityId),
  };
}

export default connect(select)(ScriptReferences);
