# LocalizationService

Provides localization through application. Based on http://i18next.com/.
Any subclasses of ``AbstractContextComponent`` can use function *i18n(key, option)*.
Function has the same annotation as *i18next* function *i18n.t(key, option)* - see http://i18next.com/pages/doc_features.html.

Translations can be found in folder: src/locales.

Look out: ``DateTimePicker`` needs additional static import (because browserify) for added locale => When locale will be added, add require (e.g. require('moment/locale/cs');) into ``LocalizationService``.

## Usage

### Any component
```javascript
...
import { i18n } from '../../../services/LocalizationService';
...
getAppName() {
  return i18n('app.name');
}
...
```

### Any AbstractContextComponent subclass
```javascript
...
getAppName() {
  return this.i18n('app.name');
}
...
```

### Localization with html
```javascript
...
getAppName() {
  return this.i18n('app.name', { escape: false });
}
...
```
