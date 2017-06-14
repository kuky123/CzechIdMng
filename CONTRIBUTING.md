# Contributing to czechidm
We want to make contributing to this project as easy and transparent as possible.

## Issues
We use Redmine issues to track bugs. Please ensure your description is clear and has sufficient instructions to be able to reproduce the issue.

## Backend

### Coding Style, convention

* https://google.github.io/styleguide/javaguide.html 
* 2 tabs for indentation
* use `{}` brackets
* Don't use abbreviations for fields, variables etc.
* Don't use camelCase in package names
* Database
 * [Convention](https://proj.bcvsolutions.eu/ngidm/doku.php?id=navrh:identifikatory#konvence_navrhu_databaze)
  * `Entity` / `Dto` has to contain jsr303 validations
* Rest
  * Rest endpoints naming with snake-case in plural, e.g. `<server>/wf-identities`, `<server>/tasks`
* Spring
  * use bean names, e.g. `@Service("identityService")`
  * use `@Transaction` annotation for service methods
  * use interfaces
    * `Configurable` for configurations
    * `Codeable` for entities with code and name - see database naming [convention](https://proj.bcvsolutions.eu/ngidm/doku.php?id=navrh:identifikatory#konvence_navrhu_databaze)
    * ...

### IDE
* [Eclipse](https://proj.bcvsolutions.eu/ngidm/doku.php?id=en:development:ide:eclipse)
* [Idea](https://proj.bcvsolutions.eu/ngidm/doku.php?id=en:development:ide:idea)

## Frontend

### Coding Style, convention

* ES6
  * use classes
  * use arrow functions (`.bind(this)` is not needed)
  * use `import` - import third party libraries
  * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array
  * Immutable
  * ...
* Use semicolons;
* Commas last,
* 2 spaces for indentation
* Prefer `'` over `"`
* 80 character line length
* Do not use the optional parameters of `setTimeout` and `setInterval`
* Use JSDoc for documentation (`@author` etc.) https://developers.google.com/closure/compiler/docs/js-for-compiler
* Make tests (mocha, chai)
* Use identity operator `!==`, `===`
* Naming convention for object is the same as in java - camelCase, "private" attributes and methods starting with `_`, e.g. `_links`, `_trimmed`
* `constructor` method has to be at the start of class
* `componentDidMount` and the next react lifecycle methods has to be in the lifecycle order.
* `render` method has to be on the end of react component
* Character `_` at the start of attribute or method => private attribute or method
* use less variables

### IDE

We are using [Atom IDE](https://atom.io/) for development with plugins installed:
* linter
* linter-eslint
* react
* linter-bootlint
* atom-bootstrap-3
* git-diff-details
* git-diff-popup
* git-plus
* git-status
* line-diff-details
* last-cursor-position
* docblockr

Tips for Atom:
* **Ctrl+Shift+P** Search any function of Atom.
* **Ctrl+R** Search method in current file.
* **Auto indent** Formatting of selected code (jsx).



## Our Development Process

1. Clone or Fork the repo and create your branch from `CzechIdMng`.

2. If you've added code that should be tested, add tests to test folder.

3. Ensure the test suite passes.

4. Make sure your code lints.

5. Do not commit anything to the `dist` folder.

## License

MIT