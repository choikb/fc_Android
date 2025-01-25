package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

class RegistrationActivity : AppCompatActivity() {

    private lateinit var spinnerCity: Spinner
    private lateinit var spinnerFireStation: Spinner
    private lateinit var spinnerCenter: Spinner
    private lateinit var spinnerRegion: Spinner
    private lateinit var editTextTeamName: EditText
    private lateinit var buttonRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        initViews()

        buttonRegister.setOnClickListener {
            val androidId = intent.getStringExtra("ANDROID_ID") ?: return@setOnClickListener
            val city = spinnerCity.selectedItem.toString()
            val fireStation = spinnerFireStation.selectedItem.toString()
            val center = spinnerCenter.selectedItem.toString()
            val region = spinnerRegion.selectedItem.toString()
            val teamName = editTextTeamName.text.toString()

            if (registerUser(androidId, city, fireStation, center, region, teamName)) {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Registration Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        spinnerCity = findViewById(R.id.spinnerCity)
        spinnerFireStation = findViewById(R.id.spinnerFireStation)
        spinnerCenter = findViewById(R.id.spinnerCenter)
        spinnerRegion = findViewById(R.id.spinnerRegion)
        editTextTeamName = findViewById(R.id.editTextTeamName)
        buttonRegister = findViewById(R.id.buttonRegister)
    }

    private fun registerUser(
        androidId: String,
        city: String,
        fireStation: String,
        center: String,
        region: String,
        teamName: String
    ): Boolean {
        val query = """
            INSERT INTO fc_app_user_info 
            (android_id, city, fire_station, center, region, team_name) 
            VALUES (?, ?, ?, ?, ?, ?)
        """
        try {
            DriverManager.getConnection("jdbc:mysql://<DB_HOST>:<DB_PORT>/<DB_NAME>", "username", "password").use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, androidId)
                    stmt.setString(2, city)
                    stmt.setString(3, fireStation)
                    stmt.setString(4, center)
                    stmt.setString(5, region)
                    stmt.setString(6, teamName)
                    stmt.executeUpdate()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
