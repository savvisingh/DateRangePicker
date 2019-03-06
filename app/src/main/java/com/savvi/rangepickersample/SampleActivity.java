package com.savvi.rangepickersample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.savvi.rangedatepicker.*;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SampleActivity extends AppCompatActivity {


    CalendarPickerView calendar;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 10);

        final Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -10);

        calendar = (CalendarPickerView) findViewById(R.id.calendar_view);
        button = (Button) findViewById(R.id.get_selected_dates);
        ArrayList<Integer> list = new ArrayList<>();
        list.add(2);

        calendar.deactivateDates(list);
        ArrayList<Date> arrayList = new ArrayList<>();
        try {
            SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy");
            String strdate = "22-2-2019";
            String strdate2 = "26-2-2019";
            Date newdate = dateformat.parse(strdate);
            Date newdate2 = dateformat.parse(strdate2);
            arrayList.add(newdate);
            arrayList.add(newdate2);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        calendar.init(lastYear.getTime(), nextYear.getTime(), new SimpleDateFormat("MMMM, YYYY", Locale.getDefault())) //
                .inMode(CalendarPickerView.SelectionMode.RANGE) //
                .withSelectedDate(new Date())
                .withDeactivateDates(list)
                .withSubTitles(getSubTitles())
                .withHighlightedDates(arrayList);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(SampleActivity.this, "list " + calendar.getSelectedDates().toString(), Toast.LENGTH_LONG).show();
            }
        });


    }

    private ArrayList<SubTitle> getSubTitles() {
        final ArrayList<SubTitle> subTitles = new ArrayList<>();
        subTitles.add(new SubTitle(new Date(), "₹ 1000"));
        return subTitles;
    }
}
