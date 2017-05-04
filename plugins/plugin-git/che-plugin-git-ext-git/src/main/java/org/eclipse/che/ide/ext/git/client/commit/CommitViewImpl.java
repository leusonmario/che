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

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ext.git.client.tree.TreeView;
import org.eclipse.che.ide.ui.ShiftableTextArea;
import org.eclipse.che.ide.ui.window.Window;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;

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
        treeView.applyCheckBoxes();
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
}
