package io.prometheus.metrics.core.metrics;

import io.prometheus.metrics.config.MetricProperties;
import io.prometheus.metrics.config.PrometheusProperties;
import io.prometheus.metrics.core.exemplars.ExemplarSampler;
import io.prometheus.metrics.core.exemplars.ExemplarSamplerConfig;
import io.prometheus.metrics.model.Exemplar;
import io.prometheus.metrics.model.GaugeSnapshot;
import io.prometheus.metrics.model.Labels;
import io.prometheus.metrics.core.observer.GaugingObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

public class Gauge extends ObservingMetric<GaugingObserver, Gauge.GaugeData> implements GaugingObserver {

    private final boolean exemplarsEnabled;
    private final ExemplarSamplerConfig exemplarSamplerConfig;

    private Gauge(Builder builder, PrometheusProperties prometheusProperties) {
        super(builder);
        MetricProperties[] properties = getMetricProperties(builder, prometheusProperties);
        exemplarsEnabled = getConfigProperty(properties, MetricProperties::getExemplarsEnabled);
        if (exemplarsEnabled) {
            exemplarSamplerConfig = new ExemplarSamplerConfig(prometheusProperties.getExemplarConfig(), 1);
        } else {
            exemplarSamplerConfig = null;
        }
    }

    @Override
    public void inc(double amount) {
        getNoLabels().inc(amount);
    }

    @Override
    public void incWithExemplar(double amount, Labels labels) {
        getNoLabels().incWithExemplar(amount, labels);
    }

    @Override
    public void set(double value) {
        getNoLabels().set(value);
    }

    @Override
    public void setWithExemplar(double value, Labels labels) {
        getNoLabels().setWithExemplar(value, labels);
    }

    @Override
    public GaugeSnapshot collect() {
        return (GaugeSnapshot) super.collect();
    }

    @Override
    protected GaugeSnapshot collect(List<Labels> labels, List<GaugeData> metricData) {
        List<GaugeSnapshot.GaugeData> data = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            data.add(metricData.get(i).collect(labels.get(i)));
        }
        return new GaugeSnapshot(getMetadata(), data);
    }

    @Override
    protected GaugeData newMetricData() {
        if (isExemplarsEnabled()) {
            return new GaugeData(new ExemplarSampler(exemplarSamplerConfig));
        } else {
            return new GaugeData(null);
        }
    }

    @Override
    protected boolean isExemplarsEnabled() {
        return exemplarsEnabled;
    }

    class GaugeData extends MetricData<GaugingObserver> implements GaugingObserver {

        private final ExemplarSampler exemplarSampler; // null if isExemplarsEnabled() is false

        private GaugeData(ExemplarSampler exemplarSampler) {
            this.exemplarSampler = exemplarSampler;
        }

        private final AtomicLong value = new AtomicLong(Double.doubleToRawLongBits(0));

        @Override
        public void inc(double amount) {
            long next = value.updateAndGet(l -> Double.doubleToRawLongBits(Double.longBitsToDouble(l) + amount));
            if (isExemplarsEnabled()) {
                exemplarSampler.observe(Double.longBitsToDouble(next));
            }
        }

        @Override
        public void incWithExemplar(double amount, Labels labels) {
            long next = value.updateAndGet(l -> Double.doubleToRawLongBits(Double.longBitsToDouble(l) + amount));
            if (isExemplarsEnabled()) {
                exemplarSampler.observeWithExemplar(Double.longBitsToDouble(next), labels);
            }
        }

        @Override
        public void set(double value) {
            this.value.set(Double.doubleToRawLongBits(value));
            if (isExemplarsEnabled()) {
                exemplarSampler.observe(value);
            }
        }

        @Override
        public void setWithExemplar(double value, Labels labels) {
            this.value.set(Double.doubleToRawLongBits(value));
            if (isExemplarsEnabled()) {
                exemplarSampler.observeWithExemplar(value, labels);
            }
        }

        private GaugeSnapshot.GaugeData collect(Labels labels) {
            // Read the exemplar first. Otherwise, there is a race condition where you might
            // see an Exemplar for a value that's not represented in getValue() yet.
            // If there are multiple Exemplars (by default it's just one), use the oldest
            // so that we don't violate min age.
            Exemplar oldest = null;
            if (isExemplarsEnabled()) {
                for (Exemplar exemplar : exemplarSampler.collect()) {
                    if (oldest == null || exemplar.getTimestampMillis() < oldest.getTimestampMillis()) {
                        oldest = exemplar;
                    }
                }
            }
            return new GaugeSnapshot.GaugeData(Double.longBitsToDouble(value.get()), labels, oldest);
        }

        @Override
        public GaugingObserver toObserver() {
            return this;
        }
    }

    public static class Builder extends ObservingMetric.Builder<Builder, Gauge> {

        private Builder(PrometheusProperties config) {
            super(Collections.emptyList(), config);
        }

        @Override
        public Gauge build() {
            return new Gauge(this, properties);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    public static class FromCallback extends Metric {

        private final DoubleSupplier callback;

        private FromCallback(Gauge.FromCallback.Builder builder) {
            super(builder);
            this.callback = builder.callback;
        }

        @Override
        public GaugeSnapshot collect() {
            return new GaugeSnapshot(getMetadata(), Collections.singletonList(
                    new GaugeSnapshot.GaugeData(callback.getAsDouble(), constLabels, null)
            ));
        }

        public static class Builder extends Metric.Builder<Gauge.FromCallback.Builder, Gauge.FromCallback> {

            private DoubleSupplier callback;

            private Builder(PrometheusProperties config) {
                super(Collections.emptyList(), config);
            }

            public Gauge.FromCallback.Builder withCallback(DoubleSupplier callback) {
                this.callback = callback;
                return this;
            }

            @Override
            public Gauge.FromCallback build() {
                return new Gauge.FromCallback(this);
            }

            @Override
            protected Gauge.FromCallback.Builder self() {
                return this;
            }
        }
    }

    public static Builder newBuilder() {
        return new Builder(PrometheusProperties.getInstance());
    }

    public static Builder newBuilder(PrometheusProperties config) {
        return new Builder(config);
    }
}