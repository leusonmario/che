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

import com.google.common.base.Optional;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.compare.ComparePresenter;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus.Status;
import org.eclipse.che.ide.resource.Path;

import javax.validation.constraints.NotNull;
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
public class TreePresenter implements TreeView.ActionDelegate {
    private final TreeView                view;
    private final AppContext              appContext;
    private final NotificationManager     notificationManager;
    private final ComparePresenter        comparePresenter;
    private final GitLocalizationConstant locale;

    private Map<String, Status> changedFiles;
    private TreeCallBack        callBack;
    private boolean             treeViewEnabled;

    @Inject
    public TreePresenter(GitLocalizationConstant locale,
                         TreeView view,
                         AppContext appContext,
                         NotificationManager notificationManager,
                         ComparePresenter comparePresenter) {
        this.locale = locale;
        this.view = view;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.comparePresenter = comparePresenter;
        this.view.setDelegate(this);
    }

    /**
     * Show window with changed files.
     *
     * @param changedFiles
     *         Map with files and their status
     */
    public void show(Map<String, Status> changedFiles, @Nullable TreeCallBack callBack) {
        this.changedFiles = changedFiles;
        this.callBack = callBack;
        view.setEnableExpandCollapseButtons(treeViewEnabled);

        viewChangedFiles();
    }

    public TreeView getView() {
        return view;
    }

    public Set<Path> getSelected() {
        return null;
    }

    @Override
    public void onFileNodeDoubleClicked(String file, final Status status) {
        appContext.getRootProject()
                  .getFile(file)
                  .then(new Operation<Optional<File>>() {
                      @Override
                      public void apply(Optional<File> file) throws OperationException {
                          if (file.isPresent()) {
                              comparePresenter.showCompareWithLatest(file.get(), status, "HEAD");
                          }
                      }
                  })
                  .catchError(new Operation<PromiseError>() {
                      @Override
                      public void apply(PromiseError error) throws OperationException {
                          notificationManager.notify(error.getMessage(), FAIL, NOT_EMERGE_MODE);
                      }
                  });
    }

    @Override
    public void onChangeViewModeButtonClicked() {
        treeViewEnabled = !treeViewEnabled;
        viewChangedFiles();
        view.setEnableExpandCollapseButtons(treeViewEnabled);
    }

    @Override
    public void onExpandButtonClicked() {
        view.expandAllDirectories();
    }

    @Override
    public void onCollapseButtonClicked() {
        view.collapseAllDirectories();
    }

    @Override
    public void onNodeSelected(@NotNull Node node) {
        if (callBack != null) {
            callBack.onNodeSelected(node);
        }
    }

    @Override
    public void onPanelrendered(Set<Path> paths) {
        callBack.onTreeRendered(paths);
    }

    private void viewChangedFiles() {
        if (treeViewEnabled) {
            view.viewChangedFilesAsTree(changedFiles);
            view.setTextToChangeViewModeButton(locale.changeListRowListViewButtonText());
        } else {
            view.viewChangedFilesAsList(changedFiles);
            view.setTextToChangeViewModeButton(locale.changeListGroupByDirectoryButtonText());
        }
    }
}
