package com.copas.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

// script_name: OverlayService
// author: brruham
// version: 1.0
// Bubble melayang (draggable) + saat tap, muncul pie menu radial
// dengan opsi Copy / Paste / Riwayat / Tutup.
public class OverlayService extends Service {

    private WindowManager wm;
    private View bubble;
    private PieMenuView pieMenu;
    private ClipDbHelper db;
    private WindowManager.LayoutParams bubbleParams;

    private static final String CHANNEL_ID = "mycopas_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        db = new ClipDbHelper(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForegroundNotif();
        addBubble();
    }

    private void startForegroundNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "MyCopas", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MyCopas aktif")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build();
        startForeground(1, n);
    }

    private void addBubble() {
        TextView tv = new TextView(this);
        tv.setText("📋");
        tv.setTextSize(24f);
        tv.setBackgroundColor(Color.parseColor("#993A7BD5"));
        tv.setPadding(24, 24, 24, 24);
        bubble = tv;

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 0;
        bubbleParams.y = 300;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float initialX, initialY, touchX, touchY;
            boolean dragged = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        dragged = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - touchX;
                        float dy = event.getRawY() - touchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) dragged = true;
                        bubbleParams.x = (int) (initialX + dx);
                        bubbleParams.y = (int) (initialY + dy);
                        wm.updateViewLayout(bubble, bubbleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragged) showPieMenu();
                        return true;
                }
                return false;
            }
        });

        wm.addView(bubble, bubbleParams);
    }

    private void showPieMenu() {
        if (pieMenu != null) return; // sudah terbuka

        String[] labels = {"Copy", "Paste", "Riwayat", "Tutup"};
        pieMenu = new PieMenuView(this, labels);

        int size = 600;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = bubbleParams.x - size / 3;
        params.y = bubbleParams.y - size / 3;

        pieMenu.setOnItemSelected(new PieMenuView.OnItemSelected() {
            @Override
            public void onSelected(int index) {
                handlePieAction(index);
                removePieMenu();
            }
        });

        wm.addView(pieMenu, params);
    }

    private void removePieMenu() {
        if (pieMenu != null) {
            wm.removeView(pieMenu);
            pieMenu = null;
        }
    }

    private void handlePieAction(int index) {
        ClipAccessibilityService svc = ClipAccessibilityService.getInstance();
        switch (index) {
            case 0: // Copy
                if (svc != null && svc.copySelectedText()) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = cm.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence text = clip.getItemAt(0).coerceToText(this);
                        db.addClip(text.toString());
                        Toast.makeText(this, "Copied (" + text.length() + " char)", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Tidak ada teks terselect", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1: // Paste dari clipboard sistem saat ini
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = cm.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0 && svc != null) {
                    String text = clip.getItemAt(0).coerceToText(this).toString();
                    boolean ok = svc.pasteText(text);
                    Toast.makeText(this, ok ? "Pasted" : "Gagal paste (field tidak editable?)", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2: // Riwayat -> buka MainActivity list riwayat
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra("open_history", true);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            case 3: // Tutup overlay
                stopSelf();
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removePieMenu();
        if (bubble != null) wm.removeView(bubble);
    }
}
