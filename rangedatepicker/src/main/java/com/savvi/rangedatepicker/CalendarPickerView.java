package com.savvi.rangedatepicker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;


public class CalendarPickerView extends RecyclerView {
    private ArrayList<SubTitle> subTitles;

    public enum SelectionMode {
        /**
         * Only one date will be selectable.  If there is already a selected date and you select a new
         * one, the old date will be unselected.
         */
        SINGLE,
        /**
         * Multiple dates will be selectable.  Selecting an already-selected date will un-select it.
         */
        MULTIPLE,
        /**
         * Allows you to select a date range.  Previous selections are cleared when you either:
         * <ul>
         * <li>Have a range selected and select another date (even if it's in the current range).</li>
         * <li>Have one date selected and then select an earlier date.</li>
         * </ul>
         */
        RANGE
    }

    private final CalendarPickerView.MonthAdapter adapter;
    private final RecyclerView.LayoutManager layoutManager;
    private final IndexedLinkedHashMap<String, List<List<MonthCellDescriptor>>> cells =
            new IndexedLinkedHashMap<>();
    final MonthView.Listener listener = new CellClickedListener();
    final List<MonthDescriptor> months = new ArrayList<>();
    final List<MonthCellDescriptor> selectedCells = new ArrayList<>();
    final List<MonthCellDescriptor> highlightedCells = new ArrayList<>();
    final List<Calendar> selectedCals = new ArrayList<>();
    final List<Calendar> highlightedCals = new ArrayList<>();
    ArrayList<Integer> deactivatedDates = new ArrayList<>();
    private Locale locale;
    private TimeZone timeZone;
    private DateFormat monthNameFormat;
    private DateFormat weekdayNameFormat;
    private DateFormat fullDateFormat;
    private Calendar minCal;
    private Calendar maxCal;
    private Calendar monthCounter;
    private boolean displayOnly;
    SelectionMode selectionMode;
    Calendar today;
    private int dividerColor;
    private int dayBackgroundResId;
    private int dayTextColorResId;
    private int titleTextColor;
    private boolean displayHeader;
    private boolean orientation;
    private int headerTextColor;
    private Typeface titleTypeface;
    private Typeface dateTypeface;

    // the max range size
    private int maxRangeSize = Integer.MAX_VALUE;

    private OnDateSelectedListener dateListener;
    private DateSelectableFilter dateConfiguredListener;
    private OnInvalidDateSelectedListener invalidDateListener =
            new DefaultOnInvalidDateSelectedListener();
    private CellClickInterceptor cellClickInterceptor;
    private List<CalendarCellDecorator> decorators;
    private DayViewAdapter dayViewAdapter = new DefaultDayViewAdapter();

    private boolean monthsReverseOrder = false;

    public void setDecorators(List<CalendarCellDecorator> decorators) {
        this.decorators = decorators;
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    public List<CalendarCellDecorator> getDecorators() {
        return decorators;
    }

    public CalendarPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarPickerView);
        final int bg = a.getColor(R.styleable.CalendarPickerView_android_background,
                res.getColor(R.color.calendar_bg));
        dividerColor = a.getColor(R.styleable.CalendarPickerView_tsquare_dividerColor,
                res.getColor(R.color.calendar_divider));
        dayBackgroundResId = a.getResourceId(R.styleable.CalendarPickerView_tsquare_dayBackground,
                R.drawable.calendar_bg_selector);
        dayTextColorResId = a.getResourceId(R.styleable.CalendarPickerView_tsquare_dayTextColor,
                R.drawable.day_text_color);
        titleTextColor = a.getColor(R.styleable.CalendarPickerView_tsquare_titleTextColor,
                res.getColor(R.color.dateTimeRangePickerTitleTextColor));
        displayHeader = a.getBoolean(R.styleable.CalendarPickerView_tsquare_displayHeader, true);
        headerTextColor = a.getColor(R.styleable.CalendarPickerView_tsquare_headerTextColor,
                res.getColor(R.color.dateTimeRangePickerHeaderTextColor));
        orientation = a.getBoolean(R.styleable.CalendarPickerView_tsquare_orientation_horizontal, false);

