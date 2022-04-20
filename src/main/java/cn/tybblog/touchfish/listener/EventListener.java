package cn.tybblog.touchfish.listener;

import cn.tybblog.touchfish.PersistentState;
import cn.tybblog.touchfish.entity.Book;
import cn.tybblog.touchfish.entity.Chapter;
import cn.tybblog.touchfish.exception.FishException;
import cn.tybblog.touchfish.util.ChapterCallback;
import cn.tybblog.touchfish.util.ConsoleUtils;
import cn.tybblog.touchfish.util.KeyMapFormatUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.status.IdeStatusBarImpl;
import java.util.Collections;
import sun.awt.AWTAccessor;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EventListener implements KeyEventPostProcessor, AWTEventListener, ChapterCallback {
    private PersistentState persistentState = PersistentState.getInstance();
    /**
     * 控制台
     */
    private StatusBar statusBar;
    /**
     * 当前书籍
     */
    public static Book book;
    /**
     * 老板键
     */
    private boolean flag = false;
    /**
     * 加载中
     */
    public static boolean loading = false;
    /**
     * 定时执行
     */
    private Timer timer;

    public static String LOADING_TEXT = "加载中...";

    public EventListener(Project project) {
        statusBar = WindowManager.getInstance().getStatusBar(project);
    }

    @Override
    public boolean postProcessKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) {
            return false;
        }
        String key = KeyMapFormatUtils.keyMapFormat(e);
        if ("".equals(key)) {
            return false;
        }

        Component source = AWTAccessor.getKeyEventAccessor().getOriginalSource(e);
        if (source instanceof IdeFrameImpl) {
            statusBar = ((IdeFrameImpl) source).getStatusBar();
            ConsoleUtils.setStatusBar(statusBar);
        }
        try {
            doRead(key);
        } catch (FishException fishException) {
            ConsoleUtils.info(fishException.getMessage());
            if (!LOADING_TEXT.equals(fishException.getMessage())) {
                EventListener.loading = false;
            }
        }
        return false;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent e = (MouseEvent) event;
            if (e.getButton() < 4) {
                return;
            }
            Object source = event.getSource();
            if (source instanceof IdeFrameImpl) {
                statusBar = ((IdeFrameImpl) source).getStatusBar();
                ConsoleUtils.setStatusBar(statusBar);
            }
            try {
                doRead("鼠标侧键" + (e.getButton() - 3));
            } catch (FishException fishException) {
                ConsoleUtils.info(fishException.getMessage());
                EventListener.loading = false;
            }
        }
    }

    public void doRead(String key) throws FishException {
        String[] stateKey = persistentState.getKey();
        if (stateKey == null) {
            return;
        }
        if (loading) {
            return;
        }
        if (stateKey[5].equals(key)) {
            flag = !flag;
            if (flag) {
                ConsoleUtils.info("");
            }
            return;
        }
        if (flag) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (!stateKey[i].equals(key)) {
                continue;
            }
            if (book == null) {
                initBook();
                return;
            }
            switch (i) {
                case 0:
                    String text = book.getChapterByIndex().pre();
                    if(null == text){
                        text = preChapter();
                    }
                    showRow(text);
                    break;
                case 1:
                    text = book.getChapterByIndex().next();
                    if(null == text){
                        text =  nextChapter();
                    }
                    showRow(text);
                    break;
                case 2:
                    preChapter();
                    break;
                case 3:
                    nextChapter();
                    break;
                default:
            }
            break;
        }
    }

    /**
     * 初始化书本
     *
     * @throws FishException
     */
    public void initBook() throws FishException {
        book = persistentState.getBookByIndex();
        Chapter chapter = persistentState.getBookByIndex().getChapterByIndex();
        List<String> chapterContent = book.loadChapter(this);
        chapter.setRowContents(chapterContent);
    }

    /**
     * 上一章
     */
    private String preChapter() {
        try {
            String text = book.preIndex();
            initBook();
            return text;
        } catch (FishException e) {
            ConsoleUtils.info(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下一章
     */
    private String nextChapter() {
        try {
            String text = book.nextIndex();
            initBook();
            return text;
        } catch (FishException e) {
            ConsoleUtils.info(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 显示行
     */
    private void showRow(String txt) {
        ConsoleUtils.info(txt);
    }

    /**
     * 获取控制台可显示长度
     *
     * @return 控制台可显示长度
     */
    private int getConsoleLen() {
        int textLen = 0;
        if (statusBar instanceof IdeStatusBarImpl) {
            IdeStatusBarImpl bar = (IdeStatusBarImpl) statusBar;
            int width = bar.getWidth();
            textLen = width / 17;
        }
        if (textLen == 0) {
            MessageDialogBuilder.yesNo("提示", "自动获取控制台长度时出错").show();
            textLen = 10;
        }
        return textLen;
    }

    /**
     * 异步回调
     *
     * @param bookText   书本内容
     */
    @Override
    public void chapter(List<String> bookText) throws FishException {
        loading = false;
        Chapter chapter = persistentState.getBookByIndex().getChapterByIndex();
        chapter.setRowContents(bookText);
    }

}