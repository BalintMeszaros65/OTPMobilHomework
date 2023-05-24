package com.codecool.homework.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class DateValidatorForNotIsoFormat {

    public boolean isValid(String date) {
        try {
            LocalDate.parse(date.replace(".", ""), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException dateTimeParseException) {
            return false;
        }
        return true;
    }
}
