/*
 * Copyright 2016 Victor Yacovlev <v.yacovlev@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.victoryacovlev.erlide;

import com.ericsson.otp.erlang.OtpAuthException;
import com.github.victoryacovlev.erlide.ui.MainWindow;
import com.github.victoryacovlev.erlide.ui.WorkspaceChooser;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Application {

    public static void main(String args[]) throws InterruptedException, IOException, OtpAuthException {
        tuneUiSettings();

        WorkspaceChooser dialog = new WorkspaceChooser();
        dialog.restoreSettings();
        dialog.setTitle(MainWindow.APPLICATION_TITLE);
        dialog.pack();

        String workspaceToUse = null;

        if (!dialog.isShownOnLaunch() && !dialog.getLastUsedDirectory().isEmpty()) {
            workspaceToUse = dialog.getLastUsedDirectory();
        }
        else {
            dialog.setVisible(true);
            workspaceToUse = dialog.getSelectedDirectory();
        }
        if (null == workspaceToUse) {
            System.exit(0);
        }

        System.setProperty("user.dir", workspaceToUse);
//        File workspaceSrc = new File(workspaceToUse+"/src");
//        File workspaceEbin = new File(workspaceToUse+"/ebin");
//        workspaceSrc.mkdirs();
//        workspaceEbin.mkdirs();
//
//        ErlangVM.getInstance();
//        AbstractTokenMakerFactory tokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
//        tokenMakerFactory.putMapping("text/erlang", ErlangTokenMaker.class.getName());

        MainWindow mw = new MainWindow();
        mw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mw.loadProjectFromWorkspace(workspaceToUse);
        mw.setVisible(true);
    }

    private static void tuneUiSettings() {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
//        try {
//            final String os = System.getProperty("os.name").toLowerCase();
//            if (os.indexOf("win") >= 0) {
//                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//            }
//            else if (os.indexOf("nux") >= 0 || os.indexOf("bsd") >= 0) {
//                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//            }
//        } catch (Exception e) {
//        }
    }

}
