// http://rackt.org/redux/docs/advanced/AsyncActions.html
// https://www.npmjs.com/package/immutable

import merge from 'object-assign';
import Immutable from 'immutable';
import _ from 'lodash';
import moment from 'moment';
//
import {
  LOGOUT,
  RECEIVE_LOGIN_EXPIRED
} from '../security/SecurityManager';
import {
  REQUEST_ENTITIES,
  RECEIVE_ENTITIES,
  CLEAR_ENTITIES,
  REQUEST_ENTITY,
  RECEIVE_ENTITY,
  DELETED_ENTITY,
  RECEIVE_ERROR,
  START_BULK_ACTION,
  PROCESS_BULK_ACTION,
  STOP_BULK_ACTION,
  RECEIVE_PERMISSIONS
} from './EntityManager';

import {
  REQUEST_DATA,
  STOP_REQUEST,
  RECEIVE_DATA,
  CLEAR_DATA
} from './DataManager';

const INITIAL_STATE = {
  entity: {
    // entities are stored as map <id, entity>
    // Identity: Immutable.Map({})
  },
  trimmed: {
    // trimmed entities are stored as map <id, entity>
    // Identity: Immutable.Map({})
  },
  ui: {
    // uiKeys are stored as object with structure:
    /*
    {
      items: [], // reference to identities
      trimmed: true, // is entities for this key is trimmed
      showLoading: true,
      total: null
      searchParameters: merge({}, DEFAULT_SEARCH_PARAMETERS, {
        sort: [{
          field: 'name',
          order: 'ASC'
        }]
      }),
      error: null
    }
    */
  },
  // ui: Immutable.Map({}),
  // bulk actions modal progress bar
  bulk: {
    action: {
      name: null,
      title: null
    },
    showLoading: false,
    counter: 0,
    size: 0
  },
  // custom data store - create, edit etc.
  data: new Immutable.Map({})
};

/**
 * Data storage - store all loaded entities from BE
 *
 * @author Radek Tomiška
 */
