package pl.khuzzuk.ui;

import com.sun.jna.CallbackReference;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WindowProc;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.util.Objects;

class WindowsMediaKeyListener implements AutoCloseable {
    private static final int WM_APPCOMMAND = 0x0319;
    private static final int APPCOMMAND_MEDIA_NEXTTRACK = 11;
    private static final int APPCOMMAND_MEDIA_PREVIOUSTRACK = 12;
    private static final int APPCOMMAND_MEDIA_STOP = 13;
    private static final int APPCOMMAND_MEDIA_PLAY_PAUSE = 14;
    private static final int APPCOMMAND_MEDIA_PLAY = 46;
    private static final int APPCOMMAND_MEDIA_PAUSE = 47;
    private static final int FAPPCOMMAND_MASK = 0xF000;
    private static final int VK_MEDIA_NEXT_TRACK = 0xB0;
    private static final int VK_MEDIA_PREV_TRACK = 0xB1;
    private static final int VK_MEDIA_STOP = 0xB2;
    private static final int VK_MEDIA_PLAY_PAUSE = 0xB3;
    private static final long DUPLICATE_COMMAND_WINDOW_MILLIS = 250;
    private final Window window;
    private final MediaKeyHandler mediaKeyHandler;
    private HHOOK hook;
    private HWND windowHandle;
    private WindowProc windowProc;
    private Pointer previousWindowProc;
    private volatile boolean running;
    private int threadId;
    private MediaCommand lastCommand;
    private long lastCommandMillis;

    WindowsMediaKeyListener(Window window, MediaKeyHandler mediaKeyHandler) {
        this.window = Objects.requireNonNull(window);
        this.mediaKeyHandler = Objects.requireNonNull(mediaKeyHandler);
    }

    void start() {
        if (!Platform.isWindows() || running || !window.isDisplayable()) {
            return;
        }

        running = true;
        installAppCommandHandler();
        Thread messageThread = new Thread(this::runMessageLoop, "windows-media-key-listener");
        messageThread.setDaemon(true);
        messageThread.start();
    }

    private void installAppCommandHandler() {
        windowHandle = new HWND(Native.getComponentPointer(window));
        windowProc = this::handleWindowMessage;
        previousWindowProc = User32.INSTANCE.SetWindowLongPtr(
                windowHandle,
                WinUser.GWL_WNDPROC,
                CallbackReference.getFunctionPointer(windowProc));
    }

    private WinDef.LRESULT handleWindowMessage(HWND hwnd, int message, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
        if (message == WM_APPCOMMAND && handleAppCommand(lParam)) {
            return new WinDef.LRESULT(1);
        }

        return User32.INSTANCE.CallWindowProc(previousWindowProc, hwnd, message, wParam, lParam);
    }

