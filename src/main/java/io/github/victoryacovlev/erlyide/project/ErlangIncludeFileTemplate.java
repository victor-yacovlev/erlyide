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

public class ErlangIncludeFileTemplate extends ErlangFileTemplate {
    public ErlangIncludeFileTemplate() {
        super("Include file", ".hrl", "simple.hrl");
    }
    @Override
    public File createFile(File rootDir, String fileName) {
        String template = getData();
        String fullPath = rootDir.getAbsolutePath()+"/"+fileName;
        String fileNameUpperCase = fileName.replace('.', '_').toUpperCase();
        String contents = template.replace("?FILE_NAME_UPPER_CASE", fileNameUpperCase);
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
