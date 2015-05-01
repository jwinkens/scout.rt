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
package org.eclipse.scout.rt.ui.html.json.desktop;

import org.eclipse.scout.rt.client.ui.desktop.outline.IFormToolButton;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.ui.html.IUiSession;
import org.eclipse.scout.rt.ui.html.json.IJsonAdapter;
import org.eclipse.scout.rt.ui.html.json.action.JsonAction;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterProperty;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterPropertyConfig;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterPropertyConfigBuilder;

public class JsonFormToolButton<T extends IFormToolButton<IForm>> extends JsonAction<T> {

  public JsonFormToolButton(T model, IUiSession uiSession, String id, IJsonAdapter<?> parent) {
    super(model, uiSession, id, parent);
  }

  @Override
  public String getObjectType() {
    return "FormToolButton";
  }

  @Override
  protected void initJsonProperties(T model) {
    super.initJsonProperties(model);

    putJsonProperty(new JsonAdapterProperty<IFormToolButton<IForm>>(IFormToolButton.PROP_FORM, model, getUiSession()) {
      @Override
      protected IForm modelValue() {
        return getModel().getForm();
      }

      @Override
      protected JsonAdapterPropertyConfig createConfig() {
        return JsonAdapterPropertyConfigBuilder.globalConfig();
      }
    });
  }
}
