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
package org.eclipse.scout.rt.testing.client.runner.statement;

import org.eclipse.scout.commons.Assertions;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.rt.client.context.ClientRunContexts;
import org.junit.runners.model.Statement;

/**
 * Statement to ensure to work on an empty <code>ClientRunContext</code>.
 *
 * @since 5.1
 */
public class ClearClientRunContextStatement extends Statement {

  protected final Statement m_next;

  public ClearClientRunContextStatement(final Statement next) {
    m_next = Assertions.assertNotNull(next, "next statement must not be null");
  }

  @Override
  public void evaluate() throws Throwable {
    ClientRunContexts.empty().run(new IRunnable() {

      @Override
      public void run() throws Exception {
        try {
          m_next.evaluate();
        }
        catch (final Exception e) {
          throw e;
        }
        catch (final Throwable e) {
          throw new Error(e);
        }
      }
    });
  }
}
