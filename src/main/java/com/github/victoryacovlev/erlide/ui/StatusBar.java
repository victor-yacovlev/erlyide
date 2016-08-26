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

package com.github.victoryacovlev.erlide.ui;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

public class StatusBar extends JPanel {

    JLabel leftSide = new JLabel(" ");
    JLabel centerSide = new JLabel(" ");
    JPanel rightSide = new JPanel();

    StatusBar() {
        leftSide.setBorder(new BevelBorder(BevelBorder.RAISED));
        centerSide.setBorder(new BevelBorder(BevelBorder.RAISED));
        rightSide.setBorder(new BevelBorder(BevelBorder.RAISED));

        leftSide.setHorizontalAlignment(SwingConstants.CENTER);
        centerSide.setHorizontalAlignment(SwingConstants.CENTER);

        leftSide.setPreferredSize(new Dimension(200, 0));
        rightSide.setPreferredSize(new Dimension(100, 0));
        rightSide.setLayout(new BorderLayout());

        setBorder(new BevelBorder(BevelBorder.LOWERED));
        setLayout(new BorderLayout());
        add(leftSide, BorderLayout.WEST);
        add(rightSide, BorderLayout.EAST);
        add(centerSide, BorderLayout.CENTER);

        createMemoryUsageMonitor(leftSide);
    }

    private void createMemoryUsageMonitor(JLabel target) {
        final Runtime runtime = Runtime.getRuntime();
        new Thread(() -> {
            while (true) {
                long total = runtime.totalMemory();
                long free = runtime.freeMemory();
                long used = total - free;
                String message = String.format("Runtime uses %1$d of %2$d Mb", used/(1024*1024), total/(1024*1024));
                leftSide.setText(message);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void setRightComponent(Component c) {
        rightSide.add(c, BorderLayout.CENTER);
    }

}
