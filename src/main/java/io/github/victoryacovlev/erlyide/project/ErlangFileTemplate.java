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
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class ErlangFileTemplate {
    private final String name;
    private final String suffix;
    private final String templateFileName;
    private String templateData = null;

    private static List<ErlangFileTemplate> templates = Arrays.asList(
            new ErlangSourceFileTemplate("Simple Module", "simple.erl"),
            new ErlangIncludeFileTemplate()
    );

    protected ErlangFileTemplate(String name, String suffix, String templateFileName) {
        this.name = name;
        this.suffix = suffix;
        this.templateFileName = templateFileName;
    }

    public static List<ErlangFileTemplate> getTemplates() {
        return templates;
    }

    public String getName() { return name; }
    @Override public String toString() { return getName(); }

    protected String getData() {
        if (null == templateData) {
            InputStream moduleTemplateStream = getClass().getResourceAsStream("/templates/"+templateFileName);
            Scanner s = new Scanner(moduleTemplateStream);
            s.useDelimiter("\\A");
            templateData = s.next();
        }
        return templateData;
    }

    public abstract File createFile(File rootDir, String fileName);

    public String getSuffix() {
        return suffix;
    }
}
