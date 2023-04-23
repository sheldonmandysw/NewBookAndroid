package com.mandysoftware.wordutil;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.DataFormatException;

public class CompositeDictionary {
    final static String TAG = "CompositeDictionary";

    final static String URL_PREFIX = "https://mandysoftware.com/res/words";

    // The index only has just the bare minimum info on each of the very few dictionaries.
    // We don't need a very high limit for this. If the file is larger than this, then
    // something's wrong.
    final static int DICTIONARY_NAMES_INDEX_SIZE_LIMIT = 32 * 1024;

    // Sleep time for our background thread while polling for commands.
    final static int BACKGROUND_THREAD_POLL_SLEEP_TIME = 50;

    final ArrayList<DictionaryInfo> dictionaryList = new ArrayList<>();
    final ArrayList<Dictionary> loadedDictionaries = new ArrayList<>();

    final ArrayList<DictionaryCallback> dictionaryCallbacks = new ArrayList<>();

    final ConcurrentLinkedQueue<Command> commandQueue = new ConcurrentLinkedQueue<>();

    boolean keepRunning = true;

    final File dictionaryRoot;

    public List<DictionaryInfo> getDictionaryList()
    {
        return dictionaryList;
    }

    public enum CommandName
    {
        INIT,
        DOWNLOAD_INDEX,
        LOAD_OFFLINE,
        LOOKUP,
        SUGGEST,
        DOWNLOAD_DICTIONARY,
        DELETE_DICTIONARY
    }

    public static class Command
    {
        public CommandName commandName;
        public Object argument;

        public Command(CommandName commandName)
        {
            this.commandName = commandName;
        }

        public Command(CommandName commandName, Object argument)
        {
            this.commandName = commandName;
            this.argument = argument;
        }
    }

    // Will process the command queue and wait for more commands if none are available.
    final Thread ourThread = new Thread(() -> {
        // Main loop to poll for and execute commands.
        while (keepRunning)
        {
            Command command = commandQueue.poll();

            if (command != null)
            {
                switch (command.commandName) {
                    case INIT:
                        backgroundInit();
                        break;
                    case DOWNLOAD_INDEX:
                        backgroundDownloadIndex();
                        break;
                    case LOAD_OFFLINE:
                        backgroundLoad();
                        break;
                    case LOOKUP:
                        backgroundLookup(command);
                        break;
                    case SUGGEST:
                        backgroundSuggest(command);
                        break;
                    case DOWNLOAD_DICTIONARY:
                        backgroundDownload(command);
                        break;
                    case DELETE_DICTIONARY:
                        backgroundDelete(command);
                        break;
                    default:
                        Log.w(TAG, "Unrecognized command enum: " + command.commandName);
                }
            }
            else
            {
                try
                {
                    Thread.sleep(BACKGROUND_THREAD_POLL_SLEEP_TIME);
                }
                catch (InterruptedException err)
                {
                    if (!keepRunning)
                    {
                        break;
                    }
                }
            }
        }

        // Final cleanup routine.
        Log.i(TAG, "Closing any dictionaries that might be open ...");

        for (Dictionary dictionary : loadedDictionaries)
        {
            try {
                dictionary.close();
            }
            catch (IOException err)
            {
                Log.e(TAG, "Error closing dictionary " + dictionary.info.name +
                        "; ignoring the error and continuing cleanup.");
            }
        }

        loadedDictionaries.clear();

        Log.i(TAG, "Completed cleanup. Exiting the background thread.");
    });

    public static class LookupResult
    {
        @NonNull final DictionaryInfo dictionaryInfo;
        @NonNull final String word;
        @NonNull final String definition;
        final boolean found;
        @Nullable final Exception err;

        public LookupResult(@NonNull DictionaryInfo dictionaryInfo,
                            @NonNull String word, @NonNull String definition,
                            boolean found,
                            @Nullable Exception err)
        {
            this.dictionaryInfo = dictionaryInfo;
            this.word = word;
            this.definition = definition;
            this.found = found;
            this.err = err;
        }

        @NonNull public DictionaryInfo getDictionaryInfo()
        {
            return dictionaryInfo;
        }

        @NonNull public String getWord()
        {
            return word;
        }

        @NonNull public String getDefinition()
        {
            return definition;
        }

        public boolean isFound()
        {
            return found;
        }

        @Nullable public Exception getErr()
        {
            return err;
        }
    }

    public static class SuggestResult
    {
        @NonNull final DictionaryInfo dictionaryInfo;
        @NonNull final String wordPrefix;
        @NonNull final List<String> suggestions;
        @Nullable final Exception err;

