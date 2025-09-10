package com.smartswitch.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.smartswitch.R
import com.smartswitch.databinding.ActivityInvitationBinding
import com.smartswitch.databinding.ActivityReceivingBinding
import java.io.File

class InvitationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInvitationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnBluetooth.setOnClickListener { shareAppViaBluetooth1() }
        binding.btnShareLink.setOnClickListener { shareAppLink() }
        binding.btnWhatsApp.setOnClickListener { shareAppViaWhatsApp() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun shareAppViaBluetooth2() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val apkFile = File(appInfo.sourceDir)

            val uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // ✅ Let user pick, Bluetooth will be shown if available
            startActivity(Intent.createChooser(intent, "Share App via"))
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAppViaBluetooth() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val apkFile = File(appInfo.sourceDir)

            val uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Find Bluetooth app dynamically
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            var bluetoothApp: String? = null
            for (info in resolveInfos) {
                if (info.activityInfo.packageName.contains("bluetooth")) {
                    bluetoothApp = info.activityInfo.packageName
                    break
                }
            }

            if (bluetoothApp != null) {
                intent.setPackage(bluetoothApp)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAppViaBluetooth1() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val apkFile = File(appInfo.sourceDir)

            val uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                setPackage("com.android.bluetooth")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share App via Bluetooth"))
        } catch (e: Exception) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Share Link (Google Play / custom link) */
    private fun shareAppLink() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Download my app here: https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share Link"))
    }

    /** ✅ Share via WhatsApp */
    private fun shareAppViaWhatsApp() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Download my app here: https://play.google.com/store/apps/details?id=$packageName"
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
}