package com.codecool.homework.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Component
public class CsvFileReader {
    // **************************************************
    // Public methods
    // **************************************************
    /**
     * Reads the data from given file and throws FileNotFoundException if not found.
     *
     * @return data read from the file as a 2d matrix of Strings.
     */
    public List<List<String>> readCsvData(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        List<List<String>> data = new ArrayList<>();
        while (scanner.hasNextLine()) {
            List<String> row = List.of(scanner.nextLine().split(";"));
            data.add(row);
        }
        scanner.close();
        return data;
    }
}
