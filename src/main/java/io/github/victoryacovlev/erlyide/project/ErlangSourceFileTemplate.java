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

import java.io.*;

public class ErlangSourceFileTemplate extends ErlangFileTemplate {
    public ErlangSourceFileTemplate(String name, String templateFileName) {
        super(name, ".erl", templateFileName);
    }
    @Override
    public File createFile(File rootDir, String fileName) {
        String template = getData();
        String fullPath = rootDir.getAbsolutePath()+"/"+fileName;
        String moduleName = fileName.endsWith(".erl") ? fileName.substring(0, fileName.length()-4) : fileName;
        String contents = template.replace("?MODULE", moduleName);
        try {
            File file = new File(fullPath);
            FileOutputStream fs = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(contents);
            bufferedWriter.close();
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
