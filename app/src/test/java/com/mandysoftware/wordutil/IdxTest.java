package com.mandysoftware.wordutil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

public class IdxTest {
    @Test
    void testInit() throws IOException
    {
        File inputFile = new File("src/test/resources/uk.idx");
        IdxReader reader = new IdxReader(inputFile);
        reader.close();

        Assertions.assertEquals(9, reader.entries.size());
        Assertions.assertEquals(684765649, reader.entries.get(0).key_a);
        Assertions.assertEquals(3501314229L, reader.entries.get(0).key_b);
        Assertions.assertEquals(0, reader.entries.get(0).value);
        Assertions.assertEquals(3501314229L, reader.entries.get(1).key_a);
        Assertions.assertEquals(3501510846L, reader.entries.get(1).key_b);
        Assertions.assertEquals(66570, reader.entries.get(1).value);
    }

    @Test
    void testSuggest() throws IOException, DataFormatException
    {
        File inputFile = new File("src/test/resources/uk.idx");
        IdxReader reader = new IdxReader(inputFile);

        // First, we test the index bounds.
        // Can we correctly look up which chunks contain the specified word prefix?
        IdxReader.Bounds indexBounds;

        // Case: the very first word of the word list.
        indexBounds = reader.findIndexBounds("(бути)");

        Assertions.assertEquals(0, indexBounds.bound_a);
        Assertions.assertEquals(0, indexBounds.bound_b);

        // Case: the edge of 2 chunks.
        indexBounds = reader.findIndexBounds("безхитріс");

        Assertions.assertEquals(0, indexBounds.bound_a);
        Assertions.assertEquals(1, indexBounds.bound_b);

        // Case: a word that is in the middle of a chunk in the center of the word list.
        indexBounds = reader.findIndexBounds("Коров");

        Assertions.assertEquals(4, indexBounds.bound_a);
        Assertions.assertEquals(4, indexBounds.bound_b);

        // Case: the very last word of the word list.
        indexBounds = reader.findIndexBounds("\uD83D\uDF7F");

        Assertions.assertEquals(8, indexBounds.bound_a);
        Assertions.assertEquals(8, indexBounds.bound_b);

        // Next, we test the suggestion results.
        ArrayList<String> suggestions;

        // Test on the very first word of the word list.
        suggestions = reader.suggest("(бути)");

        Assertions.assertEquals(1, suggestions.size());
        Assertions.assertEquals("(бути) в своєму репертуарі", suggestions.get(0));

        // Test on a prefix that actually is on the border of 2 chunks:
        // i.e., the first suggested word is the last word of chunk 0
        // while the second suggested word is the first word of chunk 1.
        // This test case makes sure that chunk edges are handled seamlessly.
        suggestions = reader.suggest("безхитріс");

        Assertions.assertEquals(2, suggestions.size());
        Assertions.assertEquals("безхитрісний", suggestions.get(0));
        Assertions.assertEquals("безхитрісно", suggestions.get(1));

        // Test on a word that is in the middle of a chunk in the middle of the word list.
        suggestions = reader.suggest("Коров");

        Assertions.assertEquals(10, suggestions.size());
        Assertions.assertEquals("коров'як", suggestions.get(0));
        Assertions.assertEquals("корова", suggestions.get(1));
        Assertions.assertEquals("коровай", suggestions.get(2));
        Assertions.assertEquals("коровайка", suggestions.get(3));
        Assertions.assertEquals("Коровайна", suggestions.get(4));
        Assertions.assertEquals("Коровайник", suggestions.get(5));
        Assertions.assertEquals("коровайниця", suggestions.get(6));
        Assertions.assertEquals("короварня", suggestions.get(7));
        Assertions.assertEquals("Коровчинський", suggestions.get(8));
        Assertions.assertEquals("коров’як", suggestions.get(9));

        // Test on the very last word of the word list.
        suggestions = reader.suggest("\uD83D\uDF7F");

        Assertions.assertEquals(1, suggestions.size());
        Assertions.assertEquals("\uD83D\uDF7F", suggestions.get(0));

        // And of course clean up.
        reader.close();
    }

}
