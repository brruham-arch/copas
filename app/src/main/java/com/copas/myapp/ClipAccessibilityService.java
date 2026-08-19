package com.copas.myapp;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

// script_name: ClipAccessibilityService
// author: brruham
// version: 1.1
// Service ini tetap hidup di background (tidak kena batasan clipboard Android 10+)
// dan bisa performAction(ACTION_COPY / ACTION_PASTE) langsung ke node target
// tanpa perlu simulasi gesture. Kalau ACTION_PASTE tidak didukung field target
// (banyak EditText custom seperti WhatsApp/Instagram tidak implement ini),
// fallback ke ACTION_SET_TEXT yang replace seluruh isi field.
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
        return lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY);
    }

    // dipanggil dari OverlayService saat tombol Paste ditekan
    public boolean pasteText(String content) {
        if (lastFocusedNode == null) return false;
        lastFocusedNode.refresh();
        if (!lastFocusedNode.isEditable()) return false;

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("mycopas", content));

        boolean ok = lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        if (ok) return true;

        // fallback: banyak field tidak implement ACTION_PASTE,
        // langsung set isi field pakai ACTION_SET_TEXT
        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content);
        return lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }
}
