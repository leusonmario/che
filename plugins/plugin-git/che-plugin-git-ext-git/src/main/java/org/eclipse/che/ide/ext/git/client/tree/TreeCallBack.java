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
import org.eclipse.che.ide.resource.Path;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public interface TreeCallBack {
    void onNodeSelected(Node node);

    void onTreeRendered(Set<Path> paths);
}
