package com.mandysoftware.livrenouveau

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.mandysoftware.wordutil.CompositeDictionary
import com.mandysoftware.wordutil.CompositeDictionary.DictionaryCallback
//import org.xwiki.rendering.wikimodel.xwiki.xwiki20.XWikiParser
import java.io.File
import java.lang.Exception

class DefinitionActivity : AppCompatActivity(), DictionaryCallback {
    val TAG = "DefinitionActivity"
    var word = ""
    var tvDefinition : TextView? = null

    var dictionary : CompositeDictionary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definition)



        val storageDir = filesDir
        val dictionaryDir = File(storageDir, "dictionary")
        tvDefinition = findViewById(R.id.tvDefinition)




        word = intent.getStringExtra("word")!!


        // INIT is the same as LOAD_OFFLINE, but INIT has an extra
        // preparation step of checking to make sure the dictionary
        // list exists, and, if it doesn't exist, downloading it
        // from the server.
        dictionary = CompositeDictionary(dictionaryDir)
        dictionary!!.addDictionaryCallback(this)
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.INIT))
        dictionary!!.postCommand(
            CompositeDictionary
                .Command(CompositeDictionary.CommandName.LOOKUP, word))




        // Set up SearchView listener
        val di = dictionary

        // Get a reference to the action bar
        supportActionBar?.apply {
            // Enable the "up" button on the action bar
            setDisplayHomeAsUpEnabled(true)

        }



    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle "up" button clicks
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this@DefinitionActivity, SearchActivity::class.java)
            intent.putExtra("word", word)
            setResult(Activity.RESULT_OK, intent)
            finish()

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDictionaryError(command: CompositeDictionary.Command?, err: Exception) {
        TODO("Not yet implemented")
    }

    override fun onDictionaryDownloadProgress(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        bytesDownloaded: Int,
        bytesTotal: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onDictionaryDownloadComplete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        TODO("Not yet implemented")
    }

    override fun onDictionaryLoad(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        if(!success){
            Toast.makeText(this, "Error loading dictionary.", Toast.LENGTH_SHORT).show()
        } //maybe add else to lookup word
    }

    override fun onDictionaryDelete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        TODO("Not yet implemented")
    }

    override fun onAllDictionariesReady(success: Boolean, err: Exception?) {
        if(!success){
            Toast.makeText(this, "Error loading ALL dictionary.", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onIndexDownloadProgress(bytesDownloaded: Int, bytesTotal: Int) {
        //do nothing
    }

    override fun onIndexDownloadComplete(success: Boolean, err: Exception?) {
        //do nothing
    }

    override fun onHeadFilesComplete(
        successCount: Int,
        totalCount: Int,
        success: Boolean,
        err: Exception?
    ) {
        TODO("Not yet implemented") //doesn't apply
    }

    override fun onLocalFilesChecked() {
        //do nothing
    }

    override fun onDictionaryLookup(
        results: MutableList<CompositeDictionary.LookupResult>,
        successful: Int,
        total: Int
    ) {


        for (result in results) {
            if (result.isFound) {
               // XWikiParser to start showing our wikitext as pretty outputs rather than not.
                runOnUiThread {

                    setTitle(result.word) //if user clicks on title take back to SearchActvity
                    //text edit by default on the search bar
                    tvDefinition!!.text = result.definition
                }
                return;
            }
        }

        runOnUiThread {
            setTitle(getString(R.string.word_not_found))
            tvDefinition!!.text = getString(R.string.word_not_found)
        }

    }

    override fun onDictionarySuggest(
        results: MutableList<CompositeDictionary.SuggestResult>,
        successful: Int,
        total: Int
    ) {
        TODO("Not yet implemented")
    }
}