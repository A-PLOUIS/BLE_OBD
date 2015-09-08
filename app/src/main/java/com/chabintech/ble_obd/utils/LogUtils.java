package com.chabintech.ble_obd.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by PIERRE-LOUIS Antonio on 11/08/2015.
 */
public class LogUtils {

    private static final String LOG_TAG = LogUtils.class.getSimpleName();
    private static final String LOG_FOLDER = Environment.getExternalStorageDirectory() + File.separator + "BLE_OBD" + File.separator;

    public static void logError(Exception p_exception) {
        String logPath = LOG_FOLDER + "errors.txt";
        Log.d(LOG_TAG, "Wrote log to : " + logPath);
        try {
            FileOutputStream out = new FileOutputStream(logPath, Boolean.TRUE);
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write("Exception\n");
            writer.write("Date : " + new Date(System.currentTimeMillis()) + '\n' + '\n');
            writer.write(p_exception.getLocalizedMessage() + '\n');
            for (StackTraceElement elem : p_exception.getStackTrace()) {
                writer.write(elem.toString() + "\n");
            }
            writer.write("\n\n\n");
            writer.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logResult(String command, String result, Long p_latency) {
        String logPath = getLatestResultFile();
        File resultFile = new File(logPath);
        Log.d(LOG_TAG, "Wrote log to : " + logPath);

        byte[] commandBytes = command.getBytes();
        String commandHex = "";
        for (byte byteTo : commandBytes) {
            commandHex += "" + Integer.toHexString(byteTo);
        }

        result = result.replaceAll("\\t", "").replaceAll("\\n", "").replaceAll("\\r", "");
        byte[] resultBytes = result.getBytes();
        String stringHex = "";
        for (byte byteTo : resultBytes) {
            stringHex += "" + Integer.toHexString(byteTo);
        }

        try {
            FileWriter writer = new FileWriter(resultFile, Boolean.TRUE);
            //Write row
            String row = "" + command
                    + '\t' + commandHex
                    + '\t' + result
                    + '\t' + stringHex
                    + '\t' + p_latency + '\n';
            writer.write(row);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logInfo(String p_message) {
        String logPath = LOG_FOLDER + "info.txt";

        try {
            FileOutputStream out = new FileOutputStream(logPath, Boolean.TRUE);
            OutputStreamWriter writer = new OutputStreamWriter(out);
            writer.write(p_message + '\n' + '\n');
            writer.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createNewResultFileForConnexion() {
        Date currentDate = new Date();
        SimpleDateFormat localDateFormat = new SimpleDateFormat("yyyyMMdd");
        String logPath = LOG_FOLDER + "result_" +
                localDateFormat.format(currentDate)
                + "_" + System.currentTimeMillis() + ".txt";

        try {
            File logFile = new File(logPath);
            FileWriter writer = new FileWriter(logFile, Boolean.TRUE);
            writer.write("Command\tCommand(hex)\tResult\tResult(Hex)\tLatency\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getLatestResultFile() {
        String sResultPath = null;

        File logsDir = new File(LOG_FOLDER);
        File[] files = logsDir.listFiles(new LogFilter());

        if (files != null && files.length > 0) {
            File mostRecentFile = files[0];
            for (File file : files) {
                if (mostRecentFile.lastModified() < file.lastModified()) {
                    mostRecentFile = file;
                }
            }

            sResultPath = mostRecentFile.getAbsolutePath();
        }
        return sResultPath;
    }

    private static class LogFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            boolean result = false;

            if (file.isFile()) {
                String fileName = file.getName();
                result = fileName.startsWith("result");
            }

            return result;
        }
    }
}
