package com.theevilroot.ampaster

import android.content.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.mikepenz.materialdrawer.DrawerBuilder
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class PasteActivity : AppCompatActivity() {

    enum class LoadResult {
        OK,
        FILE_NOT_FOUND,
        NO_PERMISSIONS,
        INVALID_FILE,
        NO_TOKEN_PROVIDED,
        UNEXPECTED
    }

    enum class RequestResult {
        OK,
        CONNECTION_ERROR,
        NO_PERMISSION,
        REQUEST_ERROR,
        UNEXPECTED,
        NOT_TOKEN_PROVIDED,
        INVALID_CREDENTIALS
    }

    private val toolbar: Toolbar by bind(R.id.toolbar)
    private val pasteField: EditText by bind(R.id.paste_field)

    private var oldEmpty = true

    private lateinit var expirationChips: Array<Chip>
    private lateinit var visibilityChips: Array<Chip>
    private lateinit var highlightsList: Spinner
    private lateinit var pasteNameField: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paste)
        setSupportActionBar(toolbar)
        pasteField.addTextChangedListener(TextWatcherWrapper(onChange = {value,_,_,_->
            if((pasteField.text.isEmpty() && !oldEmpty) || (pasteField.text.isNotEmpty() && oldEmpty))
                invalidateOptionsMenu()
            oldEmpty = pasteField.text.isEmpty()
        }))
        initDrawer()
        loadUserFile()
    }

    private fun initDrawer() {
        val view = layoutInflater.inflate(R.layout.drawer_layout, null, false)
        DrawerBuilder()
                .withActivity(this)
                .withCustomView(view)
                .withToolbar(toolbar)
                .withTranslucentStatusBar(false)
                .withDisplayBelowStatusBar(true)
                .build()
        initChips(view)
        initHighlights(view)
        initCallbacks(view)
    }

    private fun initCallbacks(view: View) {
        pasteNameField = view.findViewById(R.id.paste_name)
        expirationChips.forEach { chip ->
            chip.isCheckable = true
            chip.setOnClickListener {
                if(chip.isChecked) {
                    expirationChips.filter { it.isChecked && it.id != chip.id }.forEach { it.isChecked = false }
                }else if(!expirationChips.any { it.isChecked && it.id != chip.id }) {
                    expirationChips[0].isChecked = true
                }
            }
        }
        visibilityChips.forEach { chip ->
            chip.isCheckable = true
            chip.setOnClickListener {
                if(chip.isChecked) {
                    visibilityChips.filter { it.isChecked && it.id != chip.id }.forEach { it.isChecked = false }
                }else if(!visibilityChips.any { it.isChecked && it.id != chip.id }) {
                    visibilityChips[0].isChecked = true
                }
            }
        }
    }

    private fun initChips(view: View) {
        expirationChips = arrayOf(
                view.findViewById(R.id.chip_never),
                view.findViewById(R.id.chip_10m),
                view.findViewById(R.id.chip_1h),
                view.findViewById(R.id.chip_1d),
                view.findViewById(R.id.chip_1w),
                view.findViewById(R.id.chip_2w),
                view.findViewById(R.id.chip_1mth),
                view.findViewById(R.id.chip_6mth),
                view.findViewById(R.id.chip_1y)
        )
        visibilityChips = arrayOf(
                view.findViewById(R.id.chip_public),
                view.findViewById(R.id.chip_unlisted),
                view.findViewById(R.id.chip_private)
        )
    }

    private fun initHighlights(view: View) {
        highlightsList = view.findViewById(R.id.paste_highlight)
        highlightsList.adapter = ArrayAdapter<String>(view.context, android.R.layout.simple_dropdown_item_1line, TheHolder.highlightings.values.toTypedArray())
    }

    private fun loadUserFile() = thread(true) {
        if(TheHolder.user != null)
            return@thread onLoad(if(TheHolder.user!!.token != null) LoadResult.OK else LoadResult.NO_TOKEN_PROVIDED, TheHolder.user)
        try {
            val file = File(filesDir, "user.info")
            if (!file.exists()) {
                return@thread onLoad(LoadResult.FILE_NOT_FOUND)
            }
            val json = TheHolder.jsonParser.parse(file.readText()).asJsonObject
            if(!json.has("token"))
                return@thread onLoad(LoadResult.NO_TOKEN_PROVIDED, User(json["username"].asString, json["password"].asString))
            return@thread onLoad(LoadResult.OK,User(json["username"].asString, json["password"].asString, json["token"].asString))
        }catch (e: IOException) {
            if(e.message?.contains("EACCES") == true)
                return@thread onLoad(LoadResult.NO_PERMISSIONS)
            return@thread onLoad(LoadResult.UNEXPECTED)
        }catch (e: com.google.gson.JsonSyntaxException) {
            return@thread onLoad(LoadResult.INVALID_FILE)
        }catch (e: com.google.gson.JsonParseException) {
            return@thread onLoad(LoadResult.INVALID_FILE)
        }catch (e: Exception) {
            return@thread onLoad(LoadResult.UNEXPECTED)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_paste_menu, menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == R.id.menu_submit) {
                menu.getItem(i).isVisible = pasteField.text.isNotBlank()
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_submit -> {
                pasteField.isEnabled = false
                toolbar.isEnabled = false
                val name = if(pasteNameField.text.isEmpty()) "New Paste[Paster]" else pasteNameField.text.toString()
                val expiration = TheHolder.expirations[(expirationChips.filter { it.isChecked }.getOrNull(0)?.chipDrawable as? ChipDrawable)?.text ?: "Никогда"] ?: "N"
                val visibility = TheHolder.visibilities[(visibilityChips.filter { it.isChecked }.getOrNull(0)?.chipDrawable as? ChipDrawable)?.text ?: "Публичный"] ?: "0"
                val highlight = TheHolder.highlightings.entries.filter { it.value == (highlightsList.selectedItem as String) }.getOrNull(0)?.key ?: "null"
                TheHolder.user!!.newPaste(pasteField.text.toString(), visibility.toInt(), if(highlight == "null") null else highlight, { result, msg ->
                    runOnUiThread {
                        val dialog = MaterialDialog.Builder(this).title("Добавление").autoDismiss(false).positiveText("Открыть").neutralText("Копировать")
                        when (result) {
                            RequestResult.OK -> {
                                pasteField.setText("")
                                dialog.content(msg)
                                dialog.onPositive { _, _ ->
                                     startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(msg)))
                                }
                                dialog.onNeutral{ _, _ ->
                                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboardManager.primaryClip = ClipData.newRawUri("PasteUrl", Uri.parse(msg))
                                }
                            }
                            RequestResult.NOT_TOKEN_PROVIDED -> {
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                            else -> {
                                dialog.content("${result.name}: $msg")
                            }
                        }
                        dialog.show()
                        pasteField.isEnabled = true
                        toolbar.isEnabled = true
                    }
                }, expirationKey = expiration, name = name)
            }
            R.id.menu_user -> {
                startActivity(Intent(this, UserActivity::class.java))
            }
        }
        return true
    }

    private fun onLoad(result: LoadResult, user: User? = null) {
        when(result) {
            PasteActivity.LoadResult.OK -> {
                TheHolder.user = user
            }
            LoadResult.NO_TOKEN_PROVIDED -> {
                TheHolder.user = user
                TheHolder.user!!.login(::onAuth)
            }
            PasteActivity.LoadResult.FILE_NOT_FOUND,
            PasteActivity.LoadResult.NO_PERMISSIONS,
            PasteActivity.LoadResult.INVALID_FILE,
            PasteActivity.LoadResult.UNEXPECTED -> {
                makeToast("Please, login to your PasteBin.com account")
                runOnUiThread {
                    startActivity(Intent(this@PasteActivity, LoginActivity::class.java))
                }
            }
        }
    }

    private fun onAuth(result: RequestResult, message: String) {
        when(result) {
            RequestResult.OK -> {}
            else -> {
                makeToast("Saved data not valid. Login again please")
                startActivity(Intent(this,LoginActivity::class.java))
            }
        }
    }
}
