package com.savvi.rangedatepicker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CalendarCellView extends FrameLayout {
    private static final int[] STATE_SELECTABLE = {
            R.attr.tsquare_state_selectable
    };
    private static final int[] STATE_CURRENT_MONTH = {
            R.attr.tsquare_state_current_month
    };
    private static final int[] STATE_TODAY = {
            R.attr.tsquare_state_today
    };
    private static final int[] STATE_HIGHLIGHTED = {
            R.attr.tsquare_state_highlighted
    };
    private static final int[] STATE_RANGE_FIRST = {
            R.attr.tsquare_state_range_first
    };
    private static final int[] STATE_RANGE_MIDDLE = {
            R.attr.tsquare_state_range_middle
    };
    private static final int[] STATE_RANGE_LAST = {
            R.attr.tsquare_state_range_last
    };
    private static final int[] STATE_UNAVAILABLE = {
            R.attr.tsquare_state_unavailable
    };

    private static final int[] STATE_DEACTIVATED = {
            R.attr.tsquare_state_deactivated
    };

    private boolean isSelectable = false;
    private boolean isCurrentMonth = false;
    private boolean isToday = false;
    private boolean isHighlighted = false;
    private boolean isAvailable = false;
    private boolean isDeactivated = false;
    private RangeState rangeState = RangeState.NONE;
    private TextView dayOfMonthTextView;
    private TextView subTitleTextView;

    @SuppressWarnings("UnusedDeclaration") //
    public CalendarCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSelectable(boolean isSelectable) {
        if (this.isSelectable != isSelectable) {
            this.isSelectable = isSelectable;
            refreshDrawableState();
        }
    }

    public void setCurrentMonth(boolean isCurrentMonth) {
        if (this.isCurrentMonth != isCurrentMonth) {
            this.isCurrentMonth = isCurrentMonth;
            refreshDrawableState();
        }
    }

    public void setToday(boolean isToday) {
        if (this.isToday != isToday) {
            this.isToday = isToday;
            refreshDrawableState();
        }
    }

    public void setRangeState(RangeState rangeState) {
        if (this.rangeState != rangeState) {
            this.rangeState = rangeState;
            refreshDrawableState();
        }
    }

    public void setHighlighted(boolean isHighlighted) {
        if (this.isHighlighted != isHighlighted) {
            this.isHighlighted = isHighlighted;
            refreshDrawableState();
        }
    }

    public void setRangeUnavailable(boolean isAvailable) {
        if (this.isAvailable != isAvailable) {
            this.isAvailable = isAvailable;
            refreshDrawableState();
        }
    }

    public void setDeactivated(boolean isDeactivated) {
        if (this.isDeactivated != isDeactivated) {
            this.isDeactivated = isDeactivated;
            refreshDrawableState();
        }
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    public boolean isSelectable() {
        return isSelectable;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public RangeState getRangeState() {
        return rangeState;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace+5);

        if (isSelectable) {
            mergeDrawableStates(drawableState, STATE_SELECTABLE);
        }

        if (isCurrentMonth) {
            mergeDrawableStates(drawableState, STATE_CURRENT_MONTH);
        }

        if (isToday) {
            mergeDrawableStates(drawableState, STATE_TODAY);
        }

        if (isHighlighted) {
            mergeDrawableStates(drawableState, STATE_HIGHLIGHTED);
        }

        if (isAvailable) {
            mergeDrawableStates(drawableState, STATE_UNAVAILABLE);
        }

        if (isDeactivated) {
            mergeDrawableStates(drawableState, STATE_DEACTIVATED);
        }

        if (rangeState == RangeState.FIRST) {
            mergeDrawableStates(drawableState, STATE_RANGE_FIRST);
        } else if (rangeState == RangeState.MIDDLE) {
            mergeDrawableStates(drawableState, STATE_RANGE_MIDDLE);
        } else if (rangeState == RangeState.LAST) {
            mergeDrawableStates(drawableState, STATE_RANGE_LAST);
        }

        return drawableState;
    }

    public void setDayOfMonthTextView(TextView textView) {
        //textView.setTextSize(8);
        dayOfMonthTextView = textView;
    }

    public TextView getSubTitleTextView() {
        if (subTitleTextView == null) {
            throw new IllegalStateException(
                    "You have to setSubTitleTextView in your custom DayViewAdapter."
            );
        }
        return subTitleTextView;
    }

    public void setSubTitleTextView(TextView subTitleTextView) {
        this.subTitleTextView = subTitleTextView;
    }

    public TextView getDayOfMonthTextView() {
        if (dayOfMonthTextView == null) {
            throw new IllegalStateException(
                    "You have to setDayOfMonthTextView in your custom DayViewAdapter."
            );
        }
        return dayOfMonthTextView;
    }
}
