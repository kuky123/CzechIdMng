import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';

/**
 * WF buttons
 *
 * @author Vít Švanda
 */
class DecisionButtons extends Basic.AbstractContent {

  constructor(props) {
    super(props);
    this.state = {
      showLoading: props.showLoading,
      canGoBack: false
    };
  }

  componentDidMount() {
  }

  // TODO: workaroud - update component from email is not called
  componentWillReceiveProps(nextProps) {
    if (nextProps) {
      this.setState({
        canGoBack: true
      });
    }
  }

  getContentKey() {
    return 'content.task.DecisionButton';
  }

  _onClick(decision) {
    this.props.onClick(decision);
  }

  _getDecisionsButton(decisions, showLoading) {
    const { readOnly } = this.props;
    const buttons = [];
    if (readOnly) {
      return buttons;
    }
    for (const decision of decisions) {
      buttons.push(
                    <Basic.Button
                      type="button"
                      level={decision.level}
                      onClick={this._onClick.bind(this, decision)}
                      showLoading={showLoading}
                      tooltip={this.i18n(decision.tooltip ? decision.tooltip : `wf.decision.${decision.id}.tooltip` )}>
                        {this.i18n(decision.label ? decision.label : `wf.decision.${decision.id}.label` )}
                    </Basic.Button>
                  );
      buttons.push(' ');
    }
    return buttons;
  }

  _goBack() {
    const { task } = this.props;
    const { canGoBack } = this.state;
    if (canGoBack) {
      this.context.router.goBack();
    } else {
      // transmition to /task, history doesnt exist, for now transmition to identity task
      this.context.router.push(`tasks/identity/${task.variables.implementerIdentifier}`);
    }
  }

  render() {
    const {task, showLoading, showBackButton} = this.props;
    let decisions;
    if (task) {
      decisions = task.decisions;
    }

    return (
        <span>
          {decisions ?
            <span>
              <Basic.Button
                rendered={showBackButton}
                type="button"
                level="link"
                onClick={this._goBack.bind(this)}
                showLoading={showLoading}>{this.i18n('button.back')}
              </Basic.Button>
              {this._getDecisionsButton(decisions, showLoading)}
            </span>
          :
          <Basic.Well showLoading/>
          }
        </span>
    );
  }
}

DecisionButtons.propTypes = {
  task: PropTypes.object,
  readOnly: PropTypes.bool,
  showBackButton: PropTypes.bool
};

DecisionButtons.defaultProps = {
  task: null,
  readOnly: false,
  showBackButton: true
};

function select() {
  return {};
}

export default connect(select)(DecisionButtons);
