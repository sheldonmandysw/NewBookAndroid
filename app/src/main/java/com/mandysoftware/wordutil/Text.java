package com.mandysoftware.wordutil;

import java.text.DecimalFormat;
import java.util.Locale;

public class Text {
    final static String [] units = new String [] { "B", "k", "M", "G", "T" };

    public static String formatFileSize(long fileSize, Locale locale)
    {
        if (fileSize <= 0)
        {
            return "0";
        }

        float simpleSize = fileSize;
        int unitIndex = 0;

        while (simpleSize > 1024)
        {
            simpleSize /= 1024;
            unitIndex += 1;
        }

        return String.format(locale, "%.1f", simpleSize) + units[unitIndex];
    }
}
