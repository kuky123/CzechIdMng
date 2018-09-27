# EavForm Component

Dynamic form renders form attributes. The componnet doesn't needed form definition.

Localization will be composed with the format:

**for label**:
- `${localizationModule}:eav.${localizationType}.{$localizationKey}.${attribute.code}.label`

**for help block**
- `${localizationModule}:eav.${localizationType}.{$localizationKey}.${attribute.code}.helpBlock`

If localization with the key will not be found. For label and help block will be used given code and description from each attribute

## Parameters

| Parameter | Type | Description | Default  |
| --- | :--- | :--- | :--- |
| localizationModule  | string   | Manager for formInstance loading (generalize ``FormableEntityManager``) |  |
| localizationKey  | string   | UI identifier - it's used as key in store (saving, loading ...) |  |
| localizationType  | string   | Parent entity identifier |  |
| formAttributes  | array of objects   | For localization |  |
| useDefaultValue | bool | Use configured attribute default value as filled. | true |


## Usage
```html
...
import * as Advanced from '../../components/advanced';
...
<Advanced.EavAttributeForm
    ref="form"
    localizationKey={backendBulkAction.name}
    localizationModule="core"
    localizationType="IdmIdentity"
    formAttributes={[
      {
        "code" : "role",
        "name" : "Role",
        "persistentType" : "UUID",
        "faceType" : "ROLE-SELECT",
        "multiple" : true,
        "required" : true,
        "readonly" : false
      },
      {
        "code" : "approve",
        "name" : "Approve",
        "description" : "approoving",
        "persistentType" : "BOOLEAN",
        "defaultValue" : "true",
      }]}/>
```
