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

package com.github.victoryacovlev.erlide.ui;

import com.eleet.dragonconsole.DragonConsole;
import com.github.victoryacovlev.erlide.ErlangVM;
import com.github.victoryacovlev.erlide.erlangtools.CompileResult;
import com.github.victoryacovlev.erlide.erlangtools.ErlErrorInfo;
import com.github.victoryacovlev.erlide.project.ErlangProject;
import com.github.victoryacovlev.erlide.erlangtools.ProjectBuilder;
import com.github.victoryacovlev.erlide.ui.buildlog.BuildLog;
import com.github.victoryacovlev.erlide.ui.buildlog.BuildLogView;
import com.github.victoryacovlev.erlide.ui.editor.ErlangDocument;
import com.github.victoryacovlev.erlide.ui.editor.ErlangSyntaxTextArea;
//import com.github.victoryacovlev.erlide.ui.editor.ModuleNameChangeEvent;
import com.github.victoryacovlev.erlide.ui.editor.ModuleNameChangeListener;
import com.github.victoryacovlev.erlide.ui.shell.Interpreter;
import com.github.victoryacovlev.erlide.ui.sidebar.ProjectTreeView;
import com.github.victoryacovlev.erlide.ui.sidebar.SidebarButton;
import org.fife.ui.rtextarea.RTextScrollPane;


import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.prefs.Preferences;

class Actions {
    Action fileNewModule;
    Action fileNewFromTemplate;
    Action fileOpen;
    Action fileSaveCompileAndReload;
    Action fileSaveAndClose;
    Action fileSwitchWorkspace;
    Action fileExit;
}

class TabHeader extends JPanel implements ActionListener, ModuleNameChangeListener {
    JLabel title;
    JButton button;
    final JTabbedPane tabbedPane;
    static ImageIcon closeIcon = null;
    TabHeader(String title, JTabbedPane tabbedPane, boolean closeable) {
        super();
        setPreferredSize(new Dimension(150, 24));
        this.title = new JLabel(title);
        this.tabbedPane = tabbedPane;
        if (closeable) {
            if (null==closeIcon) {
                final String iconFileName = "/icons/close-12px.png";
                URL iconUrl = getClass().getResource(iconFileName);
                closeIcon = new ImageIcon(iconUrl);
            }
            button = new JButton(closeIcon);
            button.addActionListener(this);
            button.setPreferredSize(new Dimension(18,18));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(true);
        }
        setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        add(this.title, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        if (closeable) {
            add(button, gbc);
        }
    }

    void setTitle(String title) {
        this.title.setText(title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int tabIndex = tabbedPane.indexOfTabComponent(this);
        tabbedPane.remove(tabIndex);
    }

//    @Override
//    public void moduleNameChanged(ModuleNameChangeEvent e) {
//        setTitle(e.getModuleName() + ".erl");
//    }
}

public class MainWindow extends JFrame implements WindowListener {

    public static final String APPLICATION_TITLE = "Erlang";
    private static final String PREFS_WINDOW_GEOMETRY = "MainWindow/Geometry";
    private static final String PREFS_WINDOW_CENTRAL_DIVIDER_LOCATION = "MainWindow/CentralDividerLocation";
    private static final String PREFS_WINDOW_TOP_DIVIDER_LOCATION = "MainWindow/TopDividerLocation";
    private static final String PREFS_WINDOW_BUILD_LOG_SHOWN = "MainWindow/BuildLogShown";
    private static final String PREFS_WINDOW_PROJECT_VIEW_SHOWN = "MainWindow/ProjectViewShown";
    private final Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);
    private final JMenuBar menuBar = new JMenuBar();
    private final StatusBar statusBar = new StatusBar();
    private final JMenu menuFile = new JMenu("File");
    private final JMenu menuEdit = new JMenu("Edit");
    private final JMenu menuView = new JMenu("View");
    private final JMenu menuRun = new JMenu("Run");
    private final JMenu menuHelp = new JMenu("Help");
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final BuildLogView buildLogView = new BuildLogView();
    private final JSplitPane centralSplitPane;
    private final JSplitPane topSplitPane;
    private final DragonConsole console = new DragonConsole(true, false);
    private final ProjectTreeView projectTreeView = new ProjectTreeView();

    Actions actions = new Actions();
    private int untitledIndex = 0;
    private AbstractButton showBuildLogButton = new SidebarButton("Build Log", false);
    private AbstractButton showProjectButton = new SidebarButton("Project", true);

    ErlangProject windowProject = null;

    public MainWindow() {
        addWindowListener(this);
        setMinimumSize(new Dimension(700, 500));


        getContentPane().setLayout(new BorderLayout());;
        getContentPane().add(menuBar, BorderLayout.NORTH);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        tabbedPane.setMinimumSize(new Dimension(400, 200));
        buildLogView.setMinimumSize(new Dimension(600, 100));
        projectTreeView.setMinimumSize(new Dimension(100, 200));
        topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectTreeView, tabbedPane);
        topSplitPane.setResizeWeight(0.5);
        topSplitPane.setOneTouchExpandable(false);

        centralSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, buildLogView);
        getContentPane().add(centralSplitPane, BorderLayout.CENTER);
        centralSplitPane.setResizeWeight(0.5);
        centralSplitPane.setOneTouchExpandable(false);
        BuildLog.addView(buildLogView);

