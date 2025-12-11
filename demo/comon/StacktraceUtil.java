package com.example.demo.comon;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceUtil {
    public static String toSingleLine(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        return sw.toString()
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    public static String shortMessage(Exception e) {
        return e.getClass().getName() + ": " + e.getMessage();
    }
}
