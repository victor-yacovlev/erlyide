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

import com.ericsson.otp.erlang.*;
import io.github.victoryacovlev.erlyide.project.ErlangProject;
import io.github.victoryacovlev.erlyide.project.ErlangSourceFile;
import javafx.event.Event;
import javafx.event.EventTarget;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProjectLoader {

    private static Map<ErlangProject, ProjectLoader> loaders = new HashMap<>();
    private final ErlangVM vm;
    private final ErlangProject erlangProject;
    private Map<String, Long> loadedVersions = new HashMap<>();

    public ProjectLoader(ErlangVM vm, ErlangProject erlangProject) {
        this.vm = vm;
        this.erlangProject = erlangProject;
    }

    public static ProjectLoader getInstance(ErlangProject erlangProject) {
        ProjectLoader loader = null;
        if (loaders.containsKey(erlangProject)) {
            loader = loaders.get(erlangProject);
        }
        else {
            try {
                ErlangVM vm = ErlangVM.getInstance();
                loader = new ProjectLoader(vm, erlangProject);
                loaders.put(erlangProject, loader);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OtpAuthException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return loader;
    }

    public void loadProjectChanges(EventTarget eventTarget) {
        File ebinRoot = erlangProject.getEbinDir();
        ensureEbinInPath(ebinRoot);
        List<String> reloadedNames = new LinkedList<>();
        List<String> notReloadedNames = new LinkedList<>();
//        OtpErlangList processes = pauseProcesses();
        for (int i=0; i<erlangProject.getSourceFiles().size(); i++) {
            ErlangSourceFile sourceFile = (ErlangSourceFile) erlangProject.getSourceFiles().get(i);
            String srcName = sourceFile.getName();
            String moduleName = srcName.substring(0, srcName.length()-4);
            String beamName = moduleName + ".beam";
            String fullBeamPath = ebinRoot + "/" + beamName;
            File beamFile = new File(fullBeamPath);
            if (beamFile.exists()) {
                long beamVersion = beamFile.lastModified();
                boolean update = true;
                boolean unload = false;
                if (loadedVersions.containsKey(fullBeamPath)) {
                    unload = true;
                    long loadedVersion = loadedVersions.get(fullBeamPath).longValue();
                    if (beamVersion <= loadedVersion) {
                        update = false;
                    }
                }
                if (update) {
                    loadedVersions.put(fullBeamPath, Long.valueOf(beamVersion));
                    if (reloadBeamModule(unload, moduleName)) {
                        reloadedNames.add(moduleName);
                    }
                    else {
                        notReloadedNames.add(moduleName);
                    }
                }
            }
        }
//        resumeProcesses(processes);
        ProjectLoadFinishedEvent event = new ProjectLoadFinishedEvent(this, null, erlangProject, reloadedNames, notReloadedNames);
        Event.fireEvent(eventTarget, event);
    }

    private OtpErlangList pauseProcesses() {
        OtpErlangList erlProcessList = new OtpErlangList();
        try {
            // Get all processes to suspend
            erlProcessList = (OtpErlangList) vm.run("erlang", "processes", new OtpErlangList());
            for (OtpErlangObject erlProcess : erlProcessList.elements()) {
                OtpErlangObject suspendStatus = vm.run("sys", "suspend", new OtpErlangList(new OtpErlangObject[]{erlProcess}));
                String s = suspendStatus.toString();
                System.out.println("Suspend " +erlProcess.toString() + ": " + s);
            }

        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        }
        return erlProcessList;
    }

    private void resumeProcesses(OtpErlangList erlProcessList) {
        try {
            for (OtpErlangObject erlProcess : erlProcessList) {
                OtpErlangObject resumeStatus = vm.run("sys", "resume", new OtpErlangList(new OtpErlangObject[]{erlProcess}));
                String s = resumeStatus.toString();
                System.out.println("Resume " +erlProcess.toString() + ": " + s);
            }
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        }
    }

    private boolean reloadBeamModule(boolean unloadBefore, String moduleName) {
        try {
            OtpErlangAtom moduleAtom = new OtpErlangAtom(moduleName);
            if (unloadBefore) {
                OtpErlangObject purgeStatus = vm.run("code", "purge", new OtpErlangList(new OtpErlangObject[]{moduleAtom}));
                String ps = purgeStatus.toString();
            }
            OtpErlangObject loadStatus = vm.run("code", "load_file", new OtpErlangList(new OtpErlangObject[]{moduleAtom}));
            String ls = loadStatus.toString();
            return true;
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        }
        return false;
    }



    private void ensureEbinInPath(File ebinRoot) {
        try {
            OtpErlangObject erlPath = vm.run("code", "get_path", new OtpErlangList());
            boolean foundInPath = false;
            if (erlPath instanceof OtpErlangList) {
                OtpErlangList erlPathList = (OtpErlangList) erlPath;
                for (OtpErlangObject erlPathEntry : erlPathList.elements()) {
                    if (erlPathEntry instanceof OtpErlangString) {
                        String pathEntry = ((OtpErlangString) erlPathEntry).stringValue();
                        if (pathEntry.equals(ebinRoot.getAbsolutePath())) {
                            foundInPath = true;
                            break;
                        }
                    }
                }
            }
            if (!foundInPath) {
                OtpErlangString erlPathEntry = new OtpErlangString(ebinRoot.getAbsolutePath());
                OtpErlangObject status = vm.run("code", "add_patha", new OtpErlangList(new OtpErlangObject[]{erlPathEntry}));
                if (status instanceof OtpErlangTuple) {
                    System.err.println(status.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (OtpAuthException e) {
            e.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        }
    }
}
