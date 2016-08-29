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
import io.github.victoryacovlev.erlyide.project.ProjectFile;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class ProjectViewController {
    private final TreeView treeView;
    private final MainWindowController mainWindowController;
    private ContextMenu projectFileContextMenu;

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

        projectFileContextMenu = new ContextMenu(openFileItem, new SeparatorMenuItem(), deleteFileItem);

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
}
