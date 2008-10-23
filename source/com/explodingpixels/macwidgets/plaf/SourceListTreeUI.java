package com.explodingpixels.macwidgets.plaf;

import com.explodingpixels.macwidgets.*;
import com.explodingpixels.painter.FocusStatePainter;
import com.explodingpixels.painter.RectanglePainter;
import com.explodingpixels.widgets.IconProvider;
import com.explodingpixels.widgets.TextProvider;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * A UI delegate that paints a {@link JTree} as an <a href="http://developer.apple.com/documentation/UserExperience/Conceptual/AppleHIGuidelines/XHIGWindows/chapter_18_section_4.html#//apple_ref/doc/uid/20000961-CHDDIGDE">Apple defined</a>
 * Source List. While this UI delegate can be directly installed on existing {@code JTree}s, it is
 * recommended that you use the {@link MacWidgetFactory#makeSourceList(javax.swing.JTree)} method
 * in conjuction with {@link MacWidgetFactory#createSourceListScrollPane(javax.swing.JComponent)}.
 * </p>
 * <p>
 * For the best development experience, it is recommended that you migrate your code to use the
 * {@link SourceList} with the {@link SourceListModel}, as this component abstracts away many of the
 * complexities of {@code JTree}.
 * </p>
 * <p>
 * Pictured below are the two different rendering styles of a Source List: focused and unfocused.
 * The corresponding {@code JTree}'s focusable property drives this rendering style.
 * </p>
 * <br>
 * <table>
 * <tr><td align="center"><img src="../../../../../graphics/iTunesSourceList.png"></td>
 * <td align="center"><img src="../../../../../graphics/MailSourceList.png"></td></tr>
 * <tr><td align="center"><font size="2" face="arial"><b>Focusable SourceList<b></font></td>
 * <td align="center"><font size="2" face="arial"><b>Non-focusable SourceList<b></font></td></tr>
 * </table>
 * <br>
 * <h3>Providing Category and Item text and icons</h3>
 * <p/>
 * During the rendering process, each Category and Item node will be consulted for the text to be
 * displayed. The renderer determines the text based on these prioritized checks:
 * <ol>
 * <li>If the node is an instance of {@link DefaultMutableTreeNode}, and the
 * {@link DefaultMutableTreeNode#getUserObject()} is an instance of {@link TextProvider}, then
 * the {@code TextProvider} will be queried for the node text.</li>
 * <li>If no implementation of {@code TextProvider} is found, the standard
 * {@link JTree#convertValueToText(Object, boolean, boolean, boolean, int, boolean)} method will
 * be consulted.</li>
 * </ol>
 * Also, during rendering, each Item node will be consulted for an icon. Similarly to the above
 * mechanism for determining text, the render determines a nodes icon by the following check:
 * <ol>
 * <li>If the node is an instance of {@link DefaultMutableTreeNode}, and the
 * {@link DefaultMutableTreeNode#getUserObject()} is an instance of {@link IconProvider}, then
 * the {@code IconProvider} will be queried for the node icon.</li>
 * </ol>
 */
public class SourceListTreeUI extends BasicTreeUI {

    private static final FocusStatePainter BACKGROUND_PAINTER = new FocusStatePainter(
            new RectanglePainter(MacColorUtils.SOURCE_LIST_FOCUSED_BACKGROUND_COLOR),
            new RectanglePainter(MacColorUtils.SOURCE_LIST_FOCUSED_BACKGROUND_COLOR),
            new RectanglePainter(MacColorUtils.SOURCE_LIST_UNFOCUSED_BACKGROUND_COLOR));

    private static final FocusStatePainter SELECTION_BACKGROUND_PAINTER = new FocusStatePainter(
            MacPainterFactory.createSourceListSelectionPainter_componentFocused(),
            MacPainterFactory.createSourceListSelectionPainter_windowFocused(),
            MacPainterFactory.createSourceListSelectionPainter_windowUnfocused());

    private static final Icon COLLAPSED_ICON = new ImageIcon(
            SourceList.class.getResource(
                    "/com/explodingpixels/macwidgets/images/group_list_right_arrow.png"));

    private static final Icon EXPANDED_ICON = new ImageIcon(
            SourceList.class.getResource(
                    "/com/explodingpixels/macwidgets/images/group_list_down_arrow.png"));

    private final String SELECT_NEXT = "selectNext";
    private final String SELECT_PREVIOUS = "selectPrevious";

    @Override
    protected void completeUIInstall() {
        super.completeUIInstall();

        tree.setSelectionModel(new SourceListTreeSelectionModel());

        tree.setOpaque(false);
        tree.setCellRenderer(new SourceListTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // TODO key height off font size.
        tree.setRowHeight(20);

        // install the collapsed and expanded icons as well as the margins to indent nodes.
        setCollapsedIcon(COLLAPSED_ICON);
        setExpandedIcon(EXPANDED_ICON);
        int indent = COLLAPSED_ICON.getIconWidth() / 2 + 4;
        setLeftChildIndent(indent);
        setRightChildIndent(indent);
    }

    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();
        tree.getInputMap().put(KeyStroke.getKeyStroke("pressed DOWN"), SELECT_NEXT);
        tree.getInputMap().put(KeyStroke.getKeyStroke("pressed UP"), SELECT_PREVIOUS);
        tree.getActionMap().put(SELECT_NEXT, createNextAction());
        tree.getActionMap().put(SELECT_PREVIOUS, createPreviousAction());
    }

    @Override
    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler() {
            @Override
            public Rectangle getNodeDimensions(
                    Object value, int row, int depth, boolean expanded, Rectangle size) {

                Rectangle dimensions = super.getNodeDimensions(value, row, depth, expanded, size);
                int containerWidth = tree.getParent() instanceof JViewport
                        ? tree.getParent().getWidth() : tree.getWidth();

                dimensions.width = containerWidth - getRowX(row, depth);

                return dimensions;
            }
        };
    }

    @Override
    public Rectangle getPathBounds(JTree tree, TreePath path) {
        Rectangle bounds = super.getPathBounds(tree, path);
        // if there are valid bounds for the given path, then stretch them to fill the entire width
        // of the tree. this allows repaints on focus events to follow the standard code path, and
        // still repaint the entire selected area.
        if (bounds != null) {
            bounds.x = 0;
            bounds.width = tree.getWidth();
        }
        return bounds;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        // TODO use c.getVisibleRect to trim painting to minimum rectangle.
        // paint the background for the tree.
        Graphics2D backgroundGraphics = (Graphics2D) g.create();
        BACKGROUND_PAINTER.paint(backgroundGraphics, c, c.getWidth(), c.getHeight());
        backgroundGraphics.dispose();

        // TODO use c.getVisibleRect to trim painting to minimum rectangle.
        // paint the background for the selected entry, if there is one.
        int selectedRow = getSelectionModel().getLeadSelectionRow();
        if (selectedRow >= 0 && tree.isVisible(tree.getPathForRow(selectedRow))) {

            Rectangle bounds = tree.getRowBounds(selectedRow);

            Graphics2D selectionBackgroundGraphics = (Graphics2D) g.create();
            selectionBackgroundGraphics.translate(0, bounds.y);
            SELECTION_BACKGROUND_PAINTER.paint(
                    selectionBackgroundGraphics, c, c.getWidth(), bounds.height);
            selectionBackgroundGraphics.dispose();
        }

        super.paint(g, c);
    }

    @Override
    protected void paintHorizontalLine(Graphics g, JComponent c, int y, int left, int right) {
        // do nothing - don't paint horizontal lines.
    }

    @Override
    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
                                          TreePath path) {
        // do nothing - don't paint vertical lines.
    }

    private Action createNextAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = tree.getLeadSelectionRow();
                int rowToSelect = selectedRow + 1;
                while (rowToSelect >= 0 && rowToSelect < tree.getRowCount()) {
                    if (isItemRow(rowToSelect)) {
                        tree.setSelectionRow(rowToSelect);
                        break;
                    } else {
                        rowToSelect++;
                    }
                }
            }
        };
    }

    private Action createPreviousAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = tree.getLeadSelectionRow();
                int rowToSelect = selectedRow - 1;
                while (rowToSelect >= 0 && rowToSelect < tree.getRowCount()) {
                    if (isItemRow(rowToSelect)) {
                        tree.setSelectionRow(rowToSelect);
                        break;
                    } else {
                        rowToSelect--;
                    }
                }
            }
        };
    }

    // Utility methods. ///////////////////////////////////////////////////////////////////////////

    private boolean isCategoryRow(int row) {
        return !isItemRow(row);
    }

    private boolean isItemRow(int row) {
        return isItemPath(tree.getPathForRow(row));
    }

    private boolean isItemPath(TreePath path) {
        return path.getPathCount() > 2;
    }

    private String getTextForNode(TreeNode node, boolean selected, boolean expanded, boolean leaf,
                                  int row, boolean hasFocus) {
        String retVal;

        if (node instanceof DefaultMutableTreeNode
                && ((DefaultMutableTreeNode) node).getUserObject() instanceof TextProvider) {
            Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
            retVal = ((TextProvider) userObject).getText();
        } else {
            retVal = tree.convertValueToText(node, selected, expanded, leaf, row, hasFocus);
        }

        return retVal;
    }

    private Icon getIconForNode(TreeNode node) {
        Icon retVal = null;
        if (node instanceof DefaultMutableTreeNode
                && ((DefaultMutableTreeNode) node).getUserObject() instanceof IconProvider) {
            Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
            retVal = ((IconProvider) userObject).getIcon();
        }
        return retVal;
    }

    // Custom TreeCellRenderer. ///////////////////////////////////////////////////////////////////

    private class SourceListTreeCellRenderer implements TreeCellRenderer {

        private CategoryTreeCellRenderer iCategoryRenderer = new CategoryTreeCellRenderer();

        private ItemTreeCellRenderer iITemRenderer = new ItemTreeCellRenderer();

        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {

            TreeCellRenderer render = isCategoryRow(row) ? iCategoryRenderer : iITemRenderer;
            return render.getTreeCellRendererComponent(
                    tree, value, selected, expanded, leaf, row, hasFocus);
        }

    }

    private class CategoryTreeCellRenderer implements TreeCellRenderer {

        private JLabel fLabel = MacWidgetFactory.makeEmphasizedLabel(new JLabel(),
                MacColorUtils.MAC_SOURCE_LIST_CATEGORY_FONT_COLOR,
                MacColorUtils.MAC_SOURCE_LIST_CATEGORY_FONT_COLOR,
                EmphasizedLabelUI.DEFAULT_EMPHASIS_COLOR);

        private CategoryTreeCellRenderer() {
            fLabel.setFont(MacFontUtils.SOURCE_LIST_CATEGORY_FONT);
        }

        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            TreeNode node = (TreeNode) value;
            fLabel.setText(getTextForNode(node, selected, expanded, leaf, row, hasFocus).toUpperCase());
            return fLabel;
        }
    }

    private class ItemTreeCellRenderer implements TreeCellRenderer {

        private PanelBuilder fBuilder;

        private SourceListCountBadgeRenderer fCountRenderer = new SourceListCountBadgeRenderer();

        private JLabel fSelectedLabel = MacWidgetFactory.makeEmphasizedLabel(new JLabel(),
                MacColorUtils.MAC_SOURCE_LIST_SELECTED_ITEM_FONT_COLOR,
                MacColorUtils.MAC_SOURCE_LIST_SELECTED_ITEM_FONT_COLOR,
                MacColorUtils.MAC_SOURCE_LIST_SELECTED_ITEM_FONT_SHADOW_COLOR);

        private JLabel fUnselectedLabel = MacWidgetFactory.makeEmphasizedLabel(new JLabel(),
                MacColorUtils.MAC_SOURCE_LIST_ITEM_FONT_COLOR,
                MacColorUtils.MAC_SOURCE_LIST_ITEM_FONT_COLOR,
                MacColorUtils.MAC_SOURCE_LIST_ITEM_FONT_SHADOW_COLOR);

        private ItemTreeCellRenderer() {
            fSelectedLabel.setFont(MacFontUtils.SOURCE_LIST_ITEM_SELECTED_FONT);
            fUnselectedLabel.setFont(MacFontUtils.SOURCE_LIST_ITEM_FONT);

            // definte the FormLayout columns and rows.
            FormLayout layout = new FormLayout("fill:0px:grow, 5px, p, 5px", "3px, fill:p:grow, 3px");
            // create the builders with our panels as the component to be filled.
            fBuilder = new PanelBuilder(layout);
            fBuilder.getPanel().setOpaque(false);
        }

        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            TreeNode node = (TreeNode) value;
            JLabel label = selected ? fSelectedLabel : fUnselectedLabel;
            label.setText(getTextForNode(node, selected, expanded, leaf, row, hasFocus));
            label.setIcon(getIconForNode(node));

            fBuilder.getPanel().removeAll();
            CellConstraints cc = new CellConstraints();
            fBuilder.add(label, cc.xywh(1, 1, 1, 3));

            if (value instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) value).getUserObject() instanceof SourceListBadgeContentProvider) {
                Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
                SourceListBadgeContentProvider badgeContentProvider =
                        (SourceListBadgeContentProvider) userObject;
                if (badgeContentProvider.getCounterValue() > 0) {
                    fBuilder.add(fCountRenderer.getComponent(), cc.xy(3, 2, "center, fill"));
                    fCountRenderer.setState(badgeContentProvider.getCounterValue(), selected);
                }
            }

            return fBuilder.getPanel();
        }
    }

    // SourceListTreeSelectionModel implementation. ///////////////////////////////////////////////

    private class SourceListTreeSelectionModel extends DefaultTreeSelectionModel {
        public SourceListTreeSelectionModel() {
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        private boolean canSelect(TreePath path) {
            return isItemPath(path);
        }

        @Override
        public void setSelectionPath(TreePath path) {
            if (canSelect(path)) {
                super.setSelectionPath(path);
            }
        }

        @Override
        public void setSelectionPaths(TreePath[] paths) {
            if (canSelect(paths[0])) {
                super.setSelectionPaths(paths);
            }
        }
    }

}
