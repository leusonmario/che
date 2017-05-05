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
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFileNode;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFolderNode;
import org.eclipse.che.ide.ext.git.client.tree.TreeView;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.ShiftableTextArea;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.window.Window;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The implementation of {@link CommitView}.
 *
 * @author Andrey Plotnikov
 */
@Singleton
public class CommitViewImpl extends Window implements CommitView {
    interface CommitViewImplUiBinder extends UiBinder<Widget, CommitViewImpl> {
    }

    private static CommitViewImplUiBinder uiBinder = GWT.create(CommitViewImplUiBinder.class);

    @UiField(provided = true)
    final TextArea message;
    @UiField
    FlowPanel filesPanel;
    @UiField
    CheckBox  amend;
    @UiField(provided = true)
    final GitResources            res;
    @UiField(provided = true)
    final GitLocalizationConstant locale;

    private Button         btnCommit;
    private Button         btnCancel;
    private ActionDelegate delegate;

    /**
     * Create view.
     */
    @Inject
    protected CommitViewImpl(GitResources res,
                             GitLocalizationConstant locale) {
        this.res = res;
        this.locale = locale;
        this.message = new ShiftableTextArea();
        this.ensureDebugId("git-commit-window");

        this.setTitle(locale.commitTitle());

        Widget widget = uiBinder.createAndBindUi(this);
        this.setWidget(widget);

        btnCancel = createButton(locale.buttonCancel(), "git-commit-cancel", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        btnCommit = createButton(locale.buttonCommit(), "git-commit-commit", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onCommitClicked();
            }
        });
        btnCommit.addStyleName(resources.windowCss().primaryButton());

        addButtonToFooter(btnCommit);
        addButtonToFooter(btnCancel);
    }

    @Override
    protected void onEnterClicked() {
        if (isWidgetFocused(btnCommit)) {
            delegate.onCommitClicked();
            return;
        }

        if (isWidgetFocused(btnCancel)) {
            delegate.onCancelClicked();
        }
    }

    @NotNull
    @Override
    public String getMessage() {
        return message.getText();
    }

    @Override
    public void setMessage(@NotNull String message) {
        this.message.setText(message);
    }

    @Override
    public boolean isAmend() {
        return amend.getValue();
    }

    @Override
    public void setEnableCommitButton(boolean enable) {
        btnCommit.setEnabled(enable);
    }

    @Override
    public void focusInMessageField() {
        new Timer() {
            @Override
            public void run() {
                message.setFocus(true);
            }
        }.schedule(300);
    }

    @Override
    public void close() {
        this.hide();
    }

    @Override
    public void showDialog() {
        this.show();
    }

    @Override
    public void setTreeView(TreeView treeView) {
        treeView.setTreeRender(new ChangedListRender(treeView));
        this.filesPanel.add(treeView);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("message")
    public void onMessageChanged(KeyUpEvent event) {
        delegate.onValueChanged();
    }

    @UiHandler("amend")
    public void onAmendValueChange(final ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            this.delegate.setAmendCommitMessage();
        } else {
            this.message.setValue("");
        }
        delegate.onValueChanged();
    }
    private class ChangedListRender extends DefaultPresentationRenderer<Node> {

        private final TreeView   treeView;
        private final Set<Path>  unselectedPaths;
        private final List<Path> allPaths;

        ChangedListRender(TreeView treeView) {
            super(treeView.getTreeStyles());
            this.treeView = treeView;
            this.unselectedPaths = new HashSet<>();
            this.allPaths = treeView.getPaths();
        }

        @Override
        public Element render(final Node node, final String domID, final Tree.Joint joint, final int depth) {
            Element rootContainer = super.render(node, domID, joint, depth);
            Element nodeContainer = rootContainer.getFirstChildElement();
            final Element checkBoxElement = new CheckBox().getElement();
            final InputElement checkBoxInputElement = (InputElement)checkBoxElement.getElementsByTagName("input").getItem(0);
            final Path nodePath = Path.valueOf(node instanceof ChangedFolderNode ? ((ChangedFolderNode)node).getPath() : node.getName());
            checkBoxInputElement.setChecked(!unselectedPaths.contains(nodePath));
            Event.sinkEvents(checkBoxElement, Event.ONCLICK);
            Event.setEventListener(checkBoxElement, new EventListener() {
                @Override
                public void onBrowserEvent(Event event) {
                    if (Event.ONCLICK == event.getTypeInt() && event.getTarget().getTagName().equalsIgnoreCase("label")) {
                        if (node instanceof ChangedFileNode) {
                            delegate.onFileNodeCheckBoxValueChanged(Path.valueOf(node.getName()), !checkBoxInputElement.isChecked());
                        }

                        List<Path> paths = new ArrayList<>(allPaths);
                        Collections.sort(paths, new Comparator<Path>() {
                            @Override
                            public int compare(Path path1, Path path2) {
                                return path1.toString().compareTo(path2.toString());
                            }
                        });

                        for (Path path : paths) {
                            if (path.equals(nodePath) || path.isEmpty()) {
                                continue;
                            }
                            if (path.isPrefixOf(nodePath) && !hasSelectedChildes(path)) {
                                updateFilesList(checkBoxInputElement.isChecked(), path);
                            }
                        }

                        Collections.sort(paths, new Comparator<Path>() {
                            @Override
                            public int compare(Path o1, Path o2) {
                                return o2.toString().compareTo(o1.toString());
                            }
                        });

                        for (Path path : paths) {
                            if (path.isEmpty()) {
                                continue;
                            }
                            if (nodePath.isPrefixOf(path)) {
                                updateFilesList(checkBoxInputElement.isChecked(), path);
                            } else if (path.isPrefixOf(nodePath) && !hasSelectedChildes(path)) {
                                updateFilesList(checkBoxInputElement.isChecked(), path);
                            }
                        }

                        treeView.refreshNodes();
                        delegate.onValueChanged();
                    }
                }
            });

            nodeContainer.insertAfter(checkBoxElement, nodeContainer.getFirstChild());
            return rootContainer;
        }

        private void updateFilesList(boolean checkBoxValue, Path path) {
            if (checkBoxValue) {
                unselectedPaths.add(path);
            } else {
                unselectedPaths.remove(path);
            }
            if (delegate.getFilesToCommit().contains(path.toString())) {
                delegate.onFileNodeCheckBoxValueChanged(path, !checkBoxValue);
            }
        }

        private boolean hasSelectedChildes(Path givenPath) {
            for (Path path : allPaths) {
                if (givenPath.isPrefixOf(path) && !path.equals(givenPath) && !unselectedPaths.contains(path)) {
                    return true;
                }
            }
            return false;
        }
    }
}
