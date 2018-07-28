package com.theevilroot.ampaster

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class LoginActivity: AppCompatActivity() {

    private val usernameField: EditText by bind(R.id.login_username_field)
    private val passwordField: EditText by bind(R.id.login_password_field)
    private val dnrCheck: CheckBox by bind(R.id.login_do_not_remember_check)
    private val submitButton:Button by bind(R.id.login_submit_button)
    private val toolbar: Toolbar by bind(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        submitButton.setOnClickListener {
            if(usernameField.text.isBlank() || passwordField.text.isBlank())
                return@setOnClickListener makeToast("Заполине все поля, это обязательно!")
            val user = User(usernameField.text.toString(),passwordField.text.toString())
            user.login { result, message ->
                when(result) {
                    PasteActivity.RequestResult.OK -> {
                        TheHolder.user = user
                        if(!dnrCheck.isChecked)
                            TheHolder.user!!.save(this)
                        this.finish()
                    }
                    else -> {
                        runOnUiThread {
                            toolbar.subtitle = message
                        }
                    }
                }
            }
        }

    }
}