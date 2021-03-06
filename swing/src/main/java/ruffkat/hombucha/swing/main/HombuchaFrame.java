package ruffkat.hombucha.swing.main;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.JXRootPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.MultiSplitLayout;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.search.RecentSearches;
import org.jdesktop.swingx.search.Searchable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ruffkat.hombucha.swing.ferments.FermentTreeModel;
import ruffkat.hombucha.swing.ferments.FermentsPanel;
import ruffkat.hombucha.swing.items.ItemsPanel;
import ruffkat.hombucha.swing.mothers.MothersPanel;
import ruffkat.hombucha.swing.recipes.RecipesPanel;
import ruffkat.hombucha.swing.sources.SourcesPanel;
import ruffkat.hombucha.swing.vessels.VesselsPanel;
import ruffkat.swing.action.ActionRepository;
import ruffkat.swing.action.EmptyAction;
import ruffkat.swing.module.ModulePanel;
import ruffkat.swing.module.ModuleView;
import ruffkat.swing.statusbar.MemoryMeter;
import ruffkat.swing.statusbar.StatusDisplay;
import ruffkat.swing.task.BackgroundTask;
import ruffkat.swing.task.ModalHandler;
import ruffkat.swing.task.Task;
import ruffkat.swing.task.TaskListModel;
import ruffkat.swing.task.TaskListPanel;
import ruffkat.swing.ui.FindBar;
import ruffkat.swing.ui.InfiniteProgressPanel;
import ruffkat.swing.ui.UserInterface;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

