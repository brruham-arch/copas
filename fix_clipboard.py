path = "app/src/main/java/com/copas/myapp/OverlayService.java"

with open(path, "r") as f:
    content = f.read()

old = """    private void handlePieAction(int index) {
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
                    Toast.makeText(this, ok ? "Pasted" : "Gagal paste (field tidak mendukung)", Toast.LENGTH_SHORT).show();
                } else if (svc == null) {
                    Toast.makeText(this, "Accessibility service belum aktif", Toast.LENGTH_SHORT).show();
                }
                break;"""

new = """    private void handlePieAction(int index) {
        ClipAccessibilityService svc = ClipAccessibilityService.getInstance();
        switch (index) {
            case 0: // Copy
                if (svc == null) {
                    Toast.makeText(this, "Accessibility service belum aktif", Toast.LENGTH_SHORT).show();
                    break;
                }
                String copied = svc.copySelectedText();
                if (copied != null) {
                    db.addClip(copied);
                    Toast.makeText(this, "Copied (" + copied.length() + " char)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Tidak ada teks terselect", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1: // Paste dari clipboard sistem saat ini
                if (svc == null) {
                    Toast.makeText(this, "Accessibility service belum aktif", Toast.LENGTH_SHORT).show();
                    break;
                }
                boolean ok = svc.pasteFromClipboard();
                Toast.makeText(this, ok ? "Pasted" : "Gagal paste (field tidak mendukung / clipboard kosong)", Toast.LENGTH_SHORT).show();
                break;"""

if old not in content:
    print("PATTERN TIDAK KETEMU")
else:
    content = content.replace(old, new)
    with open(path, "w") as f:
        f.write(content)
    print("Berhasil di-patch.")
