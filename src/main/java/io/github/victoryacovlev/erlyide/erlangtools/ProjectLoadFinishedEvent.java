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

package io.github.victoryacovlev.erlyide.erlangtools;

import io.github.victoryacovlev.erlyide.project.ErlangProject;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

import java.util.List;

public class ProjectLoadFinishedEvent extends Event {

    public static EventType<ProjectLoadFinishedEvent> PROJECT_LOAD_FINISHED =
            new EventType<>(Event.ANY, "PROJECT_LOAD_FINISHED");
    private final ErlangProject project;
    private final List<String> reloadedNames;
    private final List<String> notReloadedNames;

    public ProjectLoadFinishedEvent(ProjectLoader loader, EventTarget target, ErlangProject project, List<String> reloadedNames, List<String> notReloadedNames) {
        super(loader, target, PROJECT_LOAD_FINISHED);
        this.project = project;
        this.reloadedNames = reloadedNames;
        this.notReloadedNames = notReloadedNames;
    }

    public ErlangProject getProject() {
        return project;
    }

    public List<String> getReloadedNames() {
        return reloadedNames;
    }

    public List<String> getNotReloadedNames() {
        return notReloadedNames;
    }
}
