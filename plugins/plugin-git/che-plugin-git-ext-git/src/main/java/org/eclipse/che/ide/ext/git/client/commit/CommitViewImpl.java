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

import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFileNode;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFolderNode;
import org.eclipse.che.ide.ext.git.client.tree.TreeView;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.ShiftableTextArea;
import org.eclipse.che.ide.ui.dropdown.DropdownList;
import org.eclipse.che.ide.ui.dropdown.DropdownListItem;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.window.Window;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
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

    private ListBox  listBox;
    private CheckBox pushToRemote;

    private Button         btnCommit;
    private Button         btnCancel;
    private ActionDelegate delegate;

    private ChangedListRender render;

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

        FlowPanel pushPanel = new FlowPanel();
        listBox = new ListBox(false);
        listBox.setEnabled(false);
        listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                delegate.onDropdownSelected(listBox.getSelectedValue());
            }
        });

        pushToRemote = new CheckBox();
        pushToRemote.setHTML("Push commited changes ");
        pushToRemote.addStyleName(resources.windowCss().center());
        pushToRemote.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                listBox.setEnabled(event.getValue());
                delegate.onPushToRemoteCheBoxSelected(event.getValue());
            }
        });

        pushPanel.add(pushToRemote);
        pushPanel.add(listBox);
        getFooter().add(pushPanel);

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
    public void setUnselected(Set<Path> paths) {
        for (Path path : paths) {
            render.setNodeCheckBoxSelection(path, false);
        }
    }

    @Override
    public void setAllPaths(Set<Path> paths) {
        render.setAllPaths(paths);
    }

    @Override
    public void setMessage(@NotNull String message) {
        this.message.setText(message);
    }

    @Override
    public void addToDropdown(List<Remote> remotes) {
        listBox.clear();
        for (Remote remote : remotes) {
            listBox.addItem(remote.getName());
        }
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
        render.clearUnselected();
        this.show();
    }

    @Override
    public void setTreeView(TreeView treeView) {
        this.render = new ChangedListRender(treeView);
        treeView.setTreeRender(render);
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

        private final TreeView  treeView;
        private final Set<Path> unselectedPaths;

        private Set<Path> allPaths;

        ChangedListRender(TreeView treeView) {
            super(treeView.getTreeStyles());
            this.treeView = treeView;
            this.unselectedPaths = new HashSet<>();
            this.allPaths = new HashSet<>();
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

                        setNodeCheckBoxSelection(nodePath, checkBoxInputElement.isChecked());

                        treeView.refreshNodes();
                        delegate.onValueChanged();
                    }
                }
            });

            nodeContainer.insertAfter(checkBoxElement, nodeContainer.getFirstChild());
            return rootContainer;
        }

        public void setNodeCheckBoxSelection(Path nodePath, boolean isChecked) {
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
                    saveSelection(isChecked, path);
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
                    saveSelection(isChecked, path);
                } else if (path.isPrefixOf(nodePath) && !hasSelectedChildes(path)) {
                    saveSelection(isChecked, path);
                }
            }
        }

        private void saveSelection(boolean checkBoxValue, Path path) {
            if (checkBoxValue) {
                unselectedPaths.add(path);
            } else {
                unselectedPaths.remove(path);
            }
            if (delegate.getChangedFiles().contains(path.toString())) {
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

        public void setAllPaths(Set<Path> allPaths) {
            this.allPaths = allPaths;
        }

        public void clearUnselected() {
            unselectedPaths.clear();
            unselectedPaths.addAll(allPaths);
        }
    }
}