        public SuggestResult(@NonNull DictionaryInfo dictionaryInfo,
                             @NonNull String wordPrefix, @NonNull List<String> suggestions,
                             @Nullable Exception err)
        {
            this.dictionaryInfo = dictionaryInfo;
            this.wordPrefix = wordPrefix;
            this.suggestions = suggestions;
            this.err = err;
        }

        public @NonNull DictionaryInfo getDictionaryInfo()
        {
            return dictionaryInfo;
        }

        public @NonNull String getWordPrefix()
        {
            return wordPrefix;
        }

        public @NonNull List<String> getSuggestions()
        {
            return suggestions;
        }

        public @Nullable Exception getErr()
        {
            return err;
        }
    }

    public interface DictionaryCallback
    {
        void onDictionaryError(Command command, @NonNull Exception err);
        void onDictionaryDownloadProgress(@NonNull DictionaryInfo dictionaryInfo,
                                          int bytesDownloaded, int bytesTotal);
        void onDictionaryDownloadComplete(@NonNull DictionaryInfo dictionaryInfo, boolean success,
                                          @Nullable Exception err);
        void onDictionaryLoad(@NonNull DictionaryInfo dictionaryInfo, boolean success,
                              @Nullable Exception err);
        void onDictionaryDelete(@NonNull DictionaryInfo dictionaryInfo, boolean success,
                                @Nullable Exception err);
        void onAllDictionariesReady(boolean success, @Nullable Exception err);
        void onIndexDownloadProgress(int bytesDownloaded, int bytesTotal);
        void onIndexDownloadComplete(boolean success, @Nullable Exception err);

        // Since there may be multiple dictionaries (en, fr, etc.),
        // the result is a list of LookupResult.
        void onDictionaryLookup(@NonNull List<LookupResult> results, int successful, int total);

        // Same with suggestions.
        void onDictionarySuggest(@NonNull List<SuggestResult> results, int successful, int total);
    }

    // Use this to post dictionary commands to run asynchronously.
    // Don't call the load*() and lookup methods directly from the UI thread.
    // This one exits quickly but will cause the background thread to execute this command.
    public void postCommand(Command command)
    {
        commandQueue.add(command);
    }

    public void addDictionaryCallback(DictionaryCallback callback)
    {
        dictionaryCallbacks.add(callback);
    }

    // This function exits quickly, but it signals our thread to clean up and exit.
    public void close()
    {
        Log.i(TAG, "close() called; interrupting our background thread ...");

        keepRunning = false;

        ourThread.interrupt();
    }

    public static class DictionaryInfo
    {
        // Name is the short two-letter name (en, etc.) that we use internally.
        // We might still show this to the user too, but it's mainly for internal.
        final String name;
        // Description is what we show to the user in a GUI list of dictionaries.
        final String description;

