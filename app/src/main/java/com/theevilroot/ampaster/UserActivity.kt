package com.theevilroot.ampaster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import kotlin.concurrent.thread

class UserActivity: AppCompatActivity() {

    private val toolbar by bind<Toolbar>(R.id.toolbar)
    private val pastesView by bind<RecyclerView>(R.id.pastes_view)
    private val avatarView by bind<ImageView>(R.id.user_avatar)
    private val locationView by bind<TextView>(R.id.user_location)
    private val usernameView by bind<TextView>(R.id.user_name)
    private val emailView by bind<TextView>(R.id.user_email)
    private val proChip by bind<Chip>(R.id.pro_chip)

    private lateinit var adapter: PasteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        adapter = PasteAdapter(TheHolder.user!!)
        pastesView.layoutManager = LinearLayoutManager(this)
        pastesView.adapter = adapter
        try{
            initUserInfo()
        }catch (e: Exception) {
            thread(true) {
                with(TheHolder.user!!.loadUserInfoAndSettings()) {
                    when(first) {
                        PasteActivity.RequestResult.OK -> {
                            runOnUiThread { initUserInfo() }
                        }
                        else -> {
                            runOnUiThread {
                                toolbar.visibility = View.GONE
                                makeToast("Произошла ошибка при получении данных пользователя: $second")
                            }
                        }
                    }
                }
            }
        }
        TheHolder.user!!.pastes(true) { result, message ->
            runOnUiThread {
                when (result) {
                    PasteActivity.RequestResult.OK -> {
                        adapter.notifyDataSetChanged()
                    }
                    else -> {
                        toolbar.subtitle = message
                    }
                }
            }
        }
    }

    private fun initUserInfo() {
        if(TheHolder.user!!.avatarBitmap != null)
            avatarView.setImageBitmap(TheHolder.user!!.avatarBitmap)
        else Picasso.get().load(TheHolder.user!!.avatarUrl).into(avatarView)
        usernameView.text = TheHolder.user!!.username
        locationView.text = if(TheHolder.user!!.location.isBlank()) "Unknown" else TheHolder.user!!.location
        emailView.text = TheHolder.user!!.email
        if(TheHolder.user!!.type == "1") {
            proChip.setRawText("Pro")
        }else {
            proChip.setRawText("Free")
            proChip.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pastebin.com/pro")))
            }
        }
    }

}