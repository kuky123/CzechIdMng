import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
//
import * as Basic from '../../basic';
import * as Domain from '../../../domain';
import { TreeNodeManager, TreeTypeManager } from '../../../redux';
import EntitySelectBox from '../EntitySelectBox/EntitySelectBox';
import Tree from '../Tree/Tree';

const treeTypeManager = new TreeTypeManager(); // default manager in manager in props is not given
const treeNodeManager = new TreeNodeManager(); // default manager in manager in props is not given

/**
* Select tree node
*
* TODO: multi select (see role select)
* TODO: onChange support
*
* @author Radek Tomiška
*/
export default class TreeNodeSelect extends Basic.AbstractFormComponent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      ...this.state,
      defaultTreeType: props.defaultTreeType,
      treeTypeId: null,
      selectedTreeType: null, // modal
      selectedTreeNodeId: null, // modal
      showTree: false
    };
  }

  componentDidMount() {
    super.componentDidMount();
    //
    if (!this.props.defaultTreeType) {
      // fetch default tree type - will be used by default
      // lookout: component doesn't support redux select => class is generalized
      this.context.store.dispatch(this.getTreeTypeManager().fetchDefaultTreeType((defaultTreeType, error) => {
        if (error) {
          this.addError(error);
        } else {
          this.setState({
            defaultTreeType
          });
        }
      }));
    }
  }

  getComponentKey() {
    return 'component.advanced.Tree';
  }

  getManager() {
    return this.props.manager;
  }

  getTreeTypeManager() {
    return this.props.treeTypeManager;
  }

  getUiKey() {
    return this.props.uiKey;
  }

  getValue() {
    return this.refs.treeNode.getValue();
  }

  setValue(value, cb) {
    this.refs.treeNode.setValue(value, cb);
  }

  isValid() {
    return this.refs.treeNode.isValid();
  }

  validate(showValidationError, cb) {
    const { readOnly, rendered } = this.props;
    //
    if (readOnly || !rendered) {
      return true;
    }
    return this.refs.treeNode.validate(true, cb);
  }

  setState(json, cb) {
    super.setState(json, () => {
      // FIXME: abstract form component everride standard state to show validations => we need to propage this state into component
      if (json && json.showValidationError !== undefined) {
        this.refs.treeNode.setState({ showValidationError: json.showValidationError}, cb);
      } else if (cb) {
        cb();
      }
    });
  }

  /**
   * Focus input field
   */
  focus() {
    this.refs.treeNode.focus();
  }

  /**
   * Set selected tree tree tree
   *
   * @param {TreeType} treeType
   */
  setTreeType(treeType) {
    this.setState({
      treeTypeId: treeType ? treeType.id : null,
      selectedTreeType: treeType
    }, () => {
      if (this.refs.treeType) {
        this.refs.treeType.setValue(treeType);
      }
    });
  }

  /**
   * Type on form
   *
   * @param  {TreeType} treeType
   */
  onChangeTreeType(treeType) {
    this.setState({
      treeTypeId: treeType ? treeType.id : null,
      selectedTreeType: treeType
    }, () => {
      this.refs.treeNode.setValue(null);
    });
  }

  /**
   * Type on modal
   *
   * @param  {TreeType} treeType
   */
  onChangeSelectedTreeType(treeType) {
    this.setState({
      selectedTreeType: treeType
    });
  }

  /**
   * Show modal with tree
   *
   * @param  {event} event
   */
  showTree(event) {
    if (event) {
      event.preventDefault();
    }
    //
    // load selected tree type, when no type is given.
    // TODO: this not works for form selectbox (find a better place to load tree type)
    const treeNodeId = this.refs.treeNode.getValue();
    if (treeNodeId && !this.state.selectedTreeType) {
      const treeNode = this.getManager().getEntity(this.context.store.getState(), treeNodeId);
      if (treeNode) {
        const treeType = this.getTreeTypeManager().getEntity(this.context.store.getState(), treeNode.treeType);
        if (!treeType) {
          this.setState({
            showTree: true,
            selectedTreeType: treeNode.treeType
          });
        } else {
          this.setState({
            showTree: true,
            selectedTreeType: this.getTreeTypeManager().getEntity(this.context.store.getState(), treeNode.treeType)
          });
        }
      } else {
        this.setState({
          showTree: true
        });
      }
    } else {
      this.setState({
        showTree: true
      });
    }
  }

  /**
   * Hide modal with tree
   *
   * @param  {event} event
   */
  hideTree(event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      showTree: false
    });
  }

  /**
   * Move selected type and node from tree to form
   *
   * @param  {event} event
   */
  onSelect(nodeId = null, event) {
    if (event) {
      event.preventDefault();
    }
    // selected type and node from tree to form
    const { selectedTreeType, selectedTreeNodeId } = this.state;
    const _nodeId = nodeId || selectedTreeNodeId;
    //
    this.setState({
      treeTypeId: selectedTreeType ? selectedTreeType.id : null,
    }, () => {
      if (this.refs.treeType) {
        // tree type could be hidden
        this.refs.treeType.setValue(selectedTreeType);
      }
      this.refs.treeNode.setValue(this.getManager().getEntity(this.context.store.getState(), _nodeId));
    });
    //
    this.hideTree();
  }

  /**
   * Tree node field label
   *
   * @return  {string}
   */
  getLabel() {
    const { label } = this.props;
    if (label !== undefined) {
      return label;
    }
    return this.i18n('entity.TreeNode._type');
  }

  /**
   * Select box field placeholder
   *
   * @return  {string}
   */
  getPlaceholder() {
    const { placeholder } = this.props;
    if (placeholder !== undefined) {
      return placeholder;
    }
    return null;
  }

  /**
   * Select box field help block
   *
   * @return  {string}
   */
  getHelpBlock() {
    const { helpBlock } = this.props;
    if (helpBlock !== undefined) {
      return helpBlock;
    }
    return null;
  }

  /**
   * Callback for select node in tree
   *
   * @param  {event} event
   */
  onModalSelect(treeNodeId, event) {
    if (event) {
      event.preventDefault();
    }
    this.setState({
      selectedTreeNodeId: treeNodeId
    });
  }

  /**
   * Tree (modal) header
   *
   * @return  {string}
   */
  getHeader() {
    const { header } = this.props;
    //
    if (header) {
      return header;
    }
    return this.i18n('header');
  }

  _renderShowTreeIcon() {
    const { showTreeType, readOnly } = this.props;
    //
    return (
      <div>
        <Basic.LabelWrapper label={ showTreeType || this.getLabel() ? (<span style={{ visibility: 'hidden' }}>T</span>) : null }>
          <Basic.Button
            level="default"
            icon="fa:folder-open"
            style={{ marginLeft: 5 }}
            onClick={ this.showTree.bind(this) }
            title={ this.i18n('showTree.link.title') }
            titlePlacement="bottom"
            disabled={ readOnly }/>
        </Basic.LabelWrapper>
      </div>
    );
  }

  render() {
    const {
      rendered,
      useFirstType,
      showTreeType,
      forceSearchParameters,
      required,
      readOnly,
      value,
      hidden
    } = this.props;
    const { treeTypeId, showTree, selectedTreeNodeId, defaultTreeType } = this.state;
    //
    // resolve selected tree type from given tree node
    const selectedTreeType = this.state.selectedTreeType || defaultTreeType;
    //
    let _forceTreeType = false;
    if (forceSearchParameters && forceSearchParameters.getFilters().has('treeTypeId')) {
      _forceTreeType = true;
    }
    //
    let formForceSearchParameters = forceSearchParameters || new Domain.SearchParameters();
    if (_forceTreeType) {
      // force tree type is already given => cannot be changed
    } else if (treeTypeId) {
      formForceSearchParameters = formForceSearchParameters.setFilter('treeTypeId', treeTypeId);
    } else if (defaultTreeType) {
      formForceSearchParameters = formForceSearchParameters.setFilter('treeTypeId', defaultTreeType.id);
    } if (showTreeType) {
      formForceSearchParameters = formForceSearchParameters.setFilter('treeTypeId', Domain.SearchParameters.BLANK_UUID);
    }
    let modalForceSearchParameters = new Domain.SearchParameters();
    if (_forceTreeType) {
      modalForceSearchParameters = modalForceSearchParameters.setFilter('treeTypeId', forceSearchParameters.getFilters().get('treeTypeId'));
    } else {
      let selectedTreeTypeId = Domain.SearchParameters.BLANK_UUID;
      if (selectedTreeType) {
        if (_.isObject(selectedTreeType)) {
          selectedTreeTypeId = selectedTreeType.id;
        } else {
          selectedTreeTypeId = selectedTreeType;
        }
      }
      modalForceSearchParameters = modalForceSearchParameters.setFilter('treeTypeId', selectedTreeTypeId);
    }
    //
    if (!rendered) {
      return null;
    }
    //
    return (
      <span className={ classNames({ hidden }) }>
        {
          !showTreeType || _forceTreeType
          ||
          <div style={{ display: 'flex' }}>
            <div style={{ flex: 1 }}>
              <EntitySelectBox
                entityType="treeType"
                ref="treeType"
                manager={ this.getTreeTypeManager() }
                label={ this.i18n('entity.TreeNode.treeType.label') }
                onChange={ this.onChangeTreeType.bind(this) }
                useFirst={ useFirstType && !selectedTreeType }
                required={ required }
                readOnly={ readOnly }/>
            </div>
            { this._renderShowTreeIcon() }
          </div>
        }

        <div style={ showTreeType ? {} : { display: 'flex' } }>
          <div style={ showTreeType ? {} : { flex: 1 } }>
            <EntitySelectBox
              entityType="treeNode"
              ref="treeNode"
              manager={ this.getManager() }
              label={ this.getLabel() }
              placeholder={ this.getPlaceholder() }
              helpBlock={ this.getHelpBlock() }
              forceSearchParameters={ formForceSearchParameters }
              readOnly={ readOnly || (treeTypeId === null && showTreeType) }
              required={ required }
              value={ value }/>
          </div>
          {
            showTreeType
            ||
            this._renderShowTreeIcon()
          }
        </div>

        <Basic.Modal
          show={ showTree }
          onHide={ this.hideTree.bind(this) }
          backdrop="static"
          keyboard>
          <Basic.Modal.Header text={ this.getHeader() } closeButton/>
          <Basic.Modal.Body style={{ padding: 0 }}>
            <Tree
              ref="organizationTree"
              uiKey={ this.getUiKey() }
              manager={ this.getManager() }
              forceSearchParameters={ modalForceSearchParameters }
              ŕendered={ showTree }
              traverse
              header={
                _forceTreeType
                ?
                null
                :
                <EntitySelectBox
                  entityType="treeType"
                  useFirst={ useFirstType && !selectedTreeType }
                  ref="selectedTreeType"
                  value={ selectedTreeType ? selectedTreeType : null }
                  manager={ this.getTreeTypeManager() }
                  onChange={ this.onChangeSelectedTreeType.bind(this) }
                  clearable={ false }
                  className="small"
                  style={{ marginBottom: 0 }}/>
              }
              onSelect={ this.onModalSelect.bind(this) }
              onDoubleClick={ (nodeId) => this.onSelect(nodeId) }
              bodyStyle={{ overflowY: 'auto', maxHeight: 450 }}
              />
          </Basic.Modal.Body>
          <Basic.Modal.Footer>
            <Basic.Button
              level="link"
              onClick={ this.hideTree.bind(this) }>
              { this.i18n('button.cancel') }
            </Basic.Button>

            <Basic.Button
              level="success"
              showLoadingIcon
              onClick={ this.onSelect.bind(this, null) }
              disabled={ selectedTreeNodeId === null ? true : false }>
              {this.i18n('button.select')}
            </Basic.Button>
          </Basic.Modal.Footer>
        </Basic.Modal>
      </span>
    );
  }
}

