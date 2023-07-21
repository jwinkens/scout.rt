/*
 * Copyright (c) 2010-2023 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.opentelemetry.sdk;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.rt.opentelemetry.api.IHistogramViewHintProvider;
import org.eclipse.scout.rt.opentelemetry.api.IMetricProvider;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IPlatform.State;
import org.eclipse.scout.rt.platform.IPlatformListener;
import org.eclipse.scout.rt.platform.PlatformEvent;
import org.eclipse.scout.rt.platform.config.AbstractBooleanConfigProperty;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.ApplicationNameProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;

/**
 * Initialize {@link GlobalOpenTelemetry} instance, "auto" configured by using environment properties, system properties
 * and/or Scout config properties.
 *
 * @see AutoConfiguredOpenTelemetrySdk
 * @see <a href=
 *      "https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md">OpenTelemetry
 *      SDK Autoconfigure</a>
 */
public class OpenTelemetryInitializer implements IPlatformListener {

  private static final Logger LOG = LoggerFactory.getLogger(OpenTelemetryInitializer.class);

  protected OpenTelemetrySdk m_openTelemetry;

  @Override
  public void stateChanged(PlatformEvent event) {
    if (event.getState() == State.BeanManagerPrepared) {
      initOpenTelemetry();
      initMetrics();
    }

    if (event.getState() == State.PlatformStopping) {
      shutdownOpenTelemetry();
    }
  }

  protected void initOpenTelemetry() {
    if (!CONFIG.getPropertyValue(OpenTelemetryInitializerEnabled.class)) {
      LOG.info("Skip Scout OpenTelemetry initialization.");
      return;
    }
    LOG.info("Initialize OpenTelemetry");

    // configuration provided by environment variables and/or system properties
    m_openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
        .addPropertiesSupplier(this::getDefaultProperties)
        .addMeterProviderCustomizer(this::customizeMeterProvider)
        .disableShutdownHook()
        .setResultAsGlobal()
        .build()
        .getOpenTelemetrySdk();
  }

  protected Map<String, String> getDefaultProperties() {
    Map<String, String> defaultConfig = new HashMap<>();
    defaultConfig.put("otel.traces.exporter", "none");
    defaultConfig.put("otel.logs.exporter", "none");
    defaultConfig.put("otel.exporter.otlp.protocol", "http/protobuf");
    defaultConfig.put("otel.metric.export.interval", "1000"); // TODO bko: define default, 30s?

    defaultConfig.put("otel.service.name", CONFIG.getPropertyValue(ApplicationNameProperty.class));
    return defaultConfig;
  }

  protected SdkMeterProviderBuilder customizeMeterProvider(SdkMeterProviderBuilder builder, ConfigProperties config) {
    for (IHistogramViewHintProvider viewHintProvider : BEANS.all(IHistogramViewHintProvider.class)) {
      LOG.info("Initialize view from {}", viewHintProvider.getClass().getName());
      builder.registerView(
          InstrumentSelector.builder()
              .setName(viewHintProvider.getInstrumentName())
              .setType(InstrumentType.HISTOGRAM)
              .build(),
          View.builder()
              .setAggregation(Aggregation.explicitBucketHistogram(viewHintProvider.getExplicitBuckets()))
              .build());
    }
    return builder;
  }

  protected void initMetrics() {
    for (IMetricProvider metricProvider : BEANS.all(IMetricProvider.class)) {
      LOG.info("Initialize metrics from {}", metricProvider.getClass().getName());
      metricProvider.register(GlobalOpenTelemetry.get());
    }
  }

  protected void shutdownOpenTelemetry() {
    LOG.info("Shutting down OpenTelemetry");
    if (m_openTelemetry == null) {
      return;
    }

    BEANS.all(IMetricProvider.class).forEach(IMetricProvider::close);
    m_openTelemetry.close();
  }

  public static class OpenTelemetryInitializerEnabled extends AbstractBooleanConfigProperty {

    @Override
    public String getKey() {
      return "scout.otel.initializerEnabled";
    }

    @Override
    public String description() {
      return "Property to specify if the application is using the Scout OpenTelemetry initializer. Default is true. Set to false if you are using the OpenTelemetry Java Agent.";
    }

    @Override
    public Boolean getDefaultValue() {
      return Boolean.TRUE;
    }
  }
}
