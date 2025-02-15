/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.shared.extension.dto.fixture;

import javax.annotation.Generated;

import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractValueFieldData;

/**
 * <b>NOTE:</b><br>This class is auto generated by the Scout SDK. No manual modifications recommended.
 */
@Generated(value = "org.eclipse.scout.rt.shared.extension.dto.fixture.WrappedFormFieldOuterForm", comments = "This class is auto generated by the Scout SDK. No manual modifications recommended.")
public class WrappedFormFieldOuterFormData extends AbstractFormData {
  private static final long serialVersionUID = 1L;

  public String getString() {
    return getFieldByClass(String.class);
  }

  public static class String extends AbstractValueFieldData<java.lang.String> {
    private static final long serialVersionUID = 1L;
  }
}
