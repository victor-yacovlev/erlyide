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

package io.github.victoryacovlev.erlyide.fxui;


import io.github.victoryacovlev.erlyide.erlangtools.ErlangCompiler;
import io.github.victoryacovlev.erlyide.erlangtools.ProjectBuilder;
import io.github.victoryacovlev.erlyide.erlangtools.ProjectLoader;
import io.github.victoryacovlev.erlyide.fxui.workspacechooser.WokrspaceChooserController;
import io.github.victoryacovlev.erlyide.project.ErlangProject;
import io.github.victoryacovlev.erlyide.fxui.mainwindow.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ErlIDEApp extends Application {

    Stage mainWindowStage;
    Stage workspaceChooserStage;
    MainWindowController mainWindowController;
    WokrspaceChooserController workspaceChooserController;

    @Override
    public void start(Stage primaryStage) throws Exception {

        createMainWindow(primaryStage);
        createWorkspaceChooser();

        mainWindowController.setWokrspaceChooserController(workspaceChooserController);

        primaryStage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                System.gc();
        });

        String workspaceToUse = null;

        if (workspaceChooserController.isShowOnStartup()) {
            if (workspaceChooserController.execDialog(null)) {
                workspaceToUse = workspaceChooserController.getSelectedWorkspacePath();
            }
        }
        else {
            workspaceToUse = workspaceChooserController.getSelectedWorkspacePath();
        }

        if (null == workspaceToUse) {
            System.exit(0);
        }


        mainWindowController.loadSettings();

        ErlangProject project = new ErlangProject(workspaceToUse, null);
        ProjectBuilder builder = ProjectBuilder.instance();
        ProjectLoader projectLoader = ProjectLoader.getInstance(project);
        builder.setLoader(projectLoader);
        ErlangCompiler.getInstance();
        mainWindowController.setErlangProject(project);
        builder.buildProjectAsync(project, mainWindowController.getRoot());

        mainWindowStage.show();
        mainWindowController.setInitialFocus();
    }

    @Override
    public void stop() throws Exception {
        mainWindowController.saveSettings();
        Thread.sleep(500);
        System.exit(0);
    }

    private void createMainWindow(Stage primaryStage) throws Exception {
        mainWindowStage = primaryStage;
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/MainWindow.fxml"));
        Parent root = loader.load();
        mainWindowController = loader.getController();
        mainWindowController.setStage(primaryStage);
        mainWindowController.setRoot(root);
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/MainWindow.css");

        // CSS files applied to scene but not for individual components,
        // so add editor syntax highlighting here
        scene.getStylesheets().add("/styles/EditorSyntaxHighlight.css");
        mainWindowStage.setScene(scene);
    }

    private void createWorkspaceChooser() throws Exception {
        workspaceChooserStage = new Stage();
        workspaceChooserStage.setTitle("Choose workspace");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/WorkspaceChooser.fxml"));
        VBox root = loader.load();
        workspaceChooserController = loader.getController();
        workspaceChooserController.setStage(workspaceChooserStage);
        Scene scene = new Scene(root);
        workspaceChooserStage.setScene(scene);
        double w = root.getPrefWidth();
        double h = root.getPrefHeight() + 30;
        workspaceChooserStage.setMinWidth(w);
        workspaceChooserStage.setMaxWidth(w);
        workspaceChooserStage.setMinHeight(h);
        workspaceChooserStage.setMaxHeight(h);
        workspaceChooserController.restoreSettings();
    }

    public static void main(String []args) {
        launch(args);
    }
}
