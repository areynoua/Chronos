package com.reynouard.alexis.chronos.model.csv;

import com.reynouard.alexis.chronos.model.DateConverter;
import com.reynouard.alexis.chronos.model.Task;
import com.reynouard.alexis.chronos.model.Work;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * tools.ietf.org/html/tfc4180
 */
public class CsvAdapter {

    public static final String RECORD_BREAK = "\r\n";

    private static final Pattern FIELD_SEPARATORS = Pattern.compile("\"?,\"?");

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

    public static Task toTask(String csv) {
        String[] csvFields = getFields(csv);
        int id = Integer.valueOf(csvFields[0]);
        int repetition = Integer.valueOf(csvFields[3]);
        int hyperPeriod = Integer.valueOf(csvFields[4]);
        int duration = Integer.valueOf(csvFields[5]);
        boolean durationFlexible = csvFields[6].equals("1");
        boolean isImportant = csvFields[7].equals("1");
        boolean isRepetitive = csvFields[8].equals("1");
        return new Task(id, csvFields[1], csvFields[2], repetition, hyperPeriod, duration, durationFlexible, isImportant, isRepetitive);
    }

    public static Work toWork(String csv) {
        String[] csvFields = getFields(csv);
        int id = Integer.valueOf(csvFields[0]);
        int taskId = Integer.valueOf(csvFields[1]);
        Date date = DateConverter.fromIsoFormat(csvFields[2]);
        return new Work(id, taskId, date);
    }

    private static String[] getFields(String line) {
        String[] strings = FIELD_SEPARATORS.split(line);
        if (strings[0].charAt(0) == '"') {
            strings[0] = strings[0].substring(1);
        }
        int l = strings.length;
        int ll = strings[l-1].length();
        if (strings[l-1].charAt(ll) == '"') {
            strings[l-1] = strings[l-1].substring(0, ll - 1);
        }
        for (int i = 0; i < strings.length; ++i) {
            strings[i] = strings[i].replace("\"\"", "\"");
        }
        return strings;
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