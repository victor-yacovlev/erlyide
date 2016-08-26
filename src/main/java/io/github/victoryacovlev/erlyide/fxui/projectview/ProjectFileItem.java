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

import io.github.victoryacovlev.erlyide.project.ProjectFile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;

public class ProjectFileItem extends TreeItem<String> {
    private final ProjectFile projectFile;
    private final StringProperty value;

    @Override
    public boolean isLeaf() { return true; }

    public ProjectFileItem(ProjectFile aprojectFile) {
        projectFile = aprojectFile;
        value = new SimpleStringProperty(projectFile.getName()) {
            @Override
            public String get() {
                return projectFile.getName();
            }
        };
        valueProperty().bindBidirectional(value);
    }

    public ProjectFile getProjectFile() {
        return projectFile;
    }
}
