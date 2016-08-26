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

package io.github.victoryacovlev.erlyide.ui.buildlog;

import io.github.victoryacovlev.erlyide.erlangtools.ErlErrorInfo;

import java.util.LinkedList;
import java.util.List;

public class BuildLog {

    private static List<BuildLogView> views = new LinkedList<>();
    private static List<ErlErrorInfo> errors = new LinkedList<>();

    public static void clearLog() {
        errors.clear();
        for (BuildLogView view : views) {
            view.clearLog();
        }
    }

    public static boolean isEmpty() {
        return errors.isEmpty();
    }

    public static void addView(BuildLogView view) {
        views.add(view);
        updateView(view);
    }

    private static void updateView(BuildLogView view) {
        view.clearLog();
        for (ErlErrorInfo info : errors)
            view.addItem(info);
    }

    public static void addItem(ErlErrorInfo errorInfo) {
        errors.add(errorInfo);
        for (BuildLogView view : views) {
            view.addItem(errorInfo);
        }
    }
}
