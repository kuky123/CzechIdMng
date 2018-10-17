

import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import moment from 'moment';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { RoleManager, AutomaticRoleAttributeRuleManager } from '../../redux';
import AutomaticRoleAttributeRuleDetail from '../automaticrole/attribute/AutomaticRoleAttributeRuleDetail';

/**
* Table for keep automatic role rules concept. Input are all current assigned roles's rules
* Designed for use in task detail.
*
* @author Vít Švanda
*/
const roleManager = new RoleManager();
const automaticRoleAttributeRuleManager = new AutomaticRoleAttributeRuleManager();

export class AutomaticRoleRuleTable extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      conceptData: {},
      showRoleCatalogue: false,
      filterOpened: this.props.filterOpened,
      detail: {
        show: false,
        entity: {},
        add: false
      }
    };
  }

  componentDidMount() {
    // We have to create concept from props here, because same instance this component
    //  could be used in past (in this case may be this.prosp and nextProps same)
    this._setConcept(this.props);
  }

  getContentKey() {
    return 'content.automaticRoleRequestDetail';
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps && (
      JSON.stringify(nextProps.currentData) !== JSON.stringify(this.props.currentData) ||
      JSON.stringify(nextProps.addedConcepts) !== JSON.stringify(this.props.addedConcepts) ||
      JSON.stringify(nextProps.removedConcepts) !== JSON.stringify(this.props.removedConcepts) ||
      JSON.stringify(nextProps.changedConcepts) !== JSON.stringify(this.props.changedConcepts)
    )) {
      this._setConcept(nextProps);
    }
  }

  /**
   * Set input arrays (with current, added, removed, changed data) to state (first do clone) and call compile conceptData
   * @param  {array}  currentData         Original not modified data
   * @param  {array}  addedConcepts    Added data
   * @param  {array}  removedConcepts  Removed data (ids)
   * @param  {array}  changedConcepts} Changed data (every object in array contains only changed fields and ID)
   */
  _setConcept({ currentData, addedConcepts, removedConcepts, changedConcepts}) {
    this.setState({conceptData: this._compileConceptData({ currentData, addedConcepts, removedConcepts, changedConcepts})});
  }

  /**
   * Show modal dialog
   * @param  {Object}  entity           Entity show in dialog
   * @param  {Boolean} isEdit = false   If is false then form in dialog will be read only
   */
  _showDetail(entity, isEdit = false, multiAdd = false) {
    const entityFormData = _.merge({}, entity, {
      attributeName: entity.attributeName
    });

    this.setState({
      detail: {
        show: true,
        edit: isEdit && !entity.automaticRole,
        entity: entityFormData,
        add: multiAdd
      }
    }, () => {
      this.refs.detail.getForm().setData(entityFormData);
    });
  }

  /**
   * Close modal dialog
   */
  _closeDetail() {
    this.setState({
      showRoleCatalogue: false,
      detail: {
        ... this.state.detail,
        show: false,
        add: false
      }
    });
  }

  /**
   * Save added or changed entities to arrays and recompile concept data.
   */
  _saveConcept(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.detail.getForm().isFormValid()) {
      return;
    }
    const {createConceptFunc, updateConceptFunc} = this.props;
    const entity = this.refs.detail.getCompiledData();
    if (entity._added) {
      if (!entity.id) {
        const roleRule = _.merge({}, entity, {_added: true});
        roleRule._embedded = {};
        createConceptFunc(roleRule, 'ADD');
      } else {
        entity._embedded = {};
        updateConceptFunc(entity, 'ADD');
      }
    } else {
      const existedConcept = this._findChangedConceptByRuleId(entity.id);
      const changedConcept = _.merge({}, entity);
      changedConcept.rule = entity.id;
      // Id will sets only if concept for same rule exists
      changedConcept.id = existedConcept ? existedConcept.id : null;

      let changed = false;
      const formAttribute = this._findChange('formAttribute', entity);
      const attributeName = this._findChange('attributeName', entity);
      const value = this._findChange('value', entity);
      const type = this._findChange('type', entity);
      const comparison = this._findChange('comparison', entity);

      if (formAttribute.changed) {
        changedConcept.formAttribute = formAttribute.value;
        changed = true;
      }
      if (attributeName.changed) {
        changedConcept.attributeName = attributeName.value;
        changed = true;
      }
      if (type.changed) {
        changedConcept.type = type.value;
        changed = true;
      }
      if (value.changed) {
        changedConcept.value = value.value;
        changed = true;
      }
      if (comparison.changed) {
        changedConcept.comparison = comparison.value;
        changed = true;
      }

      if (changed && changedConcept) {
        if (changedConcept.id) {
          updateConceptFunc(changedConcept, 'UPDATE');
        } else {
          createConceptFunc(changedConcept, 'UPDATE');
        }
      }
    }
    this.setState({conceptData: this._compileConceptData(this.props)});
    this._closeDetail();
  }

  _findChangedConceptByRuleId(id) {
    const {changedConcepts} = this.props;
    for (const changedConcept of changedConcepts) {
      if (changedConcept.rule === id) {
        return changedConcept;
      }
    }
    return null;
  }

  /**
   * Return true, if in original data exists rule for given rule ID.
   */
  _existsEntityForThisId(id) {
    const {currentData} = this.props;
    if (!currentData) {
      return false;
    }
    for (const entity of currentData) {
      if (entity.rule === id) {
        return true;
      }
    }
    return false;
  }

  /**
   * Find and apply changes to changedConcept array
   */
  _findChange(property, entity) {
    const {conceptData} = this.state;
    const changedPropertyName = '_' + property + 'Changed';
    for (const concept of conceptData) {
      if (concept.id === entity.id) {
        let propertyWithNewValue = property;
        if (entity.hasOwnProperty(changedPropertyName)) {
          propertyWithNewValue = changedPropertyName;
        }
        if (entity[propertyWithNewValue] !== concept[propertyWithNewValue]) {
          return {changed: true, value: entity[propertyWithNewValue]};
        }
      }
    }
    return {changed: false};
  }

  /**
   * Delete operation for added (delete whole object from array added entities),
   * changed (delete changes ... object from array changed entities),
   * removed (remove id from array of deleted entities)
   */
  _deleteConcept(data) {
    this._internalDeleteConcept(data);
  }

  _internalDeleteConcept(data) {
    const {createConceptFunc, removeConceptFunc} = this.props;

    if (data._added) {
      removeConceptFunc(data);
    } else if (data._removed) {
      removeConceptFunc(data.id, 'REMOVE');
    } else if (data._changed) {
      removeConceptFunc(data.id, 'UPDATE');
    } else {
      const concept = _.merge({}, data);
      concept.rule = concept.id;
      concept.id = null;
      createConceptFunc(concept, 'REMOVE');
      return;
    }
  }

  /**
   * Create final data for concept table by input arrays
   * @param  {array}  currentData         Original not modified data
   * @param  {array}  addedConcepts    Added data
   * @param  {array}  removedConcepts  Removed data (ids)
   * @param  {array}  changedConcepts} Changed data (every object in array contains only changed fields and ID)
   * @return {array}  conceptData           Final data for concept table
   */
  _compileConceptData({ currentData, addedConcepts, removedConcepts, changedConcepts}) {
    let concepts = _.merge([], currentData);
    if (addedConcepts) {
      for (const addedConcept of addedConcepts) {
        addedConcept._added = true;
        // if (this._existsEntityForThisId(addedConcept.rule)) {
        //   concepts = _.concat(concepts, addedConcept);
        // }
      }
      concepts = _.concat(concepts, addedConcepts);
    }

    for (const concept of concepts) {
      if (removedConcepts && removedConcepts.includes(concept.id)) {
        concept._removed = true;
      }
      if (changedConcepts) {
        for (const changedConcept of changedConcepts) {
          if (changedConcept.rule === concept.id) {
            concept._changed = true;
            for (const property in changedConcept) {
              if (changedConcept.hasOwnProperty(property)) {
                const key = '_' + property + 'Changed';
                concept[key] = changedConcept[property];
              }
            }
          }
        }
      }
    }
    return concepts;
  }

  /**
   * Compute background color row (added, removed, changed)
   */
  _rowClass({rowIndex, data}) {
    if (data[rowIndex]._added) {
      return 'bg-success';
    }
    if (data[rowIndex]._removed) {
      return 'bg-danger';
    }
    if (data[rowIndex]._changed) {
      return 'bg-warning';
    }
    return null;
  }

  /**
   * Generate date cell for concept. If is entity changed, will use 'changed' property. In title show old value.
   */
  _conceptDateCell({rowIndex, data, property}) {
    const changedProperty = '_' + property + 'Changed';
    // Load old value and add to title
    const format = this.i18n('format.date');
    const propertyOldValue = Basic.Cell.getPropertyValue(data[rowIndex], property);
    const dataOldValue = propertyOldValue ? moment(propertyOldValue).format(format) : null;
    const oldValueMessage = dataOldValue ? this.i18n('oldValue', {oldValue: dataOldValue}) : this.i18n('oldValueNotExist');
    const changedPropertyExist = data[rowIndex].hasOwnProperty(changedProperty);
    return (
      <Basic.DateCell
        className={changedPropertyExist ? 'text-danger' : ''}
        property={changedPropertyExist ? changedProperty : property}
        rowIndex={rowIndex}
        data={data}
        title={changedPropertyExist ? oldValueMessage : null}
        format={format}/>
    );
  }

  /**
   * Create new IdentityRoleConcet with virtual ID (UUID)
   */
  _addConcept() {
    const newIdentityRoleConcept = {_added: true};
    this._showDetail(newIdentityRoleConcept, true, true);
  }

  /**
   * Method prefill roles to selectbox by folder (role catalogue)
   */
  _changeRoleCatalogue(catalogue, event) {
    if (event) {
      event.preventDefault();
    }
    //
    if (!catalogue) {
      this.refs.role.setValue([]);
      return;
    }
    //
    this.context.store.dispatch(
      roleManager.fetchEntities(
        roleManager.getDefaultSearchParameters().setFilter('roleCatalogue', catalogue.id), null,
          roles => {
            if (roles && roles._embedded) {
              const rolesToSet = [];
              for (let index = 0; index < roles._embedded.roles.length; index++) {
                const role = roles._embedded.roles[index];
                rolesToSet.push(role);
              }
              this.refs.role.setValue(rolesToSet);
            }
          }
        )
      );
  }

  /**
   * Generate cell with actions (buttons)
   */
  _conceptActionsCell({rowIndex, data}) {
    const {readOnly, showLoadingButtonRemove} = this.props;
    const actions = [];
    const value = data[rowIndex];
    const notModificated = !(value._added || value._removed || value._changed);
    const isAutomaticRole = value.automaticRole;

    actions.push(
      <Basic.Button
        level={'danger'}
        onClick={this._deleteConcept.bind(this, data[rowIndex])}
        className="btn-xs"
        disabled={readOnly || isAutomaticRole}
        showLoading={showLoadingButtonRemove}
        role="group"
        title={this.i18n('button.delete')}
        titlePlacement="bottom">
        <Basic.Icon icon={notModificated ? 'trash' : 'remove'}/>
      </Basic.Button>
    );
    if (!value._removed) {
      actions.push(
        <Basic.Button
          level={'warning'}
          onClick={this._showDetail.bind(this, data[rowIndex], true, false)}
          className="btn-xs"
          disabled={readOnly || isAutomaticRole || !notModificated}
          role="group"
          title={this.i18n('button.edit')}
          titlePlacement="bottom">
          <Basic.Icon icon={'edit'}/>
        </Basic.Button>
      );
    }
    return (
      <div className="btn-group" role="group">
        {actions}
      </div>
    );
  }

  render() {
    const { showLoading, readOnly, className } = this.props;
    const { conceptData, detail } = this.state;
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Toolbar>
          <div className="pull-right">
            <Basic.Button
              level="success"
              className="btn-xs"
              disabled={readOnly}
              onClick={this._addConcept.bind(this)}>
              <Basic.Icon value="fa:plus"/>
              {' '}
              {this.i18n('button.add')}
            </Basic.Button>
          </div>
          <div className="clearfix"></div>
        </Basic.Toolbar>
        <Basic.Table
          hover={false}
          showLoading={showLoading}
          data={conceptData}
          rowClass={this._rowClass}
          className={className}
          showRowSelection={false}
          noData={this.i18n('component.basic.Table.noData')}>
          <Basic.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this._showDetail.bind(this, data[rowIndex], !data[rowIndex]._removed, false)}/>
                );
              }
            }
            sort={false}/>
          <Basic.Column
            property="attributeName"
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                if (!entity) {
                  return '';
                }
                const attributeName = entity.attributeName ? entity.attributeName : entity._embedded.formAttribute.code;
                return attributeName;
              }
            }
            header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.attributeName')}
            sort/>
          <Basic.Column
            header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.formAttribute')}
            property="formAttribute"
            rendered={false}
            cell={
              ({ rowIndex, data }) => {
                const entity = data[rowIndex];
                if (!entity) {
                  return '';
                }
                const formAttribute = entity.formAttribute;
                return (
                  <Basic.Icon value={formAttribute ? 'fa:check-square-o' : 'fa:square-o'} disabled/>
                );
              }
            }
            />
          <Basic.Column
            header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.value.label')}
            property="value"
            />
          <Basic.Column
            header={this.i18n('entity.AutomaticRoleAttributeRuleRequest.comparison')}
            property="comparison"
            />
          <Basic.Column
            header={this.i18n('label.action')}
            className="action"
            cell={this._conceptActionsCell.bind(this)}/>
        </Basic.Table>

        <Basic.Modal
          bsSize="large"
          show={detail.show}
          onHide={this._closeDetail.bind(this)}
          backdrop="static"
          keyboard={!showLoading}>

          <form onSubmit={this._saveConcept.bind(this)}>
            <Basic.Modal.Header
              closeButton={!showLoading}
              text={this.i18n('create.header')}
              rendered={detail.entity.id === undefined}/>
            <Basic.Modal.Header
              closeButton={!showLoading}
              text={this.i18n('edit.header')}
              rendered={detail.entity.id !== undefined}/>
            <Basic.Modal.Body>
                <AutomaticRoleAttributeRuleDetail ref="detail" entity={detail.entity} manager={automaticRoleAttributeRuleManager} attributeId={detail.entity.id} />
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this._closeDetail.bind(this)}
                showLoading={showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>

              <Basic.Button
                type="submit"
                level="success"
                showLoading={showLoading}
                showLoadingIcon
                rendered={detail.edit}>
                {this.i18n('button.set')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

AutomaticRoleRuleTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  roleId: PropTypes.string.isRequired,
  className: PropTypes.string
};

AutomaticRoleRuleTable.defaultProps = {
  filterOpened: false,
  showLoading: false,
  showLoadingButtonRemove: false
};


export default AutomaticRoleRuleTable;
