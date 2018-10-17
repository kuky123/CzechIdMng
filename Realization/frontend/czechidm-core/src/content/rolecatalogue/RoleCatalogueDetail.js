import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import * as Basic from '../../components/basic';
import * as Utils from '../../utils';
import { RoleCatalogueManager } from '../../redux';
//
const manager = new RoleCatalogueManager();

/**
* Role catalogue detail.
* Combined node detail and role detail.
*
* @author Odřej Kopr
* @author Radek Tomiška
*/
class RoleCatalogueDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      showLoading: false,
    };
  }

  getContentKey() {
    return 'content.roleCatalogues';
  }

  componentDidMount() {
    const { entity } = this.props;
    if (entity !== undefined) {
      this.refs.form.setData(entity);
      this.refs.code.focus();
    }
  }

  save(event) {
    const { uiKey } = this.props;

    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }

    this.setState({
      showLoading: true
    }, this.refs.form.processStarted());
    //
    const entity = this.refs.form.getData();
    if (entity.id === undefined) {
      this.context.store.dispatch(manager.createEntity(entity, `${uiKey}-detail`, (createdEntity, error) => {
        this._afterSave(createdEntity, error);
      }));
    } else {
      this.context.store.dispatch(manager.updateEntity(entity, `${uiKey}-detail`, this._afterSave.bind(this)));
    }
  }

  /**
  * Just set showloading to false and set processEnded to form.
  * Call after save/create
  */
  _afterSave(entity, error) {
    if (error) {
      this.setState({
        showLoading: false
      }, this.refs.form.processEnded());
      this.addError(error);
      return;
    }
    this.context.store.dispatch(manager.clearEntities());
    this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
    this.context.router.replace(`role-catalogues`);
  }

  render() {
    const { uiKey, entity, _permissions } = this.props;
    const { showLoading } = this.state;
    //
    return (
      <div>
        <Helmet title={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('edit.title')} />

        <form onSubmit={ this.save.bind(this) }>
          <Basic.Panel className={ Utils.Entity.isNew(entity) ? '' : 'no-border last' }>
            <Basic.PanelHeader text={ Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('tabs.basic') } />

            <Basic.PanelBody style={ Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 } }>
              <Basic.AbstractForm
                showLoading={ showLoading }
                ref="form"
                uiKey={ uiKey }
                readOnly={ !manager.canSave(entity, _permissions) }>
                <Basic.Row>
                  <Basic.Col lg={ 2 }>
                    <Basic.TextField
                      ref="code"
                      label={this.i18n('entity.RoleCatalogue.code.name')}
                      helpBlock={this.i18n('entity.RoleCatalogue.code.help')}
                      required
                      min={0}
                      max={255}/>
                  </Basic.Col>
                  <Basic.Col lg={ 10 }>
                    <Basic.TextField
                      ref="name"
                      label={this.i18n('entity.RoleCatalogue.name.name')}
                      helpBlock={this.i18n('entity.RoleCatalogue.name.help')}
                      required
                      min={0}
                      max={255}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.Row>
                  <Basic.Col lg={ 4 }>
                    <Basic.TextField
                      ref="urlTitle"
                      label={this.i18n('entity.RoleCatalogue.urlTitle')}/>
                  </Basic.Col>
                  <Basic.Col lg={ 8 }>
                    <Basic.TextField
                      ref="url"
                      label={this.i18n('entity.RoleCatalogue.url')}/>
                  </Basic.Col>
                </Basic.Row>
                <Basic.SelectBox
                  ref="parent"
                  label={this.i18n('entity.RoleCatalogue.parent.name')}
                  manager={manager}/>
                <Basic.TextArea
                  ref="description"
                  label={this.i18n('entity.RoleCatalogue.description')}
                  max={255}/>
              </Basic.AbstractForm>
            </Basic.PanelBody>

            <Basic.PanelFooter showLoading={showLoading}>
              <Basic.Button type="button" level="link" onClick={this.context.router.goBack}>{this.i18n('button.back')}</Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoadingIcon
                showLoadingText={ this.i18n('button.saving') }
                rendered={ manager.canSave(entity, _permissions) }>
                { this.i18n('button.save') }
              </Basic.Button>
            </Basic.PanelFooter>
          </Basic.Panel>
        </form>
      </div>
    );
  }
}

RoleCatalogueDetail.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
RoleCatalogueDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  if (!component.entity) {
    return {};
  }
  return {
    _permissions: manager.getPermissions(state, null, component.entity.id)
  };
}

export default connect(select)(RoleCatalogueDetail);
