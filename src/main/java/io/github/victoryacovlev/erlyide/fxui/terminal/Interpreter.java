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

package io.github.victoryacovlev.erlyide.fxui.terminal;

import java.io.*;


public class Interpreter extends CommandProcessor {


    private final ProcessBuilder processBuilder;
    private Process process = null;
    private IOHelper stdoutHandler;
    private IOHelper stderrHandler;
    private boolean justStarted = true;
    private Boolean pauseSupervisor = Boolean.FALSE;


    public Interpreter(final String workspaceDir, final String[] commandLineArgs) {
        super();
        processBuilder = new ProcessBuilder(commandLineArgs);
        processBuilder.directory(new File(workspaceDir));
        new Supervisor();
    }

    public void start() {
        try {
            process = processBuilder.start();
            justStarted = true;
            stdoutHandler = new IOHelper(1, process.getInputStream());
            stderrHandler = new IOHelper(2, process.getErrorStream());
            synchronized (pauseSupervisor) {
                pauseSupervisor = Boolean.FALSE;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (null != stderrHandler) {
            stderrHandler.stop();
        }
        if (null != stdoutHandler) {
            stdoutHandler.stop();
        }
    }

    @Override
    public void processCommand(String input) {
        if (null != process) {
            try {
//                output("\n");
                process.getOutputStream().write(input.getBytes("UTF-8"));
                process.getOutputStream().write('\n');
                process.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForStarted() {
        while (null==process) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void terminate() {
        if (null != process) {
            stop();
            synchronized (pauseSupervisor) {
                pauseSupervisor = Boolean.TRUE;
            }
//            try {
//                byte sequenceToQuit [] = {0x07 /* Ctrl+G */, 'q', 0x0A /* Enter */};
//                process.getOutputStream().write(sequenceToQuit);
//                process.getOutputStream().flush();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            int maxChecks = 100;
//            boolean correctlyClosed = false;
//            for (int i=maxChecks; i>0; --i) {
//                if (process.isAlive()) {
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        break;
//                    }
//                }
//                else {
//                    correctlyClosed = true;
//                    break;
//                }
//            }
//            if (!correctlyClosed) {
                process.destroy();
//            }
        }
    }

    private class IOHelper {
        private final Thread thread;
        IOHelper(int fd, InputStream is) throws UnsupportedEncodingException {
            InputStreamReader reader = new InputStreamReader(is, "UTF8");
            this.thread = new Thread(() -> {
                char[] buffer = new char[1000];
                while (true) {
                    TerminalFlavouredTextArea console = getConsole();
                    try {
                        if (null != console) {
                            try {
                                int bytesRead = reader.read(buffer);
                                if (bytesRead > 0) {
                                    String text = new StringBuilder().append(buffer, 0, bytesRead).toString();
                                    if (justStarted) {
                                        justStarted = false;
                                        text = text.replace("(abort with ^G)", "");
                                    }
                                    if (2 == fd) {
                                        outputError(text);
                                    }
                                    else {
                                        output(text);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            thread.start();
        }
        void stop() {
            thread.interrupt();
        }
    }

    private class Supervisor {
        Supervisor() {
            new Thread(() -> {
                while(true) {
                    boolean ignore = false;

                    synchronized (pauseSupervisor) {
                        ignore = pauseSupervisor.booleanValue();
                    }
                    if (null == process || !process.isAlive()) {
                        if (!ignore)
                            start();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }

                }
            }).start();
        }
    }

}
