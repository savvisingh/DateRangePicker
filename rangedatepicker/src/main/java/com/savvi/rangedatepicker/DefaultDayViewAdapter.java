package com.savvi.rangedatepicker;

import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DefaultDayViewAdapter implements DayViewAdapter {
    @Override
    public void makeCellView(CalendarCellView parent) {

        /*TextView tv = new TextView(parent.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        parent.addView(tv);*/

        TextView subTitleTextView = new TextView(
                new ContextThemeWrapper(parent.getContext(), R.style.CalendarCell_SubTitle));
        subTitleTextView.setDuplicateParentStateEnabled(true);
        subTitleTextView.setGravity(Gravity.BOTTOM);
        parent.addView(subTitleTextView);


        TextView textView = new TextView(
                new ContextThemeWrapper(parent.getContext(), R.style.CalendarCell_CalendarDate));
        textView.setDuplicateParentStateEnabled(true);
        parent.addView(textView);

        parent.setDayOfMonthTextView(textView);
        parent.setSubTitleTextView(subTitleTextView);
    }
}
