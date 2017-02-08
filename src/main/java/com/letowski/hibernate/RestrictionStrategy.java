package com.letowski.hibernate;

import org.hibernate.Criteria;

import java.util.List;
import java.util.Map;

public interface RestrictionStrategy {
    void setCriteria(Criteria criteria);
    void setTable(Class table);
    void setReflectionStrategy(ReflectionStrategy reflectionStrategy);
    void createAlias(Map<String, ?> map);
    void addRestrictionsLike(Map<String, String> map);
    void addRestrictionsEq(Map<String, String> map);
    void addRestrictionsNullable(Map<String, Boolean> map);
    void addRestrictionsGt(Map<String, String> map);
    void addRestrictionsLt(Map<String, String> map);
    void addRestrictionsGe(Map<String, String> map);
    void addRestrictionsLe(Map<String, String> map);
    void addRestrictionsIn(Map<String, List<String>> map);
    void addOrder(Map<String, Boolean> map);
}
