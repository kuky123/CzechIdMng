import React, { PropTypes } from 'react';
import classNames from 'classnames';
import Select from 'react-select';
import Joi from 'joi';
import _ from 'lodash';
import Waypoint from 'react-waypoint';
//
import Icon from '../Icon/Icon';
import Tooltip from '../Tooltip/Tooltip';
import FlashMessage from '../FlashMessages/FlashMessage';
import AbstractFormComponent from '../AbstractFormComponent/AbstractFormComponent';
import EntityManager from '../../../redux/data/EntityManager';
import SearchParameters from '../../../domain/SearchParameters';

const NICE_LABEL = 'niceLabel';
const ITEM_FULL_KEY = 'itemFullKey';
const ITEM_VALUE = 'value';

/**
 * A Select control - async loads option from BE.
 *
 * @author svanda
 * @author Radek Tomiška
 */
class SelectBox extends AbstractFormComponent {

  constructor(props) {
    super(props);
    this.getOptions = this.getOptions.bind(this);
    this.state = {
      ...this.state,
      options: [],
      error: null,
      actualPage: 0
    };
  }

  getComponentKey() {
    return 'component.basic.SelectBox';
  }

  componentWillReceiveProps(nextProps) {
    super.componentWillReceiveProps(nextProps);
    const { forceSearchParameters} = nextProps;
    if (!SearchParameters.is(forceSearchParameters, this.props.forceSearchParameters)) {
      this._initComponent(nextProps);
    }
  }

  // Did mount only call initComponent method
  componentDidMount() {
    super.componentDidMount();
    this._initComponent(this.props);
  }

  /**
   * Method for init component from didMount method and from willReceiveProps method
   * @param  {properties of component} props For didmount call is this.props for call from willReceiveProps is nextProps.
   */
  _initComponent(props) {
    // initialize value
    // We have to propagate actual forceSearchParameters (maybe from this.props, maybe from nextProps)
    const { useFirst, forceSearchParameters } = props;
    if (useFirst) {
      this.getOptions('', forceSearchParameters, true);
    }
  }

  getRequiredValidationSchema() {
    if (this.props.multiSelect === true) {
      return Joi.array().min(1).required();
    }
    return Joi.object().required();
  }

  /**
   * Merge hard and user deffined search parameters
   */
  _createSearchParameters(inputText, forceSearchParameters) {
    const { manager, pageSize, loadMoreContent } = this.props;
    // user input
    let searchParameters = manager.getDefaultSearchParameters().setFilter('text', inputText).setSize(pageSize || SearchParameters.getDefaultSize());
    if (manager.supportsAuthorization()) {
      searchParameters = searchParameters.setName(SearchParameters.NAME_AUTOCOMPLETE);
    }
    // hard filters
    let _forceSearchParameters = null;
    if (forceSearchParameters) {
      // we dont want override setted pagination
      _forceSearchParameters = forceSearchParameters.setSize(null).setPage(null);
    }
    // if set loadMoreContent it is neccessary override page
    if (loadMoreContent && forceSearchParameters && forceSearchParameters.getPage()) {
      _forceSearchParameters = _forceSearchParameters.setPage(forceSearchParameters.getPage());
    }
    return manager.mergeSearchParameters(searchParameters, _forceSearchParameters);
  }

