# DateTimePicker component

Component for select date and time. Extended from AbstractFormComponent.

## Parameters

All parameters from AbstractFormComponent are supported. Added parameters:

| Parameter | Type | Description | Default  |
| --- | :--- | :--- | :--- |
| mode  | oneOf(['date', 'time', 'datetime'])   |  Defined mode of component see @DateTimePicker. Use 'datetime' for DateTime columns, timezone is ignored for LocalDate columns. | 'datetime' |
| dateFormat | string   | Defined input date format (use moment.js)  | DD.MM.YYYY |
| timeFormat | string   | Defined input time format (use moment.js)  | HH:mm |
| componentSpan  | string | defined span for component | col-sm-5 |
| isValidDate | func | Define the dates that can be selected. The function receives (currentDate, selectedDate) and shall return a true or false whether the currentDate is valid or not. | ||

## Usage

### Date time:
```html
<DateTimePicker
  ref="expireDate"
  label='Expire Date'/>
```

### Only date:
```html
<DateTimePicker
  ref="expireDate"
  mode="date"
  label='Expire Date'/>
```

### Only time:
```html
<DateTimePicker
  ref="expireDate"
  mode="time"
  label='Expire Date'/>
```

### Date time with specific format:
```html
<DateTimePicker
  ref="expireDate"
  dateFormat="DD/MM/YYYY"
  timeFormat="HH:mm"
  label='Expire Date'/>
```
### Pick date only in future
```html
isValidDate(current) {
  if (!current) {
    return true; // if value is not required
  }
  const date = new Date();
  const yesterday = date.setDate(date.getDate() - 1);
  return current.isAfter(yesterday);
}
...
<DateTimePicker
    ref="dateInFuture"
    label="Pick some date in future"
    isValidDate={ this.isValidDate.bind(this) }/>
```
