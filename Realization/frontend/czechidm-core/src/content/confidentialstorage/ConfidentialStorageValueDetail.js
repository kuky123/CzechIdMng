import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { ConfidentialStorageValueManager } from '../../redux';

/**
 * Detail for confidential storage values
 * * owner id
 * * owner type class
 * * key
 * * value
 *
 * @author Patrik Stloukal
 */
export default class ConfidentialStorageValueDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.confidentialStorageValueManager = new ConfidentialStorageValueManager();
  }

  getContentKey() {
    return 'content.confidentialStorage';
  }

  componentDidMount() {
    const { entity } = this.props;
    this.selectNavigationItem('confidential-storage');
    this._initForm(entity);
  }

  /**
   * Method check if props in this component isn't different from new props.
   */
  componentWillReceiveProps(nextProps) {
    // check id of old and new entity
    if (nextProps.entity.id !== this.props.entity.id) {
      this._initForm(nextProps.entity);
    }
  }

  /**
   * Method for basic initial form
   */
  _initForm(entity) {
    if (entity !== undefined) {
      this.refs.form.setData(entity);
    }
  }

  render() {
    const { uiKey, entity } = this.props;
    //
    return (
      <Basic.Panel>
        <Basic.AbstractForm
          ref="form"
          uiKey={uiKey}
          readOnly
          style={{ padding: '15px 15px 0 15px' }}>
          <Basic.Row>
            <div className="col-lg-6">
              <Basic.TextField
                ref="ownerId"
                label={this.i18n('entity.ConfidentialStorageValue.ownerId')}
                max={255}/>
            </div>
            <div className="col-lg-6">
              <Basic.TextField
                ref="ownerType"
                label={this.i18n('entity.ConfidentialStorageValue.ownerType')}/>
            </div>
          </Basic.Row>
          <Basic.TextField ref="key" label={this.i18n('entity.ConfidentialStorageValue.key')} />
          <Basic.TextField ref="serializableValue" label={this.i18n('entity.ConfidentialStorageValue.serializableValue')} />
          <Basic.Row>
            <div className="col-lg-6">
              <Basic.LabelWrapper
                label={ this.i18n('entity.ConfidentialStorageValue.creator') }>
                <Advanced.EntityInfo
                  entityType="identity"
                  entityIdentifier={ entity !== null ? entity.creator : null }
                  face="popover"
                  ref="creator"/>
              </Basic.LabelWrapper>
            </div>
            <div className="col-lg-6">
              <Basic.DateTimePicker
                ref="created"
                readOnly
                label={this.i18n('entity.ConfidentialStorageValue.created')}
                timeFormat={ this.i18n('format.times') }/>
            </div>
          </Basic.Row>
        </Basic.AbstractForm>

        <Basic.PanelFooter>
          <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
        </Basic.PanelFooter>
      </Basic.Panel>
    );
  }
}

ConfidentialStorageValueDetail.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
};
ConfidentialStorageValueDetail.defaultProps = {
};
