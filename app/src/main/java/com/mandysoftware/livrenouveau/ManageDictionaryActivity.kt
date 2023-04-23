package com.mandysoftware.livrenouveau

import android.content.Context
import android.icu.lang.UCharacter.VerticalOrientation
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mandysoftware.wordutil.CompositeDictionary
import com.mandysoftware.wordutil.CompositeDictionary.DictionaryInfo
import java.io.File
import java.lang.Exception

class ManageDictionaryActivity : AppCompatActivity(), CompositeDictionary.DictionaryCallback {
    val TAG = "ManageDictionaryAty"

    var dictionary : CompositeDictionary? = null

    var rvDictionaryList : RecyclerView? = null

    inner class DictionaryManagerAdapter : RecyclerView.Adapter<DictionaryItemViewHolder>()
    {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DictionaryItemViewHolder {
            val root = LayoutInflater.from(this@ManageDictionaryActivity)
                .inflate(R.layout.dictionary_manager_item, parent, false)
            return DictionaryItemViewHolder(root)
        }

        override fun getItemCount(): Int {
            return dictionary!!.dictionaryList.size
        }

        override fun onBindViewHolder(holder: DictionaryItemViewHolder, position: Int) {
            holder.bind(dictionary!!.dictionaryList[position])
        }
    }

    class DictionaryItemViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tvName : TextView
        val tvSize : TextView
        val tvDesc : TextView

        val btnDownload : Button
        val btnUpdate : Button

        val pbDownload : ProgressBar

        init {
            tvName = root.findViewById(R.id.tvName)
            tvSize = root.findViewById(R.id.tvSize)
            tvDesc = root.findViewById(R.id.tvDescription)
            btnDownload = root.findViewById(R.id.btnDownload)
            btnUpdate = root.findViewById(R.id.btnUpdate)
            pbDownload = root.findViewById(R.id.pbDownload)
        }

        fun bind(info : DictionaryInfo)
        {
            tvName.text = info.name
            tvSize.text = ""
            tvDesc.text = info.description
            btnUpdate.visibility = View.GONE
            pbDownload.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dictionary = CompositeDictionary(File(filesDir, "dictionary"))
        dictionary!!.addDictionaryCallback(this)
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.INIT))

        setContentView(R.layout.activity_manage_dictionary)

        rvDictionaryList = findViewById(R.id.rvDictionaryList)
        rvDictionaryList!!.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)
        rvDictionaryList!!.adapter = DictionaryManagerAdapter()
    }

    override fun onDestroy() {
        dictionary!!.close()
        dictionary = null

        super.onDestroy()
    }

    override fun onDictionaryError(command: CompositeDictionary.Command?, err: Exception) {
        Log.e(TAG, "Dictionary error: ${err.message}")
    }

    override fun onDictionaryDownloadProgress(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        bytesDownloaded: Int,
        bytesTotal: Int
    ) {
        // TODO update the UI to show download percentage
    }

    override fun onDictionaryDownloadComplete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // TODO refresh the UI to show the dictionary has downloaded
    }

    override fun onDictionaryLoad(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // nothing to do
    }

    override fun onDictionaryDelete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // TODO refresh the UI to show the new list of dictionaries
    }

    override fun onAllDictionariesReady(success: Boolean, err: Exception?) {
        // nothing to do
    }

    override fun onIndexDownloadProgress(bytesDownloaded: Int, bytesTotal: Int) {
        // nothing to do
    }

    override fun onIndexDownloadComplete(success: Boolean, err: Exception?) {
        // TODO refresh the UI to show the updated dictionary list
    }

    override fun onDictionaryLookup(
        results: MutableList<CompositeDictionary.LookupResult>,
        successful: Int,
        total: Int
    ) {
        // Nothing to do. This activity does not look up words.
    }

    override fun onDictionarySuggest(
        results: MutableList<CompositeDictionary.SuggestResult>,
        successful: Int,
        total: Int
    ) {
        // Nothing to do. This activity does not search words.
    }

    //
}
