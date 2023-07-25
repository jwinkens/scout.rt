/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.dataobject;

/**
 * Interface to a data mapper that uses lazy-deserialization, thus might return data objects that might cause
 * {@link ClassCastException} if data object itself or attributes are accessed in case they couldn't be deserialized
 * successfully and therefore haven't the expected type.
 *
 * @see IDataObjectMapper
 */
public interface ILenientDataObjectMapper extends IDataObjectMapper {
}
