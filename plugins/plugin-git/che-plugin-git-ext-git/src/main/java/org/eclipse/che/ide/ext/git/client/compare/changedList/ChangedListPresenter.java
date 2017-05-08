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
package org.eclipse.che.ide.ext.git.client.compare.changedList;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.ext.git.client.compare.ComparePresenter;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus.Status;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFileNode;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFolderNode;
import org.eclipse.che.ide.ext.git.client.tree.TreeCallBack;
import org.eclipse.che.ide.ext.git.client.tree.TreePresenter;
import org.eclipse.che.ide.resource.Path;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.NOT_EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;

/**
 * Presenter for displaying list of changed files.
 *
 * @author Igor Vinokur
 * @author Vlad Zhukovskyi
 */
@Singleton
public class ChangedListPresenter implements ChangedListView.ActionDelegate {
    private final ChangedListView     view;
    private final NotificationManager notificationManager;
    private final TreePresenter       treePresenter;
    private final ComparePresenter    comparePresenter;
    private final TreeCallBack        callBack;

    private Project project;
    private String  file;
    private String  revisionA;
    private String  revisionB;
    private Status  status;

    @Inject
    public ChangedListPresenter(final ChangedListView view,
                                ComparePresenter comparePresenter,
                                NotificationManager notificationManager,
                                TreePresenter treePresenter) {
        this.comparePresenter = comparePresenter;
        this.view = view;
        this.notificationManager = notificationManager;
        this.treePresenter = treePresenter;
        this.view.setDelegate(this);

        callBack = new TreeCallBack() {
            @Override
            public void onNodeSelected(Node node) {
                if (node instanceof ChangedFolderNode) {
                    ChangedListPresenter.this.view.setEnableCompareButton(false);
                    return;
                }
                ChangedListPresenter.this.view.setEnableCompareButton(true);
                ChangedListPresenter.this.file = node.getName();
                ChangedListPresenter.this.status = ((ChangedFileNode)node).getStatus();
            }

            @Override
            public void onTreeRendered(Set<Path> paths) {
                //ignore
            }
        };

        this.view.setTreeView(treePresenter.getView());
    }

    /**
     * Show window with changed files.
     *
     * @param changedFiles
     *         Map with files and their status
     * @param revisionA
     *         hash of the first revision or branch.
     *         If it is set to {@code null}, compare with empty repository state will be performed
     * @param revisionB
     *         hash of the second revision or branch.
     *         If it is set to {@code null}, compare with latest repository state will be performed
     */
    public void show(Map<String, Status> changedFiles, @Nullable String revisionA, @Nullable String revisionB, Project project) {
        this.project = project;
        this.revisionA = revisionA;
        this.revisionB = revisionB;

        view.setEnableCompareButton(false);

        treePresenter.show(changedFiles, callBack);

        view.showDialog();
    }

    @Override
    public void onCloseClicked() {
        view.close();
    }

    @Override
    public void onCompareClicked() {
        showCompare();
    }

    private void showCompare() {
        if (revisionB == null) {
            project.getFile(file)
                   .then(new Operation<Optional<File>>() {
                       @Override
                       public void apply(Optional<File> file) throws OperationException {
                           if (file.isPresent()) {
                               comparePresenter.showCompareWithLatest(file.get(), status, revisionA);
                           }
                       }
                   })
                   .catchError(new Operation<PromiseError>() {
                       @Override
                       public void apply(PromiseError error) throws OperationException {
                           notificationManager.notify(error.getMessage(), FAIL, NOT_EMERGE_MODE);
                       }
                   });
        } else {
            comparePresenter.showCompareBetweenRevisions(Path.valueOf(file), status, revisionA, revisionB);
        }
    }
}
