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

public class ProjectBuildFinishedEvent extends Event {

    public static EventType<ProjectBuildFinishedEvent> PROJECT_BUILD_FINISHED =
            new EventType<>(Event.ANY, "PROJECT_BUILD_FINISHED");
    private final ErlangProject project;
    private final CompileResult result;

    public ErlangProject getProject() {
        return project;
    }

    public CompileResult getResult() {
        return result;
    }

    public ProjectBuildFinishedEvent(ProjectBuilder builder, EventTarget target, ErlangProject project, CompileResult result) {
        super(builder, target, PROJECT_BUILD_FINISHED);
        this.project = project;
        this.result = result;
        for (ErlErrorInfo erl : result.getErrorsAndWarnings()) {
            erl.erlangProject = project;
        }
    }
}
