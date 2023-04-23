package com.mandysoftware.livrenouveau

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mandysoftware.wordutil.CompositeDictionary
import com.mandysoftware.wordutil.CompositeDictionary.DictionaryInfo
import com.mandysoftware.wordutil.Text
import java.io.File
import java.lang.Exception
import java.util.*

class ManageDictionaryActivity : AppCompatActivity(), CompositeDictionary.DictionaryCallback {
    val TAG = "ManageDictionaryAty"

    var dictionary : CompositeDictionary? = null

    var rvDictionaryList : RecyclerView? = null
    var rvAdapter : DictionaryManagerAdapter? = null

    inner class DictionaryManagerAdapter : RecyclerView.Adapter<DictionaryItemViewHolder>()
    {
        // An int that tells us a type. We're only using one type for this RecyclerView.
        val TYPE_DICTIONARY_ITEM = 1

        // Creates a view holder for TYPE_DICTIONARY_ITEM.
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DictionaryItemViewHolder {
            if (viewType == TYPE_DICTIONARY_ITEM) {
                val root = LayoutInflater.from(this@ManageDictionaryActivity)
                    .inflate(R.layout.dictionary_manager_item, parent, false)
                return DictionaryItemViewHolder(root)
            }
            else
            {
                throw IllegalArgumentException("Urecognized view type.")
            }
        }

        override fun getItemCount(): Int {
            return dictionary!!.dictionaryList.size
        }

        override fun onBindViewHolder(holder: DictionaryItemViewHolder, position: Int) {
            holder.bind(dictionary!!.dictionaryList[position])
        }

        // For this one, all of them are of the same type
        // because each card in the list shows info about a dictionary.
        override fun getItemViewType(position: Int): Int {
            return TYPE_DICTIONARY_ITEM
        }
    }

    inner class DictionaryItemViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tvName : TextView
        val tvSize : TextView
        val tvDesc : TextView

        val btnDownload : Button
        val btnUpdate : Button

        val pbDownload : ProgressBar

        var downloadBtnState = "download"
        var dictionaryInfo : DictionaryInfo? = null

        init {
            tvName = root.findViewById(R.id.tvName)
            tvSize = root.findViewById(R.id.tvSize)
            tvDesc = root.findViewById(R.id.tvDescription)
            btnDownload = root.findViewById(R.id.btnDownload)
            btnUpdate = root.findViewById(R.id.btnUpdate)
            pbDownload = root.findViewById(R.id.pbDownload)

            btnDownload.setOnClickListener {
                if (downloadBtnState.equals("download"))
                {
                    dictionary!!.postCommand(
                        CompositeDictionary.Command(
                            CompositeDictionary.CommandName.DOWNLOAD_DICTIONARY,
                            dictionaryInfo!!.name
                        )
                    )
                }
                else if (downloadBtnState.equals("delete"))
                {
                    dictionary!!.postCommand(
                        CompositeDictionary.Command(
                            CompositeDictionary.CommandName.DELETE_DICTIONARY,
                            dictionaryInfo!!.name
                        )
                    )
                }
            }

            btnUpdate.setOnClickListener {
                dictionary!!.postCommand(
                    CompositeDictionary.Command(
                        CompositeDictionary.CommandName.DOWNLOAD_DICTIONARY,
                        dictionaryInfo!!.name
                    )
                )
            }
        }

