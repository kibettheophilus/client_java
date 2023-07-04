package io.prometheus.metrics.expositionformats;

import io.prometheus.metrics.config.ExpositionFormatProperties;
import io.prometheus.metrics.config.PrometheusProperties;

public class ExpositionFormats {

    private final PrometheusProtobufWriter prometheusProtobufWriter;
    private final PrometheusTextFormatWriter prometheusTextFormatWriter;
    private final OpenMetricsTextFormatWriter openMetricsTextFormatWriter;

    private ExpositionFormats(PrometheusProtobufWriter prometheusProtobufWriter,
                              PrometheusTextFormatWriter prometheusTextFormatWriter,
                              OpenMetricsTextFormatWriter openMetricsTextFormatWriter) {
        this.prometheusProtobufWriter = prometheusProtobufWriter;
        this.prometheusTextFormatWriter = prometheusTextFormatWriter;
        this.openMetricsTextFormatWriter = openMetricsTextFormatWriter;
    }

    public static ExpositionFormats init() {
        return init(PrometheusProperties.get().getExpositionFormatConfig());
    }

    public static ExpositionFormats init(ExpositionFormatProperties properties) {
        return new ExpositionFormats(
                new PrometheusProtobufWriter(),
                new PrometheusTextFormatWriter(properties.getIncludeCreatedTimestamps()),
                new OpenMetricsTextFormatWriter(properties.getIncludeCreatedTimestamps())
        );
    }

    public ExpositionFormatWriter findWriter(String acceptHeader) {
        if (prometheusProtobufWriter.accepts(acceptHeader)) {
            return prometheusProtobufWriter;
        }
        if (openMetricsTextFormatWriter.accepts(acceptHeader)) {
            return openMetricsTextFormatWriter;
        }
        return prometheusTextFormatWriter;
    }

    public PrometheusProtobufWriter getPrometheusProtobufWriter() {
        return prometheusProtobufWriter;
    }

    public PrometheusTextFormatWriter getPrometheusTextFormatWriter() {
        return prometheusTextFormatWriter;
    }

    public OpenMetricsTextFormatWriter getOpenMetricsTextFormatWriter() {
        return openMetricsTextFormatWriter;
    }
}
