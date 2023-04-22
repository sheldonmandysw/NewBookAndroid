package com.mandysoftware.wordutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.zip.DataFormatException;

public class IdxReader {
    final int DEFAULT_SUGGESTION_COUNT_LIMIT = 10;

    final File inputFile;
    final FileInputStream inputStream;
    final long fileSize;

    final ArrayList<IndexEntry.Suggest> entries = new ArrayList<>();
    long endofs = 0;

    static class PeekPrefix implements Search.Peek
    {
        static final PeekPrefix Instance = new PeekPrefix();

        @Override
        public Comparable peek(final List arr, final int idx, final Object param)
        {
            Integer prefixLen = (Integer) param;
            String text = (String) arr.get(idx);

            if (text.length() > prefixLen)
            {
                text = text.substring(0, prefixLen);
            }

            return text;
        }
    }

    public class Bounds
    {
        final int bound_a;
        final int bound_b;
        final int cumulative;

        public Bounds(int a, int b)
        {
            bound_a = a;
            bound_b = b;
            cumulative = 0;
        }

        public Bounds(int a, int b, int cumulative)
        {
            bound_a = a;
            bound_b = b;
            this.cumulative = cumulative;
        }
    }

    public IdxReader(File idxFile) throws IOException
    {
        inputFile = idxFile;
        inputStream = new FileInputStream(idxFile);
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
        endofs = java.nio.ByteBuffer.wrap(fourBytes)
                .order(ByteOrder.BIG_ENDIAN).getInt();

        int nidx = (int) ((idxendofs - endofs) / 16);
        inputStream.getChannel().position(endofs);
        entries.clear();

        DataInputStream dis = new DataInputStream(inputStream);
        for (int i = 0; i < nidx; i += 1)
        {
            int ofs = dis.readInt();
            int cnt = dis.readInt();
            int key_a = dis.readInt();
            int key_b = dis.readInt();
            entries.add(new IndexEntry.Suggest(key_a, key_b, ofs, cnt));
        }
    }

    Bounds findIndexBounds(String prefix)
    {
        byte [] prefixRaw = prefix.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        long key = 0;
        long mask = 0;

        // We build the key and the mask which are both Long values to
        // compare with in the binary search.
        for (int i = 0; i < prefixRaw.length && i < IndexEntry.SUGGEST_KEY_LENGTH; i += 1)
        {
            int shiftBits = 8 * (IndexEntry.SUGGEST_KEY_LENGTH - i - 1);

            key += (Byte.toUnsignedLong(prefixRaw[i]) << shiftBits);
            mask |= (0xFFL << shiftBits);
        }

        // For a non-end-point, there is no guarantee that no words came before this prefix
        // that we're looking for, so we have to search using the L (<) and G (>) modes.
        int ic_idx_a = Search.BinarySearch(entries, key, IndexEntry.Suggest.PeekA.Instance, mask,
                Search.Mode.L);
        int ic_idx_b = Search.BinarySearch(entries, key, IndexEntry.Suggest.PeekB.Instance, mask,
                Search.Mode.G);

        // For the endpoints, however, the above will return -1 because for the
        // very FIRST word, there's no other word that's less than (<) this prefix,
        // so we check for that by matching LE (<=) instead which is inclusive.
        if (ic_idx_a == -1)
        {
            ic_idx_a = Search.BinarySearch(entries, key, IndexEntry.Suggest.PeekA.Instance, mask,
                    Search.Mode.LE);
        }

        // Similarly, for the LAST word, there's no other word that's greater than (>)
        // this prefix, so we likewise match with GE (>=) which includes the prefix entry.
        if (ic_idx_b == -1)
        {
            ic_idx_b = Search.BinarySearch(entries, key, IndexEntry.Suggest.PeekB.Instance, mask,
                    Search.Mode.GE);
        }

        return new Bounds(ic_idx_a, ic_idx_b);
    }

