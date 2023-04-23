package com.mandysoftware.livrenouveau

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.mandysoftware.wordutil.CompositeDictionary
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity(), CompositeDictionary.DictionaryCallback {
    val TAG = "MainActivity"

    var dictionary : CompositeDictionary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storageDir = filesDir
        val dictionaryDir = File(storageDir, "dictionary")

        // INIT is the same as LOAD_OFFLINE, but INIT has an extra
        // preparation step of checking to make sure the dictionary
        // list exists, and, if it doesn't exist, downloading it
        // from the server.
        dictionary = CompositeDictionary(dictionaryDir)
        dictionary!!.addDictionaryCallback(this)
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.INIT))

        // The following was used for testing. Don't uncomment unless you want to test it again.
        // It's manual testing, so you just look in LogCat to see that it's working.
        //testCommands()

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLaunchManager).setOnClickListener({
            val intent = Intent(this, ManageDictionaryActivity::class.java)
            startActivity(intent)
        })
    }

    override fun onDestroy() {
        dictionary!!.close()
        dictionary = null
        super.onDestroy()
    }

    // Function just for testing purposes. Will remove later.
    fun testCommands()
    {
        val di = dictionary

        if (di != null)
        {
            // Note that these operations will be completed in the
            // order in which they are issued here.
            // They will be posted to a queue, and all
            // these postCommand() methods will return
            // immediately, but the background thread
            // of CompositeDictionary will go through
            // them in order sequentially.

            // As each command completes, its appropriate
            // callback method will be called due to
            // us having called addDictionaryCallback()
            // as ourself ('this') in onCreate().

            // Delete the Ukrainian dictionary if one already exists.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.DELETE_DICTIONARY, "uk"
            ))

            // Refresh our dictionary list into RAM to purge it if it was in RAM.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.LOAD_OFFLINE
            ))

            // Word should be not found yet.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.LOOKUP, "apple"
            ))

            // Suggestions should be empty.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.SUGGEST, "app"
            ))

            // Here we download the Ukrainian dictionary (because it's the smallest) for testing.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.DOWNLOAD_DICTIONARY, "uk"
            ))

            // Once it's downloaded, we have to re-load the dictionaries into RAM again.
            // LOAD_OFFLINE will reload whatever is available offline without making
            // any trips to the server.
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.LOAD_OFFLINE
            ))

            // Once that is complete, we then attempt to look up the words again.
            // This should now find the Ukrainian dictionary entry for the English word "apple".
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.LOOKUP, "apple"
            ))

            // And this should now have some suggestions that start with "app".
            di.postCommand(CompositeDictionary.Command(
                CompositeDictionary.CommandName.SUGGEST, "app"
            ))
        }
    }

    override fun onDictionaryError(command: CompositeDictionary.Command?, err: Exception) {
        // TODO let the user know
        Log.e(TAG, "Dictionary error: " + err.message)
    }

    override fun onDictionaryDownloadProgress(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        bytesDownloaded: Int,
        bytesTotal: Int
    ) {
        // TODO uncomment and update user progress bar inside this lambda here
        // (only ever update the GUI from the UI thread or else the app will crash)
        //
        //runOnUiThread({
        //    .... code to update the UI goes here ...
        //})
    }

    override fun onDictionaryDownloadComplete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // TODO update the UI with whatever

        Log.i(TAG, "Dictionary download complete; success: " +
                success + "; dictionary: " + dictionaryInfo.name)
    }

    override fun onDictionaryLoad(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // TODO update the UI if needed

        if (success) {
            Log.i(
                TAG, "Dictionary loaded - ready to search words in " +
                        dictionaryInfo.name
            )
        }
        else
        {
            Log.e(TAG, "Error loading dictionary ${dictionaryInfo.name}")
        }
    }

    override fun onDictionaryDelete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // TODO update the UI if needed

        Log.i(TAG, "Dictionary deleted - " + dictionaryInfo.name +
                "; success: $success")
    }

    override fun onAllDictionariesReady(success: Boolean, err: Exception?) {
        // TODO update the UI if needed

        Log.i(TAG, "All dictionaries loaded. Success: $success")
    }

    override fun onIndexDownloadProgress(bytesDownloaded: Int, bytesTotal: Int) {
        // no need to update the UI for this one; it's like 1 KB in size.

        Log.i(TAG, "Index download progress: $bytesDownloaded of $bytesTotal")
    }

    override fun onIndexDownloadComplete(success: Boolean, err: Exception?) {
        // not sure if we need any UI updates for this one either;
        // we only care for when the actual dictionaries are all completed loading

        Log.i(TAG, "Dictionary index loaded; success: $success")
    }

    override fun onHeadFilesComplete(successful : Int, total : Int,
                                     success: Boolean, err: Exception?) {
        // nothing to do
    }

    override fun onLocalFilesChecked() {
        // nothing to do
    }

    override fun onDictionaryLookup(
        results: MutableList<CompositeDictionary.LookupResult>,
        successful: Int,
        total: Int
    ) {
        // TODO update the UI as necessary
        // (use runOnUiThread() of course!)

        // Logging.
        for (result in results)
        {
            if (result.isFound)
            {
                Log.i(TAG, "Found word <${result.word}> in dictionary " +
                    "${result.dictionaryInfo.name}; definition: ${result.definition}")
            }
            else
            {
                Log.i(TAG, "Word <${result.word}> not found in " +
                        result.dictionaryInfo.name)
            }
        }
    }

    override fun onDictionarySuggest(
        results: MutableList<CompositeDictionary.SuggestResult>,
        successful: Int,
        total: Int
    ) {
        // TODO update the UI as necessary

        // Logging.
        for (result in results)
        {
            if (result.suggestions.size > 0)
            {
                Log.i(TAG, "Found suggestions for <${result.wordPrefix}> in " +
                    "dictionary ${result.dictionaryInfo.name}; first suggestion: " +
                    result.suggestions[0])
            }
            else
            {
                Log.i(TAG, "Prefix <${result.wordPrefix}> not found in " +
                    result.dictionaryInfo.name)
            }
        }
    }
}
