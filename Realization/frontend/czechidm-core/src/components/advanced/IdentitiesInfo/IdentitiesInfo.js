import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import IdentityInfo from '../IdentityInfo/IdentityInfo';
import * as Basic from '../../basic';

/**
 * Cells for Candicates
 * maxEntry - max entry in candidates
 */

class IdentitiesInfo extends Basic.AbstractComponent {

  constructor(props, context) {
    super(props, context);
  }

  render() {
    const { identities, maxEntry, showOnlyUsername} = this.props;
    let candidatesResult;
    let isMoreResults = false;
    let infoCandidates = [];
    if (identities) {
      isMoreResults = identities.length > maxEntry;
      candidatesResult = _.uniq(identities);

      if (maxEntry !== undefined) {
        candidatesResult = _.slice(candidatesResult, 0, maxEntry);
      }
      for (const candidate of candidatesResult) {
        infoCandidates.push(<IdentityInfo key={candidate} entityIdentifier={candidate} face="popover" showOnlyUsername={showOnlyUsername} />);
        infoCandidates.push(', ');
      }

      infoCandidates = _.slice(infoCandidates, 0, infoCandidates.length - 1);
    }

    return (
      <span>
        {infoCandidates}
        {
          !isMoreResults
          ||
          ', ...'
        }
      </span>
    );
  }
}

IdentitiesInfo.propTypes = {
  identities: PropTypes.array,
  maxEntry: PropTypes.number,
  showOnlyUsername: PropTypes.bool
};

IdentitiesInfo.defaultProps = {
  showOnlyUsername: true,
  maxEntry: 2
};

export default IdentitiesInfo;
