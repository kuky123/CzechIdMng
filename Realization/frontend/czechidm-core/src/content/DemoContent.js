import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import faker from 'faker';
import moment from 'moment';
//
import * as Basic from '../components/basic';
import ApiOperationTypeEnum from '../enums/ApiOperationTypeEnum';
import { IdentityService } from '../services';

class DemoContent extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      loading: false
    };
    this.identityService = new IdentityService();
  }

  componentDidMount() {
    this.selectNavigationItem('home');
  }

  _showLoading() {
    this.setState({loading: true});
    setTimeout(() => this._hideLoading(), 3000);
  }

  _showPanelLoading() {
    this.setState({panelLoading: true});
    setTimeout(() => this._hidePanelLoading(), 3000);
  }

  _hideLoading() {
    this.addMessage({level: 'info', title: 'Operace dokončena', message: 'Nějaká operace trvající 4s doběhla ...'});
    this.setState({loading: false});
  }

  _hidePanelLoading() {
    this.addMessage({level: 'info', title: 'Operace dokončena', message: 'Nějaká operace trvající 4s doběhla ...'});
    this.setState({panelLoading: false});
  }

  render() {
    const entityId = 'admin';
    const randomNames = [];
    for (let i = 0; i < 10; i ++) {
      randomNames.push({
        'Termín': moment(faker.date.future()).format(this.i18n('format.date')),
        'Popis': faker.lorem.sentence()
        // Name: faker.name.findName(),
        // Email: faker.internet.email()
      });
    }
    return (
      <div>
        <Basic.Loading className="global" showLoading={this.state.loading}/>
        <div className="row">
          <div className="col-sm-6">
            <Basic.Panel>
              <Basic.PanelHeader text="Aktuality" help="/page#kotva" />
              <Basic.PanelBody>
                {faker.lorem.paragraphs()}<br />
                {faker.lorem.paragraphs()}
              </Basic.PanelBody>

              <Basic.PanelFooter>
                <Basic.Button
                  level="link"
                  onClick={this._showLoading.bind(this)}>
                  Global loading
                </Basic.Button>
                <Basic.Button
                  level="link"
                  onClick={this._showPanelLoading.bind(this)}>
                  Panel loading
                </Basic.Button>
                <Basic.Button
                  level="link"
                  onClick={this.addMessage.bind(this, {key: 'global-error', level: 'error', title: 'Fatal error', message: faker.lorem.paragraphs()})}>
                  Add message
                </Basic.Button>
                <Basic.Button
                  level="link"
                  onClick={this.hideAllMessages.bind(this)}>
                  Hide all messages
                </Basic.Button>
              </Basic.PanelFooter>

              <Basic.PanelHeader text="Changelog" />
              <Basic.PanelBody>
                {faker.lorem.paragraphs()}
              </Basic.PanelBody>
            </Basic.Panel>

            <Basic.Panel>
              <Basic.Loading showLoading className="static"/>
            </Basic.Panel>
          </div>

          <div className="col-sm-6">
            <Basic.Panel>
              <Basic.PanelHeader text="Moje úkoly" help="/page#kotva" />
              <Basic.Loading showLoading={this.state.panelLoading}>
                <Basic.Table data={randomNames}/>
                <Basic.PanelFooter>
                  <Basic.Button level="link" onClick={this.addMessage.bind(this, {})}>Link</Basic.Button>
                </Basic.PanelFooter>
              </Basic.Loading>
            </Basic.Panel>

            <Basic.Panel>
              <Basic.PanelHeader text="Selectbox" help="/to"/>
              <div>
                <div className="panel-body">
                  <Basic.PanelBody>
                    <Basic.SelectBox
                      ref="selectBox"
                      label="Select box"
                      service={this.identityService}
                      value = {entityId}
                      searchInFields={['lastName', 'name', 'email']}
                      placeholder="Vyberte uživatele ..."
                      multiSelect={false}
                      required/>

                    <Basic.SelectBox
                      ref="selectBoxMulti"
                      label="Select box multi"
                      service={this.identityService}
                      value = {[entityId]}
                      searchInFields={['lastName', 'name', 'email']}
                      placeholder="Vyberte uživatele ..."
                      multiSelect
                      required/>

                    <Basic.EnumSelectBox
                      ref="enumSelectBox"
                      label="Enum select"
                      placeholder="Vyberte enumeraci ..."
                      multiSelect={false}
                      value={ApiOperationTypeEnum.DELETE}
                      enum={ApiOperationTypeEnum}
                    />

                    <Basic.EnumSelectBox
                      ref="enumSelectBoxMulti"
                      label="Enum select multi"
                      placeholder="Vyberte enumeraci ..."
                      multiSelect
                      value={[ApiOperationTypeEnum.DELETE, ApiOperationTypeEnum.CREATE, ApiOperationTypeEnum.UPDATE, ApiOperationTypeEnum.GET]}
                      enum={ApiOperationTypeEnum}
                      required/>

                    <Basic.EnumSelectBox
                      ref="anySelectBoxMulti"
                      label="Any select multi"
                      placeholder="Vyberte něco ..."
                      multiSelect
                      value={['item1', 'item2']}
                      options={[{value: 'item1', niceLabel: 'NiceItem1'}, {value: 'item2', niceLabel: 'NiceItem2'}, {value: 'item3', niceLabel: 'NiceItem3'}]}
                      required/>

                  </Basic.PanelBody>
                </div>
              </div>
            </Basic.Panel>
          </div>
        </div>

        <Basic.Panel>
          <Basic.Loading showLoading className="static"/>
        </Basic.Panel>

        <Basic.Panel>
          <Basic.Loading showLoading className="static"/>
        </Basic.Panel>
      </div>
    );
  }
}

DemoContent.propTypes = {
  userContext: PropTypes.object
};

DemoContent.defaultProps = {
  userContext: null
};

function select(state) {
  return {
    userContext: state.security.userContext
  };
}

export default connect(select)(DemoContent);
