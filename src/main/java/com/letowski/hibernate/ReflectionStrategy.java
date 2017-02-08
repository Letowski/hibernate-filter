package com.letowski.hibernate;

public interface ReflectionStrategy {
    String returnType(Class table, String fieldName);
    String entityName(String fieldName);
    String columnName(Class table, String fieldName);
}
