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

package io.github.victoryacovlev.erlyide.ui;

import org.json.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

public class WorkspaceChooser extends JDialog {

    private static final String PREFS_WINDOW_GEOMETRY = "WorkspaceChooser/Geometry";
    private static final String PREFS_HISTORY = "WorkspaceChooser/History";
    public static final String PREFS_LAST = "WorkspaceChooser/Last";
    public static final String PREFS_SHOW_ON_LAUNCH = "WorkspaceChooser/ShowOnLaunch";
    private Preferences preferences = Preferences.userNodeForPackage(MainWindow.class);

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboBox;
    private JButton browseButton;
    private JCheckBox showThisDialogOnCheckBox;
    private String result;

    public String getSelectedDirectory() {
        return result;
    }

    public String getLastUsedDirectory() {
        return preferences.get(PREFS_LAST, "");
    }

    public WorkspaceChooser() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseLocation();
            }
        });
    }

    private void browseLocation() {
        String path = (String)comboBox.getSelectedItem();
        File f = new File(path);
        if (!f.exists() || !f.isDirectory()) {
            path = System.getProperty("user.home");
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select workspace location...");
        chooser.setCurrentDirectory(new File(path));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setControlButtonsAreShown(true);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            path = chooser.getSelectedFile().getAbsolutePath();
            int selectIndex = -1;
            for (int i=0; i<comboBox.getItemCount(); ++i) {
                final String existing = (String) comboBox.getItemAt(i);
                if (existing.equals(path)) {
                    selectIndex = i;
                    break;
                }
            }
            if (-1 == selectIndex) {
                comboBox.addItem(path);
                selectIndex = comboBox.getItemCount()-1;
            }
            comboBox.setSelectedIndex(selectIndex);
            comboBox.getEditor().selectAll();
        }
    }

    private void onOK() {
        saveSettings();
        result = (String)comboBox.getSelectedItem();
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    public boolean isShownOnLaunch() {
        boolean result = preferences.getBoolean(PREFS_SHOW_ON_LAUNCH, true);
        return result;
    }

    public void restoreSettings() {
        int x = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/x", 0);
        int y = preferences.getInt(PREFS_WINDOW_GEOMETRY + "/y", 0);
        boolean showOnLaunch = isShownOnLaunch();
        String historyJson = preferences.get(PREFS_HISTORY, "[]");
        JSONArray jsonArray = new JSONArray(historyJson);
        String last = preferences.get(PREFS_LAST, "");
        int lastIndex = -1;
        for (int i=0; i<jsonArray.length(); ++i) {
            String path = jsonArray.getString(i);
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                comboBox.addItem(path);
                if (path.equals(last))
                    lastIndex = comboBox.getItemCount()-1;
            }
        }
        if (-1 != lastIndex) {
            comboBox.setSelectedIndex(lastIndex);
        }
        if (0 == comboBox.getItemCount()) {
            final String defaultPath = System.getProperty("user.home")+"/ErlangWorkspace";
            comboBox.addItem(defaultPath);
        }
        comboBox.getEditor().selectAll();
        showThisDialogOnCheckBox.setSelected(showOnLaunch);
        setLocation(x, y);
    }

    private void saveSettings() {
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/x", getX());
        preferences.putInt(PREFS_WINDOW_GEOMETRY + "/y", getY());
        List<String> items = new LinkedList<>();
        for (int i=0; i<comboBox.getItemCount(); ++i) {
            items.add((String)comboBox.getItemAt(i));
        }
        JSONArray jsonArray = new JSONArray(items);
        String serialized = jsonArray.toString(4);
        preferences.put(PREFS_HISTORY, serialized);
        preferences.put(PREFS_LAST, (String)comboBox.getSelectedItem());
        preferences.putBoolean(PREFS_SHOW_ON_LAUNCH, showThisDialogOnCheckBox.isSelected());
    }

}
