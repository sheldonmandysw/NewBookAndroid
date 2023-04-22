package com.mandysoftware.wordutil;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {
    static final int DOWNLOAD_BUFFER_SIZE = 8192;

    public interface ProgressCallback
    {
        void onDownloadProgress(int loaded, int total);
        void onDownloadComplete(boolean success, @Nullable Exception err, int totalLoaded);
    }

    public static void Download(String inputUrl, String outputFilename, ProgressCallback callback)
    {
        int fileLength = -1;

        try {
            URL url = new URL(inputUrl);
            URLConnection connection = url.openConnection();

            connection.connect();

            fileLength = connection.getContentLength();

            callback.onDownloadProgress(0, fileLength);

            InputStream inputStream = new BufferedInputStream(connection.getInputStream(),
                    DOWNLOAD_BUFFER_SIZE);
            OutputStream outputStream = new FileOutputStream(outputFilename);

            byte [] data = new byte[DOWNLOAD_BUFFER_SIZE];

            int progressCumulative = 0;

            while (inputStream.available() > 0)
            {
                int bytesRead = inputStream.read(data);

                if (bytesRead <= 0)
                {
                    break;
                }

                outputStream.write(data, 0, bytesRead);

                progressCumulative += bytesRead;

                callback.onDownloadProgress(progressCumulative, fileLength);
            }

            outputStream.flush();

            outputStream.close();
            inputStream.close();

            if (progressCumulative != fileLength)
            {
                throw new IOException("Can't download more, but only downloaded " +
                        progressCumulative + " of " + fileLength + " bytes.");
            }
        }
        catch (IOException err)
        {
            callback.onDownloadComplete(false, err, fileLength);
            return;
        }

        callback.onDownloadComplete(true, null, fileLength);
    }
}
