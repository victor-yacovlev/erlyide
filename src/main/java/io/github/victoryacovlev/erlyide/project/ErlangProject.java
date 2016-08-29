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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ErlangProject extends ProjectFile {


    public File getAppsDir() {
        return appsDir;
    }


    public enum StructureType {
        OtpWithApps, SimpleOtp, PlainFolder;
    }

    private StructureType structureType;

    public StructureType getStructureType() {
        return structureType;
    }

    public File getRootDir() {
        return rootDir;
    }

    public File getEbinDir() {
        return ebinDir;
    }

    public File getSrcDir() {
        return srcDir;
    }

    public File getRebarConfigFile() {
        return rebarConfigFile;
    }

    public List<String> getDeps() {
        return deps;
    }

    public ObservableList getSourceFiles() {
        return sourceFiles;
    }

    private File rootDir;
    private File ebinDir;
    private File srcDir;

    public File getIncludeDir() {
        return includeDir;
    }

    public File getPrivDir() {
        return privDir;
    }

    public File getDepsDir() {
        return depsDir;
    }

    private File includeDir = null;
    private File privDir = null;
    private File depsDir = null;
    private File rebarConfigFile = null;
    private File appsDir = null;

    private List<String> deps = Collections.synchronizedList(new LinkedList<>());
    private ObservableList<ErlangSourceFile> sourceFiles = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
//    private ObservableList<ErlangSourceFile> sourceFiles = FXCollections.observableArrayList();

    public ErlangProject(final String projectRootPath) {
        super(new File(projectRootPath));
        rootDir = file;
        rootDir.mkdirs();
        rebarConfigFile = new File(rootDir.getAbsolutePath() + "/rebar.config");

        if (rebarConfigFile.exists()) {
            parseRebarConfig();
        }
        else {
            detectStructureType();
        }
        switch (structureType) {
            case SimpleOtp:
                srcDir = new File(projectRootPath + "/src");
                ebinDir = new File(projectRootPath + "/ebin");
                includeDir = new File(projectRootPath + "/include");
                privDir = new File(projectRootPath + "/priv");
                depsDir = new File(projectRootPath + "/deps");
                break;
            case OtpWithApps:
                appsDir = new File(projectRootPath + "/apps");
                break;
            default:
                srcDir = ebinDir = includeDir = privDir = new File(projectRootPath);
        }

        if (srcDir!=null) srcDir.mkdirs();
        if (ebinDir!=null) ebinDir.mkdirs();
        if (includeDir!=null) includeDir.mkdirs();
        if (privDir!=null) privDir.mkdirs();
        if (depsDir!=null) depsDir.mkdirs();
        if (appsDir!=null) appsDir.mkdirs();

        new Thread(() -> {
            while (true) {
                scanForSourceFilesChanges();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }


    public ErlangSourceFile createSourceFile(String fileName, String templateContents) {
        String fullPath = getSrcDir().getAbsolutePath()+"/"+fileName;
        try {
            File file = new File(fullPath);
            FileOutputStream fs = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fs, "UTF-8");
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(templateContents);
            bufferedWriter.close();
            ErlangSourceFile erlangSourceFile = new ErlangSourceFile(file);
            sourceFiles.add(erlangSourceFile);
            return erlangSourceFile;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void detectStructureType() {
        File apps = new File(getRootDir() + "/apps");
        File src = new File(getRootDir() + "/src");
        File ebin = new File(getRootDir() + "/ebin");
        if (apps.exists()) {
            structureType = StructureType.OtpWithApps;
        }
        else {
            File[] listOfFiles = getRootDir().listFiles(pathname -> pathname.getName().endsWith(".erl"));
            if (listOfFiles.length > 0 && !src.exists() && !ebin.exists()) {
                structureType = StructureType.PlainFolder;
            }
            else {
                structureType = StructureType.SimpleOtp;
            }
        }
    }

    private void scanForSourceFilesChanges() {
        synchronized (sourceFiles) {
            File[] listOfFiles = getSrcDir().listFiles(pathname -> pathname.getName().endsWith(".erl"));
            List newFilesList = new LinkedList(Arrays.asList(listOfFiles));
            LinkedList<ProjectFile> toRemove = new LinkedList<>();
            LinkedList<ErlangSourceFile> toAdd = new LinkedList<>();
            for (ProjectFile of : sourceFiles) {
                boolean found = false;
                final String ofPath = of.getFile().getAbsolutePath();
                for (File f : listOfFiles) {
                    final String fPath = f.getAbsolutePath();
                    if (fPath.equals(ofPath)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toRemove.add(of);
                }
            }
            for (File f : listOfFiles) {
                final String fPath = f.getAbsolutePath();
                boolean found = false;
                for (ProjectFile of : sourceFiles) {
                    final String ofPath = of.getFile().getAbsolutePath();
                    if (ofPath.equals(fPath)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toAdd.add(new ErlangSourceFile(f));
                }
            }
            sourceFiles.removeAll(toRemove);
            sourceFiles.addAll(toAdd);
        }
    }

    private void parseRebarConfig() {

    }

    public void removeFile(ProjectFile projectFile) {
        if (sourceFiles.contains(projectFile)) {
            sourceFiles.remove(projectFile);
            projectFile.getFile().delete();
        }
    }

}
