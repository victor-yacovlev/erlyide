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

package com.github.victoryacovlev.erlide.fxui;


import com.github.victoryacovlev.erlide.project.ErlangProject;
import com.github.victoryacovlev.erlide.fxui.mainwindow.MainWindowController;
import com.github.victoryacovlev.erlide.ui.WorkspaceChooser;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ErlIDEApp extends Application {

    Stage mainWindowStage;
    MainWindowController mainWindowController;

    @Override
    public void start(Stage primaryStage) throws Exception {

        createMainWindow(primaryStage);

        primaryStage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                System.gc();
        });

        WorkspaceChooser dialog = new WorkspaceChooser();
        dialog.restoreSettings();
        dialog.setTitle(mainWindowController.APPLICATION_TITLE);
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
        mainWindowController.setErlangProject(new ErlangProject(workspaceToUse));
        mainWindowController.loadSettings();
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

    public static void main(String []args) {
        launch(args);
    }
}
