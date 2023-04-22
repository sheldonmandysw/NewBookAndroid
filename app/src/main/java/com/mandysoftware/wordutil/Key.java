package com.mandysoftware.wordutil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Key {
    public static int MakeKey(String word)
    {
        try
        {
            byte[] wordBytes = word.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte [] digest = md.digest(wordBytes);

            int lo = (digest[1] & 0xFF);
            int hi = (digest[0] & 0xFF);

            return hi * 256 + lo;
        }
        catch (NoSuchAlgorithmException err)
        {
            return -1;
        }
    }
}