  getOptions(input, forceSearchParameters, useFirst = false, addToEnd = false) {
    const { manager } = this.props;
    const { options } = this.state;
    const searchParameters = this._createSearchParameters(input, forceSearchParameters);
    const timeInMs = Date.now();
    // We create unique key for this call and save it to component state
    const uiKey = `${manager.getEntityType()}_${(timeInMs)}`;
    this.setState({
      isLoading: true,
      uiKeyCurrent: uiKey
    }, () => {
      this.context.store.dispatch(manager.fetchEntities(searchParameters, uiKey, (json, error, resultUiKey) => {
        if (!error) {
          // We confirm if is uiKey in result same as uiKey saved in component state
          // If is key different, then is result older and we don`t want him.
          if (resultUiKey !== this.state.uiKeyCurrent) {
            this.getLogger().debug(`[SelectBox]: Recieved data is too old, we will throw out data! [${resultUiKey}|${this.state.uiKeyCurrent}]`);
            this.setState({
              isLoading: false
            });
            return;
          }

          const result = json;
          let data = null;
          const results = result._embedded[manager.getCollectionType()];
          if (results) {
            for (const item in results) {
              if (!results.hasOwnProperty(item)) {
                continue;
              }
              this.itemRenderer(results[item], input);
              // use the first value
              if (this.state.value === null && useFirst) {
                this.onChange(results[item]);
              }
            }
            let finalOptions = result._embedded[manager.getCollectionType()];
            // if we want add options to end, create sum of options
            if (addToEnd) {
              // remove last options (info about size)
              const optionsWithoutInfoItem = _.dropRight(options);
              // final options is sum before options and current options
              finalOptions = _.concat(optionsWithoutInfoItem, result._embedded[manager.getCollectionType()]);
            }
            data = {
              options: finalOptions,
              complete: finalOptions.length >= result.page.totalElements,
            };
            if (!data.complete) {
              data.options.push({
                [NICE_LABEL]: <Waypoint onEnter={this._loadMoreContent.bind(this, input)}>{this.i18n('results', { escape: false, count: data.options.length, total: result.page.totalElements})}</Waypoint>,
                [ITEM_FULL_KEY]: input,
                disabled: true // info only
              });
            } else {
              data.options.push({
                [NICE_LABEL]: this.i18n('results', { escape: false, count: data.options.length, total: result.page.totalElements}),
                [ITEM_FULL_KEY]: input,
                disabled: true // info only
              });
            }
          }
          this.setState({
            options: data.options,
            isLoading: false
          });
        } else {
          this.setState({
            isLoading: false,
            error
          });
        }
      }));
    });
  }

  /**
   * Method load more content to options state and increment actualPage
   */
  _loadMoreContent(input = '') {
    const { actualPage } = this.state;
    const { pageSize, manager, useFirst, loadMoreContent, forceSearchParameters } = this.props;
    if (!loadMoreContent) {
      return;
    }
    const finalPageSize = pageSize || SearchParameters.getDefaultSize();
    // increment actualPage
    const newActualPage = actualPage + 1;
    // if exists force search parameters merge it
    let finalSearchParameters = manager.getDefaultSearchParameters().setSize(finalPageSize).setPage(newActualPage);
    if (forceSearchParameters) {
      finalSearchParameters = manager.mergeSearchParameters(forceSearchParameters, finalSearchParameters);
    }
    //
    this.getOptions(input, finalSearchParameters, useFirst, true);
    this.setState({
      actualPage: newActualPage
    });
  }

  getValue() {
    const { returnProperty } = this.props;
    //
    if (!this.state.value) {
      if (this.props.multiSelect === true) {
        return [];
      }
      return null;
    }
    // value is array ... multiselect
    if (this.state.value instanceof Array && this.props.multiSelect === true) {
      const copyValues = [];
      for (const item of this.state.value) {
        const copyValue = this._deletePrivateField(_.merge({}, item));
        if (returnProperty) {
          copyValues.push(copyValue[returnProperty]);
        } else {
          copyValues.push(copyValue);
        }
      }
      return copyValues;
    }
    // value is not array
    const copyValue = _.merge({}, this.state.value);
    this._deletePrivateField(copyValue);
    // result property value - if value is false, then whole object is returned
    if (returnProperty) {
      return copyValue[returnProperty];
    }
    return copyValue;
  }

  _deletePrivateField(item) {
    if (item.hasOwnProperty(NICE_LABEL)) {
      delete item[NICE_LABEL];
    }
    if (item.hasOwnProperty(ITEM_FULL_KEY)) {
      delete item[ITEM_FULL_KEY];
    }
    return item;
  }

  isValid() {
    if (this.state.isLoading === true) {
      // value still loading ... component is invalid
      return false;
    }
    return super.isValid();
  }