        fun bind(info : DictionaryInfo)
        {
            dictionaryInfo = info

            tvName.text = info.name
            tvDesc.text = info.description

            if (info.fileSizeLocal > 0) {
                tvSize.text = Text.formatFileSize(info.fileSizeLocal, Locale.getDefault())
            }
            else
            {
                tvSize.text = ""
            }

            if (info.isAvailableOffline)
            {
                btnDownload.setText(getString(R.string.manage_dictionary_delete))
                downloadBtnState = "delete"
            }
            else if (info.progress == 0 || info.progress == 100)
            {
                if (info.fileSizeRemote > 0) {
                    btnDownload.setText(
                        getString(R.string.manage_dictionary_download_size)
                            .replace(
                                "[size]",
                                Text.formatFileSize(info.fileSizeRemote, Locale.getDefault())
                            )
                    )
                }
                else
                {
                    btnDownload.setText(getString(R.string.manage_dictionary_download))
                }
                downloadBtnState = "download"
            }
            else
            {
                btnDownload.setText(getString(R.string.manage_dictionary_cancel))
                downloadBtnState = "cancel"
            }

            if (info.hasUpdate && (info.progress == 0 || info.progress == 100))
            {
                if (info.fileSizeRemote > 0) {
                    btnUpdate.setText(
                        getString(R.string.manage_dictionary_update_size)
                            .replace(
                                "[size]",
                                Text.formatFileSize(info.fileSizeRemote, Locale.getDefault())
                            )
                    )
                }
                else
                {
                    btnUpdate.setText(getString(R.string.manage_dictionary_update))
                }
                btnUpdate.visibility = View.VISIBLE
            }
            else
            {
                btnUpdate.visibility = View.GONE
            }

            if (info.progress != 0 && info.progress != 100)
            {
                pbDownload.progress = info.progress
                pbDownload.visibility = View.VISIBLE
            }
            else
            {
                pbDownload.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The generic loading - can copy-paste this into any other activity that needs it.
        dictionary = CompositeDictionary(File(filesDir, "dictionary"))
        dictionary!!.addDictionaryCallback(this)
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.INIT))

        // Then post another command that is specific only to manage dictionaries activity.
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.UPDATE_LOCAL))
        dictionary!!.postCommand(CompositeDictionary.Command(CompositeDictionary.CommandName.HEAD_FILES))

        // Then generic again. Set up the views. The setContentView() must come BEFORE any find*().
        setContentView(R.layout.activity_manage_dictionary)

        rvAdapter = DictionaryManagerAdapter()

        rvDictionaryList = findViewById(R.id.rvDictionaryList)
        rvDictionaryList!!.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.VERTICAL,
            false)
        rvDictionaryList!!.adapter = rvAdapter!!
    }

    override fun onDestroy() {
        dictionary!!.close()
        dictionary = null

        super.onDestroy()
    }

    override fun onDictionaryError(command: CompositeDictionary.Command?, err: Exception) {
        Log.e(TAG, "Dictionary error: ${err.message}")

        runOnUiThread {
            Toast.makeText(this, err.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDictionaryDownloadProgress(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        bytesDownloaded: Int,
        bytesTotal: Int
    ) {
        runOnUiThread {
            rvAdapter!!.notifyItemChanged(dictionary!!.dictionaryList.indexOf(dictionaryInfo))
        }
    }

    override fun onDictionaryDownloadComplete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        if (success) {
            dictionary!!.postCommand(
                CompositeDictionary.Command(
                    CompositeDictionary.CommandName.UPDATE_LOCAL
                )
            )
        }
    }

    override fun onDictionaryLoad(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        // nothing to do here
    }

    override fun onDictionaryDelete(
        dictionaryInfo: CompositeDictionary.DictionaryInfo,
        success: Boolean,
        err: Exception?
    ) {
        if (success) {
            dictionary!!.postCommand(
                CompositeDictionary.Command(
                    CompositeDictionary.CommandName.UPDATE_LOCAL
                )
            )
        }
    }

    override fun onAllDictionariesReady(success: Boolean, err: Exception?) {
        // nothing to do
    }

    override fun onIndexDownloadProgress(bytesDownloaded: Int, bytesTotal: Int) {
        // nothing to do
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onIndexDownloadComplete(success: Boolean, err: Exception?) {
        runOnUiThread {
            rvAdapter!!.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onHeadFilesComplete(successful : Int, total : Int,
                                     success: Boolean, err: Exception?) {
        runOnUiThread {
            rvAdapter!!.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onLocalFilesChecked() {
        runOnUiThread {
            rvAdapter!!.notifyDataSetChanged()
        }
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
