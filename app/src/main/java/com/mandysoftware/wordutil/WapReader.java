package com.mandysoftware.wordutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.DataFormatException;

public class WapReader {
    final File inputFile;
    final FileInputStream inputStream;
    final long fileSize;

    final ArrayList<IndexEntry.Lookup> entries = new ArrayList<>();
    long endofs = 0;

    static class PeekWord implements Search.Peek
    {
        static final PeekWord Instance = new PeekWord();

        @Override
        public Comparable peek(final List arr, final int idx, final Object param)
        {
            String chunk = (String) param;
            int wordStart = (Integer) arr.get(idx);
            int idxEndl = chunk.indexOf('\n', wordStart);

            return chunk.substring(wordStart, idxEndl);
        }
    }

    public WapReader(File wapFile) throws IOException
    {
        inputFile = wapFile;
        inputStream = new FileInputStream(wapFile);
        fileSize = inputFile.length();

        readIndex();
    }

    public void close() throws IOException
    {
        inputStream.close();
    }

    protected void readIndex() throws IOException
    {
        long idxendofs = fileSize - 4;
        byte [] fourBytes = new byte[4];

        inputStream.getChannel().position(idxendofs);
        inputStream.read(fourBytes);
        endofs = java.nio.ByteBuffer.wrap(fourBytes).getInt();

        int nidx = (int) ((idxendofs - endofs) / 6);
        inputStream.getChannel().position(endofs);
        entries.clear();

        DataInputStream dis = new DataInputStream(inputStream);
        for (int i = 0; i < nidx; i += 1)
        {
            int ofs = dis.readInt();
            int key = dis.readUnsignedShort();
            entries.add(new IndexEntry.Lookup(key, ofs));
        }
    }

    protected int lookupIndex(String word)
    {
        int key = Key.MakeKey(word);

        if (key >= entries.size() || entries.get(key).key != key)
        {
            key = Search.BinarySearch(
                entries, key, IndexEntry.Lookup.Peek.Instance,
                null, Search.Mode.EQ
            );
        }

        return key;
    }

    protected String lookupWord(String word) throws IOException, DataFormatException
    {
        int ic = lookupIndex(word);

        if (ic == -1)
        {
            throw new IllegalArgumentException("Word index entry not found.");
        }

        long ofsnow = entries.get(ic).value;

        int inext = ic + 1;
        long ofsnext = endofs;

        if (inext < entries.size())
        {
            ofsnext = entries.get(inext).value;
        }

        inputStream.getChannel().position(ofsnow);
        byte [] rawBytes = new byte[(int)(ofsnext - ofsnow)];
        inputStream.read(rawBytes);

        byte [] encodedText = Compression.Inflate(rawBytes);
        String textChunk = new String(encodedText, StandardCharsets.UTF_8);

        int iend = textChunk.length() - 2;

        iend = textChunk.lastIndexOf('\n', iend);

        iend += 1;

        int ofsidx = Integer.valueOf(textChunk.substring(iend));

        String wordIndexAsText = textChunk.substring(ofsidx + 1, iend - 1);
        Scanner wordIndexScanner = new Scanner(wordIndexAsText);
        ArrayList<Integer> wordIndex = new ArrayList<>();

        while (wordIndexScanner.hasNextLine())
        {
            String line = wordIndexScanner.nextLine();

            wordIndex.add(Integer.valueOf(line));
        }

        wordIndexScanner.close();

        int iw = Search.BinarySearch(wordIndex, word, PeekWord.Instance, textChunk, Search.Mode.EQ);

        if (iw == -1)
        {
            throw new IllegalArgumentException("Word not found.");
        }

        int entryStart = textChunk.indexOf('\n', wordIndex.get(iw)) + 1;
        int entryEnd = ofsidx;

        if (iw + 1 < wordIndex.size())
        {
            entryEnd = wordIndex.get(iw + 1);
        }

        return textChunk.substring(entryStart, entryEnd);
    }
}
