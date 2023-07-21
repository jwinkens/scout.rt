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

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.scout.rt.opentelemetry.api.IHistogramViewHintProvider;
import org.eclipse.scout.rt.opentelemetry.api.IMetricProvider;
import org.eclipse.scout.rt.opentelemetry.sdk.OpenTelemetryInitializerTest.OpenTelemetryInitializerPlatform;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IgnoreBean;
import org.eclipse.scout.rt.platform.Platform;
import org.eclipse.scout.rt.testing.platform.TestingDefaultPlatform;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.eclipse.scout.rt.testing.platform.runner.RunWithNewPlatform;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

@RunWith(PlatformTestRunner.class)
@RunWithNewPlatform(platform = OpenTelemetryInitializerPlatform.class)
public class OpenTelemetryInitializerTest {

  @Before
  public void before() {
    GlobalOpenTelemetry.resetForTest();
    OpenTelemetryInitializer initializer = BEANS.opt(OpenTelemetryInitializer.class);
    assertNull(initializer);
    Platform.get().getBeanManager().registerClass(OpenTelemetryInitializer.class);
    initializer = BEANS.opt(OpenTelemetryInitializer.class);
    assertNotNull(initializer);
    Platform.get().getBeanManager().registerClass(TestMetricProvider.class);
    Platform.get().getBeanManager().registerClass(TestHistogramViewHintProvider.class);
  }

  @After
  public void after() {
    Platform.get().getBeanManager().unregisterClass(TestHistogramViewHintProvider.class);
    Platform.get().getBeanManager().unregisterClass(TestMetricProvider.class);
    Platform.get().getBeanManager().unregisterClass(OpenTelemetryInitializer.class);
  }

  @Test
  public void testInitializer() {
    TestMetricProvider testMetricProvider = BEANS.get(TestMetricProvider.class);
    testMetricProvider.assertInvocations(0, 0);
    TestHistogramViewHintProvider testHistogramViewHintProvider = BEANS.get(TestHistogramViewHintProvider.class);
    testHistogramViewHintProvider.assertInvocations(0);

    OpenTelemetryInitializer initializer = BEANS.opt(OpenTelemetryInitializer.class);
    initializer.initOpenTelemetry();
    testMetricProvider.assertInvocations(0, 0);
    testHistogramViewHintProvider.assertInvocations(1);

    OpenTelemetrySdk scoutOpenTelemetry = initializer.m_openTelemetry;
    OpenTelemetry globalOpenTelemetry = GlobalOpenTelemetry.get();
    assertNotNull(globalOpenTelemetry);
    assertSame(scoutOpenTelemetry.getPropagators(), globalOpenTelemetry.getPropagators());
    assertSame(scoutOpenTelemetry.getLogsBridge(), globalOpenTelemetry.getLogsBridge());
    assertSame(scoutOpenTelemetry.getMeterProvider(), globalOpenTelemetry.getMeterProvider());
    assertSame(scoutOpenTelemetry.getTracerProvider(), globalOpenTelemetry.getTracerProvider());

    initializer.initMetrics();
    testMetricProvider.assertInvocations(1, 0);

    initializer.shutdownOpenTelemetry();
    testMetricProvider.assertInvocations(1, 1);
  }

  public static class OpenTelemetryInitializerPlatform extends TestingDefaultPlatform {

    @Override
    protected boolean acceptBean(Class<?> bean) {
      if (OpenTelemetryInitializer.class.isAssignableFrom(bean)) {
        return false;
      }
      return super.acceptBean(bean);
    }
  }

  @IgnoreBean
  public static class TestMetricProvider implements IMetricProvider {

    int numRegisterInvocations;
    int numCloseInvocations;

    @Override
    public void register(OpenTelemetry openTelemetry) {
      numRegisterInvocations++;
    }

    @Override
    public void close() {
      numCloseInvocations++;
    }

    void assertInvocations(int expectedRegisterInvocations, int expectedCloseInvocations) {
      assertEquals(expectedRegisterInvocations, numRegisterInvocations);
      assertEquals(expectedCloseInvocations, numCloseInvocations);
    }
  }

  @IgnoreBean
  public static class TestHistogramViewHintProvider implements IHistogramViewHintProvider {

    int numGetInstrumentNameInvocations;
    int numGetExplicitBucketsInvocations;

    @Override
    public String getInstrumentName() {
      numGetInstrumentNameInvocations++;
      return "test";
    }

    @Override
    public List<Double> getExplicitBuckets() {
      numGetExplicitBucketsInvocations++;
      return List.of();
    }

    void assertInvocations(int expectedInvocations) {
      assertEquals(expectedInvocations, numGetInstrumentNameInvocations);
      assertEquals(expectedInvocations, numGetExplicitBucketsInvocations);
    }
  }
}
