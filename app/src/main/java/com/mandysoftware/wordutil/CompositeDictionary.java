package com.mandysoftware.wordutil;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class CompositeDictionary {
    //

    final ArrayList<String> dictionaryNames = new ArrayList<>();
    final ArrayList<Dictionary> loadedDictionaries = new ArrayList<>();

    final ArrayList<DictionaryIndexCallback> indexCallbacks = new ArrayList<>();
    final ArrayList<DictionaryCallback> dictionaryCallbacks = new ArrayList<>();

    final File dictionaryRoot;

    final Thread ourThread = new Thread(() -> {
        //
    });

    public interface DictionaryIndexCallback
    {
        void onDictionaryIndexDownloadProgress(int bytesDownloaded, int bytesTotal);
        void onDictionaryIndexDownloadComplete(boolean success);
        void onDictionaryIndexRead(boolean success);
    }

    public interface DictionaryCallback
    {
        void onDictionaryDownloadProgress(String dictionaryName, int bytesDownloaded, int bytesTotal);
        void onDictionaryDownloadComplete(String dictionaryName, boolean success);
        void onDictionaryLoad(String name, boolean success);
        void onAllDictionariesReady(boolean success);
    }

    public void addOnDictionaryIndexCallback(DictionaryIndexCallback callback)
    {
        indexCallbacks.add(callback);
    }

    public void addOnDictionaryCallback(DictionaryCallback callback)
    {
        dictionaryCallbacks.add(callback);
    }

    // Constructs a composite dictionary class.
    // The parameter tells it where to find/store dictionary data.
    // This is a directory.
    public CompositeDictionary(File dictionaryDirectory)
    {
        dictionaryRoot = dictionaryDirectory;
    }

    // Loads the dictionary index and also any dictionaries that are available offline.
    // Downloads index.txt from the internet if it doesn't exist locally, but
    // does not download the dictionary files if they don't exist locally.
    public void load()
    {
        (new Thread(() -> {
            if (!dictionaryRoot.exists())
            {
                dictionaryRoot.mkdirs();
            }

            //
        })).start();
    }

    // TODO write composite lookup() and suggest() functions that run asynchronously

    // TODO figure out how to sort things out with lookups and search suggestions with multiple dictionaries

    static class Dictionary
    {
        final WapReader wapReader;
        final IdxReader idxReader;

        public Dictionary(String pathPrefix) throws IOException
        {
            wapReader = new WapReader(new File(pathPrefix + ".wap"));
            idxReader = new IdxReader(new File(pathPrefix + ".idx"));
        }

        public String lookup(String word) throws IOException, DataFormatException
        {
            return wapReader.lookupWord(word);
        }

        public ArrayList<String> suggest(String prefix, int suggestionCountLimit)
                throws IOException, DataFormatException
        {
            return idxReader.suggest(prefix, suggestionCountLimit);
        }

        public void close() throws IOException
        {
            wapReader.close();
            idxReader.close();
        }
    }

    // Loads the index.txt file that contains all the dictionary names.
    public void loadIndex(final String dictionaryDirectory,
                          Runnable onLoaded) throws IOException
    {
        //
    }

    // Loads the dictionaries given by the names in the list,
    // where each name is like "en", "fr", etc., where you can just
    // append ".wap" and ".idx" to make the file base name.
    // The containingDir parameter is the path to the directory (folder)
    // that contains these .wap and .idx dictionary files.
    public void loadDictionaries(List<String> names, String containingDir) throws IOException
    {
        for (Dictionary dictionary : loadedDictionaries)
        {
            dictionary.close();
        }

        loadedDictionaries.clear();

        for (String name : names)
        {
            loadedDictionaries.add(new Dictionary(containingDir + "/" + name));
        }
    }

    // Loads dictionaries from the specified dictionary directory.
    // The directory must contain a file named "index.txt" which
    // contains the names ("en", "fr", etc.), one name per line,
    // of the dictionaries to try to load.
    // It's okay for any of these to not exist in dictionaryDirectory,
    // but the ones that do exist in dictionaryDirectory (en.wap, etc.),
    // those ones will be loaded.
    //
    // The purpose of allowing non-existing entries in index.txt is
    // to simplify the code: we can simply download the index.txt from
    // our server which contains all the downloadable dictionaries.
    // Then we only download the ones that the user opts in to download.
    //
    public void loadDictionaries(File dictionaryDirectory) throws IOException
    {
        //
    }
}
