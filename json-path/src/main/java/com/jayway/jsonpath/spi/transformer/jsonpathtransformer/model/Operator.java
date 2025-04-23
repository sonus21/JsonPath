package com.jayway.jsonpath.spi.transformer.jsonpathtransformer.model;


public interface Operator {
    boolean isUnary();

    default boolean isBinary() {
        return !isUnary();
    }

    boolean supports(Class<?> type);

    String name();

    String getType();

    Object apply(Object srcValue, Object additionalValue);
}
