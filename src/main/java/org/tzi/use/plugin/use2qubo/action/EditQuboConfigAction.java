package org.tzi.use.plugin.use2qubo.action;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.main.ViewFrame;
import org.tzi.use.plugin.use2qubo.qubo.QuboConfigPaths;
import org.tzi.use.plugin.use2qubo.ui.QuboConfigView;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.tzi.use.uml.mm.MModel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class EditQuboConfigAction implements IPluginActionDelegate {

    @Override
    public void performAction(IPluginAction pluginAction) {
        PluginLog.init(pluginAction.getParent().logWriter());
        MainWindow parent = pluginAction.getParent();
        File configFile;
        try {
            configFile = QuboConfigPaths.resolveConfigFile(pluginAction.getSession().system());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(),
                    "Edit QUBO Config", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MModel model = pluginAction.getSession().system().model();
        QuboConfigView view = new QuboConfigView(configFile, model);
        ViewFrame frame = new ViewFrame("Edit QUBO Config", view, null);
        JComponent content = (JComponent) frame.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(view, BorderLayout.CENTER);
        MainWindow.instance().addNewViewFrame(frame);
        frame.setVisible(true);
    }

    @Override
    public boolean shouldBeEnabled(IPluginAction pluginAction) {
        return pluginAction.getSession().hasSystem();
    }
}
