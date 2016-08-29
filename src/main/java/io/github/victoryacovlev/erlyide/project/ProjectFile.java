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

package io.github.victoryacovlev.erlyide.project;

import io.github.victoryacovlev.erlyide.erlangtools.ProjectFileRenamedEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventTarget;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public abstract class ProjectFile {
    private SimpleStringProperty name;

    protected File file;
    protected ProjectFile(File file) {
        this.file = file;
        this.name = new SimpleStringProperty(file.getName());
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    private String rename(String newName) {
        File f = new File(newName);
        file.renameTo(f);
        file = f;
        return file.getName();
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String newName) {
        String shortNewName = rename(newName);
        name.set(shortNewName);
    }

}
