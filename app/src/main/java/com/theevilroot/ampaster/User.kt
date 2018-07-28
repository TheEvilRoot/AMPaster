package com.theevilroot.ampaster

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

data class User(val username: String, val password: String, var token: String? = null) {

    var pasteList: List<Paste>? = null
    var avatarBitmap: Bitmap? = null

    var infoLoaded = false

    lateinit var defaultSyntax: String
    lateinit var defaultExpiration: String
    lateinit var defaultVisibility: String
    lateinit var avatarUrl: String
    lateinit var webSite: String
    lateinit var email: String
    lateinit var location: String
    lateinit var type: String

    fun login(onAuth: (PasteActivity.RequestResult, String) -> Unit) = thread(true) {
        try {
            val response = Jsoup.connect("https://pastebin.com/api/api_login.php").data(mapOf(
                    "api_dev_key" to TheHolder.PASTEBIN_DEV_TOKEN,
                    "api_user_name" to username,
                    "api_user_password" to password)).post().text()
            when {
                "Bad API request" in response -> {
                    onAuth(PasteActivity.RequestResult.INVALID_CREDENTIALS, response)
                }
                else -> {
                    this.token = response
                    with(loadUserInfoAndSettings()) {
                        return@thread when(first) {
                            PasteActivity.RequestResult.OK -> onAuth(PasteActivity.RequestResult.OK, "OK")
                            else -> onAuth(first, second)
                        }
                    }
                }
            }
        }catch (e: SecurityException) {
            return@thread onAuth(PasteActivity.RequestResult.NO_PERMISSION, "NO_PERMISSION")
        }catch (e: IOException) {
            return@thread onAuth(PasteActivity.RequestResult.CONNECTION_ERROR, "CONNECTION_ERROR")
        }catch (e: Exception) {
            return@thread onAuth(PasteActivity.RequestResult.UNEXPECTED, "UNEXPECTED")
        }
    }

    fun loadUserInfoAndSettings(): Pair<PasteActivity.RequestResult, String> {
        try{
            val response = Jsoup.connect("https://pastebin.com/api/api_post.php").data(mapOf(
                    "api_dev_key" to TheHolder.PASTEBIN_DEV_TOKEN,
                    "api_user_key" to token,
                    "api_option" to "userdetails"
            )).post()
            if("Bad API request" in response.text()) {
                return PasteActivity.RequestResult.REQUEST_ERROR to response.text()
            }
            with(response.select("user")) {
                defaultSyntax = select("user_format_short").text()
                defaultExpiration = select("user_expiration").text()
                defaultVisibility = select("user_private").text()
                avatarUrl = select("user_avatar_url").text()
                webSite = select("user_website").text()
                email = select("user_email").text()
                location = select("user_location").text()
                type = select("user_account_type").text()
                infoLoaded = true
            }
            return PasteActivity.RequestResult.OK to ""
        }catch (e: SecurityException) {
            return PasteActivity.RequestResult.NO_PERMISSION to "NO_PERMISSION"
        }catch (e: IOException) {
            return PasteActivity.RequestResult.CONNECTION_ERROR to "CONNECTION_ERROR"
        }catch (e: Exception) {
            return PasteActivity.RequestResult.UNEXPECTED to "UNEXPECTED"
        }
    }

    fun newPaste(text: String,
                 visibility: Int = 0,
                 highlightKey: String? = null,
                 onPasted: (PasteActivity.RequestResult, String) -> Unit,
                 expirationKey: String = "N",
                 name: String = "New Paste[Paster]") = thread(true) {
        if(token == null)
            return@thread onPasted(PasteActivity.RequestResult.NOT_TOKEN_PROVIDED, "")
        try{
            val request = Jsoup.connect("https://pastebin.com/api/api_post.php").data(mapOf(
                    "api_dev_key" to TheHolder.PASTEBIN_DEV_TOKEN,
                    "api_option" to "paste",
                    "api_paste_code" to text,
                    "api_user_key" to token,
                    "api_paste_name" to name,
                    "api_paste_private" to visibility.toString(),
                    "api_paste_expire_date" to expirationKey
            ))
            if(highlightKey != null)
                request.data("api_paste_format", highlightKey)
            val response = request.post().text()
            when {
                "Bad API request" in response -> {
                    onPasted(PasteActivity.RequestResult.REQUEST_ERROR, response)
                }
                else -> {
                    onPasted(PasteActivity.RequestResult.OK, response)
                }
            }
        }catch (e: SecurityException) {
            return@thread onPasted(PasteActivity.RequestResult.NO_PERMISSION, "NO_PERMISSION")
        }catch (e: IOException) {
            return@thread onPasted(PasteActivity.RequestResult.CONNECTION_ERROR, "CONNECTION_ERROR")
        }catch (e: Exception) {
            e.printStackTrace()
            return@thread onPasted(PasteActivity.RequestResult.UNEXPECTED, "UNEXPECTED")
        }
    }

    fun save(context: Context) = thread(true) {
        val file = File(context.filesDir,"user.info")
        val json = JsonObject();
        json.addProperty("username", username)
        json.addProperty("password", password)
        if(token != null)
            json.addProperty("token", token)
        file.writeText(TheHolder.gsonBuilder.toJson(json))
    }

    fun pastes(forceRefresh: Boolean, onLoaded: (PasteActivity.RequestResult, String) -> Unit) = thread(true) {
        if(pasteList != null && !forceRefresh)
            return@thread onLoaded(PasteActivity.RequestResult.OK, "")
        if(token == null)
            return@thread onLoaded(PasteActivity.RequestResult.NOT_TOKEN_PROVIDED, "")
        try{
            val response = Jsoup.connect("https://pastebin.com/api/api_post.php").data(mapOf(
                    "api_dev_key" to TheHolder.PASTEBIN_DEV_TOKEN,
                    "api_user_key" to token,
                    "api_option" to "list",
                    "api_results_limit" to "1000"
            )).post()
            if("Bad API request" in response.text()) {
                return@thread onLoaded(PasteActivity.RequestResult.REQUEST_ERROR, response.text())
            } else if("No pastes found" in response.text()) {
                pasteList = emptyList()
                return@thread onLoaded(PasteActivity.RequestResult.OK, "")
            }
            val ret = ArrayList<Paste>()
            response.select("paste").forEach {
                ret.add(Paste(id = it.select("paste_key").text(),
                        name = it.select("paste_title").text(),
                        size = it.select("paste_size").text().toIntOrNull() ?: -1,
                        expireDate = Date(it.select("paste_expire_date").text().toLongOrNull() ?: -1),
                        visibilityKey = it.select("paste_private").text(),
                        syntaxFull = it.select("paste_format_long").text(),
                        syntaxKey = it.select("paste_format_short").text(),
                        hits = it.select("paste_hits").text().toIntOrNull() ?: -1,
                        date = Date(it.select("paste_date").text().toLongOrNull() ?: -1)))
            }
            pasteList = ret.sortedBy { it.date.time }
            onLoaded(PasteActivity.RequestResult.OK, "")
        }catch (e: SecurityException) {
            return@thread onLoaded(PasteActivity.RequestResult.NO_PERMISSION, "NO_PERMISSION")
        }catch (e: IOException) {
            return@thread onLoaded(PasteActivity.RequestResult.CONNECTION_ERROR, "CONNECTION_ERROR")
        }catch (e: Exception) {
            e.printStackTrace()
            return@thread onLoaded(PasteActivity.RequestResult.UNEXPECTED, "UNEXPECTED")
        }
    }

}