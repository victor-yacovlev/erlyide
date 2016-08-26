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

package com.github.victoryacovlev.erlide.fxui.mainwindow;

import com.github.victoryacovlev.erlide.erlangtools.ErlangCompiler;
import com.github.victoryacovlev.erlide.fxui.editor.ErlangCodeArea;
import com.github.victoryacovlev.erlide.fxui.editor.FileSavedEvent;
import com.github.victoryacovlev.erlide.fxui.editor.ModuleNameChangeEvent;
import com.github.victoryacovlev.erlide.fxui.editor.TextEditedEvent;
import com.github.victoryacovlev.erlide.project.ProjectFile;
import com.sun.istack.internal.NotNull;
import javafx.scene.control.Tab;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.io.File;

public class EditorTab extends Tab {

    final ErlangCodeArea editor;
    final VirtualizedScrollPane<ErlangCodeArea> scrollPane;

    public EditorTab(@NotNull ProjectFile projectFile) {
        super();
        editor = new ErlangCodeArea(projectFile, ErlangCompiler.getInstance());
        scrollPane = new VirtualizedScrollPane<>(editor);
        setContent(scrollPane);
        setTitleFromFileName(projectFile.getName());
        getContent().addEventHandler(ModuleNameChangeEvent.MODULE_NAME_CHANGE, event -> {
            if (event.getEditor() == this.getEditor()) {
                boolean textChanged = ! event.getEditor().getUndoManager().isAtMarkedPosition();
                setTitleFromModuleName(event.getModuleName(), event.getEditor().getProjectFile().getName(), textChanged);
            }
        });
        getContent().addEventHandler(TextEditedEvent.TEXT_EDIT, event -> {
            if (event.getEditor() == this.getEditor()) {
                boolean textChanged = ! event.getEditor().getUndoManager().isAtMarkedPosition();
                setTitleFromModuleName(event.getEditor().getModuleName(), event.getEditor().getProjectFile().getName(), textChanged);
            }
        });
        getContent().addEventHandler(FileSavedEvent.FILE_SAVED, event -> {
            if (event.getEditor() == this.getEditor()) {
                setTitleFromFileName(event.getEditor().getProjectFile().getName());
            }
        });
    }

    private void setTitleFromFileName(String filePath) {
        File f = new File(filePath);
        setText(f.getName());
    }

    private void setTitleFromModuleName(String moduleName, String oldFileName, boolean fileChanged) {
        File f = new File(oldFileName);
        final String newName = moduleName==null? f.getName() : moduleName + ".erl";
        final String oldName = f.getName();
        final String changedMark = fileChanged ? "*" : "";
        final String text = newName.equals(oldName)
                ? oldName + changedMark
                : newName + " [" + oldName + "]" + changedMark;
        setText(text);
    }

    ErlangCodeArea getEditor() {
        return editor;
    }
}
