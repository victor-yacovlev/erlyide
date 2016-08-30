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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public abstract class ProjectFile {
    private SimpleStringProperty name;

    protected File file;
    private final ErlangProject parent;

    protected ProjectFile(File file, ErlangProject parent) {
        this.file = file;
        this.name = new SimpleStringProperty(file.getName());
        this.parent = parent;
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

    public String readAll() {
        String result = "";
        if (file!=null && file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                Scanner s = new Scanner(fis);
                s.useDelimiter("\\A");
                result = s.next();
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public ErlangProject getParent() {
        return parent;
    }

}