  normalizeValue(value) {
    const { manager, label } = this.props;
    //
    if (value) {
      // value is array ... multiselect
      if (value instanceof Array && this.props.multiSelect === true) {
        if (_.isEmpty(value)) {
          // nothing
        } else {
          let isError = false;
          const renderedValues = [];
          //
          for (const item of _.clone(value)) {
            if (item instanceof Object && !item.itemFullKey) {
              // value is object but doesn't have itemFullKey attribute
              this.itemRenderer(item, '');
            } else if (manager && ((typeof item === 'string') || (typeof item === 'number'))) {
              // value is string, we try load entity by id
              // TODO: wait for another loading get the entity
              // if (!manager.isShowLoading(this.context.store.getState(), null, item)) {
              this.setState({
                isLoading: true
                /* eslint-disable no-loop-func */
              }, () => {
                this.context.store.dispatch(manager.autocompleteEntityIfNeeded(item, null, (json, error) => {
                  if (!error) {
                    this.itemRenderer(json, '');
                    // add item to array
                    renderedValues.push(json);
                    if (renderedValues.length === value.length) { // posledni callback
                      if (!isError) {
                        this.setState({
                          value: renderedValues,
                          isLoading: false
                        }, this.validate);
                      } else {
                        this.setState({
                          value: null,
                          isLoading: false
                        });
                      }
                    }
                  } else {
                    isError = true;
                    renderedValues.push(item);
                    this.setState({
                      error
                    });
                  }
                }));
              });
              // }
            }
          }
        }
      } else if (value instanceof Object && !value.itemFullKey) {
        // value is object but doesn't have itemFullKey attribute
        this.itemRenderer(value, '');
        this.setState({isLoading: false});
      } else if (typeof value === 'string' || typeof value === 'number') {
        // value is string, we try load entity by id
        // TODO: wait for another loading get the entity
        // if (manager && !manager.isShowLoading(this.context.store.getState(), null, value)) {
        this.setState({
          isLoading: false
        }, () => {
          this.context.store.dispatch(manager.autocompleteEntityIfNeeded(value, null, (json, error) => {
            if (!error) {
              if (!json) {
                this.setState({
                  value: null,
                  isLoading: false,
                  error: {
                    title: label,
                    level: 'warning',
                    message: this.i18n('security.record.notFound')
                  }
                });
              } else {
                this.itemRenderer(json, '');
                this.setState({ value: json, isLoading: false }, this.validate);
              }
            } else {
              this.setState({
                value: null,
                isLoading: false,
                error
              });
            }
          }));
        });
        // }
      }
    }
    return value;
  }

  getFiltersDefiniton(input) {
    const filters = [];
    for (const field of this.props.searchInFields) {
      filters.push({ field, 'value': input });
    }
    return filters;
  }

  onChange(value) {
    let result = true;
    if (this.props.onChange) {
      result = this.props.onChange(value);
    }
    // if onChange listener returns false, then we can end
    if (result === false) {
      return;
    }
    this.setState({
      value
    }, () => {
      this.validate();
    });
  }

  focus() {
    this.refs.selectComponent.focus();
  }

  itemRenderer(item, input) {
    const { niceLabel } = this.props;

    let _niceLabel;
    if (niceLabel) {
      _niceLabel = niceLabel(item);
    } else {
      _niceLabel = this.props.manager.getNiceLabel(item);
    }

    const inputLower = input.toLowerCase();

    let itemFullKey = _niceLabel;
    if (inputLower) {
      if (_niceLabel !== null && !_niceLabel.toLowerCase().indexOf(inputLower) >= 0) {
        for (const field of this.props.searchInFields) {
          if (item[field] !== null && item[field].toLowerCase().indexOf(inputLower) >= 0) {
            itemFullKey = itemFullKey + ' (' + item[field] + ')';
            continue;
          }
        }
      }
    }

    _.merge(item, {[NICE_LABEL]: _niceLabel, [ITEM_FULL_KEY]: itemFullKey});
  }

  onInputChange(value) {
    // after change input set actualPage to zero, we want start from begin
    this.setState({
      actualPage: 0
    });
    this.getOptions(value, this.props.forceSearchParameters);
    return value;
  }

  getBody(feedback) {
    const { labelSpan, label, componentSpan, required } = this.props;
    const { value, disabled, isLoading, error } = this.state;
    //
    const labelClassName = classNames(labelSpan, 'control-label');
    //
    let showAsterix = false;
    if (required && !value) {
      showAsterix = true;
    }
    if (error) {
      return (
        <FlashMessage message={ this.flashMessagesManager.convertFromError(error) } className="no-margin" />
      );
    }
    //
    return (
      <div className={showAsterix ? 'has-feedback' : ''}>
        {
          !label
          ||
          <label
            className={labelClassName}>
            {label}
            { this.renderHelpIcon() }
          </label>
        }
        <div className={componentSpan}>
          {
            (disabled === true && this.props.multiSelect !== true)
            ?
            <div ref="selectComponent" className="form-text">
              { value ? value[this.props.fieldLabel] : null }
              <Icon type="fa" icon="refresh" className="icon-loading" rendered={ isLoading === true } showLoading/>
            </div>
            :
            <Tooltip ref="popover" placement={ this.getTitlePlacement() } value={ this.getTitle() }>
              <span>
                {this.getSelectComponent()}
                {
                  feedback != null
                  ?
                  feedback
                  :
                  <span>
                    {
                      showAsterix
                      ?
                      <span className="form-control-feedback" style={{ color: 'red', zIndex: 0 }}>*</span>
                      :
                      ''
                    }
                  </span>
                }
              </span>
            </Tooltip>
          }
          {this.props.children}
          { !label ? this.renderHelpIcon() : null }
          { this.renderHelpBlock() }
        </div>
      </div>
    );
  }

