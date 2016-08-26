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

package com.github.victoryacovlev.erlide.ui.sidebar;

import javax.swing.*;
import java.awt.*;

public class SidebarButton extends JToggleButton {

    private boolean vertical;

    public SidebarButton(String text, boolean vertical) {
        super(text);
        this.vertical = vertical;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Color bgColor = UIManager.getColor("Panel.background");
        super.paintComponent(g2);
    }

    @Override
    public Dimension getPreferredSize() {
        return rotateSize(super.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
        return rotateSize(super.getMinimumSize());
    }

    private Dimension rotateSize(Dimension s) {
        return new Dimension(s.height, s.width);
    }
}
