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
import sun.reflect.generics.tree.Tree;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


public class ProjectTreeItem extends ProjectFileItem {
    private final ProjectGroupTreeItem sources;
    private final ProjectGroupTreeItem includes;
    private final ProjectGroupTreeItem resources;
    private final ProjectGroupTreeItem deps;
    private final ProjectGroupTreeItem apps;
    private final ProjectGroupTreeItem unsorted;
    private final ObservableList<TreeItem<String>> groups = FXCollections.observableArrayList();
    private final StringProperty value;

    public ProjectTreeItem(ErlangProject project) {
        super(project);
        sources = new ProjectGroupTreeItem("Sources", project.getSrcDir(), "folder-outline");
        sources.setExpanded(true);
        includes = new ProjectGroupTreeItem("Includes", project.getIncludeDir(), "folder-outline");
        resources = new ProjectGroupTreeItem("Resources", project.getPrivDir(), "folder-outline");
        deps = new ProjectGroupTreeItem("Libraries", project.getDepsDir(), "folder-multiple-outline");
        apps = new ProjectGroupTreeItem("Subprojects", project.getAppsDir(), "folder-multiple-outline");
        apps.setExpanded(true);
        unsorted = new ProjectGroupTreeItem("Non-OTP Sources", project.getRootDir(), "folder-outline");

        switch (project.getStructureType()) {
            case OtpWithApps:
                groups.addAll(apps, deps);
                break;
            case SimpleOtp:
                sources.bindCollection(project.getSourceFiles());
                groups.addAll(sources, includes, resources, deps);
                break;
            default:
                groups.add(unsorted);
                break;
        }

        this.value = new SimpleStringProperty(project.getName()) {
            @Override
            public String get() {
                return "Project [" + project.getName() + "]";
            }
        };

        super.getChildren().setAll(groups);
        valueProperty().bindBidirectional(value);
        this.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/folder-16px.png"))));
    }

    @Override
    public boolean isLeaf() { return false; }



}
