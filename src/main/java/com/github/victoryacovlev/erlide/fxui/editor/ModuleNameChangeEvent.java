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

package com.github.victoryacovlev.erlide.fxui.editor;

import com.sun.istack.internal.NotNull;
import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class ModuleNameChangeEvent extends Event {
    public static final EventType<ModuleNameChangeEvent> MODULE_NAME_CHANGE =
            new EventType<>(Event.ANY, "MODULE_NAME_CHANGE");

    private final String moduleName;
    private final ErlangCodeArea editor;

    public ModuleNameChangeEvent(@NotNull ErlangCodeArea editor, EventTarget target, @NotNull String moduleName) {
        super(editor, target, MODULE_NAME_CHANGE);
        this.moduleName = moduleName;
        this.editor = editor;
    }

    public @NotNull String getModuleName() {
        return moduleName;
    }
    public @NotNull ErlangCodeArea getEditor() { return editor; }
}