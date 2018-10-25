package com.reynouard.alexis.chronos.model.csv;

import android.util.Log;

import com.reynouard.alexis.chronos.model.DateConverter;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;
import com.reynouard.alexis.chronos.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * tools.ietf.org/html/tfc4180
 */
public class CsvAdapter {

    public static final String RECORD_BREAK = "\r\n";

    private static final Pattern FIELD_SEPARATORS = Pattern.compile(",");

    private CsvAdapter(){}

    public static String toCsv(Task task) {
        final CsvStringBuilder builder = new CsvStringBuilder(64);
        builder.appendField(task.getId())
                .appendField(task.getName())
                .appendField(task.getDescription())
                .appendField(task.getRepetition())
                .appendField(task.getHyperPeriod())
                .appendField(task.getDuration())
                .appendField(task.isDurationFlexible())
                .appendField(task.isImportant())
                .appendField(task.isRepetitive());

        return builder.toString();
    }

    public static String toCsv(Work work) {
        return new CsvStringBuilder(32)
                .appendField(work.getId())
                .appendField(work.getTaskId())
                .appendField(work.getDate())
                .toString();
    }

    public static Task toTask(BufferedReader reader) throws NumberFormatException, IndexOutOfBoundsException, IOException {
        String record = getRecord(reader);
        if (record.equals("")) {
            return null;
        }
        return toTask(record);
    }

    public static Work toWork(BufferedReader reader) throws NumberFormatException, IndexOutOfBoundsException, IOException {
        String record = getRecord(reader);
        if (record.equals("")) {
            return null;
        }
        return toWork(record);
    }

    public static Task toTask(String csv) throws NumberFormatException, IndexOutOfBoundsException, IOException {
        try {
            List<String> csvFields = getFields(csv);
            int id = Integer.valueOf(csvFields.get(0));
            int repetition = Integer.valueOf(csvFields.get(3));
            int hyperPeriod = Integer.valueOf(csvFields.get(4));
            int duration = Integer.valueOf(csvFields.get(5));
            boolean durationFlexible = csvFields.get(6).equals("1");
            boolean isImportant = csvFields.get(7).equals("1");
            boolean isRepetitive = csvFields.get(8).equals("1");
            return new Task(id, csvFields.get(1), csvFields.get(2), repetition, hyperPeriod, duration, durationFlexible, isImportant, isRepetitive);
        }
        catch (NumberFormatException | IndexOutOfBoundsException | IOException e) {
            Log.e("CSV", "toTask: " + e.getMessage());
            throw e;
        }
    }

    public static Work toWork(String csv) throws NumberFormatException, IndexOutOfBoundsException, IOException {
        try {
            List<String> csvFields = getFields(csv);
            int id = Integer.valueOf(csvFields.get(0));
            int taskId = Integer.valueOf(csvFields.get(1));
            Date date = DateConverter.fromIsoFormat(csvFields.get(2));
            return new Work(id, taskId, date);
        }
        catch (NumberFormatException | IndexOutOfBoundsException | IOException e) {
            Log.e("CSV", "toTask: " + e.getMessage());
            throw e;
        }
    }

    private static List<String> getFields(String line) throws IOException {

        // Split fields: remove ,
        String[] strings = FIELD_SEPARATORS.split(line);
        // Merge strings from same field but separated due to a comma
        List<String> fields = new ArrayList<>(strings.length);
        String field = "";
        for (int i = 0; i < strings.length; ++i) {
            field = strings[i];
            while (i < strings.length-1 && StringUtils.countOccurrencesOf("\"", field) % 2 != 0) {
                field += "," + strings[++i];
            }
            if (field.length() > 0) {
                if (field.charAt(0) == '"' || field.charAt(field.length() - 1) == '"') {
                    if (field.charAt(0) == field.charAt(field.length() - 1)) {
                        field = field.substring(1,field.length()-1);
                    }
                    else {
                        throw new IOException("Ill-formed csv " + line);
                    }
                }
            }
            fields.add(field);
        }
        if (strings.length == 0 || StringUtils.countOccurrencesOf("\"", field) % 2 != 0) {
            throw new IOException("Ill-formed csv " + line);
        }
        // Double " to simple "
        for (int i = 0; i < strings.length; ++i) {
            strings[i] = strings[i].replace("\"\"", "\"");
        }
        return fields;
    }

    private static String getRecord(BufferedReader reader) throws IOException {
        StringBuilder recordBuilder = new StringBuilder(reader.readLine());
        while (StringUtils.countOccurrencesOf("\"", recordBuilder.toString()) % 2 != 0) {
            recordBuilder.append(reader.readLine());
        }
        return recordBuilder.toString();
    }
}

class CsvStringBuilder {

    private StringBuilder mStringBuilder;
    private Pattern mSpecialCharacters = Pattern.compile("(?m)(\\r\\n)|[\\r\\n\",]");

    CsvStringBuilder(int capacity) {
        mStringBuilder = new StringBuilder(capacity);
    }

    public CsvStringBuilder appendField(long field) {
        mStringBuilder.append(field).append(',');
        return this;
    }

    public CsvStringBuilder appendField(String field) {
        if (mSpecialCharacters.matcher(field).find()) {
            mStringBuilder.append('"')
                    .append(field.replace("\"", "\"\""))
                    .append('"')
                    .append(',');
        }
        else {
            mStringBuilder.append(field).append(',');
        }
        return this;
    }

    public CsvStringBuilder appendField(boolean b) {
        mStringBuilder.append(b ? '1' : '0').append(',');
        return this;
    }

    public CsvStringBuilder appendField(Date date) {
        appendField(DateConverter.fromDate(date));
        return this;
    }

    @Override
    public String toString() {
        return mStringBuilder.toString().substring(0, mStringBuilder.length() - 1);
    }
}