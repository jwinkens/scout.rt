/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.services.common.security;

import java.security.Permission;
import java.security.Permissions;
import java.util.Collection;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.shared.security.BasicHierarchyPermission;
import org.eclipse.scout.rt.shared.servicetunnel.RemoteServiceAccessDenied;
import org.eclipse.scout.rt.shared.validate.IValidationStrategy;
import org.eclipse.scout.rt.shared.validate.InputValidation;

/**
 * Access control facility.
 * The default implementation in org.eclipse.scout.rt.shared.services.common.security.AccessControlService has service
 * ranking low (-1)
 * and caches permissions per user principal for maximum performance when using stateless request/response patterns as
 * in webservice environments.
 */
@Priority(-3)
@InputValidation(IValidationStrategy.PROCESS.class)
public interface IAccessControlService extends IService {

  String getUserIdOfCurrentSubject();

  boolean checkPermission(Permission p);

  int getPermissionLevel(Permission p);

  /**
   * only use this method to transfer permissions, don't use it for access
   * control or adding permissions see also {@link #checkPermission(Permission)} and
   * {@link #getPermissionLevel(BasicHierarchyPermission)}
   */
  Permissions getPermissions();

  /**
   * @return true if this service is a proxy to the real access control service
   *         This property is queried by {@link com.bsiag.security.BasicHierarchyPermission} to decide
   *         whether fine-grained access control can be calculated right away
   *         (no proxy) or must be delegated to the real access control service
   *         in the backend
   */
  boolean isProxyService();

  /**
   * Clear all caches. This can be useful when some permissions and/or user-role mappings have changed.
   */
  @RemoteServiceAccessDenied
  void clearCache();

  /**
   * Clear cache of specified userIds.<br>
   * This can be useful when some permissions and/or user-role mappings have
   * changed.
   * This method is lenient.
   */
  @RemoteServiceAccessDenied
  void clearCacheOfUserIds(Collection<String> userIds);
}
