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

public class ErlErrorInfo {
    public final static int NO_ERROR = 0;
    public final static int ERROR = 1;
    public final static int WARNNING = 2;

    public int line;
    public final int type;
    public final String moduleFileName;
    public final String message;
    public ErlangProject erlangProject;

    public ErlErrorInfo(int line, int type, String moduleFileName, String message, ErlangProject project) {
        this.line = line;
        this.type = type;
        this.moduleFileName = moduleFileName;
        this.message = message;
        this.erlangProject = project;
    }

}
