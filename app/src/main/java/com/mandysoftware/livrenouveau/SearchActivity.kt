package com.mandysoftware.livrenouveau

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mandysoftware.wordutil.CompositeDictionary
import java.io.File

class SearchActivity : AppCompatActivity(), CompositeDictionary.DictionaryCallback {

    val TAG = "SearchActivity"

    private lateinit var searchView: SearchView
    private lateinit var rvSuggestions: RecyclerView

    private val definitionActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val word = data.getStringExtra("word")
                // do something with the returned word
                runOnUiThread {

                    searchView.setQuery(word, false) //false because we don't want auto search
                    searchView.requestFocus() // focus for simplicity
                }
            }
        }
    }

    var dictionary : CompositeDictionary? = null

    inner class SuggestionsAdapter(private val suggestions: List<String>) :
        RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(   R.layout.suggested_item,
                    parent,
                    false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int)
        {

            holder.bind(suggestions[position], position)


            /*
            holder.itemView.apply{

                val tvSuggestions = findViewById<TextView>(R.id.tvSuggestion)
                tvSuggestions.text = suggestions[position]
            }
            */
        }

        override fun getItemCount(): Int = suggestions.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val tvSuggestion: TextView = itemView.findViewById(R.id.tvSuggestion)
            protected var word = ""


            init {
                tvSuggestion.setOnClickListener({

                    val intent = Intent(this@SearchActivity, DefinitionActivity::class.java)
                    intent.putExtra("word", word)


                    //startActivity(intent)

                    definitionActivityResultLauncher.launch(intent)

                })
            }

            fun bind(suggestion: String, position: Int) {
                word = suggestion
                tvSuggestion.text = suggestion

                if(position % 2 == 0){
                    tvSuggestion.setBackgroundColor(ContextCompat.getColor(this@SearchActivity, R.color.search_suggest_color_even))

                    //tvSuggestion.setBackgroundColor(getResources().getColor(R.color.search_suggest_color_even)
                } else {
                    tvSuggestion.setBackgroundColor(ContextCompat.getColor(this@SearchActivity, R.color.search_suggest_color_odd))

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val storageDir = filesDir
        val dictionaryDir = File(storageDir, "dictionary")

        //rvSuggestions = findViewById(R.id.rvSuggestions)


        // INIT is the same as LOAD_OFFLINE, but INIT has an extra
        // preparation step of checking to make sure the dictionary
        // list exists, and, if it doesn't exist, downloading it
        // from the server.
        dictionary = CompositeDictionary(dictionaryDir)
        dictionary!!.addDictionaryCallback(this)
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.INIT))
        dictionary!!.postCommand(
            CompositeDictionary
            .Command(CompositeDictionary.CommandName.SUGGEST, ""))

        // Initialize SearchView
        searchView = findViewById(R.id.svWord)


            rvSuggestions = findViewById(R.id.rvSuggestions)
            rvSuggestions.layoutManager = LinearLayoutManager(this)

        // Set up SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            val di = dictionary

            override fun onQueryTextSubmit(query: String?): Boolean {

                //println("Text Submitted: " + query.toString())
                // This method is called when the user submits their search query
                // Here I'll use back-end method to search for the string
                if (di != null) {
                    di.postCommand(
                        CompositeDictionary.Command(
                            CompositeDictionary.CommandName.LOOKUP, query
                        )
                    )
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (di != null) {
                    di.postCommand(
                        CompositeDictionary.Command(
                            CompositeDictionary.CommandName.SUGGEST, newText
                        )
                    )
                }
                return true
            }
        })

        //searchView.setFocusable(true)
        searchView.setIconified(false)
        //searchView.requestFocusFromTouch()
        //searchView.requestFocus()
    }

    override fun onDictionaryError(command: CompositeDictionary.Command?, err: Exception) {
        println("ERROR: " + command.toString() + " Message: " + err.message.toString())
    }

    override fun onDictionaryDownloadProgress(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        bytesDownloaded: Int,
        bytesTotal: Int
    ) {

    }

    override fun onDictionaryDownloadComplete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {

    }

    override fun onDictionaryLoad(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {

    }

    override fun onDictionaryDelete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {

    }

    override fun onAllDictionariesReady(success: Boolean, err: Exception?) {

    }

    override fun onIndexDownloadProgress(bytesDownloaded: Int, bytesTotal: Int) {

    }

    override fun onIndexDownloadComplete(success: Boolean, err: Exception?) {

    }

    override fun onHeadFilesComplete(
        successCount: Int,
        totalCount: Int,
        success: Boolean,
        err: Exception?
    ) {

    }

    override fun onLocalFilesChecked() {


    }

    override fun onDictionaryLookup(
        results: MutableList<CompositeDictionary.LookupResult>,
        successful: Int,
        total: Int
    ) {

    }

    override fun onDictionarySuggest(
        results: MutableList<CompositeDictionary.SuggestResult>,
        successful: Int,
        total: Int
    ) {
        val m_suggestions = mutableListOf<String>()


        if(results.size > 0){

            results[0].suggestions.forEach { s ->
                m_suggestions.add(s)
            }

        }


        runOnUiThread {
            // Create a new instance of your adapter with the list of suggestions
            //val sugs = mutableListOf("apple", "banana", "cherry", "date", "elderberry")
            val adapter = SuggestionsAdapter(m_suggestions)
            rvSuggestions.adapter = adapter


        }



    }
}