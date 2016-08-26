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

import io.github.victoryacovlev.erlyide.erlangtools.ErlErrorInfo;
import io.github.victoryacovlev.erlyide.project.ErlangProject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class IssuesLogEntry {

    private final ErlErrorInfo erlErrorInfo;
    private final StringProperty message;
    private final StringProperty kind;
    private final StringProperty location;

    public IssuesLogEntry(ErlErrorInfo erlErrorInfo) {
        this.erlErrorInfo = erlErrorInfo;
        message = new SimpleStringProperty(this, "message", erlErrorInfo.message) {
            @Override
            public String get() {
                return erlErrorInfo.message;
            }
        };
        kind = new SimpleStringProperty(this, "kind") {
            @Override
            public String get() {
                if (erlErrorInfo.type==ErlErrorInfo.ERROR) {
                    return "Error";
                }
                else if (erlErrorInfo.type==ErlErrorInfo.WARNNING) {
                    return "Warning";
                }
                else {
                    return "(REMOVED)";
                }
            }
        };
        location = new SimpleStringProperty(this, "location") {
            @Override
            public String get() {
                ErlangProject project = erlErrorInfo.erlangProject;
                final String base = project.getSrcDir().getAbsolutePath();
                String result;
                if (erlErrorInfo.moduleFileName.startsWith(base)) {
                    result = erlErrorInfo.moduleFileName.substring(base.length()+1);
                }
                else {
                    result = erlErrorInfo.moduleFileName;
                }
                return result + ":" + Integer.toString(erlErrorInfo.line);
            }
        };
    }


    public String getMessage() {
        return message.get();
    }

    public StringProperty messageProperty() {
        return message;
    }

    public ErlErrorInfo getErlErrorInfo() {
        return erlErrorInfo;
    }

    public String getKind() {
        return kind.get();
    }

    public StringProperty kindProperty() {
        return kind;
    }

    public String getLocation() {
        return location.get();
    }

    public StringProperty locationProperty() {
        return location;
    }

}
