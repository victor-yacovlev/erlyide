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

package com.github.victoryacovlev.erlide.ui.shell;

import com.eleet.dragonconsole.CommandProcessor;
import com.eleet.dragonconsole.DragonConsole;

import java.io.*;


public class Interpreter extends CommandProcessor {


    private final ProcessBuilder processBuilder;
    private Process process = null;
    private IOHelper stdoutHandler;
    private IOHelper stderrHandler;
    private boolean justStarted = true;


    public Interpreter(final String[] commandLineArgs) {
        super();
        final String workspaceDir = System.getProperty("user.dir");
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
                output("\n");
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

    private class IOHelper {
        private final Thread thread;
        IOHelper(int fd, InputStream is) throws UnsupportedEncodingException {
            InputStreamReader reader = new InputStreamReader(is, "UTF8");
            this.thread = new Thread(() -> {
                char[] buffer = new char[1000];
                while (true) {
                    DragonConsole console = getConsole();
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
                                    text = text.replace("&", "&&");
                                    if (2 == fd) {
                                        outputError(text);
                                    }
                                    else {
                                        output("&bw"+text);
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
                    if (null == process || !process.isAlive()) {
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
