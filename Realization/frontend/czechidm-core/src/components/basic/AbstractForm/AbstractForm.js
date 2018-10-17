
import React from 'react';
import PropTypes from 'prop-types';
import AbstractContextComponent from '../AbstractContextComponent/AbstractContextComponent';
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import merge from 'object-assign';
import ApiOperationTypeEnum from '../../../enums/ApiOperationTypeEnum';
import Loading from '../Loading/Loading';
import _ from 'lodash';

/**
 * @author Vít Švanda
 */
class AbstractForm extends AbstractContextComponent {

  constructor(props, context) {
    super(props, context);
    this.findFormComponentsKeys = this.findFormComponentsKeys.bind(this);
    const keys = [];
    // We need find our form components keys (only ReactElement) in form
    this.findFormComponentsKeys(this.props.children, keys, this);
    const mode = this.props.mode ? this.props.mode : ApiOperationTypeEnum.UPDATE;
    const { showLoading } = props;
    this.state = { mode, showLoading: (showLoading != null ? showLoading : true), componentsKeys: keys};
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.children !== this.props.children) {
      const {componentsKeys} = this.state;
      const newComponentKeys = [];
      // We need find our form components keys and compare they with previous keys.
      // If keys (component) changed, then we set new keys and set data.
      this.findFormComponentsKeys(this.props.children, newComponentKeys, this);
      if (!(_.isEqual(componentsKeys ? componentsKeys.sort() : [], newComponentKeys.sort()))) {
        this.setState({componentsKeys: newComponentKeys}, () => {
          this.setData(nextProps.data);
        });
      }
    }
    if (nextProps.readOnly !== this.props.readOnly) {
      this.setReadOnly(nextProps.readOnly);
    }
    if (nextProps.disabled !== this.props.disabled) {
      this.setDisabled(nextProps.disabled);
    }
    if (nextProps.showLoading !== this.props.showLoading) {
      this.setState({showLoading: nextProps.showLoading});
    }
    if (nextProps.data !== this.props.data) {
      this.setData(nextProps.data);
    }
  }

  componentDidMount() {
    const {readOnly, data, disabled} = this.props;
    this.setReadOnly(readOnly);
    this.setDisabled(disabled);
    this.setData(data);
  }

  /**
  * Find all form-components keys and add them to componentsKey array;
  */
  findFormComponentsKeys(children, keys, basicForm) {
    React.Children.map(children, function findChildKey(child) {
      if (!child) {
        return null;
      }
     // We will add only AbstractFormComponent
      if (child.type && child.type.prototype instanceof AbstractFormComponent) {
        if (child.ref && (!child.props || !child.props.notControlled)) {
          keys.push(child.ref);
        }
        return null;
      }
      if (child.props && child.props.children && !(child.type && child.type === AbstractForm)) {
        basicForm.findFormComponentsKeys(child.props.children, keys, basicForm);
        return null;
      }
    });
    return null;
  }

  getFooter() {
    // some footer elements. Is override in childe (etc. BasicForm)
  }

  // method for handle json to state
  setData(json, error, operationType) {
    if (error) {
      this.processEnded(error, operationType);
      return;
    }
    if (!this.props.rendered) {
      return;
    }
    this.setState({allData: merge({}, json)});
    if (json) {
      for (const componentRef in this.state.componentsKeys) {
        if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
          continue;
        }
        const key = this.state.componentsKeys[componentRef];
        const component = this.getComponent(key);
        if (!component) {
          // component could not be rendered
          continue;
        }
        if (json.hasOwnProperty(key)) {
          const value = json[key];
          // set new value to component
          component.setValue(value);
        } else {
          component.setValue(null);
        }
      }
    }
    this.processEnded(null, operationType);
  }

  isFormValid() {
    for (const componentRef in this.state.componentsKeys) {
      if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
        continue;
      }
      if (!this.props.rendered) {
        return true;
      }
      const component = this.getComponent(this.state.componentsKeys[componentRef]);
      if (!component.isValid()) {
        this.showValidationError(true);
        // focus invalid component
        component.focus();
        return false;
      }
    }
    return true;
  }

  showValidationError(show) {
    for (const componentRef in this.state.componentsKeys) {
      if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
        continue;
      }
      const component = this.getComponent(this.state.componentsKeys[componentRef]);
      component.setState({showValidationError: show});
    }
  }


  /**
   * Method for compile data from all component
   * @param  mergeToAll = If is true, then will be returned whole object with
   * merged fields by components in form.
   * If is false, then will be returned only object with fields by components in form.
   */
  getData(mergeToAll = true) {
    let result = null;
    if (mergeToAll) {
      result = merge({}, this.state.allData);
    } else {
      result = {};
    }
    if (!this.props.rendered) {
      return result;
    }
    for (const componentRef in this.state.componentsKeys) {
      if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
        continue;
      }
      const key = this.state.componentsKeys[componentRef];
      const component = this.getComponent(this.state.componentsKeys[componentRef]);
      if (component) { // component cannot be rendered
        const componentValue = component.getValue();
        if (componentValue === undefined) {
          // undefined values are not sent (confidential properties etc.)
          delete result[key];
        } else {
          // merge new value
          merge(result, {
            [key]: componentValue
          });
        }
      }
    }
    return result;
  }

  /**
   * Return form component by key
   */
  getComponent(key) {
    if (this.state.componentsKeys) {
      let hasKey = false;
      for (const componentRef in this.state.componentsKeys) {
        if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
          continue;
        }
        const k = this.state.componentsKeys[componentRef];
        if (k === key) {
          hasKey = true;
        }
      }
      if (!hasKey) {
        return null;
      }
      // work around ... I need get instance of react component
      const ownerRefs = this._reactInternalInstance._currentElement._owner._instance.refs;
      return ownerRefs[key];
    }
    return null;
  }

  /**
   * Operation called after asynchrone operation with form is ended.
   * @param  {object} error
   * @param  {Symbol} operationType
   * @return void
   */
  processEnded(error, operationType) {
    if (error) {
      this.addError(error);
    } else if (ApiOperationTypeEnum.UPDATE === operationType) {
      this.addMessage({title: this.i18n('message.success.update'), level: 'success'});
    } else if (ApiOperationTypeEnum.CREATE === operationType) {
      this.addMessage({title: this.i18n('message.success.create'), level: 'success'});
    } else if (ApiOperationTypeEnum.DELETE === operationType) {
      this.addMessage({title: this.i18n('message.success.delete'), level: 'success'});
    }
    this.setState({showLoading: false});
    // Form can be enabled only if props.disabled is false
    this.setDisabled(false || this.props.disabled);
  }

  processStarted() {
    this.setState({showLoading: true});
    this.setDisabled(true);
  }

  setReadOnly(readOnly) {
    this.readOnlyComponents(readOnly);
  }

  setDisabled(disabled) {
    this.disableComponents(disabled);
  }

  readOnlyComponents(readOnly) {
    for (const componentRef in this.state.componentsKeys) {
      if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
        continue;
      }
      const component = this.getComponent(this.state.componentsKeys[componentRef]);
      if (!component || !this.props.rendered) {
        return;
      }
      if (component.props.readOnly === null || component.props.readOnly === false) {
        component.setState({readOnly, formReadOnly: readOnly});
      } else {
        component.setState({formReadOnly: readOnly});
      }
    }
  }

  disableComponents(disabled) {
    for (const componentRef in this.state.componentsKeys) {
      if (!this.state.componentsKeys.hasOwnProperty(componentRef)) {
        continue;
      }
      const component = this.getComponent(this.state.componentsKeys[componentRef]);
      if (!component || !this.props.rendered) {
        return;
      }
      if (component.props.disabled === null || component.props.disabled === false) {
        component.setState({disabled, formDisabled: disabled});
      } else {
        component.setState({formDisabled: disabled});
      }
    }
  }

  render() {
    const {rendered, className, showLoading, children, style} = this.props;
    if (!rendered) {
      return null;
    }
    return (
      <Loading showLoading={showLoading || this.state.showLoading}>{/* props.showLoading has higher priority */}
        {/* TODO: remove className defs? Somethimes different className e.g. form-inline is needed */}
        <div className={'abstract-form clearfix ' + className} style={style}>
          {children}
        </div>
        {this.getFooter()}
      </Loading>
    );
  }
}

AbstractForm.contextTypes = {
  ...AbstractContextComponent.contextTypes,
  // TODO: router is not used?
  router: PropTypes.object
};

AbstractForm.propTypes = {
  showLoading: PropTypes.bool,
  readOnly: PropTypes.bool,
  disabled: PropTypes.bool,
  data: PropTypes.object
};

AbstractForm.defaultProps = {
  showLoading: false,
  readOnly: false,
  disabled: false,
  rendered: true,
  data: null
};

export default AbstractForm;
