package com.savvi.rangedatepicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SubTitle {
    private final Date date;
    private final String title;

    public SubTitle(Date date, String title) {
        this.date = date;
        this.title = title;
    }

    public static SubTitle getByDate(ArrayList<SubTitle> subTitles, Date date) {
        if (subTitles != null && subTitles.size() > 0) {
            for (final SubTitle subTitle : subTitles) {
                if (isSameDay(subTitle.getDate(), date)) {
                    return subTitle;
                }
            }
        }
        return null;
    }

    private static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        boolean isSameDay = cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
        return isSameDay;
    }

    public Date getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }
}
