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
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventTarget;

import java.io.*;
import java.util.*;

public abstract class ProjectFile {
    private SimpleStringProperty name;

    protected File file;
    private final ErlangProject parent;
    private EditingInterface editor = null;

    protected ProjectFile(File file, ErlangProject parent) {
        this.file = file;
        this.name = new SimpleStringProperty(file.getName());
        this.parent = parent;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    private String rename(String newName, boolean preprocessFile, boolean updateUsages) {
        String oldName = file.getName();
        File f = new File(file.getParentFile().getAbsolutePath() + "/" + newName);
        file.renameTo(f);
        file = f;
        if (preprocessFile) {
            List<ProjectFileChange> changeList = preprocessToMatchNewName(oldName, newName);
            if (! changeList.isEmpty()) {
                applyChanges(changeList);
            }
        }
        return file.getName();
    }

    private void applyChanges(List<ProjectFileChange> changeList) {
        if (editor != null) {
            editor.applyChanges(changeList);
        }
        else {
            String source = getContents();
            StringBuilder builder = new StringBuilder();
            int prevPos = 0;
            for (int i=0; i<changeList.size(); ++i) {
                ProjectFileChange change = changeList.get(i);
                int changeStart = change.from;

                builder.append(source.substring(prevPos, changeStart));

                int changeLength = change.length;
                String replacement = change.replacement;
                builder.append(replacement);

                prevPos = changeStart + changeLength;
            }
            builder.append(source.substring(prevPos));
            String newContents = builder.toString();
            write(newContents);
        }
    }

    protected String getContents() {
        if (editor!=null) {
            return editor.getText();
        }
        else {
            return readAll();
        }
    }

    public void setEditor(EditingInterface editor) {
        this.editor = editor;
    }

    protected List<ProjectFileChange> preprocessToMatchNewName(String oldName, String newName) {
        return Collections.EMPTY_LIST;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String newName, boolean preprocessFile, boolean updateUsages) {
        String shortNewName = rename(newName, preprocessFile, updateUsages);
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

    public void write(String contents) {
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(contents);
            bufferedWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ErlangProject getParent() {
        return parent;
    }

}
