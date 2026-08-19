package com.copas.myapp;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// script_name: ClipAccessibilityService
// author: brruham
// version: 1.0
// Service ini tetap hidup di background (tidak kena batasan clipboard Android 10+)
// dan bisa performAction(ACTION_COPY / ACTION_PASTE) langsung ke node target
// tanpa perlu simulasi gesture.
public class ClipAccessibilityService extends AccessibilityService {

    private static ClipAccessibilityService instance;
    private AccessibilityNodeInfo lastFocusedNode;

    public static ClipAccessibilityService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        // start overlay service otomatis begitu accessibility aktif
        startForegroundService(new Intent(this, OverlayService.class));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
                || type == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            AccessibilityNodeInfo src = event.getSource();
            if (src != null) {
                if (lastFocusedNode != null) lastFocusedNode.recycle();
                lastFocusedNode = src;
            }
        }
    }

    @Override
    public void onInterrupt() { }

    // dipanggil dari OverlayService saat tombol Copy ditekan
    public boolean copySelectedText() {
        if (lastFocusedNode == null) return false;
        lastFocusedNode.refresh();
        // kalau ada text yang ter-select, ACTION_COPY akan copy selection-nya
        return lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY);
    }

    // dipanggil dari OverlayService saat tombol Paste ditekan
    public boolean pasteText(String content) {
        if (lastFocusedNode == null) return false;
        lastFocusedNode.refresh();
        if (!lastFocusedNode.isEditable()) return false;

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("mycopas", content));
        return lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }
}
