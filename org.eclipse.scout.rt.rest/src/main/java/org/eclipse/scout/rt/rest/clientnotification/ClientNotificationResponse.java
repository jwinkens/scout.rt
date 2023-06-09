/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.rest.clientnotification;

import java.util.Collection;
import java.util.List;

import javax.annotation.Generated;

import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.DoList;

public class ClientNotificationResponse extends DoEntity {
  public DoList<DoEntity> notifications() {
    return doList("notifications");
  }

  /* **************************************************************************
   * GENERATED CONVENIENCE METHODS
   * *************************************************************************/

  @Generated("DoConvenienceMethodsGenerator")
  public ClientNotificationResponse withNotifications(Collection<? extends DoEntity> notifications) {
    notifications().updateAll(notifications);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public ClientNotificationResponse withNotifications(DoEntity... notifications) {
    notifications().updateAll(notifications);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public List<DoEntity> getNotifications() {
    return notifications().get();
  }
}
