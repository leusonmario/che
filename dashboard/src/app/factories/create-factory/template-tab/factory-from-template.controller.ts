/*
 * Copyright (c) 2015-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';
import {CheAPI} from '../../../../components/api/che-api.factory';
import {CheNotification} from '../../../../components/notification/che-notification.factory';

/**
 * Controller for creating factory from a template.
 * @author Oleksii Orel
 */
export class FactoryFromTemplateCtrl {
  private $filter: ng.IFilterService;
  private cheAPI: CheAPI;
  private cheNotification: CheNotification;
  private isImporting: boolean;
  private factoryContent: any;
  private editorOptions: any;
  private templateName: string;
  private editorErrors: Array<{id: string; message: string}> = [];

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($rootScope: ng.IRootScopeService, $filter: ng.IFilterService, cheAPI: CheAPI, cheNotification: CheNotification, $timeout: ng.ITimeoutService) {
    this.$filter = $filter;
    this.cheAPI = cheAPI;
    this.cheNotification = cheNotification;

    this.isImporting = false;
    this.factoryContent = null;
    this.templateName = 'minimal';
    this.getFactoryTemplate(this.templateName);

    this.editorOptions = {
      onLoad: (editor: any) => {
        editor.on('change', (codeMirror: any) => {
          const doc = codeMirror.getDoc();
          $timeout(() => {
            this.editorErrors.length = 0;
            let editorErrors: Array<{id: string; message: string}> = doc.getAllMarks().filter((mark: any) => {
              return mark.className && mark.className.includes('error');
            }).map((mark: any) => {
              const annotation = '__annotation';
              return {id: mark.id, message: mark[annotation] ? mark[annotation].message : 'Parse error'};
            });
            editorErrors.forEach((editorError: {id: string; message: string}) => {
              this.editorErrors.push(editorError);
            });
          }, 1000);
        });
      }
    };
  }

  // gets factory template.
  getFactoryTemplate(templateName: string) {
    let factoryContent = this.cheAPI.getFactoryTemplate().getFactoryTemplate(templateName);

    if (factoryContent) {
      this.factoryContent = this.$filter('json')(factoryContent, 2);
      return;
    }

    this.isImporting = true;

    // fetch it:
    let promise = this.cheAPI.getFactoryTemplate().fetchFactoryTemplate(templateName);

    //let factoryTemplate = this.cheAPI.getFactoryTemplate().getFactoryTemplate().getFactoryTemplate(templateName);

    promise.then((factoryTemplate: any) => {
      this.isImporting = false;
      this.factoryContent = this.$filter('json')(factoryTemplate, 2);
    }, (error: any) => {
      this.isImporting = false;
      this.cheNotification.showError(error.data.message ? error.data.message : 'Fail to get factory template.');
    });
  }

}
