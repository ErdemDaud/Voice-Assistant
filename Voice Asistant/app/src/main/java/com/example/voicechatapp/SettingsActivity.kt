package com.example.voicechatapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.net.InetAddress

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCurrentIp: TextView
    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var btnAutoDiscover: Button
    private lateinit var btnSaveSettings: Button
    private lateinit var btnTestConnection: Button
    private lateinit var tvStatus: TextView
    private lateinit var prefs: SharedPreferences
    private val client = OkHttpClient()
    private var discoveryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize SharedPreferences
        prefs = getSharedPreferences("ServerSettings", Context.MODE_PRIVATE)

        // Initialize views
        tvCurrentIp = findViewById(R.id.tvCurrentIp)
        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        btnAutoDiscover = findViewById(R.id.btnAutoDiscover)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnTestConnection = findViewById(R.id.btnTestConnection)
        tvStatus = findViewById(R.id.tvStatus)

        // Load current settings
        loadCurrentSettings()

        // Setup button listeners
        setupButtonListeners()
    }

    private fun loadCurrentSettings() {
        val serverIp = prefs.getString("server_ip", "10.64.195.116") ?: "10.64.195.116"
        val serverPort = prefs.getInt("server_port", 5000)

        tvCurrentIp.text = "$serverIp:$serverPort"
        etServerIp.setText(serverIp)
        etServerPort.setText(serverPort.toString())
    }

    private fun setupButtonListeners() {
        // Auto-discover button
        btnAutoDiscover.setOnClickListener {
            autoDiscoverServer()
        }

        // Save settings button
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        // Test connection button
        btnTestConnection.setOnClickListener {
            testConnection()
        }
    }

    private fun autoDiscoverServer() {
        showStatus("🔍 Searching for server on network...", "#FFD700")
        btnAutoDiscover.isEnabled = false

        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get local IP to determine network range
                val localIp = getLocalIpAddress()
                if (localIp == null) {
                    withContext(Dispatchers.Main) {
                        showStatus("❌ Could not detect network", "#FF5252")
                        btnAutoDiscover.isEnabled = true
                    }
                    return@launch
                }

                // Extract network prefix (e.g., "192.168.1" from "192.168.1.100")
                val ipParts = localIp.split(".")
                if (ipParts.size != 4) {
                    withContext(Dispatchers.Main) {
                        showStatus("❌ Invalid network configuration", "#FF5252")
                        btnAutoDiscover.isEnabled = true
                    }
                    return@launch
                }

                val networkPrefix = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"
                val port = etServerPort.text.toString().toIntOrNull() ?: 5000

                // Scan common IP addresses in the network
                var foundServer: String? = null
                for (i in 1..254) {
                    if (!isActive) break // Check if job was cancelled

                    val testIp = "$networkPrefix.$i"

                    withContext(Dispatchers.Main) {
                        showStatus("🔍 Checking $testIp...", "#FFD700")
                    }

                    if (testServerConnection(testIp, port)) {
                        foundServer = testIp
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    if (foundServer != null) {
                        etServerIp.setText(foundServer)
                        showStatus("✅ Server found at $foundServer:$port", "#4CAF50")
                        Toast.makeText(this@SettingsActivity, "Server discovered!", Toast.LENGTH_LONG).show()
                    } else {
                        showStatus("❌ No server found on network", "#FF5252")
                        Toast.makeText(this@SettingsActivity, "Could not find server. Try manual entry.", Toast.LENGTH_LONG).show()
                    }
                    btnAutoDiscover.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showStatus("❌ Discovery failed: ${e.message}", "#FF5252")
                    btnAutoDiscover.isEnabled = true
                }
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress

            return String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun testServerConnection(ip: String, port: Int): Boolean {
        return try {
            val formBody = FormBody.Builder()
                .add("message", "ping")
                .build()

            val request = Request.Builder()
                .url("http://$ip:$port/receive")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    private fun saveSettings() {
        val ip = etServerIp.text.toString().trim()
        val port = etServerPort.text.toString().toIntOrNull() ?: 5000

        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show()
            return
        }

        // Save to SharedPreferences
        prefs.edit().apply {
            putString("server_ip", ip)
            putInt("server_port", port)
            apply()
        }

        tvCurrentIp.text = "$ip:$port"
        showStatus("✅ Settings saved successfully", "#4CAF50")
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        val ip = etServerIp.text.toString().trim()
        val port = etServerPort.text.toString().toIntOrNull() ?: 5000

        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show()
            return
        }

        showStatus("🔌 Testing connection to $ip:$port...", "#FFD700")
        btnTestConnection.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val success = testServerConnection(ip, port)

            withContext(Dispatchers.Main) {
                if (success) {
                    showStatus("✅ Connection successful!", "#4CAF50")
                    Toast.makeText(this@SettingsActivity, "Server is reachable!", Toast.LENGTH_SHORT).show()
                } else {
                    showStatus("❌ Connection failed. Check IP and make sure server is running.", "#FF5252")
                    Toast.makeText(this@SettingsActivity, "Cannot reach server", Toast.LENGTH_SHORT).show()
                }
                btnTestConnection.isEnabled = true
            }
        }
    }

    private fun showStatus(message: String, color: String) {
        tvStatus.text = message
        tvStatus.setTextColor(android.graphics.Color.parseColor(color))
        tvStatus.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryJob?.cancel()
    }
}
