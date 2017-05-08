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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.ErrorCodes;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.dialogs.DialogFactory;
import org.eclipse.che.ide.api.git.GitServiceClient;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.ext.git.client.DateTimeFormatter;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus.Status;
import org.eclipse.che.ide.ext.git.client.outputconsole.GitOutputConsole;
import org.eclipse.che.ide.ext.git.client.outputconsole.GitOutputConsoleFactory;
import org.eclipse.che.ide.ext.git.client.tree.TreeCallBack;
import org.eclipse.che.ide.ext.git.client.tree.TreePresenter;
import org.eclipse.che.ide.processes.panel.ProcessesPanelPresenter;
import org.eclipse.che.ide.resource.Path;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.che.api.git.shared.DiffType.NAME_STATUS;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.NOT_EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.ext.git.client.compare.FileStatus.defineStatus;
import static org.eclipse.che.ide.util.ExceptionUtils.getErrorCode;

/**
 * Presenter for commit changes on git.
 *
 * @author Ann Zhuleva
 * @author Vlad Zhukovskyi
 * @author Igor Vinokur
 */
@Singleton
public class CommitPresenter implements CommitView.ActionDelegate {
    private static final String COMMIT_COMMAND_NAME = "Git commit";

    private final TreePresenter           treePresenter;
    private final DialogFactory           dialogFactory;
    private final AppContext              appContext;
    private final CommitView              view;
    private final GitServiceClient        service;
    private final GitLocalizationConstant constant;
    private final NotificationManager     notificationManager;
    private final DateTimeFormatter       dateTimeFormatter;
    private final GitOutputConsoleFactory gitOutputConsoleFactory;
    private final ProcessesPanelPresenter consolesPanelPresenter;

    private Project      project;
    private Set<String>  allFiles;
    private List<String> checkedFiles;
    private TreeCallBack treeCallBack;
    private List<Remote> remotes;
    private String       selectedRemote;
    private boolean      pushAfterCommit;

    @Inject
    public CommitPresenter(final CommitView view,
                           GitServiceClient service,
                           TreePresenter treePresenter,
                           GitLocalizationConstant constant,
                           NotificationManager notificationManager,
                           DialogFactory dialogFactory,
                           AppContext appContext,
                           DateTimeFormatter dateTimeFormatter,
                           GitOutputConsoleFactory gitOutputConsoleFactory,
                           ProcessesPanelPresenter processesPanelPresenter) {
        this.view = view;
        this.treePresenter = treePresenter;
        this.dialogFactory = dialogFactory;
        this.appContext = appContext;
        this.dateTimeFormatter = dateTimeFormatter;
        this.gitOutputConsoleFactory = gitOutputConsoleFactory;
        this.consolesPanelPresenter = processesPanelPresenter;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.notificationManager = notificationManager;

        this.checkedFiles = new ArrayList<>();
        this.view.setTreeView(treePresenter.getView());
    }

    public void showDialog(Project project) {
        this.project = project;

        view.setEnableCommitButton(!view.getMessage().isEmpty());
        view.showDialog();
        view.focusInMessageField();
        actionPerformed();
    }

    public void actionPerformed() {

        final Project project = appContext.getRootProject();

        checkState(project != null, "Null project occurred");
        checkState(project.getLocation().isPrefixOf(appContext.getResource().getLocation()),
                   "Given selected item is not descendant of given project");


        service.diff(appContext.getDevMachine(),
                     project.getLocation(),
                     null,
                     NAME_STATUS, false, 0, "HEAD", false)
               .then(new Operation<String>() {
                   @Override
                   public void apply(String diff) throws OperationException {
                       if (diff.isEmpty()) {
                           dialogFactory.createMessageDialog(constant.compareMessageIdenticalContentTitle(),
                                                             constant.compareMessageIdenticalContentText(), null).show();
                       } else {
                           final String[] changedFiles = diff.split("\n");
                           Map<String, Status> items = new HashMap<>();
                           for (String item : changedFiles) {
                               items.put(item.substring(2, item.length()), defineStatus(item.substring(0, 1)));
                           }
                           CommitPresenter.this.checkedFiles.clear();
                           CommitPresenter.this.checkedFiles.addAll(items.keySet());
                           CommitPresenter.this.allFiles = items.keySet();

                           treeCallBack = new TreeCallBack() {
                               @Override
                               public void onNodeSelected(Node node) {
                                   //ignore
                               }

                               @Override
                               public void onTreeRendered(Set<Path> paths) {
                                   view.setAllPaths(paths);

                                   Set<Path> unselectedPaths = new HashSet<>();
                                   for (Resource resource : appContext.getResources()) {
                                       unselectedPaths.add(resource.getLocation().removeFirstSegments(1));
                                   }
                                   view.setUnselected(unselectedPaths);
                               }
                           };

                           treePresenter.show(items, treeCallBack);
                       }
                   }
               })
               .catchError(new Operation<PromiseError>() {
                   @Override
                   public void apply(PromiseError arg) throws OperationException {
                       notificationManager.notify(constant.diffFailed(), FAIL, NOT_EMERGE_MODE);
                   }
               });

        service.remoteList(appContext.getDevMachine(), project.getLocation(), null, true)
               .then(new Operation<List<Remote>>() {
                   @Override
                   public void apply(List<Remote> remotes) throws OperationException {
                       CommitPresenter.this.remotes = remotes;
                       view.addToDropdown(remotes);
                   }
               })
               .catchError(new Operation<PromiseError>() {
                   @Override
                   public void apply(PromiseError error) throws OperationException {
                       handleError(error.getCause());
                   }
               });
    }