@Component
public class HombuchaFrame
        extends JXFrame
        implements ModalHandler, StatusDisplay, Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());

    @Autowired
    private UserInterface ui;

    @Autowired
    @Qualifier("globalActions")
    private ActionRepository actions;

    @Autowired
    private Searchable searchable;

    @Autowired
    private FermentTreeModel fermentTreeModel;

    @Autowired
    private SourcesPanel sourcesPanel;

    @Autowired
    private MothersPanel mothersPanel;

    @Autowired
    private ItemsPanel itemsPanel;

    @Autowired
    private VesselsPanel vesselsPanel;

    @Autowired
    private RecipesPanel recipesPanel;

    @Autowired
    private FermentsPanel fermentsPanel;

    private TaskListModel taskListModel;
    private InfiniteProgressPanel glassPaneProgress;
    private JLabel statusBarMessage;
    private JFileChooser fileChooser;
    private FindBar findBar;

    public final void run() {
        try {
            prepareComponent();
        } catch (Exception e) {
            onError("Failure launching application", e);
            triggerShutdown();
        }
    }

    public final void prepareComponent()
            throws Exception {
        prepareSharedComponents();
        registerActions();
        JXRootPane rootPane = getRootPaneExt();
        rootPane.setGlassPane(buildGlassPane());
        rootPane.setContentPane(buildContentPane());
        rootPane.setStatusBar(buildStatusBar());
        rootPane.setJMenuBar(buildMenuBar());
        pack();
        setTitle(ui.string("HombuchaFrame.title"));
        setIconImage(ui.image("HombuchaFrame.image"));
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                populateFermentsTree();
            }
        });
        setVisible(true);
    }

    public void setStatus(String pattern, Object... arguments) {
        statusBarMessage.setText(MessageFormat.format(pattern, arguments));
    }

    public void allowInteraction() {
        if (SwingUtilities.isEventDispatchThread()) {
            doAllowInteraction();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    doAllowInteraction();
                }
            });
        }
    }

    public void blockInteraction() {
        if (SwingUtilities.isEventDispatchThread()) {
            doBlockInteraction();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    doBlockInteraction();
                }
            });
        }
    }

    public void onError(String message, Throwable error) {
        log.error(message, error);
        JXErrorPane.showDialog(this, new ErrorInfo("Error", message, error.getMessage(), null, error, null, null));
    }

    private void triggerShutdown() {
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void doAllowInteraction() {
        if (glassPaneProgress.isVisible()) {
            glassPaneProgress.setVisible(false);
        }
    }

    private void doBlockInteraction() {
        if (!glassPaneProgress.isVisible()) {
            glassPaneProgress.setVisible(true);
        }
    }

    private void registerActions() {
        // Application actions
        actions.register("application.exit", new AbstractActionExt() {
            public void actionPerformed(ActionEvent e) {
                triggerShutdown();
            }
        });
        // Menu actions
        actions.register("menu.file", new EmptyAction());
        actions.register("menu.edit", new EmptyAction());
        actions.register("menu.search", new EmptyAction());
        // Search actions
        actions.register("find.show", new AbstractActionExt() {
            public void actionPerformed(ActionEvent e) {
                findBar.setVisible(!findBar.isVisible());
            }
        });
    }

    private void prepareSharedComponents() {
        taskListModel = new TaskListModel();

        findBar = new FindBar(actions.getConfiguration(), searchable,
                new RecentSearches(preferences, "searches.recent"));
        findBar.setVisible(true);

        fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setCurrentDirectory(lastDirectory());
    }

    private JComponent buildGlassPane() {
        glassPaneProgress = new InfiniteProgressPanel(false);
        return glassPaneProgress;
    }

    private JComponent buildContentPane() {
        MultiSplitLayout.Leaf ferments = new MultiSplitLayout.Leaf("Ferments");
        ferments.setWeight(0.3);
        MultiSplitLayout.Leaf desktop = new MultiSplitLayout.Leaf("Desktop");
        desktop.setWeight(0.7);

        JXMultiSplitPane splitPane = new JXMultiSplitPane();
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        splitPane.setModel(new MultiSplitLayout.RowSplit(
                ferments,
                new MultiSplitLayout.Divider(),
                desktop));

        ModuleView fermentsView = new ModuleView(ui, buildFermentsPanel());
        fermentsView.setPreferredSize(new Dimension(200, 400));
        fermentsView.setActive(true);

        ModuleView desktopView = new ModuleView(ui, buildDesktopPanel());
        desktopView.setPreferredSize(new Dimension(500, 400));
        desktopView.setActive(true);

        splitPane.add(fermentsView.createTitledPanel(), "Ferments");
        splitPane.add(desktopView.createTitledPanel(), "Desktop");

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(splitPane, BorderLayout.CENTER);
        return contentPane;
    }

    private ModulePanel buildFermentsPanel() {
        JXTree fermentsTree = new JXTree();
        fermentsTree.setLeafIcon(ui.icon("domain.ferment.icon"));
        fermentsTree.setDragEnabled(true);
        fermentsTree.setExpandsSelectedPaths(true);
        fermentsTree.setRootVisible(false);
        fermentsTree.setShowsRootHandles(true);
        fermentsTree.setTransferHandler(new TransferHandler() {
        });
        fermentsTree.setModel(fermentTreeModel);

        ModulePanel modulePanel = new ModulePanel(ui, "HombuchaFrame.FermentsPanel.title");
        modulePanel.add(new JScrollPane(fermentsTree), BorderLayout.CENTER);
        return modulePanel;
    }

    private ModulePanel buildDesktopPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Sources", sourcesPanel);
        tabbedPane.addTab("Mothers", mothersPanel);
        tabbedPane.addTab("Items", itemsPanel);
        tabbedPane.addTab("Vessels", vesselsPanel);
        tabbedPane.addTab("Recipes", recipesPanel);
        tabbedPane.addTab("Ferments", fermentsPanel);

        ModulePanel modulePanel = new ModulePanel(ui, "HombuchaFrame.DesktopPanel.title");
        modulePanel.add(tabbedPane, BorderLayout.CENTER);
        return modulePanel;
    }

    private JXStatusBar buildStatusBar() {
        statusBarMessage = new JLabel(ui.string("StatusBar.ready"));

        JXStatusBar bar = new JXStatusBar();
        bar.setResizeHandleEnabled(false);
        bar.add(statusBarMessage,
                new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        bar.add(new TaskListPanel(ui, taskListModel, actions.getConfiguration()),
                new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FIXED));
        bar.add(new MemoryMeter(),
                new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FIXED));
        return bar;
    }

    private JMenuBar buildMenuBar() {
        List<List<String>> structure = new ArrayList<List<String>>();
        structure.add(Arrays.asList("menu.file", null, "application.exit"));
        structure.add(Arrays.asList("menu.edit"));
        structure.add(Arrays.asList("menu.search", "find.show"));
        return actions.getContainerFactory().createMenuBar(structure);
    }

    private Task<Void> populateFermentsTree() {
        Task<Void> task = new BackgroundTask<Void, Void>("Loading Brews", this) {
            @Override
            protected Void doInBackground() {
                fermentTreeModel.initialize();
                return null;
            }

            @Override
            protected void completed(Void result) {
                fermentTreeModel.fireTreeStructureChanged();
            }
        };
        taskListModel.register(task);
        task.execute();
        return task;
    }

    private File lastDirectory() {
        return new File(preferences.get("directory", "."));
    }

    private void lastDirectory(File directory) {
        preferences.put("directory", directory.getAbsolutePath());
    }
}
