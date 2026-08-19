package com.copas.myapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// script_name: MainActivity
// author: brruham
// version: 1.0
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("MyCopas Setup");
        title.setTextSize(22f);
        root.addView(title);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. Izinkan Overlay");
        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(MainActivity.this)) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } else {
                    Toast.makeText(MainActivity.this, "Overlay sudah diizinkan", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(btnOverlay);

        Button btnAccessibility = new Button(this);
        btnAccessibility.setText("2. Aktifkan Accessibility Service");
        btnAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(btnAccessibility);

        Button btnStart = new Button(this);
        btnStart.setText("3. Start Overlay Service");
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startForegroundService(new Intent(MainActivity.this, OverlayService.class));
                Toast.makeText(MainActivity.this, "Overlay started", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnStart);

        setContentView(root);
    }
}