    @Override
    public void onCommitClicked() {
        service.add(appContext.getDevMachine(), appContext.getRootProject().getLocation(), false, toRelativePaths())
               .then(new Operation<Void>() {
                   @Override
                   public void apply(Void arg) throws OperationException {
                       service.commit(appContext.getDevMachine(),
                                      project.getLocation(),
                                      view.getMessage(),
                                      false,
                                      toRelativePaths(),
                                      view.isAmend())
                              .then(new Operation<Revision>() {
                                  @Override
                                  public void apply(Revision revision) throws OperationException {
                                      onCommitSuccess(revision);
                                      view.close();
                                  }
                              });
                   }
               })
               .catchError(new Operation<PromiseError>() {
                   @Override
                   public void apply(PromiseError error) throws OperationException {
                       handleError(error.getCause());
                       view.close();
                   }
               });
    }

    private Path[] toRelativePaths() {
        final Path[] paths = new Path[checkedFiles.size()];

        for (int i = 0; i < checkedFiles.size(); i++) {
            paths[i] = Path.valueOf(checkedFiles.get(i));
        }

        return paths;
    }

    private void onCommitSuccess(@NotNull final Revision revision) {
        String date = dateTimeFormatter.getFormattedDate(revision.getCommitTime());
        String message = constant.commitMessage(revision.getId(), date);

        if ((revision.getCommitter() != null && revision.getCommitter().getName() != null &&
             !revision.getCommitter().getName().isEmpty())) {
            message += " " + constant.commitUser(revision.getCommitter().getName());
        }
        GitOutputConsole console = gitOutputConsoleFactory.create(COMMIT_COMMAND_NAME);
        console.print(message);
        consolesPanelPresenter.addCommandOutput(appContext.getDevMachine().getId(), console);
        notificationManager.notify(message);
        view.setMessage("");


    }

    private void handleError(@NotNull Throwable exception) {
        if (exception instanceof ServerException &&
            ((ServerException)exception).getErrorCode() == ErrorCodes.NO_COMMITTER_NAME_OR_EMAIL_DEFINED) {
            dialogFactory.createMessageDialog(constant.commitTitle(), constant.committerIdentityInfoEmpty(), null).show();
            return;
        }
        String exceptionMessage = exception.getMessage();
        String errorMessage = (exceptionMessage != null && !exceptionMessage.isEmpty()) ? exceptionMessage : constant.commitFailed();
        GitOutputConsole console = gitOutputConsoleFactory.create(COMMIT_COMMAND_NAME);
        console.printError(errorMessage);
        consolesPanelPresenter.addCommandOutput(appContext.getDevMachine().getId(), console);
        notificationManager.notify(constant.commitFailed(), errorMessage, FAIL, FLOAT_MODE);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    @Override
    public void onValueChanged() {
        boolean amend = view.isAmend();
        view.setEnableCommitButton(!view.getMessage().isEmpty() && (!checkedFiles.isEmpty() || amend));
    }

    @Override
    public void onDropdownSelected(String remote) {
        this.selectedRemote = remote;
    }

    @Override
    public void onPushToRemoteCheBoxSelected(boolean value) {
        pushAfterCommit = value;
    }

    @Override
    public void setAmendCommitMessage() {
        service.log(appContext.getDevMachine(), project.getLocation(), null, false)
               .then(new Operation<LogResponse>() {
                   @Override
                   public void apply(LogResponse log) throws OperationException {
                       final List<Revision> commits = log.getCommits();
                       String message = "";
                       if (commits != null && (!commits.isEmpty())) {
                           final Revision tip = commits.get(0);
                           if (tip != null) {
                               message = tip.getMessage();
                           }
                       }
                       CommitPresenter.this.view.setMessage(message);
                       CommitPresenter.this.view.setEnableCommitButton(!message.isEmpty());
                   }
               })
               .catchError(new Operation<PromiseError>() {
                   @Override
                   public void apply(PromiseError error) throws OperationException {
                       if (getErrorCode(error.getCause()) == ErrorCodes.INIT_COMMIT_WAS_NOT_PERFORMED) {
                           dialogFactory.createMessageDialog(constant.commitTitle(),
                                                             constant.initCommitWasNotPerformed(),
                                                             null).show();
                       } else {
                           CommitPresenter.this.view.setMessage("");
                           notificationManager.notify(constant.logFailed(), FAIL, NOT_EMERGE_MODE);
                       }
                   }
               });
    }

    @Override
    public void onFileNodeCheckBoxValueChanged(Path path, boolean newCheckBoxValue) {
        if (newCheckBoxValue) {
            checkedFiles.add(path.toString());
        } else {
            checkedFiles.remove(path.toString());
        }
    }

    @Override
    public Set<String> getChangedFiles() {
        return allFiles;
    }
}
