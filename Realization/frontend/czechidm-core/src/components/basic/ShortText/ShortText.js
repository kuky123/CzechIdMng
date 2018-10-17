import React from 'react';
import PropTypes from 'prop-types';

import UiUtils from '../../../utils/UiUtils';
import AbstractComponent from '../AbstractComponent/AbstractComponent';
import Tooltip from '../Tooltip/Tooltip';

/**
 * Simple component to make text shorter
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
class ShortText extends AbstractComponent {

  constructor(props) {
    super(props);
  }

  render() {
    const { text, value, maxLength, cutPointEnd, cutChar, className, rendered } = this.props;
    const _text = text || value;
    //
    if (!rendered || !_text) {
      return null;
    }
    let shortText = '';
    if (_text.length > maxLength) {
      if (cutPointEnd) {
        shortText = UiUtils.substringBegin(_text, maxLength, cutChar, '...');
      } else {
        shortText = UiUtils.substringEnd(_text, maxLength, cutChar, '...');
      }
    } else {
      shortText = _text;
    }
    return (
      <Tooltip value={ _text }>
        <span className={ className }>
          { shortText }
        </span>
      </Tooltip>
    );
  }
}

ShortText.propTypes = {
  /**
   * String to be shorten
   */
  text: PropTypes.string,
  /**
   * String to be shorten (text alias - text has higher priority)
   */
  value: PropTypes.string,
  /**
   * Number of lenght for string to be shorten to
   */
  maxLength: PropTypes.number,
  /**
   * Character acording which you cut
   */
  cutChar: PropTypes.string,
  /**
   * Boolean that indicates if you cut from begining or ending
   */
  cutPointEnd: PropTypes.bool
};

ShortText.defaultProps = {
  ...AbstractComponent.defaultProps,
  /**
   * default shorten value of maxLength
   */
  maxLength: 20,
  /**
   * default character for cutting
   */
  cutChar: ' ',
  /**
   * default point of cutting
   */
  cutPointEnd: true
};

export default ShortText;
