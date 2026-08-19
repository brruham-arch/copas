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
// version: 1.2
// Semua akses ClipboardManager HARUS lewat service ini, karena Android 10+
// membatasi baca clipboard di background hanya untuk app fokus, IME,
// atau AccessibilityService. OverlayService (Service biasa) tidak exempt,
// jadi kalau baca clipboard dari sana akan gagal diam-diam (getPrimaryClip
// bisa return null atau item kosong tanpa exception).
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
    // return isi text yang berhasil di-copy, atau null kalau gagal
    public String copySelectedText() {
        if (lastFocusedNode == null) return null;
        lastFocusedNode.refresh();

        boolean ok = lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY);
        if (!ok) return null;

        // baca hasil copy dari sini (exempt dari pembatasan clipboard background)
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            return text != null ? text.toString() : null;
        }
        return null;
    }

    // dipanggil dari OverlayService saat tombol Paste ditekan.
    // Baca clipboard SENDIRI (bukan dari OverlayService) supaya tidak
    // kena blokir background clipboard access, lalu paste ke field target.
    public boolean pasteFromClipboard() {
        if (lastFocusedNode == null) return false;
        lastFocusedNode.refresh();
        if (!lastFocusedNode.isEditable()) return false;

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return false;

        CharSequence content = clip.getItemAt(0).coerceToText(this);
        if (content == null) return false;

        boolean ok = lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        if (ok) return true;

        // fallback: banyak field tidak implement ACTION_PASTE
        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content);
        return lastFocusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }
}
