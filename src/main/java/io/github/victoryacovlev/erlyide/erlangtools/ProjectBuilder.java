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

import com.ericsson.otp.erlang.OtpAuthException;
import io.github.victoryacovlev.erlyide.project.ErlangProject;
import io.github.victoryacovlev.erlyide.project.ErlangSourceFile;
import javafx.event.Event;
import javafx.event.EventTarget;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectBuilder {

    private final ErlangCompiler compiler;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ExecutorService buildService = Executors.newSingleThreadExecutor();

    private static ProjectBuilder singleton = null;

    public static ProjectBuilder instance() {
        if (singleton==null) {
            singleton = new ProjectBuilder();
        }
        return singleton;
    }

    ProjectBuilder() {
        ErlangCompiler compiler = null;
        try {
            ErlangVM vm = ErlangVM.getInstance();
            compiler = new ErlangCompiler(vm);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.compiler = compiler;
    }


    public CompileResult buildProject(ErlangProject project) {
        List<String> fileNames = prepareListOfChangedFiles(project);
        List<ErlErrorInfo> errorMessages = new LinkedList<>();
        List<String> compiledBeamFiles = Collections.synchronizedList(new LinkedList<>());
        List<Callable<Object>> tasks = new LinkedList<>();
        // TODO do compilation in parallel
        for ( final String sourceFileName : fileNames ) {
//            tasks.add(() -> {
                CompileResult fileCompileResult = compiler.compile(sourceFileName);
                List<String> fileOutputs = fileCompileResult.getOutputFileNames();
//                synchronized (compiledBeamFiles) {
                    compiledBeamFiles.addAll(fileOutputs);
//                }
//                synchronized (errorMessages) {
                    errorMessages.addAll(fileCompileResult.getErrorsAndWarnings());
//                }
//                return null;
//            });
        }
//        try {
//            List<Future<Object>> futures = threadPool.invokeAll(tasks);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return new CompileResult(compiledBeamFiles, errorMessages);
    }

    public void buildProjectAsync(ErlangProject project, EventTarget eventTarget) {
        buildService.submit(() -> {
            CompileResult result = buildProject(project);
            ProjectBuildFinishedEvent event = new ProjectBuildFinishedEvent(this, null, project, result);
            Event.fireEvent(eventTarget, event);
        });
    }

    private List<String> prepareListOfChangedFiles(ErlangProject project) {
        File binaryDir = project.getEbinDir();
        List<ErlangSourceFile> sourceFiles = project.getSourceFiles();
        List<String> result = new LinkedList<>();
        for (int i=0; i<sourceFiles.size(); ++i) {
            final File sourceFile = sourceFiles.get(i).getFile();
            final String baseName = sourceFile.getName();
            final int dotPos = baseName.lastIndexOf('.');
            final String moduleName = baseName.substring(0, dotPos);
            final File outFile = new File(binaryDir.getAbsolutePath() + "/" + moduleName + ".beam");
            if (!outFile.exists()) {
                result.add(sourceFile.getAbsolutePath());
            }
            else {
                long sourceTimeStamp = sourceFile.lastModified();
                long binaryTimeStamp = outFile.lastModified();
                if (sourceTimeStamp > binaryTimeStamp) {
                    result.add(sourceFile.getAbsolutePath());
                }
            }
        }
        return result;
    }

}