  getPlaceholder(placeholder) {
    if (placeholder !== null && placeholder !== undefined) {
      return placeholder;
    }
    // default placeholder
    return this.i18n('label.searchSelect', { defaultValue: 'Select or type to search ...' });
  }

  /**
   * Load first page on input is opened
   */
  onOpen() {
    // it is neccessary set actualPage to zero and remove all options, after open
    this.setState({
      actualPage: 0,
      options: []
    });
    // loads default first page.
    this.getOptions('', this.props.forceSearchParameters);
  }

  getSelectComponent() {
    const { placeholder, fieldLabel, multiSelect, clearable} = this.props;
    const { isLoading, options, readOnly, disabled, value } = this.state;
    //
    // from new version react-select is necessary turn off onBlurResetsInput and closeOnSelect
    // onBlurResetsInput made problems with submit form and focus
    return (
        <Select
          ref="selectComponent"
          isLoading={isLoading}
          value={value}
          onChange={this.onChange}
          disabled={readOnly || disabled}
          ignoreCase
          ignoreAccents={false}
          multi={ multiSelect }
          onValueClick={this.gotoContributor}
          valueKey={ITEM_FULL_KEY}
          labelKey={fieldLabel}
          onBlurResetsInput={false}
          closeOnSelect={ !multiSelect }
          noResultsText={this.i18n('component.basic.SelectBox.noResultsText')}
          placeholder={this.getPlaceholder(placeholder)}
          searchingText={this.i18n('component.basic.SelectBox.searchingText')}
          searchPromptText={this.i18n('component.basic.SelectBox.searchPromptText')}
          clearable={clearable}
          onInputChange={this.onInputChange.bind(this)}
          options={options}
          onOpen={ this.onOpen.bind(this) }/>
    );
  }
}

SelectBox.propTypes = {
  ...AbstractFormComponent.propTypes,
  placeholder: PropTypes.string,
  manager: PropTypes.instanceOf(EntityManager).isRequired,
  searchInFields: PropTypes.arrayOf(PropTypes.string).isRequired,
  fieldLabel: PropTypes.string,
  value: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.object),
    PropTypes.object,
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.string
  ]),
  multiSelect: PropTypes.bool,
  /**
   * "Hard filters"
   */
  forceSearchParameters: PropTypes.object,
  /**
  * If object is selected, then this property value will be returned. If value is false, then whole object is returned.
  */
  returnProperty: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.bool
  ]),
  /**
   * Selected options can be cleared
   */
  clearable: PropTypes.bool,
  /**
   * Function with transform label in select box
   */
  niceLabel: PropTypes.func,
  /**
   * Use the first searched value, if value is empty
   */
  useFirst: PropTypes.bool,
  /**
   * Search results page size
   * @see SearchParameters.getDefaultSize()
   * @see SearchParameters.MAX_SIZE
   */
  pageSize: PropTypes.number,
  /**
   * Boolean flag that call load automaticaly more content after reach last item in options
   * @type {[type]}
   */
  loadMoreContent: PropTypes.bool
};

SelectBox.defaultProps = {
  ...AbstractFormComponent.defaultProps,
  fieldLabel: NICE_LABEL,
  multiSelect: false,
  returnProperty: 'id',
  searchInFields: [],
  clearable: true,
  useFirst: false,
  pageSize: SearchParameters.getDefaultSize(),
  loadMoreContent: true
};

SelectBox.NICE_LABEL = NICE_LABEL;
SelectBox.ITEM_FULL_KEY = ITEM_FULL_KEY;
SelectBox.ITEM_VALUE = ITEM_VALUE;


export default SelectBox;
