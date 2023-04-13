/**************
 * Search.java
 *
 * Utility class for searching.
 *
 * Copyright (C) 2023 by Mandy Software, LLC.
 *
 * Anyone is welcome to re-use this for any purpose
 * as long as the copyright notice is maintained and
 * with the understanding that there is no warranty,
 * express or implied.
 *
 */
package com.mandysoftware.wordutil;

import java.util.List;

public class Search
{
    static final int MAX_SEARCH_STEPS = 200;

    public interface Peek
    {
        Comparable peek(final List arr, final int idx, final Object param);
    }

    public enum Mode
    {
        EQ,
        G,
        L,
        GE,
        LE
    }

    public static int BinarySearch(final List arr, final Comparable key,
                                   final Peek peek, final Object peekParam,
                                   final int searchBoundFrom, final int searchBoundTo,
                                   final Mode mode)
    {
        int ia = searchBoundFrom;
        int ib = searchBoundTo;
        int ic = (ia + ib) / 2;
        int steps = 0;

        while (steps < MAX_SEARCH_STEPS && peek.peek(arr, ic, peekParam).compareTo(key) != 0)
        {
            if (peek.peek(arr, ic, peekParam).compareTo(key) > 0)
            {
                ib = ic;
            }
            else if (ia < ic)
            {
                ia = ic;
            }
            else if (ia + 1 < ib)
            {
                ia += 1;
            }

            ic = (ia + ib) / 2;

            if (ia + 1 >= ib)
            {
                break;
            }

            steps += 1;
        }

        if (Mode.EQ.equals(mode))
        {
            if (peek.peek(arr, ic, peekParam).compareTo(key) != 0)
            {
                return -1;
            }
        }

        if (Mode.GE.equals(mode))
        {
            while (peek.peek(arr, ic, peekParam).compareTo(key) >= 0 && ic > searchBoundFrom)
            {
                ic -= 1;
            }

            while (peek.peek(arr, ic, peekParam).compareTo(key) < 0 && ic + 1 < searchBoundTo)
            {
                ic += 1;
            }

            if (peek.peek(arr, ic, peekParam).compareTo(key) < 0)
            {
                return -1;
            }
        }

        if (Mode.LE.equals(mode))
        {
            while (peek.peek(arr, ic, peekParam).compareTo(key) <= 0 && ic + 1 < searchBoundTo)
            {
                ic += 1;
            }

            while (peek.peek(arr, ic, peekParam).compareTo(key) > 0 && ic > searchBoundFrom)
            {
                ic -= 1;
            }

            if (peek.peek(arr, ic, peekParam).compareTo(key) > 0)
            {
                return -1;
            }
        }

        if (Mode.G.equals(mode))
        {
            while (peek.peek(arr, ic, peekParam).compareTo(key) > 0 && ic > searchBoundFrom)
            {
                ic -= 1;
            }

            while (peek.peek(arr, ic, peekParam).compareTo(key) <= 0 && ic + 1 < searchBoundTo)
            {
                ic += 1;
            }

            if (peek.peek(arr, ic, peekParam).compareTo(key) <= 0)
            {
                return -1;
            }
        }

        if (Mode.L.equals(mode))
        {
            while (peek.peek(arr, ic, peekParam).compareTo(key) < 0 && ic + 1 < searchBoundTo)
            {
                ic += 1;
            }

            while (peek.peek(arr, ic, peekParam).compareTo(key) >= 0 && ic > searchBoundFrom)
            {
                ic -= 1;
            }

            if (peek.peek(arr, ic, peekParam).compareTo(key) >= 0)
            {
                return -1;
            }
        }

        return ic;
    }

    public static int BinarySearch(final List arr, final Comparable key,
                                   final Peek peek, final Object peekParam,
                                   final Mode mode)
    {
        return BinarySearch(arr, key, peek, peekParam, 0, arr.size(), mode);
    }
}
