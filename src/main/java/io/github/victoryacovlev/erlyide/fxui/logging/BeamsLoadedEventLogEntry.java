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

import java.io.File;
import java.util.List;

public class BeamsLoadedEventLogEntry extends EventLogEntry {
    private final List<String> reloadedModules;
    private final List<String> notReloadedModules;

    public BeamsLoadedEventLogEntry(List<String> reloadedModules, List<String> notReloadedModules) {
        super(String.format("%1d modules (re)loaded", reloadedModules.size()));
        this.reloadedModules = reloadedModules;
        this.notReloadedModules = notReloadedModules;
    }

    @Override
    public void mergeAfter(EventLogEntry previous) {
        if (previous instanceof BuildFinishedEventLogEntry) {
            BuildFinishedEventLogEntry bf = (BuildFinishedEventLogEntry) previous;
            if (reloadedModules.size() == 0) {
                messageProperty().set(previous.getMessage());
            }
            else {
                StringBuilder builder = new StringBuilder("Build finished. ");
                if (1==reloadedModules.size()) {
                    builder.append("Module '" + reloadedModules.get(0) + "' reloaded");
                }
                else {
                    builder.append(Integer.toString(reloadedModules.size()) + " modules reloaded");
                }
                if (bf.getErrorFiles().size() > 0) {
                    if (1==bf.getErrorFiles().size()) {
                        String fileName = new File(bf.getErrorFiles().get(0)).getName();
                        builder.append(", file '"+fileName+"' has errors");
                    }
                    else {
                        builder.append(", " + Integer.toString(bf.getErrorFiles().size())+" have errors");
                    }
                }
                messageProperty().set(builder.toString());
            }
        }
    }
}