export function data(state = INITIAL_STATE, action) {
  const uiKey = action.uiKey;
  switch (action.type) {
    case REQUEST_ENTITIES: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: true,
          searchParameters: action.searchParameters,
          error: null
        })
      });
      return merge({}, state, {
        ui: merge({}, state.ui, ui)
      });
    }
    case RECEIVE_ENTITIES: {
      const entityType = action.entityType;
      let entities = state.entity[entityType] || new Immutable.Map({});
      let trimmed = state.trimmed[entityType] || new Immutable.Map({});
      const ids = [];
      let isTrimmed = false;
      action.entities.map(entity => {
        ids.push(entity.id);
        if (entity._trimmed === true) {
          isTrimmed = true;
          trimmed = trimmed.set(entity.id, entity);
        } else {
          // check modified date ... only newer
          if (entities.has(entity.id)) {
            // check trimmed and modified date
            if (!entities.get(entity.id).modified || moment(entity.modified).isAfter(entities.get(entity.id).modified)) {
              entities = entities.set(entity.id, entity);
            }
          } else {
            entities = entities.set(entity.id, entity);
          }
        }
      });
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false,
          searchParameters: action.searchParameters,
          entityType,
          items: ids,
          trimmed: isTrimmed,
          total: action.total,
          error: null
        })
      });
      return merge({}, state, {
        entity: merge({}, state.entity, {
          [entityType]: entities
        }),
        trimmed: merge({}, state.trimmed, {
          [entityType]: trimmed
        }),
        ui: merge({}, state.ui, ui)
      });
    }
    case REQUEST_DATA:
    case REQUEST_ENTITY: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: true,
          id: action.id,
          error: null
        })
      });
      return merge({}, state, {
        ui: merge({}, state.ui, ui)
      });
    }
    case STOP_REQUEST: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false,
          id: action.id,
          error: action.error
        })
      });
      return merge({}, state, {
        ui: merge({}, state.ui, ui)
      });
    }
    case RECEIVE_ENTITY: {
      const entityType = action.entityType;
      let entities = state.entity[entityType] || new Immutable.Map({});
      let trimmed = state.trimmed[entityType] || new Immutable.Map({});
      const isTrimmed = action.entity && action.entity._trimmed === true;
      if (isTrimmed) {
        // trimmed
        trimmed = trimmed.set(action.id, action.entity);
      } else {
        // set both - trimmed entity should have the same structure
        trimmed = trimmed.set(action.id, action.entity);
        entities = entities.set(action.id, action.entity);
      }
      // TODO: trimmed items ...
      let items = [];
      if (state.ui[uiKey] && state.ui[uiKey].items) {
        items = state.ui[uiKey].items;
      }
      if (!_.includes(items, action.id)) {
        items = _.slice(items);
        items.push(action.id);
      }
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false,
          entityType,
          id: action.id,
          error: null,
          items
        })
      });
      return merge({}, state, {
        entity: merge({}, state.entity, {
          [entityType]: entities
        }),
        trimmed: merge({}, state.trimmed, {
          [entityType]: trimmed
        }),
        ui: merge({}, state.ui, ui)
      });
    }
    case RECEIVE_PERMISSIONS: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          permissions: action.permissions
        })
      });
      return merge({}, state, {
        ui: merge({}, state.ui, ui)
      });
    }
    case DELETED_ENTITY: {
      const entityType = action.entityType;
      let entities = state.entity[entityType] || new Immutable.Map({});
      let trimmed = state.trimmed[entityType] || new Immutable.Map({});
      if (entities.has(action.id)) {
        entities = entities.delete(action.id);
      }
      if (trimmed.has(action.id)) {
        trimmed = trimmed.delete(action.id);
      }
      // clear entity.id from all uiKeys items
      let ui = merge({}, state.ui);
      for (const processUiKey in state.ui) {
        if (state.ui[processUiKey].items) {
          const items = _.without(state.ui[processUiKey].items, action.id);
          ui = merge(ui, {
            [processUiKey]: merge({}, state.ui[processUiKey], {
              showLoading: false,
              id: action.id,
              error: null,
              items
            })
          });
        }
      }
      return merge({}, state, {
        entity: merge({}, state.entity, {
          [entityType]: entities
        }),
        trimmed: merge({}, state.trimmed, {
          [entityType]: trimmed
        }),
        ui
      });
    }
    case RECEIVE_ERROR: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false,
          error: action.error
        })
      });
      return merge({}, state, {
        ui: merge({}, state.ui, ui)
      });
    }
    case CLEAR_ENTITIES: {
      const entityType = action.entityType;
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false,
          searchParameters: null,
          items: [],
          total: 0,
          error: null
        })
      });
      return merge({}, state, {
        entity: merge({}, state.entity, {
          [entityType]: new Immutable.Map({})
        }),
        trimmed: merge({}, state.trimmed, {
          [entityType]: new Immutable.Map({})
        }),
        ui: merge({}, state.ui, ui)
      });
    }
    case RECEIVE_LOGIN_EXPIRED: {
      // clear whole state - except configurations (clear showloading is needed)
      return merge({}, INITIAL_STATE);
    }
    case LOGOUT: {
      // clear whole state - except configurations
      return merge({}, INITIAL_STATE);
    }
    case RECEIVE_DATA: {
      const ui = merge({}, state.ui, {
        [uiKey]: merge({}, state.ui[uiKey], {
          showLoading: false
        })
      });
      return merge({}, state, {
        data: state.data.set(uiKey, action.data),
        ui: merge({}, state.ui, ui)
      });
    }
    case CLEAR_DATA: {
      // preserve state, if data doesn't contin given uiKey
      if (!state.data.has(uiKey)) {
        return state;
      }
      return merge({}, state, {
        data: state.data.delete(uiKey)
      });
    }
    case START_BULK_ACTION: {
      return merge({}, state, {
        bulk: {
          action: action.action,
          showLoading: true,
          counter: 0,
          size: action.size
        }
      });
    }
    case PROCESS_BULK_ACTION: {
      return merge({}, state, {
        bulk: merge({}, state.bulk, {
          counter: state.bulk.counter + 1
        })
      });
    }
    case STOP_BULK_ACTION: {
      return merge({}, state, {
        bulk: merge({}, state.bulk, {
          showLoading: false
        })
      });
    }

    default: {
      return state;
    }
  }
}
