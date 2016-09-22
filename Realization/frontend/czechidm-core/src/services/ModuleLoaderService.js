import Immutable from 'immutable';

let allModules = new Immutable.Map();

export default class ModuleLoaderService {

  /**
  * Modules inicialization
  */
  constructor(modules, cb) {
    allModules = modules;
    cb();
  }

  static getModuleDescriptor(key) {
    return allModules[key];
  }

  static getAllModuleDescriptors() {
    return allModules;
  }
}
