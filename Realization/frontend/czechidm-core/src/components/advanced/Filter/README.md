# Filter Component

Encapsulates all features for filter. Filter works nice together with advanced table.

## Child components:

| Component | Description |
| --- | :--- | :--- | :--- |
| FilterButtons | Renders buttons for use and clear filter |
| FilterToogleButton | Renders button for filter collapsing |
| FilterTextField | Text input. Default operator LIKE |
| FilterEnumSelectBox | Enumeration select. Supports multiselect. Default operator EQ (multi values are appended with OR clausule) |
| FilterBooleanSelectBox | Boolean select |
| FilterDateTimePicker | DateTime select. Default operator EQ |
| FilterDate | Advanced date filter for dates form - till. |

## Usage
```html
...
import * as Basic from '../../components/basic';
import * as Advanced from '../../../../components/advanced';
...
<Advanced.Filter onSubmit={ this.useFilter.bind(this) }>
  <Basic.AbstractForm ref="filterForm">
    <Basic.Row>
      <Basic.Col lg={ 8 }>
        <Advanced.Filter.FilterDate ref="fromTill"/>
      </Basic.Col>      
      <Basic.Col lg={ 4 } className="text-right">
        <Advanced.Filter.FilterButtons cancelFilter={ this.cancelFilter.bind(this) }/>
      </Basic.Col>
    </Basic.Row>
    <Basic.Row>
      <Basic.Col lg={ 4 }>
        <Advanced.Filter.TextField
          ref="email"
          placeholder={ this.i18n('entity.Identity.email') }/>
      </Basic.Col>
      <Basic.Col lg={ 4 }>
        <Advanced.Filter.TextField
          ref="subject"
          placeholder={ this.i18n('entity.EmailLog.subject') }/>
      </Basic.Col>
      <Basic.Col lg={ 4 }>
      </Basic.Col>
    </Basic.Row>
    <Basic.Row className="last">
      <Basic.Col lg={ 4 }>
        <Advanced.Filter.BooleanSelectBox
          ref="success"
          placeholder={ this.i18n('filter.success.placeholder') }/>
      </Basic.Col>
      <Basic.Col lg={ 8 }>
      </Basic.Col>
    </Basic.Row>
  </Basic.AbstractForm>
</Advanced.Filter>
```

### FilterDate

Advanced date filter component for dates form - till. Components saves complex object with from and till nested properties into result filter.

## Parameters

All parameters form parent component ``AbstractFormComponent`` is supported.
<br><br>Extra component parameters:

| Parameter | Type | Description | Default  |
| --- | :--- | :--- | :--- |
| mode  | FilterDateTimePicker.propTypes.mode  | Defined mode of component see @DateTimePicker. Use 'datetime' for DateTime columns, timezone is ignored for LocalDate columns. | 'datetime' |
| faceProperty | string | Property face - face - will be used for get/set value (configurable - more date filters can be used on the same content) | 'face' |
| fromProperty | string | Property name - from - will be used for get/set value | 'from' |
| tillProperty | string | Property name - till - will be used for get/set value | 'till' |
| facePlaceholder | string | Face select box placeholder | default i18n('face.placeholder') from locale |
| fromPlaceholder | string | Face select box placeholder | default i18n('from.placeholder') from locale |
| tillPlaceholder | string | Face select box placeholder | default i18n('till.placeholder') from locale |
