package main_project.insight.util;

import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class InsightOutputValidator {

    private InsightOutputValidator() {
    }

    public static void validate(
            JsonNode value,
            JsonNode schema,
            Set<String> metricIds,
            Set<String> trendIds,
            Set<String> conditionIds
    ) {

        shape(value, schema);

        Set<String> evidenceIds = new HashSet<>(metricIds);
        evidenceIds.addAll(trendIds);

        Set<String> analysisIds = new HashSet<>();
        Set<String> actionIds = new HashSet<>();
        Set<String> usedTrends = new HashSet<>();

        for (JsonNode analysis : value.path("personalAnalysis")) {
            String id = analysis.path("id").asText();

            require(id.matches("a[1-3]"), "invalid analysis id");
            require(analysisIds.add(id), "duplicate analysis id");

            refs(analysis.path("metricIds"), metricIds);
            refs(analysis.path("conditionIds"), conditionIds);

            require(
                    !analysis.path("metricIds").isEmpty(),
                    "analysis requires evidence"
            );
        }

        for (JsonNode action : value.path("recommendations")) {
            String id = action.path("id").asText();

            require(id.matches("r[1-3]"), "invalid recommendation id");
            require(actionIds.add(id), "duplicate recommendation id");

            refs(action.path("evidenceIds"), evidenceIds);
            refs(action.path("conditionIds"), conditionIds);

            require(
                    !action.path("evidenceIds").isEmpty(),
                    "recommendation requires evidence"
            );
        }

        for (JsonNode trend : value.path("relatedTrends")) {
            String id = trend.path("trendId").asText();

            require(trendIds.contains(id), "unknown trend");
            require(usedTrends.add(id), "duplicate trend");
        }

        refs(
                value.path("finalSummary").path("nextActionIds"),
                actionIds
        );

        if ("insufficient_data".equals(
                value.path("status").asText()
        )) {
            require(
                    value.path("personalAnalysis").isEmpty()
                            && value.path("recommendations").isEmpty()
                            && value.path("relatedTrends").isEmpty()
                            && value.path("finalSummary")
                            .path("nextActionIds").isEmpty(),
                    "invalid insufficient data response"
            );
        }
    }

    private static void refs(
            JsonNode values,
            Set<String> allowed
    ) {

        Set<String> seen = new HashSet<>();

        for (JsonNode value : values) {
            String id = value.asText();

            require(
                    allowed.contains(id) && seen.add(id),
                    "unknown or duplicate reference"
            );
        }
    }

    private static void shape(
            JsonNode value,
            JsonNode schema
    ) {

        require(value != null, "missing value");

        switch (schema.path("type").asText()) {
            case "object" -> {
                require(value.isObject(), "object required");

                JsonNode properties = schema.path("properties");

                for (JsonNode required : schema.path("required")) {
                    require(
                            value.has(required.asText()),
                            "missing field"
                    );
                }

                Iterator<String> fields = value.propertyNames().iterator();

                while (fields.hasNext()) {
                    String field = fields.next();

                    require(
                            properties.has(field),
                            "unknown field"
                    );

                    shape(
                            value.get(field),
                            properties.get(field)
                    );
                }
            }

            case "array" -> {
                require(value.isArray(), "array required");

                require(
                        value.size()
                                <= schema.path("maxItems").asInt(20),
                        "too many items"
                );

                for (JsonNode item : value) {
                    shape(item, schema.path("items"));
                }
            }

            case "string" -> {
                require(value.isTextual(), "string required");

                require(
                        !value.asText().isBlank(),
                        "blank text"
                );

                require(
                        value.asText().length()
                                <= schema.path("maxLength").asInt(1200),
                        "text too long"
                );

                if (schema.has("enum")) {
                    boolean matched = false;

                    for (JsonNode allowed : schema.path("enum")) {
                        if (allowed.equals(value)) {
                            matched = true;
                            break;
                        }
                    }

                    require(matched, "invalid enum");
                }
            }

            default -> throw new IllegalArgumentException(
                    "unsupported schema type"
            );
        }
    }

    private static void require(
            boolean condition,
            String message
    ) {

        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