TreeNodeSelect.propTypes = {
  ...Basic.AbstractFormComponent.propTypes,
  /**
   * Ui key - identifier for loading data
   */
  uiKey: PropTypes.string,
  /**
   * TreeNodeManager instance - manager controls fetching data etc. Default maganer is constructed if no manager is given.
   */
  manager: PropTypes.object,
  /**
   * TreeTypeManager instance - manager controls fetching tree types, default tree type etc. Default maganer is constructed if no manager is given.
   */
  treeTypeManager: PropTypes.object,
  /**
   * Defautl tree type - if no default tree type is defined, then:
   * - all nodes will be filtered together in the form select box
   * - the first tree type will be preset, when modal tree is shown
   * @type {[type]}
   */
  defaultTreeType: PropTypes.object,
  /**
   * Use the first searched value, if value is empty
   */
  useFirstType: PropTypes.bool,
  /**
   * Tree node selectbox label
   */
  label: PropTypes.string,
  /**
   * Tree node selectbox placeholder
   */
  placeholder: PropTypes.string,
  /**
   * Tree node selectbox help block
   */
  helpBlock: PropTypes.string,
  /**
   * Tree (modal) header
   */
  header: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.element
  ]),
  /**
   * Show tree type select - form tree type select will be hidden.
   * Could be orchestrated with force search parameters with tree type is filled.
   */
  showTreeType: PropTypes.bool,
  /**
   * "Hard filters".
   */
  forceSearchParameters: PropTypes.object
};

TreeNodeSelect.defaultProps = {
  ...Basic.AbstractFormComponent.defaultProps,
  manager: treeNodeManager,
  treeTypeManager,
  uiKey: 'tree-node-tree',
  defaultTreeType: null,
  useFirstType: true,
  showTreeType: false
};
