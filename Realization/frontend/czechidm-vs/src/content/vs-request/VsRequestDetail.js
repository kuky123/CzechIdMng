import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import _ from 'lodash';
//
import { Basic, Utils, Advanced, Domain } from 'czechidm-core';
import { VsRequestManager } from '../../redux';
import VsRequestInfo from '../../components/advanced/VsRequestInfo/VsRequestInfo';
import VsRequestTable from './VsRequestTable';
import VsValueChangeType from '../../enums/VsValueChangeType';

const manager = new VsRequestManager();

/**
 * Virtual system request detail
 *
 * @author Vít Švanda
 */
class VsRequestDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    //
    this.state = {
      showLoading: false
    };
  }

  /**
   * "Shorcut" for localization
   */
  getContentKey() {
    return 'vs:content.vs-request.detail';
  }

  componentDidMount() {
    super.componentDidMount();
    //
    const { entity } = this.props;
    if (entity) {
      this._initConnectorObject(entity.id);
    }
  }

  /**
   * Component will receive new props, try to compare with actual,
   * then init form
   */
  componentWillReceiveProps(nextProps) {
    const { entity } = this.props;
    if (entity && nextProps.entity && entity.id !== nextProps.entity.id) {
      this._initConnectorObject(nextProps.entity.id);
    }
    if (!entity && nextProps.entity && nextProps.entity.id) {
      this._initConnectorObject(nextProps.entity.id);
    }
  }

  realize(entity) {
    if (!entity) {
      return;
    }
    manager.realize('realize', [entity ? entity.id : null], this, () => {
      this._initConnectorObject(entity.id);
    });
  }

  cancel(entity) {
    if (!entity) {
      return;
    }
    manager.cancel('cancel', [entity ? entity.id : null], this, () => {
      this._initConnectorObject(entity.id);
    });
  }

  _initConnectorObject(entityId) {
    manager.getService().getConnectorObject(entityId)
    .then(json => {
      this.setState({connectorObject: json});
    })
    .catch(error => {
      this.addError(error);
    });
    manager.getService().getWishConnectorObject(entityId)
    .then(json => {
      this.setState({wishConnectorObject: json});
    })
    .catch(error => {
      this.addError(error);
    });
  }

  _getImplementers(entity) {
    if (!entity || !entity.implementers) {
      return '';
    }
    const identities = [];
    for (const implementer of entity.implementers) {
      identities.push(implementer.id);
    }

    return (
      <Advanced.IdentitiesInfo identities={identities} maxEntry={100} showOnlyUsername={false}/>
    );
  }

  /**
   * Return data (attributes) for request table
   */
  _getRequestAccountData(entity) {
    if (entity && entity.connectorObject) {
      const attributes = entity.connectorObject.attributes;
      const compiledAttributes = this._compileAttributes(attributes);
      // sort by property
      return _(compiledAttributes).sortBy('property').value();
    }
  }

  /**
   * Return data (attributes) for account table.
   * Show wish for this request = current VS account attributes + changes from request.
   */
  _getWishData() {
    const {wishConnectorObject} = this.state;
    if (wishConnectorObject) {
      // sort by name
      return _(wishConnectorObject.attributes).sortBy('name').value();
    }
    return null;
  }

  _compileAttributes(attributes) {
    const accountData = {};
    for (const schemaAttributeId in attributes) {
      if (!attributes.hasOwnProperty(schemaAttributeId)) {
        continue;
      }
      let content = '';
      const attribute = attributes[schemaAttributeId];
      const propertyValue = attribute.values;
      if (attribute.multiValue) {
        content = propertyValue;
      } else {
        content = propertyValue ? propertyValue[0] : null;
      }
      accountData[attribute.name] = {property: attribute.name, value: content, multiValue: attribute.multiValue};
    }
    return accountData;
  }


  /**
   * Create value (highlights changes) cell for attributes table
   */
  _getValueCell({ rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || !entity.value) {
      return '';
    }

    if (entity.multiValue) {
      const listResult = [];
      for (const item of entity.value) {
        listResult.push(item.value + ' ');
      }
      return listResult;
    }

    return entity.value;
  }
  /**
   * Create value (highlights changes) cell for attributes table
   */
  _getWishValueCell( old = false, showChanges = true, { rowIndex, data}) {
    const entity = data[rowIndex];
    if (!entity || (!entity.value && !entity.values)) {
      return '';
    }
    if (entity.multivalue) {
      const listResult = [];
      if (!entity.values) {
        return '';
      }
      for (const item of entity.values) {
        const value = old ? item.oldValue : item.value;
        if (!old && item.change && showChanges) {
          listResult.push(<Basic.Label
            key={value}
            level={VsValueChangeType.getLevel(item.change)}
            title={item.change ? this.i18n(`attribute.diff.${item.change}`) : null}
            style={item.change === 'REMOVED' ? {textDecoration: 'line-through'} : null}
            text={value}/>);
        } else {
          listResult.push(value ? (item.value + ' ') : '');
        }
        listResult.push(' ');
      }
      return listResult;
    }

    if (!entity.value) {
      return '';
    }
    const value = old ? entity.value.oldValue : entity.value.value;
    if (!old && entity.value.change && showChanges) {
      return (<Basic.Label
        title={entity.value.change ? this.i18n(`attribute.diff.${entity.value.change}`) : null}
        level={VsValueChangeType.getLevel(entity.value.change)}
        style={entity.value.change === 'REMOVED' ? {textDecoration: 'line-through'} : null}
        text={value !== null ? value + '' : '' }/>);
    }
    return value !== null ? value + '' : '';
  }

  render() {
    const { entity, _permissions, showLoading } = this.props;
    const _showLoading = showLoading || this.state.showLoading;

    if (_showLoading) {
      return (<Basic.Panel>
        <Basic.PanelHeader text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('basic') } />
        <Basic.PanelBody>
          <Basic.Loading show isStatic />
        </Basic.PanelBody>
      </Basic.Panel>);
    }

    const searchBefore = new Domain.SearchParameters()
    .setFilter('uid', entity ? entity.uid : null)
    .setFilter('systemId', entity ? entity.system : Domain.SearchParameters.BLANK_UUID)
    .setFilter('createdBefore', entity ? entity.created : null)
    .setFilter('state', 'IN_PROGRESS');

    const searchAfter = new Domain.SearchParameters()
    .setFilter('uid', entity ? entity.uid : null)
    .setFilter('systemId', entity ? entity.system : Domain.SearchParameters.BLANK_UUID)
    .setFilter('createdAfter', entity ? entity.created : null)
    .setFilter('state', 'IN_PROGRESS');

    const wishData = this._getWishData();
    const isInProgress = entity ? (entity.state === 'IN_PROGRESS') : false;
    const isDeleteOperation = entity ? (entity.operationType === 'DELETE') : false;
    const isCreateOperation = entity ? (entity.operationType === 'CREATE') : false;
    return (
      <div>
        <Basic.Confirm ref="confirm-realize" level="danger"/>
        <Basic.Confirm ref="confirm-cancel" level="danger">
          <div style={{marginTop: '20px'}}>
            <Basic.AbstractForm ref="cancel-form" uiKey="confirm-cancel" >
              <Basic.TextArea
                ref="cancel-reason"
                placeholder={this.i18n('vs:content.vs-requests.cancel-reason.placeholder')}
                required/>
            </Basic.AbstractForm>
          </div>
        </Basic.Confirm>
        <Helmet title={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('edit.title') } />

          <Basic.Panel>
            <Basic.PanelHeader text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('basic') } />
            <Basic.PanelBody>
              <div>
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <VsRequestInfo entityIdentifier={entity ? entity.id : null} entity={entity} face="full" showLink={false}/>
                    <Basic.LabelWrapper readOnly ref="implementers" label={this.i18n('vs:entity.VsRequest.implementers.label') + ':'}>
                      {this._getImplementers(entity)}
                    </Basic.LabelWrapper>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Basic.TextArea
                      ref="reason"
                      label={this.i18n('vs:entity.VsRequest.reason.label')}
                      readOnly
                      rows={6}
                      rendered={entity && entity.state === 'CANCELED' }
                      value={entity ? entity.reason : null}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 12 }>
                    <Basic.LabelWrapper readOnly label={this.i18n('wishAttributes') + ':'}>
                      <Basic.Alert
                        level="warning"
                        icon="trash"
                        rendered={isDeleteOperation && isInProgress}
                        style={{display: 'block', margin: 'auto', marginTop: '30px', marginBottom: '30px', maxWidth: '600px'}}
                        text={this.i18n('alert.accountShouldBeDeleted')}/>
                      <Basic.Alert
                        level="success"
                        icon="ok"
                        rendered={isCreateOperation && isInProgress}
                        style={{display: 'block', margin: 'auto', marginTop: '30px', marginBottom: '30px', maxWidth: '600px'}}
                        text={this.i18n('alert.accountShouldBeCreated')}/>
                      <Basic.Table
                        data={wishData}
                        rendered={!isDeleteOperation}
                        noData={this.i18n('component.basic.Table.noData')}
                        rowClass={({rowIndex, data}) => { return (data[rowIndex].changed) ? 'warning' : ''; }}
                        className="table-bordered">
                        <Basic.Column property="name" header={this.i18n('label.property')}/>
                        <Basic.Column property="value" header={this.i18n('label.targetValue')} cell={this._getWishValueCell.bind(this, false, isInProgress)}/>
                        <Basic.Column property="oldValue" rendered={isInProgress} header={this.i18n('label.oldValue')} cell={this._getWishValueCell.bind(this, true, false)}/>
                      </Basic.Table>
                    </Basic.LabelWrapper>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 6 }>
                    <Basic.LabelWrapper readOnly ref="vs-request-table-before" label={this.i18n('beforeRequests.label') + ':'}>
                      <VsRequestTable
                        uiKey="vs-request-table-before"
                        columns= {['state', 'operationType', 'created', 'uid']}
                        showFilter={false}
                        forceSearchParameters={searchBefore}
                        showToolbar={false}
                        showPageSize={false}
                        showRowSelection={false}
                        showId={false}
                        filterOpened={false} />
                    </Basic.LabelWrapper>
                  </Basic.Col>
                  <Basic.Col lg={ 6 }>
                    <Basic.LabelWrapper readOnly ref="vs-request-table-after" label={this.i18n('afterRequests.label') + ':'}>
                      <VsRequestTable
                        uiKey="vs-request-table-after"
                        columns= {['state', 'operationType', 'created', 'uid']}
                        showFilter={false}
                        forceSearchParameters={searchAfter}
                        showToolbar={false}
                        showPageSize={false}
                        showRowSelection={false}
                        showId={false}
                        filterOpened={false} />
                    </Basic.LabelWrapper>
                  </Basic.Col>
                </Basic.Row>
              </div>
            </Basic.PanelBody>
            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={ this.context.router.goBack }>{ this.i18n('button.back') }</Basic.Button>
              <Basic.SplitButton
                level="success"
                id="request-realize"
                title={ this.i18n('button.request.realize') }
                onClick={this.realize.bind(this, entity) }
                showLoading={ _showLoading }
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ manager.canSave(entity, _permissions) && isInProgress }
                pullRight
                dropup>
                <Basic.MenuItem
                  eventKey="1"
                  onClick={this.cancel.bind(this, entity)}>
                  {this.i18n('button.request.cancel')}
                </Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          </Basic.Panel>
      </div>
    );
  }
}

VsRequestDetail.propTypes = {
  /**
   * Loaded entity
   */
  entity: PropTypes.object,
  /**
   * Entity, permissions etc. fro this content are stored in redux under given key
   */
  uiKey: PropTypes.string.isRequired,
  /**
   * Logged identity permissions - what can do with currently loaded entity
   */
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
VsRequestDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  if (!component.entity) {
    return {};
  }
  return {
    _permissions: manager.getPermissions(state, null, component.entity.id)
  };
}

export default connect(select)(VsRequestDetail);
