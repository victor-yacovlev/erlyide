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

package io.github.victoryacovlev.erlyide.fxui.mainwindow;

import com.eleet.dragonconsole.DragonConsole;
import io.github.victoryacovlev.erlyide.erlangtools.ErlangVM;
import io.github.victoryacovlev.erlyide.erlangtools.CompileResult;
import io.github.victoryacovlev.erlyide.erlangtools.ErlErrorInfo;
import io.github.victoryacovlev.erlyide.erlangtools.ProjectBuildFinishedEvent;
import io.github.victoryacovlev.erlyide.erlangtools.ProjectBuilder;
import io.github.victoryacovlev.erlyide.fxui.projectview.ProjectFileItem;
import io.github.victoryacovlev.erlyide.fxui.projectview.ProjectTreeItem;
import io.github.victoryacovlev.erlyide.project.ErlangProject;
import io.github.victoryacovlev.erlyide.fxui.editor.ErlangCodeArea;
import io.github.victoryacovlev.erlyide.fxui.logging.EventLogEntry;
import io.github.victoryacovlev.erlyide.fxui.logging.IssuesLogEntry;
import io.github.victoryacovlev.erlyide.fxui.logging.Logger;
import io.github.victoryacovlev.erlyide.project.ErlangSourceFile;
import io.github.victoryacovlev.erlyide.project.ProjectFile;
import io.github.victoryacovlev.erlyide.ui.shell.Interpreter;
import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.joda.time.DateTime;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MainWindowController implements Initializable {

    public static final String APPLICATION_TITLE = "Erlang";
    private static final String PREFS_WINDOW_GEOMETRY = "MainWindow/Geometry";
    private static final String PREFS_WINDOW_BOTTOM_DIVIDER_POSITION = "MainWindow/CentralDividerPosition";
    private static final String PREFS_WINDOW_LEFT_DIVIDER_POSITION = "MainWindow/TopDividerPosition";
    private static final String PREFS_WINDOW_ISSUES_VISIBLE = "MainWindow/IssuesVisible";
    private static final String PREFS_WINDOW_EVENTS_VISIBLE = "MainWindow/EventsVisible";
    private static final String PREFS_WINDOW_PROJECT_VISIBLE = "MainWindow/ProjectsVisible";
    private static final String PREFS_WINDOW_MAXIMIZED = "MainWindow/Maximized";
    private final Preferences preferences = Preferences.userNodeForPackage(MainWindowController.class);

    private DragonConsole console;
    private SwingNode terminalProxy;

    @FXML private Label clock;
    @FXML private Label labelMemoryUsage;
    @FXML private SplitPane bottomSplitPane;
    @FXML private SplitPane leftSplitPane;
    @FXML private Tab shellTab;
    @FXML private TabPane tabPane;
    @FXML private TableView issuesView;
    @FXML private TableView eventsView;
    @FXML private StackPane bottomPane;
    @FXML private Label showEventButton;
    @FXML private Label showIssuesButton;
    @FXML private Label showProjectButton;
    @FXML private TreeView projectView;

    private Stage stage;
    private int untitledIndex = 0;
    private ErlangProject erlangProject = null;
    private Node rootNode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createTerminal();
        createMemoryUsageMonitor();
        createClockUpdater();
        issuesView.setPlaceholder(new Label("No issues"));
        eventsView.setPlaceholder(new Label("No events"));
        clock.setVisible(false);
        bottomSplitPane.getDividers().get(0).positionProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.99) {
                setBottomPaneVisible(false, false);
            }
        }));
        tabPane.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue) {
//                System.out.println("Focus to tab pane");
                Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                currentTab.getContent().requestFocus();
            }
        }));
        showIssuesButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean issuesVisible = isIssuesVisible();
            setIssuesVisible(!issuesVisible);
        });
        showEventButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean eventsVisible = isEventsVisible();
            setEventsVisible(!eventsVisible);
        });
        showProjectButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean projectVisible = isProjectVisible();
            setProjectVisible(!projectVisible);
        });
        Logger.getInstance().addIssuesTableView(issuesView);
        Logger.getInstance().addEventsTableView(eventsView);
        Logger.getInstance().addLastEventView(showEventButton);

        projectView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2) {
                Object obj = projectView.getSelectionModel().getSelectedItem();
                if (obj == null) return;
                if (obj instanceof ProjectFileItem) {
                    ProjectFileItem pfi = (ProjectFileItem) obj;
                    ProjectFile projectFile = pfi.getProjectFile();
                    openProjectFile(projectFile);
                }

            }
        });
    }

    private void createTerminal() {
        terminalProxy = new SwingNode();
        shellTab.setContent(terminalProxy);
        SwingUtilities.invokeLater(() -> {
            console = new DragonConsole(true, false);
            terminalProxy.setContent(console);
            try {
                ErlangVM vm = ErlangVM.getInstance();
                Interpreter interpreter = vm.getInterpreter();
                console.setCommandProcessor(interpreter);
            } catch (Exception e) {
                console.appendErrorMessage(e.getMessage());
                e.printStackTrace();
            }
            console.setMacStyle();
        });
        terminalProxy.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
//                System.out.println("Focus to proxy");
                SwingUtilities.invokeLater(() -> {
//                    System.out.println("Focus to tab console");
                    console.requestFocus();
                });
            }
        });

    }

    public void setInitialFocus() {
        Platform.runLater(() -> {
            terminalProxy.requestFocus();
        });
    }

    private void createClockUpdater() {
        new Thread(() -> {
            while (true) {
                DateTime dateTime = DateTime.now();
                int h = dateTime.getHourOfDay();
                int m = dateTime.getMinuteOfHour();
                String format = String.format("%1$d:%2$d", h, m);
                Platform.runLater(() -> {
                    if (null!=clock) {
                        clock.setText(format);
                    }
                });
                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public boolean isPresentationMode() {
        return clock.isVisible();
    }

    @FXML
    private void togglePresentationMode() {
        setPresentationMode(!isPresentationMode());
    }

    private void createMemoryUsageMonitor() {
        final Runtime runtime = Runtime.getRuntime();
        new Thread(() -> {
            while (true) {
                long total = runtime.totalMemory();
                long free = runtime.freeMemory();
                long used = total - free;
                String message = String.format("Runtime uses %1$d of %2$d Mb", used/(1024*1024), total/(1024*1024));
                Platform.runLater(() -> {
                    if (labelMemoryUsage!=null)
                        labelMemoryUsage.setText(message);
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @FXML
    public void fileNew() {
        final String moduleName = generateUntitledModuleName();
        final String moduleTemplateFileName = "/templates/simple.erl";
        InputStream moduleTemplateStream = getClass().getResourceAsStream(moduleTemplateFileName);
        Scanner s = new Scanner(moduleTemplateStream);
        s.useDelimiter("\\A");
        String moduleTemplate = s.next();
        moduleTemplate = moduleTemplate.replace("?MODULE_NAME", moduleName);
        createNewSourceFile(moduleName, moduleName + ".erl", moduleTemplate);
    }

    private String generateUntitledModuleName() {
        File srcRoot = erlangProject.getSrcDir();
        File unnamed = new File(srcRoot.getAbsoluteFile() + "/unnamed.erl");
        if (!unnamed.exists()) {
            return "unnamed";
        }
        if (0 == untitledIndex)
            untitledIndex = 1;
        while (true) {
            String path = srcRoot.getAbsolutePath() + String.format("/unnamed_%1$d.erl", untitledIndex);
            File f = new File(path);
            if (!f.exists()) {
                final String result = String.format("unnamed_%1$d", untitledIndex);
                untitledIndex ++;
                return result;
            }
            else {
                untitledIndex ++;
            }
        }
    }

    public void createNewSourceFile(final String moduleName, final String fileName, final String templateContents) {
        ErlangSourceFile projectFile = erlangProject.createSourceFile(fileName, templateContents);
        openProjectFile(projectFile);
    }

    public void openProjectFile(ProjectFile projectFile) {
        EditorTab existingTab = null;
        for (int i=1; i<tabPane.getTabs().size(); ++i) {
            Tab tab = tabPane.getTabs().get(i);
            EditorTab editorTab = (EditorTab) tab;
            if (editorTab!=null) {
                ProjectFile editorFile = editorTab.getEditor().getProjectFile();
                if (editorFile == projectFile) {
                    existingTab = editorTab;
                    break;
                }
            }
        }
        if (existingTab != null) {
            tabPane.getSelectionModel().select(existingTab);
            existingTab.getEditor().requestFocus();
        }
        else {
            EditorTab editorTab = new EditorTab(projectFile);
            tabPane.getTabs().add(editorTab);
            tabPane.getSelectionModel().select(editorTab);
            editorTab.getEditor().requestFocus();
        }
    }

    public void saveAll() {
        for (int i=1; i<tabPane.getTabs().size(); ++i) {
            Tab tab = tabPane.getTabs().get(i);
            EditorTab editorTab = (EditorTab) tab;
            if (editorTab!=null) {
                editorTab.getEditor().saveFileIfChanged();
            }
        }
        System.gc();
    }

    @FXML
    public void saveAllCompileAndReload() {
        saveAll();
        Logger.getInstance().addEventEntry(new EventLogEntry("Building project..."));
        ProjectBuilder.instance().buildProjectAsync(erlangProject, rootNode);
    }

    private void handleProjectBuildFinished(@NotNull ProjectBuildFinishedEvent event) {
        if (event.getProject() == erlangProject) {
            CompileResult compileResult = event.getResult();
            Logger.getInstance().clearIssues();
            for (ErlErrorInfo errorInfo : compileResult.getErrorsAndWarnings()) {
                Logger.getInstance().addIssueEntry(new IssuesLogEntry(errorInfo));
            }
            for (int i=1; i<tabPane.getTabs().size(); ++i) {
                Tab tab = tabPane.getTabs().get(i);
                EditorTab editorTab = (EditorTab) tab;
                ErlangCodeArea editor = editorTab.getEditor();
                editor.fireEvent(event.copyFor(this, editor));
            }
        }
        Logger.getInstance().addEventEntry(new EventLogEntry("Build finished", 5000));
        if (Logger.getInstance().hasIssues()) {
            setIssuesVisible(true);
        }
        event.consume();
    }

    public void setPresentationMode(boolean presentationMode) {
        clock.setVisible(presentationMode);
        stage.setFullScreen(presentationMode);

    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle(APPLICATION_TITLE);
    }

    public void loadSettings() {
        int x = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/x", 0);
        int y = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/y", 0);
        int w = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/w", 800);
        int h = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/h", 600);
        boolean maximized = preferences.getBoolean(PREFS_WINDOW_MAXIMIZED, false);
        stage.setWidth(w);
        stage.setHeight(h);
        stage.setX(x);
        stage.setY(y);
        stage.setMaximized(maximized);
        Platform.runLater(() -> {
            boolean issuesVisible = preferences.getBoolean(PREFS_WINDOW_ISSUES_VISIBLE, false);
            boolean eventsVisible = preferences.getBoolean(PREFS_WINDOW_EVENTS_VISIBLE, false);
            boolean projectVisible = preferences.getBoolean(PREFS_WINDOW_PROJECT_VISIBLE, false);
            setBottomPaneVisible(issuesVisible, eventsVisible);
            setLeftPaneVisible(projectVisible);
        });
    }

    public void saveSettings() {
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/x", (int) stage.getX());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/y", (int) stage.getY());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/w", (int) stage.getWidth());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/h", (int) stage.getHeight());
        preferences.putBoolean(PREFS_WINDOW_ISSUES_VISIBLE, isIssuesVisible());
        preferences.putBoolean(PREFS_WINDOW_EVENTS_VISIBLE, isEventsVisible());
        preferences.putBoolean(PREFS_WINDOW_PROJECT_VISIBLE, isProjectVisible());
        preferences.putBoolean(PREFS_WINDOW_MAXIMIZED, stage.isMaximized());
        if (isProjectVisible()) {
            preferences.putDouble(PREFS_WINDOW_LEFT_DIVIDER_POSITION, leftSplitPane.getDividerPositions()[0]);
        }
        if (isEventsVisible() || isIssuesVisible()) {
            preferences.putDouble(PREFS_WINDOW_BOTTOM_DIVIDER_POSITION, bottomSplitPane.getDividerPositions()[0]);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public void setLeftPaneVisible(boolean projectVisible) {
        if (!projectVisible) {
            double dividerPosition = leftSplitPane.getDividerPositions()[0];
            preferences.putDouble(PREFS_WINDOW_LEFT_DIVIDER_POSITION, dividerPosition);
            leftSplitPane.setDividerPositions(0.0);
            leftSplitPane.lookupAll(".split-pane-divider").stream().forEach((Node div) -> {
                Node parent = div.parentProperty().get();
                if (parent == leftSplitPane) {
                    div.setMouseTransparent(true);
                    div.setVisible(false);
                    div.getStyleClass().add("hidden-split-pane-divider");
                }
            });
        }
        if (projectVisible) {
            double dividerPosition = preferences.getDouble(PREFS_WINDOW_LEFT_DIVIDER_POSITION, 0.2);
            if (dividerPosition < 0.2) {
                dividerPosition = 0.2;
            }
            leftSplitPane.setDividerPositions(dividerPosition);
            leftSplitPane.lookupAll(".split-pane-divider").stream().forEach((Node div) -> {
                Node parent = div.parentProperty().get();
                if (parent == leftSplitPane) {
                    div.setMouseTransparent(false);
                    div.setVisible(true);
                    div.getStyleClass().removeAll("hidden-split-pane-divider");
                }
            });
        }
        if (projectVisible) {
            showProjectButton.getStyleClass().add("panel-button-pressed");
        }
        else {
            showProjectButton.getStyleClass().removeAll("panel-button-pressed");
        }
    }

    public void setBottomPaneVisible(boolean issuesVisible, boolean eventsVisible) {
        if (!issuesVisible && !eventsVisible) {
            double dividerPosition = bottomSplitPane.getDividerPositions()[0];
            preferences.putDouble(PREFS_WINDOW_BOTTOM_DIVIDER_POSITION, dividerPosition);
            bottomSplitPane.setDividerPosition(0, 1.0);
            bottomSplitPane.lookupAll(".split-pane-divider").stream().forEach((Node div) -> {
                Node parent = div.parentProperty().get();
                if (parent == bottomSplitPane) {
                    div.setMouseTransparent(true);
                    div.setVisible(false);
                    div.getStyleClass().add("hidden-split-pane-divider");
                }
            });
        }
        if (issuesVisible || eventsVisible) {
            double dividerPosition = preferences.getDouble(PREFS_WINDOW_BOTTOM_DIVIDER_POSITION, 0.7);
            if (dividerPosition > 0.7) {
                dividerPosition = 0.7;
            }
            bottomSplitPane.setDividerPositions(dividerPosition);
            bottomSplitPane.lookupAll(".split-pane-divider").stream().forEach((Node div) -> {
                Node parent = div.parentProperty().get();
                if (parent == bottomSplitPane) {
                    div.setMouseTransparent(false);
                    div.setVisible(true);
                    div.getStyleClass().removeAll("hidden-split-pane-divider");
                }
            });
        }
        if (issuesVisible) {
            issuesView.setVisible(true);
            eventsView.setVisible(false);
            showIssuesButton.getStyleClass().add("panel-button-pressed");
            showEventButton.getStyleClass().removeAll("panel-button-pressed");
        }
        else if (eventsVisible) {
            eventsView.setVisible(true);
            issuesView.setVisible(false);
            showEventButton.getStyleClass().add("panel-button-pressed");
            showIssuesButton.getStyleClass().removeAll("panel-button-pressed");
        }
        else {
            eventsView.setVisible(false);
            issuesView.setVisible(false);
            showEventButton.getStyleClass().removeAll("panel-button-pressed");
            showIssuesButton.getStyleClass().removeAll("panel-button-pressed");
        }
    }

    public boolean isProjectVisible() {
        return showProjectButton.getStyleClass().contains("panel-button-pressed");
    }

    public void setProjectVisible(boolean visible) {
        setLeftPaneVisible(visible);
    }

    public boolean isIssuesVisible() {
        return showIssuesButton.getStyleClass().contains("panel-button-pressed");
    }

    public boolean isEventsVisible() {
        return showEventButton.getStyleClass().contains("panel-button-pressed");
    }

    public void setIssuesVisible(boolean visible) {
        setBottomPaneVisible(visible, false);
    }

    public void setEventsVisible(boolean visible) {
        setBottomPaneVisible(false, visible);
    }

    public ErlangProject getErlangProject() {
        return erlangProject;
    }

    public void setErlangProject(ErlangProject erlangProject) {
        this.erlangProject = erlangProject;
        projectView.setRoot(new ProjectTreeItem(erlangProject));
        projectView.getRoot().setExpanded(true);
        projectView.setShowRoot(true);
    }


    public void setRoot(Node root) {
        this.rootNode = root;
        root.addEventHandler(ProjectBuildFinishedEvent.PROJECT_BUILD_FINISHED, this::handleProjectBuildFinished);
    }

}
