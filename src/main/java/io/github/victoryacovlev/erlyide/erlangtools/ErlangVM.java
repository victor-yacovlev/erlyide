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
import io.github.victoryacovlev.erlyide.fxui.terminal.Interpreter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.Scanner;


public class ErlangVM {

    private final String clientName = "simple_erlang_ide_" + getMyPid();

    private String getMyPid() {
        final String fullName = ManagementFactory.getRuntimeMXBean().getName();
        int idx = fullName.indexOf('@');
        return fullName.substring(0, idx);
    }

    private final String serverName = "erlang" + getMyPid();
    private final String cookie = "qwerty";
    private OtpSelf client;
    private OtpPeer server;
    private OtpConnection connection;

    public Interpreter getInterpreter() {
        return interpreter;
    }

    private Interpreter interpreter;

    private static ErlangVM instance;

    public static ErlangVM getInstance() throws IOException, OtpAuthException, InterruptedException {
        if (null == instance) {
            instance = new ErlangVM();
        }
        return instance;
    }

    public String getHelpersRootPath() {
        // TODO make cross-platform implementation without com.sun.deploy.config.Config
        final String helpersRoot = System.getenv("HOME")+"/.erlyide/helpers";
//        final String helpersRoot = Config.getCacheDirectory() +
//                "/" + getClass().getName().replace('.', '/')+
//                "/erlang_helper_modules";
        return helpersRoot;
    }

    private ErlangVM() throws IOException, InterruptedException, OtpAuthException {
        unpackHelpersSources();
        interpreter = new Interpreter(new String[] {
                "erl",
                "-name", serverName+"@localhost",
                "-setcookie", cookie,
                "-pa", getHelpersRootPath()+"/ebin"
        });
        interpreter.waitForStarted();
        Thread.sleep(1000);
        client = new OtpSelf(clientName+"@localhost", cookie);
        server = new OtpPeer(serverName+"@localhost");
        connection = client.connect(server);
        compileAndLoadHelpers();
    }

    private void compileAndLoadHelpers() {
        final String modules[] = new String[] {
                "erlide_syntax_check"
        };
        final String helpersSrcPath = getHelpersRootPath() + "/src";
        final String helpersBinPath = getHelpersRootPath() + "/ebin";
        new File(helpersBinPath).mkdirs();
        for (final String moduleName : modules) {
            final String moduleInParam = helpersSrcPath + "/" + moduleName;
            OtpErlangList options = new OtpErlangList(new OtpErlangObject [] {
                    new OtpErlangAtom("return_errors"),
                    new OtpErlangAtom("return_warnings"),
                    new OtpErlangAtom("debug_info"),
                    new OtpErlangTuple(new OtpErlangObject[]{ new OtpErlangAtom("outdir"), new OtpErlangString(helpersBinPath) })
            });
            OtpErlangList arguments = new OtpErlangList(new OtpErlangObject[] {
                    new OtpErlangString(moduleInParam), options
            });
            try {
                OtpErlangTuple erlCompileResult = (OtpErlangTuple) run("compile", "file", arguments);
                final String compileStatus = ((OtpErlangAtom)erlCompileResult.elementAt(0)).atomValue();
                if (compileStatus.equals("ok")) {
                    OtpErlangTuple erlLoadResult = (OtpErlangTuple) run("code", "load_abs", new OtpErlangList(new OtpErlangObject[]{
                            new OtpErlangString(helpersBinPath + "/" + moduleName)
                    }));
                    final String loadStatus = ((OtpErlangAtom) erlLoadResult.elementAt(0)).atomValue();
                    if (loadStatus.equals("module")) {
                        OtpErlangTuple erlGetInfoResult = (OtpErlangTuple) run(moduleName, "helper_info", new OtpErlangList());
                        final String getInfoStatus = ((OtpErlangAtom) erlGetInfoResult.elementAt(0)).atomValue();
                        if (getInfoStatus.equals("ok")) {
                            final String startModuleName = ((OtpErlangAtom) erlGetInfoResult.elementAt(1)).atomValue();
                            final String startFunctionName = ((OtpErlangAtom) erlGetInfoResult.elementAt(3)).atomValue();
                            if (!startFunctionName.equals("none")) {
                                OtpErlangObject erlStartResult = run(startModuleName, startFunctionName, new OtpErlangList());
                                String status = "";
                                if (erlStartResult instanceof OtpErlangAtom) {
                                    status = ((OtpErlangAtom) erlStartResult).atomValue();
                                }
                                else if (erlStartResult instanceof OtpErlangTuple) {
                                    OtpErlangObject firstItem = ((OtpErlangTuple) erlStartResult).elementAt(0);
                                    status = ((OtpErlangAtom) firstItem).atomValue();
                                }
                                if (!status.equals("ok")) {
                                    System.err.println("Error starting helper " + moduleName);
                                }
                            }
                        }
                        else {
                            System.err.println("Error in helper interface " + moduleName);
                        }
                    }
                    else {
                        System.err.println("Error loading erlang module " + moduleName);
                    }
                }
                else {
                    System.err.println("Error compiling erlang module " + moduleName);
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

    public OtpErlangObject run(final String module, final String function, OtpErlangList arguments) throws IOException, OtpErlangExit, OtpAuthException, OtpErlangDecodeException {
        connection.sendRPC(module, function, arguments);
        OtpErlangTuple msg = (OtpErlangTuple) connection.receiveMsg().getMsg();
        return msg.elementAt(1);
    }

    private void unpackHelpersSources() {
        final String helpersSrcPath = getHelpersRootPath() + "/src";
        File helpersTargetDir = new File(helpersSrcPath);
        final String modules[] = new String[] {
            "erlide_syntax_check"
        };
        helpersTargetDir.mkdirs();
        for (final String moduleName : modules) {
            InputStream moduleInputStream =
                    getClass().getResourceAsStream("/erlang_helper_modules/src/"+moduleName+".erl");
            File moduleOutputFile = new File(helpersTargetDir.getAbsolutePath()+"/"+moduleName+".erl");
            try {
                FileOutputStream moduleOutputStream = new FileOutputStream(moduleOutputFile);
                Scanner s = new Scanner(moduleInputStream);
                s.useDelimiter("\\A");
                String moduleSource = s.next();
                OutputStreamWriter writer = new OutputStreamWriter(moduleOutputStream, "UTF-8");
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                bufferedWriter.write(moduleSource);
                bufferedWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
