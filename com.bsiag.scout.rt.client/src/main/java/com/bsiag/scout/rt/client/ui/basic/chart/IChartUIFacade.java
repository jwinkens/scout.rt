/*******************************************************************************
 * Copyright (c) 2010-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSI CRM Software License v1.0
 * which accompanies this distribution as bsi-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package com.bsiag.scout.rt.client.ui.basic.chart;

import java.math.BigDecimal;

/**
 * @since 5.2
 */
@FunctionalInterface
public interface IChartUIFacade {

  /**
   * position for all axes in IChartBean.getAxes() ordered in same order like axes.
   */
  void fireValueClickFromUI(int valueIndex, BigDecimal value, int groupIndex, String groupName);
}
