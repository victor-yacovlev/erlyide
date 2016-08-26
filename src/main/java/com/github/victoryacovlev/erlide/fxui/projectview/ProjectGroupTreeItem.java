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

package com.github.victoryacovlev.erlide.fxui.projectview;

import com.github.victoryacovlev.erlide.project.ErlangProject;
import com.github.victoryacovlev.erlide.project.ProjectFile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

class ProjectGroupTreeItem extends TreeItem<String> {
    private final String groupName;
    private final File groupSubdir;
    private final StringProperty value;

    ProjectGroupTreeItem(String name, File subdir, String icon) {
        this.groupName = name;
        this.groupSubdir = subdir;
        this.value = new SimpleStringProperty("") {
            @Override public String get() {
                if (groupSubdir!=null) {
                    return groupName + " [" + groupSubdir.getName() + "]";
                }
                else {
                    return groupName;
                }
            }
        };
        valueProperty().bindBidirectional(value);
        this.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/"+icon+"-16px.png"))));
    }

    @Override
    public boolean isLeaf() { return false; }


    private void updateFromCollection(ObservableList<ProjectFile> list) {
        List<TreeItem<String>> toRemove = new LinkedList<>();
        List<TreeItem<String>> toAdd = new LinkedList<>();
        for (ProjectFile pf : list) {
            boolean found = false;
            for (TreeItem<String> ti : super.getChildren()) {
                ProjectFileItem pfi = (ProjectFileItem) ti;
                if (pfi.getProjectFile() == pf) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (pf instanceof ErlangProject) {
                    toAdd.add(new ProjectTreeItem((ErlangProject) pf));
                }
                else {
                    toAdd.add(new ProjectFileItem(pf));
                }
            }
        }
        for (TreeItem<String> ti : super.getChildren()) {
            boolean found = false;
            ProjectFileItem pfi = (ProjectFileItem) ti;
            for (ProjectFile pf : list) {
                if (pfi.getProjectFile() == pf) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                toRemove.add(ti);
            }
        }

        super.getChildren().removeAll(toRemove);
        super.getChildren().addAll(toAdd);
    }

    public void bindCollection(ObservableList<ProjectFile> collection) {
        collection.addListener(new ListChangeListener<ProjectFile>() {
            @Override
            public void onChanged(Change<? extends ProjectFile> c) {
                ObservableList<ProjectFile> list = (ObservableList<ProjectFile>) c.getList();
                updateFromCollection(list);
            }
        });
        updateFromCollection(collection);  // build items for first time
    }

}