package com.mandysoftware.wordutil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.DataFormatException;

public class WapTest {
    @Test
    void testInit() throws IOException
    {
        File inputFile = new File("src/test/resources/uk.wap");
        WapReader reader = new WapReader(inputFile);
        reader.close();

        Assertions.assertEquals(44182, reader.entries.size());
        Assertions.assertEquals(1, reader.entries.get(0).key);
        Assertions.assertEquals(4, reader.entries.get(0).value);
        Assertions.assertEquals(2, reader.entries.get(1).key);
        Assertions.assertEquals(900, reader.entries.get(1).value);
    }

    @Test
    void testLookup() throws IOException, DataFormatException
    {
        File appleFile = new File("src/test/resources/uk.apple.txt");
        byte [] appleRaw = Files.readAllBytes(appleFile.toPath());
        String appleText = new String(appleRaw, StandardCharsets.UTF_8);

        File lastFile = new File("src/test/resources/uk.last.txt");
        byte [] lastRaw = Files.readAllBytes(lastFile.toPath());
        String lastText = new String(lastRaw, StandardCharsets.UTF_8);

        File inputFile = new File("src/test/resources/uk.wap");
        WapReader reader = new WapReader(inputFile);

        Assertions.assertEquals(5358, reader.lookupIndex("apple"));

        Assertions.assertEquals(25380,
                reader.lookupIndex("(бути) в своєму репертуарі"));

        Assertions.assertEquals(26539, reader.lookupIndex("\uD83D\uDF7F"));

        String wordEntry = reader.lookupWord("apple");

        Assertions.assertEquals(appleText, wordEntry);

        wordEntry = reader.lookupWord("(бути) в своєму репертуарі");

        Assertions.assertEquals("#ПЕРЕНАПРАВЛЕННЯ [[в своєму репертуарі]]", wordEntry);

        wordEntry = reader.lookupWord("\uD83D\uDF7F");

        Assertions.assertEquals(lastText, wordEntry);

        Exception resultException = null;

        try {
            reader.lookupWord("aaslkghaslkdghkashdglkasghl");
        }
        catch (IllegalArgumentException err)
        {
            resultException = err;
        }

        Assertions.assertNotNull(resultException);
        Assertions.assertEquals("Word not found.", resultException.getMessage());

        reader.close();
    }

    @Test
    void testKey()
    {
        Assertions.assertEquals(7992, Key.MakeKey("apple"));
    }
}
