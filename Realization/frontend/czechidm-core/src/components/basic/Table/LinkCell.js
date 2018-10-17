import React from 'react';
import { Link } from 'react-router';
import _ from 'lodash';
import Immutable from 'immutable';
//
import { SecurityManager } from '../../../redux';
import DefaultCell from './DefaultCell';
import Popover from '../Popover/Popover';
import Button from '../Button/Button';
// TODO: Localization service could not be accessed directly (advadced component)
import { LocalizationService } from '../../../services';

const TARGET_PARAMETER = '_target';

/**
 * Fills href parameter values from ginen rowData / entity object
 *
 * @param  {string} to href
 * @param  {object} rowData entity
 * @return {string} formated href
 */
function _resolveToWithParameters(to, rowData, target) {
  // const parameterNames = getParamNames(to);
  // let parameterValues = new Immutable.Map({});
  // parameterNames.map(parameter => {
  //   if (parameter === TARGET_PARAMETER && target) {
  //     parameterValues = parameterValues.set(parameter, DefaultCell.getPropertyValue(rowData, target));
  //   } else {
  //     parameterValues = parameterValues.set(parameter, DefaultCell.getPropertyValue(rowData, parameter));
  //   }
  // });
  // return formatPattern(to, parameterValues.toJS());
}

function _linkFunction(to, rowIndex, data, event) {
  if (event) {
    event.preventDefault();
  }
  if (to) {
    to({ rowIndex, data });
  }
}

/**
 * Renders cell with link and text content.
 * Parametrs are automatically propagated from table / row / column

 * @param number rowIndex
 * @param array[json] input data
 * @param property column key
 * @param to - router link
 * @param className className
 * @param title - html title
 * @param target - optional entity property could be used as `_target` property in `to` property.
 * @param access - link could be accessed, if current user has access to target agenda. Otherwise propertyValue without link is rendered.
 * @param props other optional properties
 */
const LinkCell = ({rowIndex, data, property, to, className, title, target, access, ...props}) => {
  const propertyValue = DefaultCell.getPropertyValue(data[rowIndex], property);
  const accessItems = (access && !Array.isArray(access)) ? [access] : access;
  // when is property and accessItems null, then return only default cell
  if (!propertyValue) {
    return <DefaultCell {...props}/>;
  }
  return (
    <DefaultCell {...props}>
      {
        (accessItems && !SecurityManager.hasAccess(accessItems))
        ?
        <span>
          {
            SecurityManager.isDenyAll(accessItems)
            ?
            propertyValue
            :
            <Popover
              level="warning"
              title={LocalizationService.i18n('security.access.link.denied')}
              value={
                <span>
                  {
                    accessItems.map((accessItem) => {
                      if (SecurityManager.hasAccess(accessItem)) {
                        return null;
                      }
                      return (
                        <div>
                          {/* TODO: make appropriate enum and refactor security service etc. */}
                          <strong>{ LocalizationService.i18n(`enums.AccessTypeEnum.${accessItem.type}`) }</strong>
                          {
                            !accessItem.authorities
                            ||
                            <span>
                              : <div>{ accessItem.authorities.join(', ') }</div>
                            </span>
                          }
                        </div>
                      );
                    })
                  }
                </span>
              }>
              {
                <Button level="link" style={{ padding: 0 }}>{ propertyValue }</Button>
              }
            </Popover>
          }
        </span>
        :
        <span>
          {
            _.isFunction(to)
            ?
            <a href="#" onClick={_linkFunction.bind(this, to, rowIndex, data)}>{propertyValue}</a>
            :
            <Link to={_resolveToWithParameters(to, data[rowIndex], target)} title={title} className={className}>
              {propertyValue}
            </Link>
          }
        </span>
      }
    </DefaultCell>
  );
};

export default LinkCell;
