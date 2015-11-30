package com.example.lily.timeparsing;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lily on 2015/11/26.
 */
public class SmartTimeParser {
  private static final String PATTEN_FOR_SMART_TIME = "(([今明后])[天日]|下?(?:星期|礼拜|周)([一二三四五六日天])|((?:19|20)\\d\\d)[- /.年](0?[1-9]|1[012])[- /.月](0[1-9]|[12][0-9]|3[01])[ 日])?(?:\\s*)(早上|早晨|上午|中午|下午|晚上|今晚)?(?:\\s*)(((?<=\\D)[01]?\\d|2[0-3]|([一二三四五六七八九]|十[一二三四五六七八九]?|二十[一二三四]?))[点时:]((半|[0-5]?\\d))?)";

  public static List<Long> parse(String str) {
    ArrayList<Long> times = new ArrayList<>();
    Pattern p1 = Pattern.compile(PATTEN_FOR_SMART_TIME);

    Matcher matcher = p1.matcher(str);
    int count = matcher.groupCount();
    Log.i("Regex", "GroupCount = " + count);
    while (matcher.find()) {
      Log.i("Regex", "Found the text: " + matcher.group() + " starting at: " + matcher.start() + " ending at: " + matcher.end());

      long time = parseTime(matcher);
      if (time != -1) {
        times.add(time);
      }
    }

    return times;
  }

  private static long  parseTime(Matcher matcher) {
    Calendar c = Calendar.getInstance();
    long miliseconds = c.getTimeInMillis();

    //parse date
    //Relative Date (今明后)
    String relativeDays = matcher.group(2);
    //Week 星期一
    String weekDays = matcher.group(3);
    //Absolute Time (2015.11.30)
    String year     = matcher.group(4);
    String month    = matcher.group(5);
    String day      = matcher.group(6);

    String daysOffset = matcher.group(1);
    String timeSpan           = matcher.group(7);
    if (!TextUtils.isEmpty(daysOffset) || !TextUtils.isEmpty(timeSpan)) {
      if (!TextUtils.isEmpty(weekDays)) { //周一、周二...
        if (TextUtils.equals(weekDays, "一")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        } else if (TextUtils.equals(weekDays, "二")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        } else if (TextUtils.equals(weekDays, "三")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        } else if (TextUtils.equals(weekDays, "四")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        } else if (TextUtils.equals(weekDays, "五")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        } else if (TextUtils.equals(weekDays, "六")) {
          c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        } else {
          c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        }

        if (!TextUtils.isEmpty(matcher.group(2)) && TextUtils.equals(matcher.group(2), "下")) {
          miliseconds = c.getTimeInMillis() + DateUtils.WEEK_IN_MILLIS;
          c.setTimeInMillis(miliseconds);
        }

      } else if (!TextUtils.isEmpty(relativeDays)){ //今天、明天、后天
          if (TextUtils.equals(relativeDays, "明")) {
            miliseconds += DateUtils.DAY_IN_MILLIS;
          } else if (TextUtils.equals(relativeDays, "后")) {
            miliseconds += 2 * DateUtils.DAY_IN_MILLIS;
          }

          c.setTimeInMillis(miliseconds);
      } else if (!TextUtils.isEmpty(year) && !TextUtils.isEmpty(month) && !TextUtils.isEmpty(day)) {
        try {
          c.set(Calendar.YEAR, Integer.valueOf(year));
          c.set(Calendar.MONTH, Integer.valueOf(month)-1);
          c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
        } catch (NumberFormatException e) {
          e.printStackTrace();
          return -1; //日期格式不对
        }
      }

    } else {
      return -1; //日期和时间段不可同时为空
    }

    //parse time
    String hourToParse  = matcher.group(9);
    boolean isHourInChinese = matcher.group(10)!=null;
    int hour = 0;
    if (isHourInChinese) {
      if (hourToParse.startsWith("十")) {
        hour = 10;
        hourToParse = hourToParse.substring(1);
      } else if (hourToParse.startsWith("二十")) {
        hour = 20;
        hourToParse = hourToParse.substring(2);
      }

      if (!TextUtils.isEmpty(hourToParse)) {
        if (TextUtils.equals(hourToParse, "一")) {
          hour += 1;
        } else if (TextUtils.equals(hourToParse, "二")) {
          hour += 2;
        } else if (TextUtils.equals(hourToParse, "三")) {
          hour += 3;
        } else if (TextUtils.equals(hourToParse, "四")) {
          hour += 4;
        } else if (TextUtils.equals(hourToParse, "五")) {
          hour += 5;
        } else if (TextUtils.equals(hourToParse, "六")) {
          hour += 6;
        } else if (TextUtils.equals(hourToParse, "七")) {
          hour += 7;
        } else if (TextUtils.equals(hourToParse, "八")) {
          hour += 8;
        } else if (TextUtils.equals(hourToParse, "九")) {
          hour += 9;
        }
      }
    } else {
      hour = Integer.valueOf(hourToParse);
    }

    hour = adjustHours(timeSpan, hour);
    if (hour == -1) {
      return -1;
    }

    //parse minute
    String minuteToParse = matcher.group(11);
    int minute = 0;
    if (!TextUtils.isEmpty(minuteToParse)) {
      if (TextUtils.equals(minuteToParse, "半")) {
        minute = 30;
      } else {
        try {
          minute = Integer.valueOf(minuteToParse);
        }catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
    }
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    miliseconds = c.getTimeInMillis();
    Log.d("Time Parsed: ", new Date(miliseconds).toString());

    return miliseconds;
  }

  private static int adjustHours(String span, int hour) {
    if (TextUtils.isEmpty(span)) {
      return hour;
    } else {
      int invalidHour = -1;
      if (span.contains("早")||TextUtils.equals(span,"上午")) {
        if (hour>12) {
          return  invalidHour;
        }
      } else if (TextUtils.equals(span, "中午")) {
        if ((hour>14) || (hour>2&&hour<11)) {
          return invalidHour;
        } else if (hour<2 && hour>0) {
          hour += 12;
        }
      } else if (TextUtils.equals(span, "下午")) {
        if (hour>19 || hour<1 || (hour>7&&hour<12)) {
          return invalidHour;
        } else if (hour>0 && hour<=7) {
          hour += 12;
        }
      } else { //晚上
        if ((hour>12&&hour<18) || (hour>=0&&hour<6)) {
          return invalidHour;
        } else if (hour>=6 && hour<=12){
          hour += 12;
        }
      }

      return  hour;
    }
  }

  public static List<Long> filterOutPastTime(List<Long> times) {
    ArrayList<Long> filteredTime = new ArrayList<>();
    long current = Calendar.getInstance().getTimeInMillis();
    if (times != null) {
      for (Long time:times) {
        if (time > current) {
          filteredTime.add(time);
        }
      }
    }

    return filteredTime;
  }

}
