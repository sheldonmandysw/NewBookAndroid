package com.mandysoftware.wordutil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class WapReader {
    final File inputFile;
    final FileInputStream inputStream;
    final long fileSize;

    final ArrayList<IndexEntry.Lookup> entries = new ArrayList<>();
    long endofs = 0;

    static int findEndlInRawUtf8(byte [] chunk, int i0)
    {
        int idx = i0 + 1;

        while (idx < chunk.length && chunk[idx] != '\n')
        {
            idx += 1;
        }

        return idx;
    }

    static class PeekWord implements Search.Peek
    {
        static final PeekWord Instance = new PeekWord();

        @Override
        public Comparable peek(final List arr, final int idx, final Object param)
        {
            byte [] chunk = (byte []) param;
            int wordStart = (Integer) arr.get(idx);
            int idxEndl = findEndlInRawUtf8(chunk, wordStart);

            return new String(chunk, wordStart, idxEndl - wordStart, StandardCharsets.UTF_8);
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
        endofs = java.nio.ByteBuffer.wrap(fourBytes)
                .order(ByteOrder.BIG_ENDIAN).getInt();

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

    public byte [] lookupEntry(String name) throws IOException, DataFormatException
    {
        int ic = lookupIndex(name);

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
        byte [] compressedBytes = new byte[(int)(ofsnext - ofsnow)];
        inputStream.read(compressedBytes);

        byte [] rawBytes = Compression.Inflate(compressedBytes);

        int iend = rawBytes.length - 4;
        int ofsidx = java.nio.ByteBuffer.wrap(rawBytes, iend, 4)
                .order(ByteOrder.BIG_ENDIAN).getInt();
        int nwds = (iend - ofsidx) / 4;

        IntBuffer idxlistbuf = ByteBuffer.wrap(rawBytes, ofsidx, iend - ofsidx)
                .order(ByteOrder.BIG_ENDIAN)
                .asIntBuffer();
        int [] idxlistarr = new int[nwds];

        idxlistbuf.get(idxlistarr);

        List<Integer> idxlist = new ArrayList<>(idxlistarr.length);

        for (int idx : idxlistarr)
        {
            idxlist.add(idx);
        }

        int iw = Search.BinarySearch(idxlist, name, PeekWord.Instance, rawBytes, Search.Mode.EQ);

        if (iw == -1)
        {
            throw new IllegalArgumentException("Word not found.");
        }

        int entryStart = findEndlInRawUtf8(rawBytes, idxlist.get(iw)) + 1;
        int entryEnd = ofsidx;

        if (iw + 1 < idxlist.size())
        {
            entryEnd = idxlist.get(iw + 1);
        }

        byte [] entryBytes = new byte[entryEnd - entryStart];

        System.arraycopy(rawBytes, entryStart, entryBytes, 0, entryEnd - entryStart);

        return entryBytes;
    }

    public String lookupWord(String word) throws IOException, DataFormatException
    {
        return new String(lookupEntry(word), StandardCharsets.UTF_8);
    }
}
