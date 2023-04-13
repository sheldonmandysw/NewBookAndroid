package com.mandysoftware.wordutil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Key {
    public static int MakeKey(String word)
    {
        try
        {
            byte[] wordBytes = word.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte [] digest = md.digest(wordBytes);

            return digest[0] * 256 + digest[1];
        }
        catch (NoSuchAlgorithmException err)
        {
            return -1;
        }
    }
}
