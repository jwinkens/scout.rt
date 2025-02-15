/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.jackson.dataobject.id;

import java.io.IOException;

import org.eclipse.scout.rt.dataobject.id.IId;
import org.eclipse.scout.rt.dataobject.id.IdCodec;
import org.eclipse.scout.rt.platform.util.LazyValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Custom deserializer for {@link IId} values.
 */
public class IIdDeserializer extends StdDeserializer<IId> {
  private static final long serialVersionUID = 1L;

  protected final Class<? extends IId> m_idType;
  protected final LazyValue<IdCodec> m_idCodec = new LazyValue<>(IdCodec.class);

  public IIdDeserializer(Class<? extends IId> idType) {
    super(idType);
    m_idType = idType;
  }

  @Override
  public IId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return m_idCodec.get().fromUnqualified(m_idType, p.getText());
  }
}
