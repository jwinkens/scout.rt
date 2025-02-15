/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.jackson.dataobject.fixture;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Generated;

import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.DoSet;
import org.eclipse.scout.rt.dataobject.TypeName;

@TypeName("scout.ObjectCollectionFixture")
public class ObjectCollectionFixtureDo extends DoEntity {

  public DoSet<Object> objectSet() {
    return doSet("objectSet");
  }

  /* **************************************************************************
   * GENERATED CONVENIENCE METHODS
   * *************************************************************************/

  @Generated("DoConvenienceMethodsGenerator")
  public ObjectCollectionFixtureDo withObjectSet(Collection<? extends Object> objectSet) {
    objectSet().updateAll(objectSet);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public ObjectCollectionFixtureDo withObjectSet(Object... objectSet) {
    objectSet().updateAll(objectSet);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public Set<Object> getObjectSet() {
    return objectSet().get();
  }
}
