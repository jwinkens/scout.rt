/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.mom.jms;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.eclipse.scout.rt.mom.jms.internal.JmsConnectionWrapper;

/**
 * Lambda function for a late binding JMS {@link Connection} used in {@link JmsConnectionWrapper}
 *
 * @since 6.1
 */
@FunctionalInterface
public interface ICreateJmsConnection {
  Connection create() throws JMSException;
}