        public DictionaryInfo(String name, String description)
        {
            this.name = name;
            this.description = description;
        }

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }
    }

    // Constructs a composite dictionary class.
    // The parameter tells it where to find/store dictionary data.
    // This is a directory.
    public CompositeDictionary(File dictionaryDirectory)
    {
        dictionaryRoot = dictionaryDirectory;

        ourThread.start();
    }

    // Runs in the background. Does a lookup of a word.
    protected void backgroundLookup(Command command)
    {
        String word = (String) command.argument;
        int successCount = 0;
        int totalCount = 0;

        ArrayList<LookupResult> results = new ArrayList<>();

        for (Dictionary dictionary : loadedDictionaries)
        {
            LookupResult result;

            try {
                String definition = dictionary.wapReader.lookupWord(word);

                result = new LookupResult(dictionary.info,
                        word, definition, true, null);
                successCount += 1;
            }
            catch (IllegalArgumentException err)
            {
                if (err.getMessage().contains("Word not found"))
                {
                    // Not bad. Just not found.
                    result = new LookupResult(dictionary.info,
                            word, "", false, null);
                    successCount += 1;
                }
                else
                {
                    // Bad. Some other error.
                    result = new LookupResult(dictionary.info,
                            word, "", false, err);
                }
            }
            catch (IOException|DataFormatException err)
            {
                // Bad. Some other error.
                result = new LookupResult(dictionary.info,
                        word, "", false, err);
            }

            results.add(result);

            totalCount += 1;
        }

        for (DictionaryCallback callback : dictionaryCallbacks)
        {
            callback.onDictionaryLookup(results, successCount, totalCount);
        }
    }

    // Runs in the background. Finds suggestions for a word given a prefix.
    protected void backgroundSuggest(Command command) {
        String prefix = (String) command.argument;

        int successCount = 0;
        int totalCount = 0;

        ArrayList<SuggestResult> results = new ArrayList<>();

        for (Dictionary dictionary : loadedDictionaries) {
            SuggestResult result;

            try {
                List<String> suggestions = dictionary.idxReader.suggest(prefix);

                result = new SuggestResult(dictionary.info, prefix, suggestions, null);

                successCount += 1;
            }
            catch (IOException|DataFormatException err)
            {
                // Return the empty list with the error that we caught.
                result = new SuggestResult(dictionary.info, prefix, new ArrayList<>(), err);
            }

            results.add(result);

            totalCount += 1;
        }

        for (DictionaryCallback callback : dictionaryCallbacks)
        {
            callback.onDictionarySuggest(results, successCount, totalCount);
        }
    }

    // Don't be fooled by the callbacks.
    // This method runs SYNCHRONOUSLY!!!
    // It will NOT return until the download either succeeds or fails.
    protected void backgroundDownloadIndex()
    {
        String indexUrl = URL_PREFIX + "/index.txt";
        String outputPath = getDictionaryPath(dictionaryRoot, "index.txt");
        Downloader.ProgressCallback callback = new Downloader.ProgressCallback() {
            @Override
            public void onDownloadProgress(int loaded, int total) {
                for (DictionaryCallback callbackDictionary : dictionaryCallbacks)
                {
                    callbackDictionary.onIndexDownloadProgress(loaded, total);
                }
            }

            @Override
            public void onDownloadComplete(boolean success, @Nullable Exception err,
                                           int totalLoaded) {
                for (DictionaryCallback callbackDictionary : dictionaryCallbacks)
                {
                    callbackDictionary.onIndexDownloadComplete(success, err);
                }
            }
        };

        Downloader.Download(indexUrl, outputPath, callback);
    }

    protected DictionaryInfo findInfo(String name, Command command)
    {
        DictionaryInfo info = null;

        for (DictionaryInfo dictionaryInfo : dictionaryList)
        {
            if (dictionaryInfo.name.equals(name))
            {
                info = dictionaryInfo;
                break;
            }
        }

        if (info == null)
        {
            for (DictionaryCallback callback : dictionaryCallbacks)
            {
                callback.onDictionaryError(command, new IllegalArgumentException(
                        "The dictionary name " + name + " was not found."
                ));
            }

            return null;
        }

        return info;
    }

    // Runs in the background. Downloads a dictionary given its name (en, etc.).
    // Don't be fooled by the callbacks. This method is SYNCHRONOUS!!!
    // That is, while it calls all the callbacks, the method itself
    // won't return until the operation is done.
    // That's why this only is supposed to run in the background
    // (call this only from our own thread).
    protected void backgroundDownload(Command command)
    {
        String name = (String) command.argument;

        final DictionaryInfo dictionaryInfo = findInfo(name, command);

        if (dictionaryInfo == null)
        {
            return;
        }

        String wapUrl = URL_PREFIX + "/" + name + ".wap";

        String pathPrefix = getDictionaryPath(dictionaryRoot, name);
        String wapFilename = pathPrefix + ".wap";

        // For the download progress on this one,
        // since it's two files but we need only one progress,
        // we have to start out by guessing the .idx file size.
        // The .idx is usually between 1-3% of the .wap size,
        // but the average is around 2% or so.
        final float otherSize = 0.02f;

        Downloader.ProgressCallback wapCallback = new Downloader.ProgressCallback() {
            @Override
            public void onDownloadProgress(int loaded, int total) {
                int totalBytes = (int) (total + (total * otherSize));

                for (DictionaryCallback callback : dictionaryCallbacks)
                {
                    callback.onDictionaryDownloadProgress(dictionaryInfo, loaded, totalBytes);
                }
            }

            @Override
            public void onDownloadComplete(boolean success, @Nullable Exception err,
                                           int totalLoaded) {
                if (success)
                {
                    _downloadIdxFile(totalLoaded, dictionaryInfo);
                }
                else
                {
                    for (DictionaryCallback callback : dictionaryCallbacks)
                    {
                        callback.onDictionaryDownloadComplete(dictionaryInfo,
                                false, err);
                    }
                }
            }
        };

        Downloader.Download(wapUrl, wapFilename, wapCallback);
    }

    protected void _downloadIdxFile(final int loadedOtherBytesSoFar, final DictionaryInfo info)
    {
        String idxUrl = URL_PREFIX + "/" + info.name + ".idx";

        String pathPrefix = getDictionaryPath(dictionaryRoot, info.name);
        String idxFilename = pathPrefix + ".idx";

        Downloader.ProgressCallback idxCallback = new Downloader.ProgressCallback() {
            @Override
            public void onDownloadProgress(int loaded, int total) {
                int loadedSoFar = loaded + loadedOtherBytesSoFar;

                for (DictionaryCallback callback : dictionaryCallbacks)
                {
                    callback.onDictionaryDownloadProgress(info,
                            loadedSoFar, total + loadedSoFar);
                }
            }

            @Override
            public void onDownloadComplete(boolean success, @Nullable Exception err,
                                           int totalLoaded) {
                for (DictionaryCallback callback : dictionaryCallbacks)
                {
                    callback.onDictionaryDownloadComplete(info, success, err);
                }
            }
        };

        Downloader.Download(idxUrl, idxFilename, idxCallback);
    }

    // Runs in the background. Deletes a dictionary given its name (en, etc.).
    protected void backgroundDelete(Command command)
    {
        boolean success = true;
        Exception resultError = null;
        String name = (String) command.argument;

        final DictionaryInfo dictionaryInfo = findInfo(name, command);

        if (dictionaryInfo == null)
        {
            Log.e(TAG, "Cannot delete the dictionary because we don't know about it.");
            return;
        }

        Log.i(TAG, "Operation: delete " + name);

        String pathPrefix = getDictionaryPath(dictionaryRoot, name);
        File wapFile = new File(pathPrefix + ".wap");
        File idxFile = new File(pathPrefix + ".idx");

        if (wapFile.exists() && !wapFile.delete())
        {
            resultError = new IOException("Could not delete WAP file: " + wapFile.getPath());

            Log.e(TAG, resultError.getMessage());

            success = false;
        }
        else if (idxFile.exists() && !idxFile.delete())
        {
            resultError = new IOException("Could not delete IDX file: " + idxFile.getPath());

            Log.e(TAG, resultError.getMessage());

            success = false;
        }

        if (success)
        {
            Log.i(TAG, "Dictionary " + name + " deleted successfully.");
        }

        for (DictionaryCallback callback : dictionaryCallbacks)
        {
            callback.onDictionaryDelete(dictionaryInfo, success, resultError);
        }
    }

    // Makes a string with the absolute path prefix to a dictionary file given its name.
    protected String getDictionaryPath(File containingDir, String name) {
        return new File(containingDir, name).getAbsolutePath();
    }

    // Runs in the background.
    // Does both download index first and then load offline.
    protected void backgroundInit()
    {
        Log.i(TAG, "Operation: initialize.");

        if (!dictionaryRoot.exists())
        {
            if (!dictionaryRoot.mkdirs())
            {
                Log.e(TAG, "Could not create dictionary root folder: " +
                        dictionaryRoot.getPath());
            }
        }

        String indexPath = getDictionaryPath(dictionaryRoot, "index.txt");
        File indexFile = new File(indexPath);

        if (!indexFile.exists() || indexFile.length() == 0) {
            backgroundDownloadIndex();
        }

        backgroundLoad();
    }

    // Runs in the background. Initializes things.
    //
    // Loads the dictionary index and also any dictionaries that are available offline.
    // Downloads index.txt from the internet if it doesn't exist locally, but
    // does not download the dictionary files if they don't exist locally.
    protected void backgroundLoad()
    {
        Log.i(TAG, "Operation: load.");

        try {
            loadIndex(dictionaryRoot);
            loadDictionaries(dictionaryList, dictionaryRoot);
        }
        catch (IOException err)
        {
            for (DictionaryCallback callback : dictionaryCallbacks)
            {
                callback.onAllDictionariesReady(false, err);
            }

            return;
        }

        for (DictionaryCallback callback : dictionaryCallbacks)
        {
            callback.onAllDictionariesReady(true, null);
        }
    }

    // TODO write composite lookup() and suggest() functions that run asynchronously

    // TODO figure out how to sort things out with lookups and search suggestions with multiple dictionaries

    static class Dictionary
    {
        final DictionaryInfo info;
        final WapReader wapReader;
        final IdxReader idxReader;

        public Dictionary(DictionaryInfo info, String pathPrefix) throws IOException
        {
            this.info = info;
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
    protected void loadIndex(final File dictionaryDirectory) throws IOException
    {
        String indexName = "index.txt";
        String delimRegexPattern = " %%%% ";

        Log.i(TAG, "Loading dictionary list (" + indexName + ") ...");

        File indexFile = new File(dictionaryDirectory, indexName);
        FileInputStream inputStream = new FileInputStream(indexFile);
        byte [] indexBytes = new byte[DICTIONARY_NAMES_INDEX_SIZE_LIMIT];

        int bytesRead = inputStream.read(indexBytes);

        if (inputStream.available() > 0)
        {
            Log.w(TAG, "Dictionary " + indexName +
                    " has more bytes left after reading the limit of " +
                    DICTIONARY_NAMES_INDEX_SIZE_LIMIT +
                    " bytes; the input was truncated, so corruption is possible.");
        }

        inputStream.close();

        dictionaryList.clear();

        if (bytesRead == 0)
        {
            Log.e(TAG, "No bytes read from the index file.");
            return;
        }

        String indexText = new String(indexBytes, 0, bytesRead, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(indexText);

        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            String[] parts = line.split(delimRegexPattern);

            if (parts.length > 1)
            {
                dictionaryList.add(new DictionaryInfo(parts[0], parts[1]));
            }
        }

        scanner.close();

        Log.i(TAG, "Dictionary list loaded. " +
                String.valueOf(dictionaryList.size()) +
                " entries found.");
    }

    // Loads the dictionaries given by the names in the list,
    // where each name is like "en", "fr", etc., where you can just
    // append ".wap" and ".idx" to make the file base name.
    // The containingDir parameter is the path to the directory (folder)
    // that contains these .wap and .idx dictionary files.
    protected void loadDictionaries(List<DictionaryInfo> dictionaries, File containingDir)
            throws IOException
    {
        Log.i(TAG, "Closing any dictionaries that might be open ...");

        for (Dictionary dictionary : loadedDictionaries)
        {
            dictionary.close();
        }

        loadedDictionaries.clear();

        Log.i(TAG, "Loading dictionaries ...");

        for (DictionaryInfo info : dictionaries)
        {
            String name = info.name;
            String pathPrefix = getDictionaryPath(containingDir, name);
            Dictionary dictionary = null;

            // Remember that `names` will be from index.txt, and
            // it will have ALL entries that the server has, not
            // necessarily just the entries that exist locally.
            // Check for local (offline) existence.
            //
            if (!new File(pathPrefix + ".wap").exists())
            {
                Log.i(TAG, "Dictionary " + name + " is not available offline; skipping ...");

                continue;
            }

            Log.i(TAG, "Attempting to load dictionary " + name + " ...");

            // Idea here: attempt to read the dictionary. If the file is corrupt, it'll
            // throw an exception, so we'll know it's corrupt, and we'll attempt to
            // delete it.
            try
            {
                dictionary = new Dictionary(info, pathPrefix);

                Log.v(TAG, "Dictionary " + name + " index loaded. Running sanity check ...");

                String first = dictionary.idxReader.firstWord();

                Log.v(TAG, "First word of dictionary " + name + ": " + first);

                String last = dictionary.idxReader.lastWord();

                Log.v(TAG, "Last word of dictionary " + name + ": " + last);

                String firstText = dictionary.wapReader.lookupWord(first);

                Log.v(TAG, "Definition of first word: " + firstText);

                String lastText = dictionary.wapReader.lookupWord(last);

                Log.v(TAG, "Definition of last word: " + lastText);
            }
            catch (IOException|DataFormatException err)
            {
                File wapFile = new File(pathPrefix + ".wap");
                File idxFile = new File(pathPrefix + ".idx");

                Log.e(TAG, "Error loading dictionary: " + err.getMessage());

                for (DictionaryCallback callback : dictionaryCallbacks)
                {
                    callback.onDictionaryLoad(info, false, err);
                }

                Log.w(TAG, "Due to error, dictionary " + name + " might be corrupt.");
                Log.i(TAG, "Will delete dictionary " + name + " if exists due to corrupt.");

                if (wapFile.exists())
                {
                    if (!wapFile.delete())
                    {
                        Log.e(TAG, "Could not delete WAP for " + name);
                    }
                }

                if (idxFile.exists())
                {
                    if (!idxFile.delete())
                    {
                        Log.e(TAG, "Could not delete IDX for " + name);
                    }
                }

                continue;
            }

            Log.i(TAG, "Dictionary " + name + " loaded successfully.");

            loadedDictionaries.add(dictionary);

            for (DictionaryCallback callback : dictionaryCallbacks)
            {
                callback.onDictionaryLoad(info, true, null);
            }
        }
    }

}
