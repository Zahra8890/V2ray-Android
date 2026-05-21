package com.dev7dev.v2rayandroid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.dev7dev.v2rayandroid.V2rayController
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etIp = findViewById<EditText>(R.id.et_ip)
        val etPort = findViewById<EditText>(R.id.et_port)
        val etUuid = findViewById<EditText>(R.id.et_uuid)
        val btnFixedPort = findViewById<Button>(R.id.btn_fixed_port)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnStop = findViewById<Button>(R.id.btn_stop)
        val btnCheckTraffic = findViewById<Button>(R.id.btn_check_traffic)
        val tvTrafficInfo = findViewById<TextView>(R.id.tv_traffic_info)

        btnFixedPort.setOnClickListener {
            etPort.setText("2096")
        }

        btnStart.setOnClickListener {
            val ip = etIp.text.toString().trim()
            val portStr = etPort.text.toString().trim()
            val uuid = etUuid.text.toString().trim()

            if (ip.isEmpty() || portStr.isEmpty() || uuid.isEmpty()) {
                Toast.makeText(this, "لطفاً تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portStr.toIntOrNull() ?: 443
            val security = if (port == 2096) "tls" else "none"
            
            // ساخت لینک اختصاصی VLESS
            val vlessUri = "vless://$uuid@$ip:$port?security=$security&encryption=none#MinimalVPN"

            try {
                V2rayController.startV2ray(this, "Minimal Profile", vlessUri, null)
                Toast.makeText(this, "در حال اتصال...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "خطا در استارت: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnStop.setOnClickListener {
            V2rayController.stopV2ray(this)
            Toast.makeText(this, "اتصال قطع شد", Toast.LENGTH_SHORT).show()
        }

        btnCheckTraffic.setOnClickListener {
            val ip = etIp.text.toString().trim()
            val uuid = etUuid.text.toString().trim()

            if (ip.isEmpty() || uuid.isEmpty()) {
                Toast.makeText(this, "وارد کردن IP و UUID الزامی است", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // درخواست به پورت ۲۰۹۶ برای دریافت هدر ترافیک باقی‌مانده
            val subUrl = "https://$ip:2096/sub/$uuid"
            val request = Request.Builder().url(subUrl).build()

            tvTrafficInfo.text = "در حال دریافت اطلاعات..."

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { tvTrafficInfo.text = "خطا در اتصال به سرور" }
                }

                override fun onResponse(call: Call, response: Response) {
                    val infoHeader = response.header("Subscription-Userinfo")
                    runOnUiThread {
                        if (infoHeader != null) {
                            tvTrafficInfo.text = parseTrafficHeader(infoHeader)
                        } else {
                            tvTrafficInfo.text = "هدر حجم روی سرور یافت نشد"
                        }
                    }
                }
            })
        }
    }

    private fun parseTrafficHeader(header: String): String {
        return try {
            val pairs = header.split(";").associate {
                val parts = it.trim().split("=")
                if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: 0L) else "" to 0L
            }
            val upload = pairs["upload"] ?: 0L
            val download = pairs["download"] ?: 0L
            val total = pairs["total"] ?: 0L

            val remainingBytes = total - (upload + download)
            val remainingGB = remainingBytes.toDouble() / (1024 * 1024 * 1024)

            String.format("حجم باقی‌مانده: %.2f GB", remainingGB)
        } catch (e: Exception) {
            "خطا در پردازش اطلاعات ترافیک"
        }
    }
}