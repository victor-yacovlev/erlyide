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

import io.github.victoryacovlev.erlyide.erlangtools.ErlangCompiler;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
        OtpWithApps, SimpleOtp, PlainFolder
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ErlangProject) {
            ErlangProject other = (ErlangProject) obj;
            return other.getRootDir().getAbsolutePath().equals(rootDir.getAbsolutePath());
        }
        else return super.equals(obj);
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
    public ObservableList getIncludeFiles() { return includeFiles; }

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
    private ObservableList<ErlangIncludeFile> includeFiles = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private ObservableList<ProjectFile> otherFiles = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
//    private ObservableList<ErlangSourceFile> sourceFiles = FXCollections.observableArrayList();

    public ErlangProject(final String projectRootPath, ErlangProject parent) {
        super(new File(projectRootPath), parent);
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
                scanForSourceFilesChanges(sourceFiles, ".erl", getSrcDir(), ErlangFileType.SourceFile);
                scanForSourceFilesChanges(includeFiles, ".hrl", getIncludeDir(), ErlangFileType.IncludeFile);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void scanForIncludes(ErlangSourceFile erlangSourceFile) {
        String data = erlangSourceFile.readAll();
        scanForIncludes(erlangSourceFile, data);
    }

    public void removeSourceFromIncludes(ErlangSourceFile erlangSourceFile) {
        for (int j=0; j<includeFiles.size(); ++j) {
            ErlangIncludeFile includeFile = includeFiles.get(j);
            includeFile.getUsages().remove(erlangSourceFile);
        }
    }

    public void scanForIncludes(ErlangSourceFile erlangSourceFile, String data) {
        List<String> includes = ErlangCompiler.getInstance().scanForIncludeStatements(data);
        for (ErlangIncludeFile includeFile : includeFiles) {
            String fullPath = includeFile.getFile().getAbsolutePath();
            String shortPath = includeFile.getName();
            if (includes.contains(shortPath) || includes.contains(fullPath)) {
                includeFile.getUsages().add(erlangSourceFile);
            }
        }
    }

    public void scanAllSourcesForInclude(ErlangIncludeFile includeFile) {
        String fullPath = includeFile.getFile().getAbsolutePath();
        String shortPath = includeFile.getName();
        for (ErlangSourceFile sourceFile : sourceFiles) {
            List<String> includes = ErlangCompiler.getInstance().scanForIncludeStatements(sourceFile.readAll());
            if (includes.contains(shortPath) || includes.contains(fullPath)) {
                includeFile.getUsages().add(sourceFile);
            }
        }
    }

    private ProjectFile wrapFileInContainer(File f, ErlangFileType fileType) {
        ProjectFile createdFile = null;
        switch (fileType) {
            case SourceFile:
                createdFile = new ErlangSourceFile(f, this);
                scanForIncludes((ErlangSourceFile)createdFile);
                break;
            case IncludeFile:
                createdFile = new ErlangIncludeFile(f, this);
                scanAllSourcesForInclude((ErlangIncludeFile)createdFile);
                break;
            default:
                break;
        }
        return createdFile;
    }

    public ProjectFile createNewFile(ErlangFileTemplate template, String fileName) {
        File rootDir = null;
        ObservableList collection = null;
        ErlangFileType fileType = null;
        if (template instanceof ErlangSourceFileTemplate) {
            fileType = ErlangFileType.SourceFile;
        }
        else if (template instanceof ErlangIncludeFileTemplate) {
            fileType = ErlangFileType.IncludeFile;
        }
        switch (fileType) {
            case SourceFile: rootDir = getSrcDir(); collection = sourceFiles; break;
            case IncludeFile: rootDir = getIncludeDir(); collection = includeFiles; break;
            default: rootDir = getRootDir(); collection = otherFiles;
        }
        File file = template.createFile(rootDir, fileName);
        ProjectFile createdFile = wrapFileInContainer(file, fileType);
        collection.add(createdFile);
        return createdFile;
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

    private void scanForSourceFilesChanges(ObservableList collection, String suffix, File root, ErlangFileType fileType) {
        synchronized (collection) {
            File[] listOfFiles = root.listFiles(pathname -> pathname.getName().endsWith(suffix));
            LinkedList<ProjectFile> toRemove = new LinkedList<>();
            LinkedList<ProjectFile> toAdd = new LinkedList<>();
            for (Object obj : collection) {
                ProjectFile of = (ProjectFile) obj;
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
                    if (of instanceof ErlangSourceFile) {
                        removeSourceFromIncludes((ErlangSourceFile) of);
                    }
                    toRemove.add(of);
                }
            }
            for (File f : listOfFiles) {
                final String fPath = f.getAbsolutePath();
                boolean found = false;
                for (Object obj : collection) {
                    ProjectFile of = (ProjectFile) obj;
                    final String ofPath = of.getFile().getAbsolutePath();
                    if (ofPath.equals(fPath)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ProjectFile pf = wrapFileInContainer(f, fileType);
                    toAdd.add(pf);
                }
            }
            collection.removeAll(toRemove);
            collection.addAll(toAdd);
        }
    }

    private void parseRebarConfig() {

    }

    public void removeFile(ProjectFile projectFile) {
        if (projectFile instanceof ErlangSourceFile) {
            removeSourceFromIncludes((ErlangSourceFile) projectFile);
            sourceFiles.remove(projectFile);
        }
        else if (projectFile instanceof ErlangIncludeFile) {
            includeFiles.remove(projectFile);
        }
        projectFile.getFile().delete();
    }

}
