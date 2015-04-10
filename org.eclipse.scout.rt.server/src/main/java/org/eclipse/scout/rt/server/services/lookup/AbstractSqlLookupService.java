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
package org.eclipse.scout.rt.server.services.lookup;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.VetoException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.platform.service.SERVICES;
import org.eclipse.scout.rt.server.services.common.jdbc.ISqlService;
import org.eclipse.scout.rt.server.services.common.jdbc.SQL;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.ILookupService;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;

/**
 * Sql SELECT statement for getting {@link LookupRow}s.<br>
 * <p>
 * The expected columns are
 * <ul>
 * <li>Object key
 * <li>String text
 * <li>String iconId
 * <li>String tooltip
 * <li>String background color
 * <li>String foreground color
 * <li>String font
 * <li>Boolean enabled
 * <li>Object parentKey used in hierarchical structures to point to the parents primary key
 * <li>{@link TriState} active (0,1,null,...) see {@link TriState#parse(Object)}
 * </ul>
 * <p>
 * Valid bind names are: Object key, String text, String all, Object rec, {@link TriState} active<br>
 * Valid xml tags are: &lt;key&gt;, &lt;text&gt;, &lt;all&gt;, &lt;rec&gt;
 */
public abstract class AbstractSqlLookupService<T> extends AbstractLookupService<T> {

  private static final Pattern REFUSING_ALL_TAGS_REGEX = Pattern.compile("<all>\\s*and\\s*([0-9]+)\\s*=\\s*([0-9]+)\\s*</all>", Pattern.DOTALL);

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractSqlLookupService.class);

  public AbstractSqlLookupService() {
  }

  /**
   * Sql SELECT statement
   */
  @ConfigProperty(ConfigProperty.SQL)
  @Order(10)
  protected String getConfiguredSqlSelect() {
    return null;
  }

  /**
   * sort column 0-based. -1 if sort is done in sql.
   */
  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(20)
  protected int getConfiguredSortColumn() {
    return 1;
  }

  /**
   * This method is called on server side to load lookup rows.
   */
  @ConfigOperation
  @Order(10)
  protected List<ILookupRow<T>> execLoadLookupRows(String originalSql, String preprocessedSql, ILookupCall<T> call) throws ProcessingException {
    Object[][] data = SQL.selectLimited(preprocessedSql, call.getMaxRowCount(), call);
    if (getConfiguredSortColumn() >= 0) {
      sortData(data, getConfiguredSortColumn());
    }
    try {
      Class<?> genericsParameterClass = Object.class;
      try {
        genericsParameterClass = TypeCastUtility.getGenericsParameterClass(getClass(), ILookupService.class);
      }
      catch (IllegalArgumentException e) {
        LOG.warn("Unable to calculate type parameters for lookup service '" + getClass().getName() + "'. No key type validation will be performed.");
      }
      return createLookupRowArray(data, call, genericsParameterClass);
    }
    catch (IllegalArgumentException e) {
      throw new ProcessingException("Unable to load lookup rows for lookup service '" + getClass().getName() + "'.", e);
    }
  }

  @Override
  public List<ILookupRow<T>> getDataByKey(ILookupCall<T> call) throws ProcessingException {
    String sql = getConfiguredSqlSelect();
    return execLoadLookupRows(sql, filterSqlByKey(sql), call);
  }

  @Override
  public List<ILookupRow<T>> getDataByText(ILookupCall<T> call) throws ProcessingException {
    // change wildcards * in text to db specific wildcards
    if (call.getText() != null) {
      String s = call.getText();
      String sqlWildcard = SERVICES.getService(ISqlService.class).getSqlStyle().getLikeWildcard();
      call.setText(s.replaceAll("[*]", sqlWildcard));
    }
    String sql = getConfiguredSqlSelect();
    return execLoadLookupRows(sql, filterSqlByText(sql), call);
  }

  @Override
  public List<ILookupRow<T>> getDataByAll(ILookupCall<T> call) throws ProcessingException {
    String sql = getConfiguredSqlSelect();
    if (containsRefusingAllTag(sql)) {
      throw new VetoException(ScoutTexts.get("SearchTextIsTooGeneral"));
    }
    List<ILookupRow<T>> rows = execLoadLookupRows(sql, filterSqlByAll(sql), call);
    return rows;
  }

  @Override
  public List<ILookupRow<T>> getDataByRec(ILookupCall<T> call) throws ProcessingException {
    String sql = getConfiguredSqlSelect();
    return execLoadLookupRows(sql, filterSqlByRec(sql), call);
  }

  /**
   * Process xml tags.<br>
   * Keep content of "key" tag.<br>
   * Remove text,all,rec tags.
   */
  protected String filterSqlByKey(String sqlSelect) {
    return StringUtility.removeTagBounds(StringUtility.removeTags(sqlSelect, new String[]{"text", "all", "rec"}), "key");
  }

  /**
   * Process xml tags.<br>
   * Keep content of "text" tag.<br>
   * Remove key,all,rec tags.
   */
  protected String filterSqlByText(String sqlSelect) {
    return StringUtility.removeTagBounds(StringUtility.removeTags(sqlSelect, new String[]{"key", "all", "rec"}), "text");
  }

  /**
   * Process xml tags.<br>
   * Keep content of "all" tag.<br>
   * Remove key,text,rec tags.
   */
  protected String filterSqlByAll(String sqlSelect) {
    return StringUtility.removeTagBounds(StringUtility.removeTags(sqlSelect, new String[]{"key", "text", "rec"}), "all");
  }

  protected static boolean containsRefusingAllTag(String sqlSelect) {
    Matcher m = REFUSING_ALL_TAGS_REGEX.matcher(sqlSelect.toLowerCase());
    if (m.find()) {
      if (!m.group(1).equals(m.group(2))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Process xml tags.<br>
   * Keep content of "rec" tag.<br>
   * Remove key,text,all tags.
   */
  protected String filterSqlByRec(String sqlSelect) {
    return StringUtility.removeTagBounds(StringUtility.removeTags(sqlSelect, new String[]{"key", "text", "all"}), "rec");
  }

}