        statusBar.setRightComponent(showBuildLogButton);
        showBuildLogButton.addItemListener(e -> {
            boolean pressed = e.getStateChange()==ItemEvent.SELECTED;
            setBuildLogVisible(pressed);
            if (pressed) {
                buildLogView.requestFocus();
            }
            else {
                tabbedPane.requestFocus();
            }
        });

        showProjectButton.addItemListener(e -> {
            boolean pressed = e.getStateChange()==ItemEvent.SELECTED;
            setProjectViewVisible(pressed);
            if (pressed) {
                showProjectButton.requestFocus();
            }
            else {
                tabbedPane.requestFocus();
            }
        });

        JPanel leftButtonsPane = new JPanel();
        leftButtonsPane.setLayout(new BoxLayout(leftButtonsPane, BoxLayout.Y_AXIS));
        getContentPane().add(leftButtonsPane, BorderLayout.WEST);
        leftButtonsPane.add(showProjectButton);

        createActions();
        createMenuBar();
        createErlangShellTab();
        setResizable(true);
        restoreSettings();
        pack();
        updateWindowTitle();

        console.requestFocus();
    }

    private void createErlangShellTab() {
        console.setMacStyle();
        tabbedPane.add(console);
        tabbedPane.setTabComponentAt(0, new TabHeader("Erlang Shell", tabbedPane, false));
        try {
            ErlangVM vm = ErlangVM.getInstance();
            Interpreter interpreter = vm.getInterpreter();
            console.setCommandProcessor(interpreter);
        } catch (Exception e) {
            console.appendErrorMessage(e.getMessage());
            e.printStackTrace();
        }
    }

    private void createActions() {
        actions.fileNewModule = new AbstractAction("New module") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileNew();
            }
        };
        actions.fileNewFromTemplate = new AbstractAction("New from template...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileNewFromTemplate();
            }
        };
        actions.fileOpen = new AbstractAction("Open...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileOpen();
            }
        };
        actions.fileSaveCompileAndReload = new AbstractAction("Save, compile and reload") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileSave();
            }
        };
        actions.fileSaveAndClose = new AbstractAction("Save and close") {
            @Override
            public void actionPerformed(ActionEvent e) { fileSaveAndClose(); }
        };
        actions.fileSwitchWorkspace = new AbstractAction("Switch workspace or project..") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileSwitchWorkspace();
            }
        };
        actions.fileExit = new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileExit();
            }
        };
    }

    public void fileOpen() {

    }

    private void fileExit() {
        for (int i=1; i<tabbedPane.getTabCount(); ++i) {
            saveFileInTab(i);
        }
        saveSettings();
        System.exit(0);
    }

    public void fileSwitchWorkspace() {

    }

    public void fileSaveCompileAndReload() {
        Map<String, ErlangDocument> openedDocuments = new HashMap<>();
        for (int i=1; i<tabbedPane.getTabCount(); ++i) {
            ErlangSyntaxTextArea textArea = getErlangTextAread(-1);
            ErlangDocument document = (ErlangDocument)textArea.getDocument();
            if (document.isModifiedFromLastSave()) {
                document.save();
            }
            openedDocuments.put(document.getFileName(), document);
        }

        if (windowProject!=null) {
            CompileResult result = ProjectBuilder.instance().buildProject(windowProject);

            BuildLog.clearLog();
            result.getErrorsAndWarnings().forEach((I)->BuildLog.addItem(I));
            setBuildLogVisible(!BuildLog.isEmpty());

            for (ErlErrorInfo erlErrorInfo : result.getErrorsAndWarnings()) {
                final String fileName = erlErrorInfo.moduleFileName;
                if (openedDocuments.containsKey(fileName)) {
                    ErlangDocument document = openedDocuments.get(fileName);
                    document.getErrorsAndWarnings().add(erlErrorInfo);
                }
            }
        }

        for (int i=1; i<tabbedPane.getTabCount(); ++i) {
            ErlangSyntaxTextArea textArea = getErlangTextAread(-1);
            textArea.forceReparsing();
        }

    }

    private ErlangSyntaxTextArea getErlangTextAread(int index) {
        int realIndex = -1 == index ? tabbedPane.getSelectedIndex() : index;
        if (0 == index) {
            return null;
        }
        Component tabComponent = tabbedPane.getComponentAt(realIndex);
        RTextScrollPane scrollPane = (RTextScrollPane)tabComponent;
        ErlangSyntaxTextArea erlangSyntaxTextArea = (ErlangSyntaxTextArea)scrollPane.getTextArea();
        return erlangSyntaxTextArea;
    }

    public void fileSaveAndClose() {
        int index = tabbedPane.getSelectedIndex();
        if (index > 0) {
            saveFileInTab(index);
            tabbedPane.remove(index);
        }
    }

    private void saveFileInTab(int index) {

    }

    public void fileNewFromTemplate() {

    }

    public void fileNew() {
        final String moduleName = generateUntitledModuleName();
        final String moduleTemplateFileName = "/templates/simple.erl";
        InputStream moduleTemplateStream = getClass().getResourceAsStream(moduleTemplateFileName);
        Scanner s = new Scanner(moduleTemplateStream);
        s.useDelimiter("\\A");
        String moduleTemplate = s.next();
        moduleTemplate = moduleTemplate.replace("?MODULE_NAME", moduleName);
        createNewFile(moduleName, moduleName + ".erl", moduleTemplate);
    }

    public void fileSave() {
        if (0 == tabbedPane.getSelectedIndex()) {
            fileSaveTerminalLog();
        }
        else {
            fileSaveCompileAndReload();
        }
    }

    private void fileSaveTerminalLog() {
    }

    private String generateUntitledModuleName() {
        File srcRoot = new File(System.getProperty("user.dir")+"/src");
        File unnamed = new File(srcRoot.getAbsoluteFile() + "/unnamed.erl");
        if (!unnamed.exists()) {
            return "unnamed";
        }
        if (0 == untitledIndex)
            untitledIndex = 1;
        while (true) {
            String path = srcRoot.getAbsolutePath() + String.format("unnamed_%1$d.erl", untitledIndex);
            File f = new File(path);
            if (!f.exists()) {
                final String result = String.format("unnamed_%1$d", untitledIndex);
                untitledIndex ++;
                return result;
            }
        }
    }

    public void createNewFile(final String moduleName, final String fileName, final String templateContents) {
        String fullPath = System.getProperty("user.dir")+"/src/"+fileName;
        try {
            File file = new File(fullPath);
            FileOutputStream fs = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(templateContents);
            bufferedWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ErlangSyntaxTextArea textArea = new ErlangSyntaxTextArea(moduleName, fullPath, templateContents);
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        tabbedPane.add(scrollPane);
        TabHeader tabHeader = new TabHeader(fileName, tabbedPane, true);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1, tabHeader);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
        ErlangDocument document = (ErlangDocument)textArea.getDocument();
        document.addModuleNameChangeListener(tabHeader);
        textArea.requestFocus();
    }

    private void createMenuBar() {
        menuBar.add(menuFile);
        menuBar.add(menuEdit);
        menuBar.add(menuView);
        menuBar.add(menuRun);
        menuBar.add(menuHelp);

        menuFile.add(actions.fileNewModule).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        menuFile.add(actions.fileNewFromTemplate).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK));
        menuFile.addSeparator();
        menuFile.add(actions.fileOpen).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        menuFile.add(actions.fileSaveCompileAndReload).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        menuFile.add(actions.fileSaveAndClose).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
        menuFile.addSeparator();
        menuFile.add(actions.fileSwitchWorkspace);
        menuFile.addSeparator();
        menuFile.add(actions.fileExit).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));

    }

    private void restoreSettings() {
        int x = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/x", 0);
        int y = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/y", 0);
        int w = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/w", 500);
        int h = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/h", 400);
        setPreferredSize(new Dimension(w, h));
        setLocation(x, y);
        boolean buildLogShown = preferences.getBoolean(PREFS_WINDOW_BUILD_LOG_SHOWN, false);
        setBuildLogVisible(buildLogShown);
        boolean projectViewShown = preferences.getBoolean(PREFS_WINDOW_PROJECT_VIEW_SHOWN, false);
        setProjectViewVisible(projectViewShown);
    }

    private void saveSettings() {
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/x", getX());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/y", getY());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/w", getWidth());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/h", getHeight());
        if (isBuildLogVisible())
            preferences.putInt(PREFS_WINDOW_CENTRAL_DIVIDER_LOCATION, centralSplitPane.getDividerLocation());
        preferences.putBoolean(PREFS_WINDOW_BUILD_LOG_SHOWN, isBuildLogVisible());
        if (isProjectViewVisible())
            preferences.putInt(PREFS_WINDOW_TOP_DIVIDER_LOCATION, topSplitPane.getDividerLocation());
        preferences.putBoolean(PREFS_WINDOW_PROJECT_VIEW_SHOWN, isProjectViewVisible());
    }

    private void updateWindowTitle() {
        setTitle(APPLICATION_TITLE);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        fileExit();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {
        // Run garbage collector when UI gone background
        System.gc();
    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public void setBuildLogVisible(boolean buildLogVisible) {
        if (!buildLogVisible) {
            preferences.putInt(PREFS_WINDOW_CENTRAL_DIVIDER_LOCATION, centralSplitPane.getDividerLocation());
        }
        buildLogView.setVisible(buildLogVisible);
        BasicSplitPaneUI basicSplitPaneUI = (BasicSplitPaneUI) centralSplitPane.getUI();
        basicSplitPaneUI.getDivider().setVisible(buildLogVisible);
        showBuildLogButton.setSelected(buildLogVisible);
        if (buildLogVisible) {
            int dividerLocation = preferences.getInt(PREFS_WINDOW_CENTRAL_DIVIDER_LOCATION, 300);
            MainWindow self = this;
            SwingUtilities.invokeLater(() -> {
                while (true) {
                    if (self.isVisible()) {
                        self.pack();
                        centralSplitPane.setDividerLocation(dividerLocation);
                        break;
                    } else {
                        try {
                            Thread.currentThread().sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
        }
    }

    public void setProjectViewVisible(boolean visible) {
        if (!visible) {
            preferences.putInt(PREFS_WINDOW_TOP_DIVIDER_LOCATION, topSplitPane.getDividerLocation());
        }
        projectTreeView.setVisible(visible);
        BasicSplitPaneUI basicSplitPaneUI = (BasicSplitPaneUI) topSplitPane.getUI();
        basicSplitPaneUI.getDivider().setVisible(visible);
        showProjectButton.setSelected(visible);
        if (visible) {
            int dividerLocation = preferences.getInt(PREFS_WINDOW_TOP_DIVIDER_LOCATION, 150);
            MainWindow self = this;
            SwingUtilities.invokeLater(() -> {
                while (true) {
                    if (self.isVisible()) {
                        self.pack();
                        topSplitPane.setDividerLocation(dividerLocation);
                        break;
                    } else {
                        try {
                            Thread.currentThread().sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
        }
    }

    public boolean isBuildLogVisible() {
        return buildLogView.isVisible();
    }
    public boolean isProjectViewVisible() {
        return projectTreeView.isVisible();
    }

    public void loadProjectFromWorkspace(String workspaceToUse) {
        windowProject = new ErlangProject(workspaceToUse);
        CompileResult compileResult = ProjectBuilder.instance().buildProject(windowProject);
        BuildLog.clearLog();
        compileResult.getErrorsAndWarnings().forEach((I)->BuildLog.addItem(I));
        setBuildLogVisible(!BuildLog.isEmpty());
    }
}
