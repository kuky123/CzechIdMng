# Advanced Tree component

Component for load and render tree of entities. Using EntityManager, with support for loading tree structures.

Manager has to support methods:
* manager has to extend standard ``EntityManager`` - methods for work entities are used (e.g. find entities, render nice label).
* ``getRootSearchParameters()`` - returns search parameters to find roots.
* ``getTreeSearchParameters()`` - returns search parameters to find children with 'parent' paremeter filter.


Entity has to contain properties:
* ``id`` - node id
* ``parent`` - parent node id
* ``childrenCount`` - sub nodes count (required for loading children)

Entity property names and manager's methods are not configurable - enrich entity or manager with required properties and methods.

| Parameter | Type | Description | Default  |
| --- | :--- | :--- | :--- |
| uiKey | string.isRequired | Key prefix in redux (loading / store data). | |
| manager | EntityManager.isRequired | EntityManager for fetching entities in tree | |
| forceSearchParameters | Domain.SearchParameters | "Hard filters" | |
| onSelect | func | On select node callback. Selected node is given as parameter | |
| onDoubleClick | func | On double click node callback. Selected node is given as parameter. | |
| onDetail | func | Show detail function. Detail icon is rendered in tree header. | |
| traverse | bool | raverse to selected folder | false |
| header | oneOfType([string, element]) | Tree header | this.i18n('component.advanced.Tree.header')|
| noData | oneOfType([string, element]) | If tree roots are empty, then this text will be shown | this.i18n('component.advanced.Tree.noData') |
| className | string | Tree css |  |
| style | object | Tree styles |  |
| bodyClassName | string | Tree body css |  |
| bodyStyle | object | Tree body styles |  | |

## Usage

```javascript
import { RoleCatalogueManager } from '../../redux';
//
const manager = new RoleCatalogueManager();
...

<Advanced.Tree
  ref="roleCatalogueTree"
  uiKey="role-catalogue-tree"
  manager={ manager }
  onSelect={ (nodeId) => alert('Selected: ' + nodeId) }/>

```
