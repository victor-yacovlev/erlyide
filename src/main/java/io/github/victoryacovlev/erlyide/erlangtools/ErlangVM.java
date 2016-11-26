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
import java.lang.reflect.Array;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


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
            instance = new ErlangVM(null);
        }
        return instance;
    }

    public static ErlangVM restart(final String workspaceDir) throws InterruptedException, IOException, OtpAuthException {
        if (null != instance) {
            instance.shutdown();
        }
        instance = new ErlangVM(workspaceDir);
        return instance;
    }

    public void shutdown() {
        interpreter.terminate();
    }

    public String getHelpersRootPath() {
        // TODO make cross-platform implementation without com.sun.deploy.config.Config
        final String helpersRoot = System.getenv("HOME")+"/.erlyide/helpers";
//        final String helpersRoot = Config.getCacheDirectory() +
//                "/" + getClass().getName().replace('.', '/')+
//                "/erlang_helper_modules";
        return helpersRoot;
    }

    private ErlangVM(String workspaceDir) throws IOException, InterruptedException, OtpAuthException {
        unpackHelpersSources();
        unpackHelperPackages();
        if (null == workspaceDir) {
            workspaceDir = System.getProperty("user.dir");
        }
        List<String> pas = getHelperProgramPaths();
        String[] baseArgs = new String[] {
                "erl",
                "-name", serverName+"@localhost",
                "-setcookie", cookie,
        };
        String[] allArgs = Arrays.copyOf(baseArgs,baseArgs.length+pas.size()*2);
        for (int i=0; i<pas.size(); ++i) {
            allArgs[baseArgs.length + i*2] = "-pa";
            allArgs[baseArgs.length + i*2 + 1] = pas.get(i);
        }
        interpreter = new Interpreter(workspaceDir, allArgs);
        interpreter.waitForStarted();
        Thread.sleep(1000);
        client = new OtpSelf(clientName+"@localhost", cookie);
        server = new OtpPeer(serverName+"@localhost");
        connection = client.connect(server);
        compileAndLoadHelpers();
        System.gc();
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

    public List<String> getHelperProgramPaths() {
        List<String> result = new LinkedList<>();
        result.add(getHelpersRootPath()+"/ebin");
        final String helpersAppsPath = getHelpersRootPath() + "/apps";
        File helpersAppsTargetDir = new File(helpersAppsPath);
        for (File entry : helpersAppsTargetDir.listFiles()) {
            if (entry.isDirectory()) {
                File ebinSubDir = new File(entry.getAbsoluteFile() + "/ebin");
                if (ebinSubDir.exists()) {
                    result.add(ebinSubDir.getAbsolutePath());
                }
            }
        }
        return result;
    }

    private void unpackHelperPackages() {
        final String helpersAppsPath = getHelpersRootPath() + "/apps";
        File helpersAppsTargetDir = new File(helpersAppsPath);
        helpersAppsTargetDir.mkdirs();
        final String packages[] = new String[] {
                "rebar3"
        };
        for (final String packageFileName : packages) {
            InputStream packageInputStream = getClass().getResourceAsStream("/erlang_helper_modules/pkgs/"+packageFileName);
            try {
                // Find start position of ZIP stream in escript
                packageInputStream.mark(1024);
                byte[] header = new byte[512];
                int headerSize = packageInputStream.read(header, 0, 512);
                long startPos = 0;
                for (int i=3; i<headerSize; ++i) {
                    byte a = header[i-3];
                    byte b = header[i-2];
                    byte c = header[i-1];
                    byte d = header[i-0];
                    boolean magic1 = 0x50==a && 0x4B==b && 0x03==c && 0x04==d;
                    boolean magic2 = 0x50==a && 0x4B==b && 0x05==c && 0x06==d;
                    boolean magic3 = 0x50==a && 0x4B==b && 0x07==c && 0x08==d;
                    if (magic1 || magic2 || magic3) {
                        startPos = i-3;
                        break;
                    }
                }
                packageInputStream.reset();
                packageInputStream.skip(startPos);
                ZipInputStream zipInputStream = new ZipInputStream(packageInputStream);
                ZipEntry zipEntry;
                while (null!=(zipEntry = zipInputStream.getNextEntry())) {
                    long crc = zipEntry.getCrc();
                    String outFilePath = helpersAppsTargetDir.getAbsolutePath()+"/"+zipEntry.getName();
                    File outFile = new File(outFilePath);
                    File fileDir = outFile.getParentFile();
                    fileDir.mkdirs();
                    final int entryUnpackedSize = (int) zipEntry.getSize();
                    if (outFile.exists()) {
                        long fsize = outFile.length();
                        if (fsize == entryUnpackedSize) {
                            continue;
                        }
                    }
                    FileOutputStream writer = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int bytesReadTotal = 0;
                    while (bytesReadTotal < entryUnpackedSize) {
                        int bytesRead = zipInputStream.read(buffer, 0, 1024);
                        bytesReadTotal += bytesRead;
                        writer.write(buffer, 0, bytesRead);
                    }
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
