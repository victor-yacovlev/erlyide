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

package io.github.victoryacovlev.erlyide.fxui.logging;

import java.util.List;

public class BuildFinishedEventLogEntry extends EventLogEntry {
    private final List<String> generatedFiles;
    private final List<String> errorFiles;
    private final List<String> warningFiles;

    public BuildFinishedEventLogEntry(List<String> generated, List<String> withErrors, List<String> withWarnings) {
        super(String.format("Build finished. %1d module(s) generated, %2d have errors, %3d contains warnings",
                generated.size(), withErrors.size(), withWarnings.size()));
        this.generatedFiles = generated;
        this.errorFiles = withErrors;
        this.warningFiles = withWarnings;
    }
}