    private void runMessageLoop() {
        threadId = Kernel32.INSTANCE.GetCurrentThreadId();
        LowLevelKeyboardProc keyboardProc = this::handleKeyboardEvent;
        hook = User32.INSTANCE.SetWindowsHookEx(
                WinUser.WH_KEYBOARD_LL,
                keyboardProc,
                Kernel32.INSTANCE.GetModuleHandle(null),
                0);
        if (hook == null) {
            running = false;
            return;
        }

        MSG message = new MSG();
        while (running && User32.INSTANCE.GetMessage(message, null, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(message);
            User32.INSTANCE.DispatchMessage(message);
        }
        uninstallHook();
    }

    private WinDef.LRESULT handleKeyboardEvent(int nCode, WinDef.WPARAM wParam, KBDLLHOOKSTRUCT info) {
        if (nCode >= 0 && isKeyDown(wParam)) {
            handleMediaKey(info.vkCode);
        }

        return User32.INSTANCE.CallNextHookEx(
                hook,
                nCode,
                wParam,
                new WinDef.LPARAM(Pointer.nativeValue(info.getPointer())));
    }

    private boolean isKeyDown(WinDef.WPARAM wParam) {
        int message = wParam.intValue();
        return message == WinUser.WM_KEYDOWN || message == WinUser.WM_SYSKEYDOWN;
    }

    private boolean handleAppCommand(WinDef.LPARAM lParam) {
        int command = (lParam.intValue() >> 16) & ~FAPPCOMMAND_MASK;
        return switch (command) {
            case APPCOMMAND_MEDIA_PLAY -> {
                trigger("WM_APPCOMMAND", MediaCommand.PLAY);
                yield true;
            }
            case APPCOMMAND_MEDIA_PAUSE -> {
                trigger("WM_APPCOMMAND", MediaCommand.PAUSE);
                yield true;
            }
            case APPCOMMAND_MEDIA_PLAY_PAUSE -> {
                trigger("WM_APPCOMMAND", MediaCommand.PLAY_PAUSE);
                yield true;
            }
            case APPCOMMAND_MEDIA_STOP -> {
                trigger("WM_APPCOMMAND", MediaCommand.STOP);
                yield true;
            }
            case APPCOMMAND_MEDIA_NEXTTRACK -> {
                trigger("WM_APPCOMMAND", MediaCommand.NEXT);
                yield true;
            }
            case APPCOMMAND_MEDIA_PREVIOUSTRACK -> {
                trigger("WM_APPCOMMAND", MediaCommand.PREVIOUS);
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleMediaKey(int keyCode) {
        return switch (keyCode) {
            case VK_MEDIA_PLAY_PAUSE -> {
                trigger("VK_MEDIA", MediaCommand.PLAY_PAUSE);
                yield true;
            }
            case VK_MEDIA_STOP -> {
                trigger("VK_MEDIA", MediaCommand.STOP);
                yield true;
            }
            case VK_MEDIA_NEXT_TRACK -> {
                trigger("VK_MEDIA", MediaCommand.NEXT);
                yield true;
            }
            case VK_MEDIA_PREV_TRACK -> {
                trigger("VK_MEDIA", MediaCommand.PREVIOUS);
                yield true;
            }
            default -> false;
        };
    }

    private void trigger(String source, MediaCommand command) {
        long now = System.currentTimeMillis();
        System.out.println("[media-key] received source=" + source + " command=" + command);
        if (lastCommand != null && now - lastCommandMillis < DUPLICATE_COMMAND_WINDOW_MILLIS) {
            System.out.println("[media-key] ignored duplicate source=" + source
                    + " command=" + command
                    + " previous=" + lastCommand
                    + " deltaMs=" + (now - lastCommandMillis));
            return;
        }

        lastCommand = command;
        lastCommandMillis = now;
        SwingUtilities.invokeLater(() -> dispatch(command));
    }

    private void dispatch(MediaCommand command) {
        System.out.println("[media-key] dispatch command=" + command);
        switch (command) {
            case PLAY -> mediaKeyHandler.play();
            case PAUSE -> mediaKeyHandler.pause();
            case PLAY_PAUSE -> mediaKeyHandler.playPause();
            case STOP -> mediaKeyHandler.stop();
            case NEXT -> mediaKeyHandler.playNext();
            case PREVIOUS -> mediaKeyHandler.playPrevious();
        }
    }

    @Override
    public void close() {
        running = false;
        uninstallAppCommandHandler();
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WinUser.WM_QUIT, null, null);
        }
    }

    private void uninstallHook() {
        HHOOK installedHook = hook;
        hook = null;
        if (installedHook != null) {
            User32.INSTANCE.UnhookWindowsHookEx(installedHook);
        }
    }

    private void uninstallAppCommandHandler() {
        if (windowHandle == null || previousWindowProc == null) {
            return;
        }

        User32.INSTANCE.SetWindowLongPtr(windowHandle, WinUser.GWL_WNDPROC, previousWindowProc);
        windowHandle = null;
        previousWindowProc = null;
        windowProc = null;
    }

    interface MediaKeyHandler {
        void play();

        void pause();

        void playPause();

        void stop();

        void playNext();

        void playPrevious();
    }

    private enum MediaCommand {
        PLAY,
        PAUSE,
        PLAY_PAUSE,
        STOP,
        NEXT,
        PREVIOUS
    }
}
