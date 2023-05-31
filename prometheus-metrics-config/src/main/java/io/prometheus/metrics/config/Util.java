package io.prometheus.metrics.config;

import java.util.Map;
import java.util.function.Predicate;

public class Util {

    private static String getProperty(String name, Map<Object, Object> properties) {
        Object object = properties.remove(name);
        if (object != null) {
            return object.toString();
        }
        return null;
    }

    public static Boolean loadBoolean(String name, Map<Object, Object> properties) throws PrometheusConfigException {
        String property = getProperty(name, properties);
        if (property != null) {
            if (!"true".equalsIgnoreCase(property) && !"false".equalsIgnoreCase(property)) {
                throw new PrometheusConfigException(name + "=" + property + ": Expecting 'true' or 'false'");
            }
            return Boolean.parseBoolean(property);
        }
        return null;
    }

    public static double[] loadDoubleArray(String name, Map<Object, Object> properties) throws PrometheusConfigException {
        String property = getProperty(name, properties);
        if (property != null) {
            String[] numbers = property.split(",");
            double[] result = new double[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                try {
                    if ("+Inf".equals(numbers[i].trim())) {
                        result[i] = Double.POSITIVE_INFINITY;
                    } else {
                        result[i] = Double.parseDouble(numbers[i].trim());
                    }
                } catch (NumberFormatException e) {
                    throw new PrometheusConfigException(name + "=" + property + ": Expecting comma separated list of double values");
                }
            }
            return result;
        }
        return null;
    }

    public static Integer loadInteger(String name, Map<Object, Object> properties) throws PrometheusConfigException {
        String property = getProperty(name, properties);
        if (property != null) {
            try {
                return Integer.parseInt(property);
            } catch (NumberFormatException e) {
                throw new PrometheusConfigException(name + "=" + property + ": Expecting integer value");
            }
        }
        return null;
    }

    public static Double loadDouble(String name, Map<Object, Object> properties) throws PrometheusConfigException {
        String property = getProperty(name, properties);
        if (property != null) {
            try {
                return Double.parseDouble(property);
            } catch (NumberFormatException e) {
                throw new PrometheusConfigException(name + "=" + property + ": Expecting double value");
            }
        }
        return null;
    }

    public static Long loadLong(String name, Map<Object, Object> properties) throws PrometheusConfigException {
        String property = getProperty(name, properties);
        if (property != null) {
            try {
                return Long.parseLong(property);
            } catch (NumberFormatException e) {
                throw new PrometheusConfigException(name + "=" + property + ": Expecting long value");
            }
        }
        return null;
    }

    public static <T extends Number> void assertValue(T number, Predicate<T> predicate, String message, String prefix, String name) throws PrometheusConfigException {
        if (number != null && !predicate.test(number)) {
            String fullMessage = prefix == null ? name + ": " + message : prefix + "." + name + ": " + message;
            throw new PrometheusConfigException(fullMessage);
        }
    }
}
