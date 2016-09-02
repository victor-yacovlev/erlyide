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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErlangIncludeFile extends ProjectFile {

    static final Pattern HEADER_DEFINE_SEARCH_PATTERN;

    static {
        HEADER_DEFINE_SEARCH_PATTERN = Pattern.compile("^\\s*-(ifndef|define)\\((\\w+)(,.+)?\\)\\.", Pattern.MULTILINE);
    }


    protected ErlangIncludeFile(File file, ErlangProject parent) {
        super(file, parent);
    }
    private List<ErlangSourceFile> usages = new LinkedList<>();

    public List<ErlangSourceFile> getUsages() {
        return usages;
    }

    @Override
    protected List<ProjectFileChange> preprocessToMatchNewName(String oldName, String newName) {
        String oldDefineName = oldName.toUpperCase().replace('.', '_');
        String newDefineName = newName.toUpperCase().replace('.', '_');
        List<ProjectFileChange> changes = new LinkedList<>();
        String source = getContents();
        Matcher matcher = HEADER_DEFINE_SEARCH_PATTERN.matcher(source);
        int startPos = 0;
        while (matcher.find(startPos)) {
            String matchedSubstring = matcher.group(0);
            String matchedModuleName = matcher.group(2);
            if (matchedModuleName.equals(oldDefineName)) {
                ProjectFileChange change = new ProjectFileChange();
                change.from = matcher.start(2);
                change.length = matchedModuleName.length();
                change.replacement = newDefineName;
                changes.add(change);
            }
            startPos += matchedSubstring.length();
        }
        return changes;
    }
}