        a.recycle();


        adapter = new MonthAdapter();
        if(!orientation){
            layoutManager = new LinearLayoutManager(getContext(), VERTICAL, monthsReverseOrder);
        }else {
            layoutManager = new LinearLayoutManager(getContext(), HORIZONTAL, monthsReverseOrder);
            SnapHelper snapHelper = new LinearSnapHelper();
            snapHelper.attachToRecyclerView(this);
        }

        setLayoutManager(layoutManager);
        setBackgroundColor(bg);
        timeZone = TimeZone.getDefault();
        locale = Locale.getDefault();

        if (isInEditMode()) {
            Calendar nextYear = Calendar.getInstance(timeZone, locale);
            nextYear.add(Calendar.YEAR, 1);

            init(new Date(), nextYear.getTime()) //
                    .withSelectedDate(new Date());
        }
    }

    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link SelectionMode} to {@link SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link FluentInitializer#inMode(SelectionMode)} on the
     * {@link FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the given time zone and the given locale. This means
     * that all dates will be in given time zone, all names (months, days) will be in the language
     * of the locale and the weeks start with the day specified by the locale.
     *
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public FluentInitializer init(Date minDate, Date maxDate, TimeZone timeZone, Locale locale, DateFormat monthNameFormat) {

        if (minDate == null || maxDate == null) {
            throw new IllegalArgumentException(
                    "minDate and maxDate must be non-null.  " + dbg(minDate, maxDate));
        }
        if (minDate.after(maxDate)) {
            throw new IllegalArgumentException(
                    "minDate must be before maxDate.  " + dbg(minDate, maxDate));
        }
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null.");
        }
        if (timeZone == null) {
            throw new IllegalArgumentException("Time zone is null.");
        }

        // Make sure that all calendar instances use the same time zone and locale.
        this.timeZone = timeZone;
        this.locale = locale;

        today = Calendar.getInstance(timeZone, locale);
        minCal = Calendar.getInstance(timeZone, locale);
        maxCal = Calendar.getInstance(timeZone, locale);
        monthCounter = Calendar.getInstance(timeZone, locale);
        this.monthNameFormat = monthNameFormat;
        monthNameFormat.setTimeZone(timeZone);
        weekdayNameFormat =
                new SimpleDateFormat("E", locale);
        weekdayNameFormat.setTimeZone(timeZone);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        fullDateFormat.setTimeZone(timeZone);

        this.selectionMode = SelectionMode.SINGLE;
        // Clear out any previously-selected dates/cells.
        selectedCals.clear();
        selectedCells.clear();
        highlightedCals.clear();
        highlightedCells.clear();

        // Clear previous state.
        cells.clear();
        months.clear();
        minCal.setTime(minDate);
        maxCal.setTime(maxDate);
        setMidnight(minCal);
        setMidnight(maxCal);
        displayOnly = false;

        // maxDate is exclusive: bump back to the previous day so if maxDate is the first of a month,
        // we don't accidentally include that month in the view.
        maxCal.add(MILLISECOND, -1);

        // Now iterate between minCal and maxCal and build up our list of months to show.
        monthCounter.setTime(minCal.getTime());
        final int maxMonth = maxCal.get(MONTH);
        final int maxYear = maxCal.get(YEAR);
        while ((monthCounter.get(MONTH) <= maxMonth // Up to, including the month.
                || monthCounter.get(YEAR) < maxYear) // Up to the year.
                && monthCounter.get(YEAR) < maxYear + 1) { // But not > next yr.
            Date date = monthCounter.getTime();
            MonthDescriptor month =
                    new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), date,
                            monthNameFormat.format(date));
            cells.put(monthKey(month), getMonthCells(month, monthCounter));
            Logr.d("Adding month %s", month);
            months.add(month);
            monthCounter.add(MONTH, 1);
        }

        validateAndUpdate();
        return new FluentInitializer();
    }

    public FluentInitializer init(Date minDate, Date maxDate) {
        return init(minDate, maxDate, TimeZone.getDefault(), Locale.getDefault(), new SimpleDateFormat("MMMM", Locale.getDefault()));
    }


    public FluentInitializer init(Date minDate, Date maxDate, TimeZone timeZone) {
        return init(minDate, maxDate, timeZone, Locale.getDefault(), new SimpleDateFormat("MMMM", Locale.getDefault()));
    }

    public FluentInitializer init(Date minDate, Date maxDate, DateFormat monthNameFormat) {
        return init(minDate, maxDate, TimeZone.getDefault(), Locale.getDefault(), monthNameFormat);
    }


    public FluentInitializer init(Date minDate, Date maxDate, Locale locale) {
        return init(minDate, maxDate, TimeZone.getDefault(), locale, new SimpleDateFormat("MMMM", locale));
    }

    public class FluentInitializer {


        public FluentInitializer inMode(SelectionMode mode) {
            selectionMode = mode;
            validateAndUpdate();
            return this;
        }

        public FluentInitializer withSelectedDate(Date selectedDates) {
            return withSelectedDates(Collections.singletonList(selectedDates));
        }


        public FluentInitializer withSelectedDates(Collection<Date> selectedDates) {
            if (selectionMode == SelectionMode.SINGLE && selectedDates.size() > 1) {
                throw new IllegalArgumentException("SINGLE mode can't be used with multiple selectedDates");
            }
            if (selectionMode == SelectionMode.RANGE && selectedDates.size() > 2) {
                throw new IllegalArgumentException(
                        "RANGE mode only allows two selectedDates.  You tried to pass " + selectedDates.size());
            }
            if (selectedDates != null) {
                for (Date date : selectedDates) {
                    selectDate(date);
                }
            }
            scrollToSelectedDates();

            validateAndUpdate();
            return this;
        }

        public FluentInitializer withHighlightedDates(Collection<Date> dates) {
            highlightDates(dates);
            return this;
        }


        public FluentInitializer withHighlightedDate(Date date) {
            return withHighlightedDates(Collections.singletonList(date));
        }

        @SuppressLint("SimpleDateFormat")
        public FluentInitializer setShortWeekdays(String[] newShortWeekdays) {
            DateFormatSymbols symbols = new DateFormatSymbols(locale);
            symbols.setShortWeekdays(newShortWeekdays);
            weekdayNameFormat =
                    new SimpleDateFormat("E", symbols);
            return this;
        }

        public FluentInitializer displayOnly() {
            displayOnly = true;
            return this;
        }

        public FluentInitializer withMonthsReverseOrder(boolean monthsRevOrder) {
            monthsReverseOrder = monthsRevOrder;
            return this;
        }

        public FluentInitializer withDeactivateDates(ArrayList<Integer> deactivateDates) {
            deactivateDates(deactivateDates);
            return this;
        }

        public FluentInitializer withSubTitles(ArrayList<SubTitle> subTitles) {
            setSubTitles(subTitles);
            return this;
        }

        /**
         * set max range size
         * @param size
         * @return
         */
        public FluentInitializer withMaxRangeSize(int size) {
            maxRangeSize = size;
            return this;
        }
    }

    private void setSubTitles(ArrayList<SubTitle> subTitles) {
        this.subTitles = subTitles;
        validateAndUpdate();
    }

    private void validateAndUpdate() {
        if (getAdapter() == null) {
            setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    private void scrollToSelectedMonth(final int selectedIndex) {
        scrollToSelectedMonth(selectedIndex, false);
    }

    private void scrollToSelectedMonth(final int selectedIndex, final boolean smoothScroll) {
        post(new Runnable() {
            @Override
            public void run() {
                Logr.d("Scrolling to position %d", selectedIndex);

                if (smoothScroll) {
                    smoothScrollToPosition(selectedIndex);
                } else {
                    scrollToPosition(selectedIndex);
                }
            }
        });
    }

    private void scrollToSelectedDates() {
        Integer selectedIndex = null;
        Integer todayIndex = null;
        Calendar today = Calendar.getInstance(timeZone, locale);
        for (int c = 0; c < months.size(); c++) {
            MonthDescriptor month = months.get(c);
            if (selectedIndex == null) {
                for (Calendar selectedCal : selectedCals) {
                    if (sameMonth(selectedCal, month)) {
                        selectedIndex = c;
                        break;
                    }
                }
                if (selectedIndex == null && todayIndex == null && sameMonth(today, month)) {
                    todayIndex = c;
                }
            }
        }
        if (selectedIndex != null) {
            scrollToSelectedMonth(selectedIndex);
        } else if (todayIndex != null) {
            scrollToSelectedMonth(todayIndex);
        }
    }

    public boolean scrollToDate(Date date) {
        Integer selectedIndex = null;

        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(date);
        for (int c = 0; c < months.size(); c++) {
            MonthDescriptor month = months.get(c);
            if (sameMonth(cal, month)) {
                selectedIndex = c;
                break;
            }
        }
        if (selectedIndex != null) {
            scrollToSelectedMonth(selectedIndex);
            return true;
        }
        return false;
    }

    /**
     * This method should only be called if the calendar is contained in a dialog, and it should only
     * be called once, right after the dialog is shown (using
     * {@link android.content.DialogInterface.OnShowListener} or
     * {@link android.app.DialogFragment#onStart()}).
     */
    public void fixDialogDimens() {
        Logr.d("Fixing dimensions to h = %d / w = %d", getMeasuredHeight(), getMeasuredWidth());
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = getMeasuredHeight();
        getLayoutParams().width = getMeasuredWidth();
        // Post this runnable so it runs _after_ the dimen changes have been applied/re-measured.
        post(new Runnable() {
            @Override
            public void run() {
                Logr.d("Dimens are fixed: now scroll to the selected date");
                scrollToSelectedDates();
            }
        });
    }

    /**
     * Set the typeface to be used for month titles.
     */
    public void setTitleTypeface(Typeface titleTypeface) {
        this.titleTypeface = titleTypeface;
        validateAndUpdate();
    }

    /**
     * Sets the typeface to be used within the date grid.
     */
    public void setDateTypeface(Typeface dateTypeface) {
        this.dateTypeface = dateTypeface;
        validateAndUpdate();
    }

    /**
     * Sets the typeface to be used for all text within this calendar.
     */
    public void setTypeface(Typeface typeface) {
        setTitleTypeface(typeface);
        setDateTypeface(typeface);
    }

    /**
     * This method should only be called if the calendar is contained in a dialog, and it should only
     * be called when the screen has been rotated and the dialog should be re-measured.
     */
    public void unfixDialogDimens() {
        Logr.d("Reset the fixed dimensions to allow for re-measurement");
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = LayoutParams.MATCH_PARENT;
        getLayoutParams().width = LayoutParams.MATCH_PARENT;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (months.isEmpty()) {
            throw new IllegalStateException(
                    "Must have at least one month to display.  Did you forget to call init()?");
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public Date getSelectedDate() {
        return (selectedCals.size() > 0 ? selectedCals.get(0).getTime() : null);
    }

    public List<Date> getSelectedDates() {
        List<Date> selectedDates = new ArrayList<>();
        for (MonthCellDescriptor cal : selectedCells) {
            if (!highlightedCells.contains(cal) && !deactivatedDates.contains(cal.getDate().getDay() + 1))
                selectedDates.add(cal.getDate());
        }
        Collections.sort(selectedDates);
        return selectedDates;
    }

    /**
     * Returns a string summarizing what the client sent us for init() params.
     */
    private static String dbg(Date minDate, Date maxDate) {
        return "minDate: " + minDate + "\nmaxDate: " + maxDate;
    }

    /**
     * Clears out the hours/minutes/seconds/millis of a Calendar.
     */
    static void setMidnight(Calendar cal) {
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.set(MILLISECOND, 0);
    }

    private class CellClickedListener implements MonthView.Listener {
        @Override
        public void handleClick(MonthCellDescriptor cell) {
            Date clickedDate = cell.getDate();

            if (highlightedCells.contains(cell)) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(clickedDate);

            int day = calendar.get(DAY_OF_WEEK);
            if (deactivatedDates.contains(day)) {
                return;
            }

            if (cellClickInterceptor != null && cellClickInterceptor.onCellClicked(clickedDate)) {
                return;
            }
            if (!betweenDates(clickedDate, minCal, maxCal) || !isDateSelectable(clickedDate)) {
                if (invalidDateListener != null) {
                    invalidDateListener.onInvalidDateSelected(clickedDate);
                }
            } else {
                boolean wasSelected = doSelectDate(clickedDate, cell);

                if (dateListener != null) {
                    if (wasSelected) {
                        dateListener.onDateSelected(clickedDate);
                    } else {
                        dateListener.onDateUnselected(clickedDate);
                    }
                }
            }
        }
    }


    public boolean selectDate(Date date) {
        return selectDate(date, false);
    }


    public boolean selectDate(Date date, boolean smoothScroll) {
        validateDate(date);

        MonthCellWithMonthIndex monthCellWithMonthIndex = getMonthCellWithIndexByDate(date);
        if (monthCellWithMonthIndex == null || !isDateSelectable(date)) {
            return false;
        }
        boolean wasSelected = doSelectDate(date, monthCellWithMonthIndex.cell);
        if (wasSelected) {
            scrollToSelectedMonth(monthCellWithMonthIndex.monthIndex, smoothScroll);
        }
        return wasSelected;
    }

    private void validateDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Selected date must be non-null.");
        }
        if (date.before(minCal.getTime()) || date.after(maxCal.getTime())) {
            throw new IllegalArgumentException(String.format(
                    "SelectedDate must be between minDate and maxDate."
                            + "%nminDate: %s%nmaxDate: %s%nselectedDate: %s", minCal.getTime(), maxCal.getTime(),
                    date));
        }
    }

    private boolean doSelectDate(Date date, MonthCellDescriptor cell) {
        Calendar newlySelectedCal = Calendar.getInstance(timeZone, locale);
        newlySelectedCal.setTime(date);
        // Sanitize input: clear out the hours/minutes/seconds/millis.
        setMidnight(newlySelectedCal);

        // Clear any remaining range state.
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        switch (selectionMode) {
            case RANGE:
                if (selectedCals.size() > 1) {
                    // We've already got a range selected: clear the old one.
                    clearOldSelections();
                } else if (selectedCals.size() == 1 && newlySelectedCal.before(selectedCals.get(0))) {
                    // We're moving the start of the range back in time: clear the old start date.
                    clearOldSelections();
                }
                break;

            case MULTIPLE:
                date = applyMultiSelect(date, newlySelectedCal);
                break;

            case SINGLE:
                clearOldSelections();
                break;
            default:
                throw new IllegalStateException("Unknown selectionMode " + selectionMode);
        }

        if (date != null) {
            // Select a new cell.
            if (selectedCells.size() == 0 || !selectedCells.get(0).equals(cell)) {
                selectedCells.add(cell);
                cell.setSelected(true);
            }
            selectedCals.add(newlySelectedCal);

            if (selectionMode == SelectionMode.RANGE && selectedCells.size() > 1) {
                // Select all days in between start and end.
                Date start = selectedCells.get(0).getDate();
                Date end = selectedCells.get(1).getDate();
                selectedCells.get(0).setRangeState(RangeState.FIRST);
                selectedCells.get(1).setRangeState(RangeState.LAST);

                int startMonthIndex = cells.getIndexOfKey(monthKey(selectedCals.get(0)));
                int endMonthIndex = cells.getIndexOfKey(monthKey(selectedCals.get(1)));
                for (int monthIndex = startMonthIndex; monthIndex <= endMonthIndex; monthIndex++) {
                    List<List<MonthCellDescriptor>> month = cells.getValueAtIndex(monthIndex);
                    for (List<MonthCellDescriptor> week : month) {
                        for (MonthCellDescriptor singleCell : week) {
                            if (singleCell.getDate().after(start)
                                    && singleCell.getDate().before(end)
                                    && singleCell.isSelectable()) {
                                if (highlightedCells.contains(singleCell)) {
                                    singleCell.setSelected(false);
                                    singleCell.setUnavailable(true);
                                    singleCell.setHighlighted(false);
                                    selectedCells.add(singleCell);
                                } else if (!deactivatedDates.contains(singleCell.getDate().getDay() + 1)) {
                                    singleCell.setSelected(true);
                                    singleCell.setDeactivated(false);
                                    singleCell.setRangeState(RangeState.MIDDLE);
                                    selectedCells.add(singleCell);
                                }else {
                                    singleCell.setSelected(true);
                                    singleCell.setDeactivated(true);
                                    singleCell.setRangeState(RangeState.MIDDLE);
                                    selectedCells.add(singleCell);
                                }

                            }
                        }
                    }
                }

                if (selectedCells.size() > maxRangeSize) {
                    clearSelectedDates();
                }
            }
        }

        // Update the adapter.
        validateAndUpdate();
        return date != null;
    }

    private String monthKey(Calendar cal) {
        return cal.get(YEAR) + "-" + cal.get(MONTH);
    }

    private String monthKey(MonthDescriptor month) {
        return month.getYear() + "-" + month.getMonth();
    }

    private void clearOldSelections() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            // De-select the currently-selected cell.
            selectedCell.setSelected(false);
            if (highlightedCells.contains(selectedCell)) {
                selectedCell.setUnavailable(false);
                selectedCell.setHighlighted(true);
            }

            if (dateListener != null) {
                Date selectedDate = selectedCell.getDate();

                if (selectionMode == SelectionMode.RANGE) {
                    int index = selectedCells.indexOf(selectedCell);
                    if (index == 0 || index == selectedCells.size() - 1) {
                        dateListener.onDateUnselected(selectedDate);
                    }
                } else {
                    dateListener.onDateUnselected(selectedDate);
                }
            }
        }
        selectedCells.clear();
        selectedCals.clear();
    }

    private Date applyMultiSelect(Date date, Calendar selectedCal) {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            if (selectedCell.getDate().equals(date)) {
                // De-select the currently-selected cell.
                selectedCell.setSelected(false);
                selectedCells.remove(selectedCell);
                date = null;
                break;
            }
        }
        for (Calendar cal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                selectedCals.remove(cal);
                break;
            }
        }
        return date;
    }

    public void highlightDates(Collection<Date> dates) {
        for (Date date : dates) {
            validateDate(date);

            MonthCellWithMonthIndex monthCellWithMonthIndex = getMonthCellWithIndexByDate(date);
            if (monthCellWithMonthIndex != null) {
                Calendar newlyHighlightedCal = Calendar.getInstance(timeZone, locale);
                newlyHighlightedCal.setTime(date);
                MonthCellDescriptor cell = monthCellWithMonthIndex.cell;

                highlightedCells.add(cell);
                highlightedCals.add(newlyHighlightedCal);
                cell.setHighlighted(true);
            }
        }

        validateAndUpdate();
    }

    private static final String TAG = CalendarPickerView.class.getSimpleName();

    public void deactivateDates(ArrayList<Integer> deactivatedDates) {
        this.deactivatedDates = deactivatedDates;
        validateAndUpdate();
    }

    public void clearSelectedDates() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        clearOldSelections();
        validateAndUpdate();
    }

    public void clearHighlightedDates() {
        for (MonthCellDescriptor cal : highlightedCells) {
            cal.setHighlighted(false);
        }
        highlightedCells.clear();
        highlightedCals.clear();

        validateAndUpdate();
    }

    /**
     * Hold a cell with a month-index.
     */
    private static class MonthCellWithMonthIndex {
        public MonthCellDescriptor cell;
        public int monthIndex;

        public MonthCellWithMonthIndex(MonthCellDescriptor cell, int monthIndex) {
            this.cell = cell;
            this.monthIndex = monthIndex;
        }
    }

    /**
     * Return cell and month-index (for scrolling) for a given Date.
     */
    private MonthCellWithMonthIndex getMonthCellWithIndexByDate(Date date) {
        Calendar searchCal = Calendar.getInstance(timeZone, locale);
        searchCal.setTime(date);
        String monthKey = monthKey(searchCal);
        Calendar actCal = Calendar.getInstance(timeZone, locale);

        int index = cells.getIndexOfKey(monthKey);
        List<List<MonthCellDescriptor>> monthCells = cells.get(monthKey);
        for (List<MonthCellDescriptor> weekCells : monthCells) {
            for (MonthCellDescriptor actCell : weekCells) {
                actCal.setTime(actCell.getDate());
                if (sameDate(actCal, searchCal) && actCell.isSelectable()) {
                    return new MonthCellWithMonthIndex(actCell, index);
                }
            }
        }
        return null;
    }

    private class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.MyHolder> {
        private final LayoutInflater inflater;

        private MonthAdapter() {
            inflater = LayoutInflater.from(getContext());
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MonthView view = MonthView.create(parent, inflater, weekdayNameFormat, listener, today, dividerColor,
                dayBackgroundResId, dayTextColorResId, titleTextColor, displayHeader,
                headerTextColor, decorators, locale, dayViewAdapter);

            view.setTag(R.id.day_view_adapter_class, dayViewAdapter.getClass());

            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            MonthView view = (MonthView) holder.itemView;
            view.setDecorators(decorators);
            if (monthsReverseOrder) {
                position = months.size() - position - 1;
            }
            view.init(months.get(position), cells.getValueAtIndex(position), displayOnly,
                titleTypeface, dateTypeface, deactivatedDates, subTitles);

        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return months.size();
        }

        public class MyHolder extends ViewHolder {

            public MyHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    List<List<MonthCellDescriptor>> getMonthCells(MonthDescriptor month, Calendar startCal) {
        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(startCal.getTime());
        List<List<MonthCellDescriptor>> cells = new ArrayList<>();
        cal.set(DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(DAY_OF_WEEK);
        int offset = cal.getFirstDayOfWeek() - firstDayOfWeek;
        if (offset > 0) {
            offset -= 7;
        }
        cal.add(Calendar.DATE, offset);

        Calendar minSelectedCal = minDate(selectedCals);
        Calendar maxSelectedCal = maxDate(selectedCals);

        while ((cal.get(MONTH) < month.getMonth() + 1 || cal.get(YEAR) < month.getYear()) //
                && cal.get(YEAR) <= month.getYear()) {
            Logr.d("Building week row starting at %s", cal.getTime());
            List<MonthCellDescriptor> weekCells = new ArrayList<>();
            cells.add(weekCells);
            for (int c = 0; c < 7; c++) {
                Date date = cal.getTime();
                boolean isCurrentMonth = cal.get(MONTH) == month.getMonth();
                boolean isSelected = isCurrentMonth && containsDate(selectedCals, cal);
                boolean isSelectable =
                        isCurrentMonth && betweenDates(cal, minCal, maxCal) && isDateSelectable(date);
                boolean isToday = sameDate(cal, today);
                boolean isHighlighted = containsDate(highlightedCals, cal);
                int value = cal.get(DAY_OF_MONTH);

                RangeState rangeState = RangeState.NONE;
                if (selectedCals.size() > 1) {
                    if (sameDate(minSelectedCal, cal)) {
                        rangeState = RangeState.FIRST;
                    } else if (sameDate(maxDate(selectedCals), cal)) {
                        rangeState = RangeState.LAST;
                    } else if (betweenDates(cal, minSelectedCal, maxSelectedCal)) {
                        rangeState = RangeState.MIDDLE;
                    }
                }

                weekCells.add(
                        new MonthCellDescriptor(date, isCurrentMonth, isSelectable, isSelected, isToday,
                                isHighlighted, value, rangeState));
                cal.add(DATE, 1);
            }
        }
        return cells;
    }

    private boolean containsDate(List<Calendar> selectedCals, Date date) {
        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(date);
        return containsDate(selectedCals, cal);
    }

    private static boolean containsDate(List<Calendar> selectedCals, Calendar cal) {
        for (Calendar selectedCal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                return true;
            }
        }
        return false;
    }

    private static Calendar minDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(0);
    }

    private static Calendar maxDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(selectedCals.size() - 1);
    }

    private static boolean sameDate(Calendar cal, Calendar selectedDate) {
        return cal.get(MONTH) == selectedDate.get(MONTH)
                && cal.get(YEAR) == selectedDate.get(YEAR)
                && cal.get(DAY_OF_MONTH) == selectedDate.get(DAY_OF_MONTH);
    }

    private static boolean betweenDates(Calendar cal, Calendar minCal, Calendar maxCal) {
        final Date date = cal.getTime();
        return betweenDates(date, minCal, maxCal);
    }

    static boolean betweenDates(Date date, Calendar minCal, Calendar maxCal) {
        final Date min = minCal.getTime();
        return (date.equals(min) || date.after(min)) // >= minCal
                && date.before(maxCal.getTime()); // && < maxCal
    }

    private static boolean sameMonth(Calendar cal, MonthDescriptor month) {
        return (cal.get(MONTH) == month.getMonth() && cal.get(YEAR) == month.getYear());
    }

    private boolean isDateSelectable(Date date) {
        return dateConfiguredListener == null || dateConfiguredListener.isDateSelectable(date);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        dateListener = listener;
    }

    /**
     * Set a listener to react to user selection of a disabled date.
     *
     * @param listener the listener to set, or null for no reaction
     */
    public void setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener listener) {
        invalidDateListener = listener;
    }

    /**
     * Set a listener used to discriminate between selectable and unselectable dates. Set this to
     * disable arbitrary dates as they are rendered.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setDateSelectableFilter(DateSelectableFilter listener) {
        dateConfiguredListener = listener;
    }

    /**
     * Set an adapter used to initialize {@link CalendarCellView} with custom layout.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setCustomDayView(DayViewAdapter dayViewAdapter) {
        this.dayViewAdapter = dayViewAdapter;
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Set a listener to intercept clicks on calendar cells.
     */
    public void setCellClickInterceptor(CellClickInterceptor listener) {
        cellClickInterceptor = listener;
    }

    /**
     * Interface to be notified when a new date is selected or unselected. This will only be called
     * when the user initiates the date selection.  If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnDateSelectedListener(OnDateSelectedListener)
     */
    public interface OnDateSelectedListener {
        void onDateSelected(Date date);

        void onDateUnselected(Date date);
    }

    /**
     * Interface to be notified when an invalid date is selected by the user. This will only be
     * called when the user initiates the date selection. If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener)
     */
    public interface OnInvalidDateSelectedListener {
        void onInvalidDateSelected(Date date);
    }

    /**
     * Interface used for determining the selectability of a date cell when it is configured for
     * display on the calendar.
     *
     * @see #setDateSelectableFilter(DateSelectableFilter)
     */
    public interface DateSelectableFilter {
        boolean isDateSelectable(Date date);
    }

    /**
     * Interface to be notified when a cell is clicked and possibly intercept the click.  Return true
     * to intercept the click and prevent any selections from changing.
     *
     * @see #setCellClickInterceptor(CellClickInterceptor)
     */
    public interface CellClickInterceptor {
        boolean onCellClicked(Date date);
    }

    private class DefaultOnInvalidDateSelectedListener implements OnInvalidDateSelectedListener {
        @Override
        public void onInvalidDateSelected(Date date) {
        }
    }
}
