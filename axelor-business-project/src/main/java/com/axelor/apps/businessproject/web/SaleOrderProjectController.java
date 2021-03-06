/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.businessproject.web;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.businessproject.service.projectgenerator.ProjectGeneratorFactory;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.ProjectGeneratorType;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Singleton
public class SaleOrderProjectController {

  private static final String CONTEXT_SHOW_RECORD = "_showRecord";

  @Inject private SaleOrderRepository saleOrderRepo;

  public void generateProject(ActionRequest request, ActionResponse response) {
    try {
      SaleOrder saleOrder = request.getContext().asType(SaleOrder.class);
      saleOrder = Beans.get(SaleOrderRepository.class).find(saleOrder.getId());
      String generatorType = (String) request.getContext().get("_projectGeneratorType");
      LocalDateTime startDate = getElementStartDate(request.getContext());

      ProjectGeneratorFactory factory =
          ProjectGeneratorFactory.getFactory(ProjectGeneratorType.valueOf(generatorType));
      Project project = factory.generate(saleOrder, startDate);

      response.setReload(true);
      response.setView(
          ActionView.define("Project")
              .model(Project.class.getName())
              .add("form", "project-form")
              .param("forceEdit", "true")
              .context(CONTEXT_SHOW_RECORD, String.valueOf(project.getId()))
              .map());

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void fillProject(ActionRequest request, ActionResponse response) {
    try {
      SaleOrder saleOrder = request.getContext().asType(SaleOrder.class);
      saleOrder = Beans.get(SaleOrderRepository.class).find(saleOrder.getId());
      String generatorType = (String) request.getContext().get("_projectGeneratorType");
      LocalDateTime startDate = getElementStartDate(request.getContext());

      ProjectGeneratorFactory factory =
          ProjectGeneratorFactory.getFactory(ProjectGeneratorType.valueOf(generatorType));
      ActionViewBuilder view = factory.fill(saleOrder.getProject(), saleOrder, startDate);

      response.setReload(true);
      response.setView(view.map());

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void updateLines(ActionRequest request, ActionResponse response) throws AxelorException {
    SaleOrder saleOrder = request.getContext().asType(SaleOrder.class);
    saleOrder = saleOrderRepo.find(saleOrder.getId());

    for (SaleOrderLine orderLine : saleOrder.getSaleOrderLineList()) {
      orderLine.setProject(saleOrder.getProject());
    }
    response.setValue("saleOrderLineList", saleOrder.getSaleOrderLineList());
  }

  private LocalDateTime getElementStartDate(Context context) {
    LocalDateTime date;
    String stringStartDate = (String) context.get("_elementStartDate");
    if (!Strings.isNullOrEmpty(stringStartDate)) {
      date = LocalDateTime.ofInstant(Instant.parse(stringStartDate), ZoneId.systemDefault());
    } else {
      date = Beans.get(AppBaseService.class).getTodayDate().atStartOfDay();
    }
    return date;
  }
}