    Bounds findIndexBounds(int wordIndex)
    {
        int cumulativeCount = 0;
        int foundIndex = -1;

        for (int i = 0; i < entries.size(); i += 1)
        {
            IndexEntry.Suggest entry = entries.get(i);

            if (wordIndex >= cumulativeCount && wordIndex < cumulativeCount + entry.count)
            {
                foundIndex = i;
                break;
            }

            cumulativeCount += entry.count;
        }

        return new Bounds(foundIndex, foundIndex, cumulativeCount);
    }

    ArrayList<String> decodeChunksIntoLines(Bounds bounds, String prefix, int countLimit)
            throws IOException, DataFormatException
    {
        ArrayList<String> result;
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);

        if (countLimit > 0)
        {
            result = new ArrayList<>(countLimit);
        }
        else
        {
            result = new ArrayList<>();
        }

        for (int idxChunk = bounds.bound_a; idxChunk <= bounds.bound_b; idxChunk += 1)
        {
            long ofsChunkStart = entries.get(idxChunk).value;
            long ofsChunkEnd = endofs;

            if (idxChunk + 1 < entries.size())
            {
                ofsChunkEnd = entries.get(idxChunk + 1).value;
            }

            byte [] compressedBytes = new byte[(int)(ofsChunkEnd - ofsChunkStart)];

            inputStream.getChannel().position(ofsChunkStart);
            inputStream.read(compressedBytes);

            byte [] rawBytes = Compression.Inflate(compressedBytes);
            String text = new String(rawBytes, StandardCharsets.UTF_8);
            Scanner scanner = new Scanner(text);

            while (scanner.hasNextLine() && result.size() < countLimit)
            {
                String line = scanner.nextLine();
                String word = line.trim();

                // Note that prefix can be the empty string "" which is perfectly OK.
                // In that case, all words will be matched (i.e., the below 'continue'
                // will never be reached, such that we add all words to the return list).
                //
                if (!word.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                {
                    continue;
                }

                if (word.length() > 0)
                {
                    result.add(word);
                }
            }

            scanner.close();

            if (result.size() >= countLimit)
            {
                break;
            }
        }

        return result;
    }

    ArrayList<String> decodeChunksIntoLines(Bounds bounds, String prefix)
            throws IOException, DataFormatException
    {
        return decodeChunksIntoLines(bounds, prefix, DEFAULT_SUGGESTION_COUNT_LIMIT);
    }

    public ArrayList<String> suggest(String prefix, int suggestionCountLimit)
            throws IOException, DataFormatException
    {
        Bounds indexBounds = findIndexBounds(prefix);

        return decodeChunksIntoLines(indexBounds, prefix, suggestionCountLimit);
    }

    public ArrayList<String> suggest(String prefix) throws IOException, DataFormatException
    {
        return suggest(prefix, DEFAULT_SUGGESTION_COUNT_LIMIT);
    }

    public int countWords()
    {
        int result = 0;

        for (int i = 0; i < entries.size(); i += 1)
        {
            result += entries.get(i).count;
        }

        return result;
    }

    public String readWord(int index) throws IOException, DataFormatException
    {
        Bounds bounds = findIndexBounds(index);
        int wordsInChunk = (int) entries.get(bounds.bound_a).count;
        int subIndex = index - bounds.cumulative;

        // We just want to decode up to the subIndex word since that's all we need.
        List<String> words = decodeChunksIntoLines(bounds, "", subIndex + 1);

        return words.get(subIndex);
    }

    public String randomWord() throws IOException, DataFormatException
    {
        int wordCount = countWords();
        int wordIndex = (int) Math.floor(wordCount * Math.random());

        return readWord(wordIndex);
    }

    public String firstWord() throws IOException, DataFormatException
    {
        return readWord(0);
    }

    public String lastWord() throws IOException, DataFormatException
    {
        return readWord(countWords() - 1);
    }

}
