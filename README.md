Date Range Picker 
===================
Date Range Picker is a Calendar Picker View to show a Customized Date Range Picker with improved UI

[ ![Download](https://api.bintray.com/packages/savvisingh/maven/date-range-picker/images/download.svg) ](https://bintray.com/savvisingh/maven/date-range-picker/_latestVersion)

Screenshots
-----------
<img src="device-2017-06-23-154706.png" alt="alt text" width="300" height="500"> <img src="device-2017-06-23-154757.png" alt="alt text" width="300" height="500">

Usage
-----
* 1. Add Calendar Picker View to XML

```xml
<com.savvi.rangedatepicker.CalendarPickerView
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/calendar_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingLeft="16dp"
        android:paddingRight="16dp"
        android:paddingBottom="16dp"
        android:scrollbarStyle="outsideOverlay"
        android:clipToPadding="false"
        app:tsquare_dividerColor="@color/transparent"
        app:tsquare_headerTextColor="@color/custom_header_text"
        /> 
 ```
 
* 2. Initialize it in Java Class
      
```java
calendar = (CalendarPickerView) findViewById(R.id.calendar_view);

        calendar.init(lastYear.getTime(), nextYear.getTime()) //
                .inMode(CalendarPickerView.SelectionMode.RANGE)
                .withSelectedDate(new Date())
// deactivates given dates, non selectable
                .withDeactivateDates(list)
// highlight dates in red color, mean they are aleady used.
                .withHighlightedDates(arrayList);
 
```
 * There are other Selection modes also SINGLE and MULTIPLE
 * Adding Deactivated Dates
  User wont able to select these dates they will be deactivated.
  for eg, if you dont want to provide services on Saturday and Sunday you can mark them deactivated in that case the date text color wil be grey..
  
 * You can aslo specify the month header by passing SimpleDateFormat object in constructor
   For eg. if we want to display year

```java

calendar.init(lastYear.getTime(), nextYear.getTime(), new SimpleDateFormat("MMMM, YYYY", Locale.getDefault()));

 ``` 
 
 * Getting Selected Dates
You can get selected dates with the below function call when your user click finish or next button (which will you implement).

```java

calendar.getSelectedDates()

 ``` 
 
 Import DateRangePicker dependency
------------------------------------

declare it into your pom.xml

```xml
<dependency>
  <groupId>com.savvi.datepicker</groupId>
  <artifactId>rangepicker</artifactId>
  <version>1.2.0</version>
  <type>pom</type>
</dependency>
```
or into your build.gradle

```groovy
dependencies {
    compile 'com.savvi.datepicker:rangepicker:1.2.0'
}
```

 License
---------

    Copyright 2017 Sarabjeet Singh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
