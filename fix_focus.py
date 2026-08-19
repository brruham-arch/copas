path = "app/src/main/java/com/copas/myapp/OverlayService.java"

with open(path, "r") as f:
    content = f.read()

old = """        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);"""

new = """        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);"""

if old not in content:
    print("PATTERN TIDAK KETEMU")
else:
    content = content.replace(old, new)
    with open(path, "w") as f:
        f.write(content)
    print("Berhasil di-patch.")
