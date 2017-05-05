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
package org.eclipse.che.ide.ext.git.client.commit;

import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus;
import org.eclipse.che.ide.ext.git.client.tree.TreeCallBack;
import org.eclipse.che.ide.ext.git.client.tree.TreeView;
import org.eclipse.che.ide.resource.Path;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * The view of {@link CommitPresenter}.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public interface CommitView extends View<CommitView.ActionDelegate> {
    /** Needs for delegate some function into Commit view. */
    interface ActionDelegate {
        /** Performs any actions appropriate in response to the user having pressed the Commit button. */
        void onCommitClicked();

        /** Performs any actions appropriate in response to the user having pressed the Cancel button. */
        void onCancelClicked();

        /** Performs any actions appropriate in response to the user having changed something. */
        void onValueChanged();

        /**
         * Set the commit message for an amend commit.
         */
        void setAmendCommitMessage();

        void onFileNodeCheckBoxValueChanged(Path path, boolean newCheckBoxValue);

        Set<String> getFilesToCommit();
    }

    /** @return entered message */
    @NotNull
    String getMessage();

    /**
     * Set content into message filesPanel.
     *
     * @param message
     *         text what need to insert
     */
    void setMessage(@NotNull String message);

    /** @return <code>true</code> if need to amend the last commit, and <code>false</code> otherwise */
    boolean isAmend();

    /**
     * Change the enable state of the commit button.
     *
     * @param enable
     *         <code>true</code> to enable the button, <code>false</code> to disable it
     */
    void setEnableCommitButton(boolean enable);

    /** Give focus to message filesPanel. */
    void focusInMessageField();

    /** Close dialog. */
    void close();

    /** Show dialog. */
    void showDialog();

    void setTreeView(TreeView treeView);
}
