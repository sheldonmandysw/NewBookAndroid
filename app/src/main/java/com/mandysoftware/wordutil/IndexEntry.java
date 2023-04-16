package com.mandysoftware.wordutil;

import java.util.List;

public class IndexEntry
{
    public static abstract class Base {}

    // The word keys are 4 bytes each in the word index.
    static final long DEFAULT_SUGGEST_MASK = 0xFFFFFFFF;
    static final int SUGGEST_KEY_LENGTH = 4;

    static long MakeKeyMask(Object lenParam, long defaultMask)
    {
        long mask = defaultMask;

        if (lenParam instanceof Integer)
        {
            mask = Integer.toUnsignedLong((Integer) lenParam);
        }
        else if (lenParam instanceof Long)
        {
            mask = (Long) lenParam;
        }

        return mask;
    }

    public static class Lookup extends Base
    {
        int key = 0;
        long value = 0;

        public static class Peek implements Search.Peek
        {
            public static Peek Instance = new Peek();

            @Override
            public Comparable peek(final List arr, final int idx, final Object param)
            {
                return ((Lookup) arr.get(idx)).key;
            }
        }

        public Lookup(int key, int value) {
            this.key = key;
            this.value = Integer.toUnsignedLong(value);
        }
    }

    public static class Suggest extends Base
    {
        long key_a = 0;
        long key_b = 0;
        long value = 0;

        public static class PeekA implements Search.Peek
        {
            public static PeekA Instance = new PeekA();

            @Override
            public Comparable peek(List arr, int idx, final Object param)
            {
                long mask = MakeKeyMask(param, DEFAULT_SUGGEST_MASK);

                return ((Suggest) arr.get(idx)).key_a & mask;
            }
        }

        public static class PeekB implements Search.Peek
        {
            public static PeekB Instance = new PeekB();

            @Override
            public Comparable peek(List arr, int idx, final Object param)
            {
                long mask = MakeKeyMask(param, DEFAULT_SUGGEST_MASK);

                return ((Suggest) arr.get(idx)).key_b & mask;
            }
        }

        public Suggest(int key_a, int key_b, int value)
        {
            this.key_a = Integer.toUnsignedLong(key_a);
            this.key_b = Integer.toUnsignedLong(key_b);
            this.value = Integer.toUnsignedLong(value);
        }
    }
}
