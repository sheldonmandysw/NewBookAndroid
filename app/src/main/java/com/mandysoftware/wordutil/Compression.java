package com.mandysoftware.wordutil;

import java.io.ByteArrayOutputStream;
import java.util.Vector;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Compression
{
    public static byte [] Inflate(byte [] compressedData) throws DataFormatException
    {
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] tmp = new byte[1024];

        while (!inflater.finished())
        {
            int nBytes = inflater.inflate(tmp);

            if (nBytes > 0)
            {
                outputStream.write(tmp, 0, nBytes);
            }
            else
            {
                break;
            }
        }

        if (!inflater.finished())
        {
            throw new DataFormatException("Could not uncompress the data.");
        }

        return outputStream.toByteArray();
    }
}
