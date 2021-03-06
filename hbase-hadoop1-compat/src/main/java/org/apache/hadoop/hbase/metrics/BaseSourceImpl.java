/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.metrics;

import org.apache.hadoop.metrics2.MetricsBuilder;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.metrics2.MetricsSource;
import org.apache.hadoop.metrics2.impl.JmxCacheBuster;
import org.apache.hadoop.metrics2.lib.*;
import org.apache.hadoop.metrics2.source.JvmMetricsSource;

/**
 * Hadoop 1 implementation of BaseSource (using metrics2 framework).  It handles registration to
 * DefaultMetricsSystem and creation of the metrics registry.
 *
 * All MetricsSource's in hbase-hadoop1-compat should derive from this class.
 */
public class BaseSourceImpl implements BaseSource, MetricsSource {

  private static enum DefaultMetricsSystemInitializer {
    INSTANCE;
    private boolean inited = false;
    private JvmMetricsSource jvmMetricsSource;

    synchronized void init(String name) {
      if (inited) return;
      inited = true;
      DefaultMetricsSystem.initialize(HBASE_METRICS_SYSTEM_NAME);
      jvmMetricsSource = JvmMetricsSource.create(name, "");

    }
  }

  protected final DynamicMetricsRegistry metricsRegistry;
  protected final String metricsName;
  protected final String metricsDescription;
  protected final String metricsContext;
  protected final String metricsJmxContext;

  public BaseSourceImpl(
      String metricsName,
      String metricsDescription,
      String metricsContext,
      String metricsJmxContext) {

    this.metricsName = metricsName;
    this.metricsDescription = metricsDescription;
    this.metricsContext = metricsContext;
    this.metricsJmxContext = metricsJmxContext;

    metricsRegistry = new DynamicMetricsRegistry(metricsName).setContext(metricsContext);
    DefaultMetricsSystemInitializer.INSTANCE.init(metricsName);

    //Register this instance.
    DefaultMetricsSystem.INSTANCE.registerSource(metricsJmxContext, metricsDescription, this);
    init();
  }

  public void init() {
    this.metricsRegistry.clearMetrics();
  }


  /**
   * Set a single gauge to a value.
   *
   * @param gaugeName gauge name
   * @param value     the new value of the gauge.
   */
  public void setGauge(String gaugeName, long value) {
    MetricMutableGaugeLong gaugeInt = metricsRegistry.getLongGauge(gaugeName, value);
    gaugeInt.set(value);
  }

  /**
   * Add some amount to a gauge.
   *
   * @param gaugeName The name of the gauge to increment.
   * @param delta     The amount to increment the gauge by.
   */
  public void incGauge(String gaugeName, long delta) {
    MetricMutableGaugeLong gaugeInt = metricsRegistry.getLongGauge(gaugeName, 0l);
    gaugeInt.incr(delta);
  }

  /**
   * Decrease the value of a named gauge.
   *
   * @param gaugeName The name of the gauge.
   * @param delta     the ammount to subtract from a gauge value.
   */
  public void decGauge(String gaugeName, long delta) {
    MetricMutableGaugeLong gaugeInt = metricsRegistry.getLongGauge(gaugeName, 0l);
    gaugeInt.decr(delta);
  }

  /**
   * Increment a named counter by some value.
   *
   * @param key   the name of the counter
   * @param delta the ammount to increment
   */
  public void incCounters(String key, long delta) {
    MetricMutableCounterLong counter = metricsRegistry.getLongCounter(key, 0l);
    counter.incr(delta);

  }

  @Override
  public void updateHistogram(String name, long value) {
    MetricMutableHistogram histo = metricsRegistry.getHistogram(name);
    histo.add(value);
  }

  @Override
  public void updateQuantile(String name, long value) {
    MetricMutableQuantiles histo = metricsRegistry.getQuantile(name);
    histo.add(value);
  }

  /**
   * Remove a named metric.
   *
   * @param key
   */
  public void removeMetric(String key) {
    metricsRegistry.removeMetric(key);
    JmxCacheBuster.clearJmxCache();
  }


  /**
   * Method to export all the metrics.
   *
   * @param metricsBuilder Builder to accept metrics
   * @param all            push all or only changed?
   */
  @Override
  public void getMetrics(MetricsBuilder metricsBuilder, boolean all) {
    MetricsRecordBuilder mrb = metricsBuilder.addRecord(metricsName)
                                             .setContext(metricsContext);
    metricsRegistry.snapshot(mrb, all);
  }

  /**
   * Used to get at the DynamicMetricsRegistry.
   * @return DynamicMetricsRegistry
   */
  public DynamicMetricsRegistry getMetricsRegistry() {
    return metricsRegistry;
  }

  public String getMetricsContext() {
    return metricsContext;
  }

  public String getMetricsDescription() {
    return metricsDescription;
  }

  public String getMetricsJmxContext() {
    return metricsJmxContext;
  }

  public String getMetricsName() {
    return metricsName;
  }
}
