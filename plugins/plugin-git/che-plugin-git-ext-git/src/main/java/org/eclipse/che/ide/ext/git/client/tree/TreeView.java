/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.git.client.tree;

import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus.Status;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.smartTree.TreeStyles;
import org.eclipse.che.ide.ui.smartTree.presentation.PresentationRenderer;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The view of {@link TreePresenter}.
 *
 * @author Igor Vinokur
 */
public interface TreeView extends View<TreeView.ActionDelegate> {
    /** Needs for delegate some function into Changed list view. */
    interface ActionDelegate {

        /**
         * Performs any actions appropriate in response to the user having pressed the button that changes view mode of changed files.
         */
        void onChangeViewModeButtonClicked();

        /** Performs any actions appropriate in response to the user having pressed the 'Expand all directories' button. */
        void onExpandButtonClicked();

        /** Performs any actions appropriate in response to the user having pressed the 'Collapse all directories' button. */
        void onCollapseButtonClicked();

        /**
         * Performs any action in response to the user having select any node.
         *
         * @param node
         *         selected node
         */
        void onNodeSelected(Node node);

        void onPanelrendered(Set<Path> nodes);

        /** Performs any actions appropriate in response to the user double clicked on the file node. */
        void onFileNodeDoubleClicked(String file, Status status);
    }

    /**
     * View changed files as list.
     *
     * @param files
     *         Map of changed files with their status
     */
    void viewChangedFilesAsList(@NotNull Map<String, Status> files);

    /**
     * View changed files as tree.
     *
     * @param files
     *         Map of changed files with their status
     */
    void viewChangedFilesAsTree(@NotNull Map<String, Status> files);

    /** Expand all directories in tree. */
    void expandAllDirectories();

    /** Collapse all directories in tree. */
    void collapseAllDirectories();

    /**
     * Change the enable state of the 'Expand/Collapse all directories' buttons.
     *
     * @param enabled
     *         <code>true</code> to enable the buttons, <code>false</code> to disable them
     */
    void setEnableExpandCollapseButtons(boolean enabled);

    /**
     * Set displayed text to button that changes view mode of changed files.
     *
     * @param text
     *         text that will be displayed in the button
     */
    void setTextToChangeViewModeButton(@NotNull String text);

    void setTreeRender(PresentationRenderer render);

    TreeStyles getTreeStyles();

    void refreshNodes();

    Set<Path> getPaths();
}
