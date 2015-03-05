/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.job.filter;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.commons.job.IFuture;

/**
 * Filter which discards all Futures which do not belong to the given <code>job-id</code>.
 *
 * @since 5.1
 */
public class JobIdFilter implements IFilter<IFuture<?>> {

  private final String m_id;

  public JobIdFilter(final String id) {
    m_id = id;
  }

  @Override
  public boolean accept(final IFuture<?> future) {
    return m_id == null || m_id.equals(future.getJobInput().getId());
  }
}
