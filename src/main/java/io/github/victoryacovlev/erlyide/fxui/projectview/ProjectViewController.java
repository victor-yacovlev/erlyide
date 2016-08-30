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

package io.github.victoryacovlev.erlyide.fxui.projectview;

import io.github.victoryacovlev.erlyide.fxui.mainwindow.MainWindowController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ProjectViewController {
    private final TreeView treeView;
    private final MainWindowController mainWindowController;
    private Stage mainWindowStage = null;
    private ContextMenu projectFileContextMenu;
    private RenameDialogController renameDialogController;
    private Stage renameDialogStage;

    private final class TreeCellImpl extends TreeCell {

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            }
            else {
                setText(getItem() == null ? "" : getItem().toString());
                setGraphic(getTreeItem().getGraphic());
                TreeItem ti = getTreeItem();
                if (ti instanceof ProjectTreeItem) {
                    // TODO show project-wide context menu
                }
                else if (ti instanceof ProjectGroupTreeItem) {
                    // TODO show group-related context menu
                }
                else if (ti instanceof ProjectFileItem) {
                    setContextMenu(projectFileContextMenu);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && event.getButton()==MouseButton.PRIMARY) {
                            ProjectFileItem pfi = (ProjectFileItem) ti;
                            mainWindowController.openProjectFile(pfi.getProjectFile());
                        }
                    });
                }
            }
        }
    }

    public ProjectViewController(TreeView treeView, MainWindowController mainWindowController) {
        this.treeView = treeView;
        createContextMenus();


        this.treeView.setCellFactory(param -> new TreeCellImpl());
        this.mainWindowController = mainWindowController;
    }

    private void initializeRenameDialog() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/RenameDialog.fxml"));
        renameDialogStage = new Stage();
        try {
            renameDialogStage.setScene(new Scene(loader.load()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        renameDialogController = (RenameDialogController) loader.getController();
        renameDialogController.setStage(renameDialogStage);
        renameDialogController.initialize();
        renameDialogStage.setTitle("Rename file");
        renameDialogStage.initModality(Modality.WINDOW_MODAL);
        renameDialogStage.initOwner(mainWindowStage);
        renameDialogStage.setMinWidth(470);
        renameDialogStage.setMinHeight(180);
        renameDialogStage.setMaxWidth(470);
        renameDialogStage.setMaxHeight(180);
    }

    public void setMainWindowStage(Stage stage) {
        mainWindowStage = stage;
        initializeRenameDialog();
    }

    private void createContextMenus() {

        MenuItem openFileItem = new MenuItem("Open");
        openFileItem.setOnAction(event -> {
            if (getCurrentItem()!=null) {
                mainWindowController.openProjectFile(getCurrentItem().getProjectFile());
            }
        });

        MenuItem deleteFileItem = new MenuItem("Delete");
        deleteFileItem.setOnAction(event -> {
            if (getCurrentItem()!=null) {
                mainWindowController.getErlangProject().removeFile(getCurrentItem().getProjectFile());
            }
        });

        MenuItem renameFileItem = new MenuItem("Rename...");
        renameFileItem.setOnAction(event -> {
            if (getCurrentItem()!=null) {
                renameAction(getCurrentItem());
            }
        });

        projectFileContextMenu = new ContextMenu(openFileItem, new SeparatorMenuItem(), renameFileItem, deleteFileItem);

    }


    private ProjectFileItem getCurrentItem() {
        Object obj = treeView.getSelectionModel().getSelectedItem();
        if (obj != null && obj instanceof ProjectFileItem) {
            return (ProjectFileItem) obj;
        }
        else {
            return null;
        }
    }

    private void renameAction(ProjectFileItem item) {
        renameDialogController.initializeWithItem(item);
        renameDialogStage.showAndWait();
    }
}
