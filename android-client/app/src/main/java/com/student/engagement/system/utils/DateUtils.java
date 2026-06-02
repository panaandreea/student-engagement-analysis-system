package com.student.engagement.system.utils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static String formatIsoToTime(String iso){

        if (iso == null || iso.isEmpty()) {
            return "-";
        }

        try {

            OffsetDateTime offsetDateTime =
                    OffsetDateTime.parse(iso);

            return offsetDateTime.atZoneSameInstant(
                    ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("HH:mm"));

        } catch (Exception e) {
            return "-";
        }
    }

    public static String formatIsoToDateTime(String iso) {

        if (iso == null || iso.isEmpty()) {
            return "-";
        }

        try {
            OffsetDateTime offsetDateTime =
                    OffsetDateTime.parse(iso);

            return offsetDateTime.atZoneSameInstant(
                    ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("dd MMM • HH:mm"));

        } catch (Exception e) {
            return "-";
        }
    }

    public static String formatIsoToDate(String iso) {

        if (iso == null || iso.isEmpty()) {
            return "-";
        }

        try {

            OffsetDateTime offsetDateTime =
                    OffsetDateTime.parse(iso);

            return offsetDateTime.atZoneSameInstant(
                    ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("dd MMM"));

        } catch (Exception e) {
            return "-";
        }
    }

    public static String formatToIsoUTC(java.util.Date date) {
        return date.toInstant().toString();
    }

    public static String getCurrentIsoUTC() {
        return OffsetDateTime.now(ZoneId.of("UTC")).toString();
    }
}