package dev.dev7.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import dev.dev7dev.v2rayandroid.V2rayController;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText etIp = findViewById(R.id.et_ip);
        final EditText etPort = findViewById(R.id.et_port);
        final EditText etUuid = findViewById(R.id.et_uuid);
        Button btnFixedPort = findViewById(R.id.btn_fixed_port);
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);
        Button btnCheckTraffic = findViewById(R.id.btn_check_traffic);
        final TextView tvTrafficInfo = findViewById(R.id.tv_traffic_info);

        btnFixedPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPort.setText("2096");
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etIp.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                String uuid = etUuid.getText().toString().trim();

                if (ip.isEmpty() || portStr.isEmpty() || uuid.isEmpty()) {
                    Toast.makeText(MainActivity.this, "لطفاً تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show();
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    port = 443;
                }
                
                String security = (port == 2096) ? "tls" : "none";
                String vlessUri = "vless://" + uuid + "@" + ip + ":" + port + "?security=" + security + "&encryption=none#MinimalVPN";

                try {
                    V2rayController.INSTANCE.startV2ray(MainActivity.this, "Minimal Profile", vlessUri, null);
                    Toast.makeText(MainActivity.this, "در حال اتصال...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "خطا در استارت: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    V2rayController.INSTANCE.stopV2ray(MainActivity.this);
                    Toast.makeText(MainActivity.this, "اتصال قطع شد", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "خطا در قطع اتصال", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckTraffic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etIp.getText().toString().trim();
                String uuid = etUuid.getText().toString().trim();

                if (ip.isEmpty() || uuid.isEmpty()) {
                    Toast.makeText(MainActivity.this, "وارد کردن IP و UUID الزامی است", Toast.LENGTH_SHORT).show();
                    return;
                }

                String subUrl = "https://" + ip + ":2096/sub/" + uuid;
                Request request = new Request.Builder().url(subUrl).build();

                tvTrafficInfo.setText("در حال دریافت اطلاعات...");

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvTrafficInfo.setText("خطا در اتصال به سرور");
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String infoHeader = response.header("Subscription-Userinfo");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (infoHeader != null) {
                                    tvTrafficInfo.setText(parseTrafficHeader(infoHeader));
                                } else {
                                    tvTrafficInfo.setText("هدر حجم روی سرور یافت نشد");
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private String parseTrafficHeader(String header) {
        try {
            Map<String, Long> pairs = new HashMap<>();
            String[] parts = header.split(";");
            for (String part : parts) {
                String[] kv = part.trim().split("=");
                if (kv.length == 2) {
                    try {
                        pairs.put(kv[0], Long.parseLong(kv[1]));
                    } catch (NumberFormatException ignored) {}
                }
            }

            long upload = pairs.containsKey("upload") ? pairs.get("upload") : 0L;
            long download = pairs.containsKey("download") ? pairs.get("download") : 0L;
            long total = pairs.containsKey("total") ? pairs.get("total") : 0L;

            long remainingBytes = total - (upload + download);
            double remainingGB = (double) remainingBytes / (1024 * 1024 * 1024);

            return String.format("حجم باقی‌مانده: %.2f GB", remainingGB);
        } catch (Exception e) {
            return "خطا در پردازش اطلاعات ترافیک";
        }
    }
}