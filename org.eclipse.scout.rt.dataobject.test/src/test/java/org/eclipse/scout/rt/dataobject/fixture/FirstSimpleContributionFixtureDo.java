/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.dataobject.fixture;

import javax.annotation.Generated;

import org.eclipse.scout.rt.dataobject.ContributesTo;
import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.DoValue;
import org.eclipse.scout.rt.dataobject.IDoEntityContribution;
import org.eclipse.scout.rt.dataobject.TypeName;

@TypeName("scout.FirstSimpleContributionFixture")
@ContributesTo(SimpleFixtureDo.class)
public final class FirstSimpleContributionFixtureDo extends DoEntity implements IDoEntityContribution {

  public DoValue<String> firstValue() {
    return doValue("firstValue");
  }

  /* **************************************************************************
   * GENERATED CONVENIENCE METHODS
   * *************************************************************************/

  @Generated("DoConvenienceMethodsGenerator")
  public FirstSimpleContributionFixtureDo withFirstValue(String firstValue) {
    firstValue().set(firstValue);
    return this;
  }

  @Generated("DoConvenienceMethodsGenerator")
  public String getFirstValue() {
    return firstValue().get();
  }
}
