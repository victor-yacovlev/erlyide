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

package com.github.victoryacovlev.erlide.ui.buildlog;

import com.github.victoryacovlev.erlide.erlangtools.ErlErrorInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class BuildLogView extends JPanel {
    private final JTable table = new JTable();


    class LogTableModel extends DefaultTableModel {

        private final List<ErlErrorInfo> items = new LinkedList<>();

        LogTableModel() {
            super(new String[] {"Kind", "Message", "File", "Line"}, 4);
        }

        @Override
        public int getRowCount() {
            return null==items? 0 : items.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object result = null;
            if (rowIndex >= 0 && rowIndex < items.size()) {
                ErlErrorInfo item = items.get(rowIndex);
                if (0 == columnIndex) {
                    String level;
                    if (ErlErrorInfo.ERROR == item.type)
                        level = "Error";
                    else if (ErlErrorInfo.WARNNING == item.type)
                        level = "Warning";
                    else
                        level = "Info";
                    result = level;
                }
                else if (1 == columnIndex)
                    result = item.message;
                else if (2 == columnIndex) {
                    File f = new File(item.moduleFileName);
                    final String fileName = f.getName();
                    result = fileName;
                }
                else if (3 == columnIndex) {
                    result = Integer.toString(item.line);
                }
            }
            return result;
        }

        public void clear() {
            items.clear();
        }

        public void addItem(ErlErrorInfo errorInfo) {
            items.add(errorInfo);
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            // Do nothing to prevent editing
        }
    }

    private final LogTableModel tableModel = new LogTableModel();

    public BuildLogView() {
        super();
        setLayout(new BorderLayout());

        setMinimumSize(new Dimension(400, 100));

        table.setModel(tableModel);

        add(table.getTableHeader(), BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);

        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(200);
        table.getColumnModel().getColumn(2).setMinWidth(180);
    }

    public void clearLog() {
        tableModel.clear();
    }

    public void addItem(ErlErrorInfo errorInfo) {
        tableModel.addItem(errorInfo);
        table.updateUI();
    }
}
