package com.letowski.hibernateFilter;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class TableFilter {
    private static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    private Map<String, String> eq = new HashMap<String, String>();
    private Map<String, String> gt = new HashMap<String, String>();
    private Map<String, String> lt = new HashMap<String, String>();
    private Map<String, String> ge = new HashMap<String, String>();
    private Map<String, String> le = new HashMap<String, String>();
    private Map<String, String> like = new HashMap<String, String>();
    private Map<String, Boolean> order = new HashMap<String, Boolean>();
    private Map<String, List<String>> in = new HashMap<String, List<String>>();
    private int offset = 0;
    private int limit = 20;
    private Criteria criteria;
    private Session session;
    private Class table;
    private Set<String> aliases = new HashSet<>();

    private static String ucfirst(String string) {
        String s1 = string.substring(0, 1).toUpperCase();
        return s1 + string.substring(1);
    }

    private Method reflectionGetterFlat(Class table, String fieldName) throws NoSuchMethodException {
        return table.getMethod("get" + ucfirst(fieldName));
    }

    private Class reflectionEntity(Class table, String fieldName) throws NoSuchMethodException {
        if (!fieldName.contains(".")) {
            return table;
        }
        String currentFieldName = fieldName.substring(0, fieldName.indexOf("."));
        fieldName = fieldName.substring(fieldName.indexOf(".") + 1);
        table = this.reflectionGetterFlat(table, currentFieldName).getReturnType();
        return this.reflectionEntity(table, fieldName);
    }

    private Method reflectionGetter(Class table, String fieldName) throws NoSuchMethodException {
        if (!fieldName.contains(".")) {
            return this.reflectionGetterFlat(table, fieldName);
        }
        String currentFieldName = fieldName.substring(0, fieldName.indexOf("."));
        fieldName = fieldName.substring(fieldName.indexOf(".") + 1);
        table = this.reflectionGetterFlat(table, currentFieldName).getReturnType();
        return reflectionGetter(table, fieldName);
    }

    private String returnType(Class table, String fieldName) {
        try {
            return reflectionGetter(table, fieldName).getReturnType().getSimpleName();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String columnName(Class table, String fieldName) {
        try {
            return reflectionGetter(table, fieldName).getAnnotation(Column.class).name();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String tableName(Class table, String fieldName) {
        try {
            return this.getClassAnnotationValue(reflectionEntity(table, fieldName), Table.class, "name");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String entityName(String fieldName) {
        if (!fieldName.contains(".")) {
            return "alias";
        }
        String[] pieces = fieldName.split("\\.");
        return pieces[pieces.length - 2];
    }

    private void prepare() throws Exception {
        this.createAlias(eq);
        this.createAlias(gt);
        this.createAlias(lt);
        this.createAlias(like);
        this.createAlias(order);
        this.createAlias(in);
        this.createAlias(ge);
        this.createAlias(le);
        this.criteria.setFirstResult(offset);
        this.criteria.setMaxResults(limit);

        this.addRestrictionsEq(this.eq);
        this.addRestrictionsGt(this.gt);
        this.addRestrictionsLt(this.lt);
        this.addRestrictionsLike(this.like);
        this.addRestrictionsIn(this.in);
        this.addRestrictionsGe(this.ge);
        this.addRestrictionsLe(this.le);

        this.addOrder(this.order);
    }

    public List run(Class table, Session session) throws Exception {
        this.createCriteria(table,session);
        this.prepare();
        return this.criteria.list();
    }

    public Criteria manual(Class table, Session session, Criteria criteria) throws Exception{
        this.createCriteria(table, session, criteria);
        this.prepare();
        return this.criteria;
    }

    private void createCriteria(Class table, Session session){
        this.session = session;
        this.table = table;
        this.criteria = this.session.createCriteria(this.table);
    }
    private void createCriteria(Class table, Session session, Criteria criteria){
        this.session = session;
        this.table = table;
        this.criteria = criteria;
    }

    private void addOrder(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            this.criteria.addOrder(entry.getValue() ? Order.asc(entry.getKey()) : Order.desc(entry.getKey()));
        }
    }

    /**
     * Like with Long is written not for all SQL-dialects
     * It will be working in Oracle, MySQL and maybe something else
     *
     * @param map key-value storage to search by
     */
    private void addRestrictionsLike(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        try {
                            this.criteria.add(Restrictions.sqlRestriction("{" + this.entityName(entry.getKey()) + "}"
                                    + "." + this.columnName(this.table, entry.getKey()) + " LIKE '%" + entry.getValue
                                    () + "%'"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case "String": {
                        this.criteria.add(Restrictions.like(entry.getKey(), entry.getValue(), MatchMode.ANYWHERE));
                        break;
                    }
                }
            }
        }
    }

    private void addRestrictionsEq(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        this.criteria.add(Restrictions.eq(entry.getKey(), Long.parseLong(entry.getValue())));
                        break;
                    }
                    case "String": {
                        this.criteria.add(Restrictions.eq(entry.getKey(), entry.getValue()));
                        break;
                    }
                }
            }
        }
    }

    private void addRestrictionsGt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, true);
    }

    private void addRestrictionsLt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, false);
    }

    private void addRestrictionsLtGt(Map<String, String> map, boolean isLt) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        if (isLt) {
                            this.criteria.add(Restrictions.lt(entry.getKey(), Long.parseLong(entry.getValue())));
                        } else {
                            this.criteria.add(Restrictions.gt(entry.getKey(), Long.parseLong(entry.getValue())));
                        }
                        break;
                    }
                    case "Timestamp": {
                        try {
                            Timestamp timestamp = new Timestamp(dateformat.parse(entry.getValue()).getTime());
                            if (isLt) {
                                this.criteria.add(Restrictions.lt(entry.getKey(), timestamp));
                            } else {
                                this.criteria.add(Restrictions.gt(entry.getKey(), timestamp));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    private void addRestrictionsGe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, true);
    }

    private void addRestrictionsLe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, false);
    }

    private void addRestrictionsLeGe(Map<String, String> map, boolean isLe) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        if (isLe) {
                            this.criteria.add(Restrictions.le(entry.getKey(), Long.parseLong(entry.getValue())));
                        } else {
                            this.criteria.add(Restrictions.ge(entry.getKey(), Long.parseLong(entry.getValue())));
                        }
                        break;
                    }
                    case "Timestamp": {
                        try {
                            Timestamp timestamp = new Timestamp(dateformat.parse(entry.getValue()).getTime());
                            if (isLe) {
                                this.criteria.add(Restrictions.le(entry.getKey(), timestamp));
                            } else {
                                this.criteria.add(Restrictions.ge(entry.getKey(), timestamp));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    private void addRestrictionsIn(Map<String, List<String>> map) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            values.remove("");

            switch (this.returnType(this.table, entry.getKey())) {
                case "Long": {
                    List<Long> longValues = new ArrayList<Long>();

                    for (String value : values) {
                        longValues.add(Long.parseLong(value));
                    }

                    this.criteria.add(Restrictions.in(entry.getKey(), longValues));
                    break;
                }

                case "Timestamp": {
                    try {
                        List<Timestamp> timestampValues = new ArrayList<Timestamp>();

                        for (String value : values) {
                            timestampValues.add(new Timestamp(dateformat.parse(value).getTime()));
                        }

                        this.criteria.add(Restrictions.in(entry.getKey(), timestampValues));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    break;
                }

                case "String" : {
                    this.criteria.add(Restrictions.in(entry.getKey(), values));
                }
            }
        }
    }

    private void createAlias(Map<String, ?> map) {
        String tableName;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().contains(".")) {
                String[] pieces = entry.getKey().split("\\.");
                tableName = pieces[pieces.length - 2];
                if (!this.aliases.contains(tableName)) {
                    this.criteria.createAlias(tableName, tableName);
                    this.aliases.add(tableName);
                }
            }
        }
    }

    private String getClassAnnotationValue(Class classType, Class annotationType, String attributeName) {
        String value = null;

        Annotation annotation = classType.getAnnotation(annotationType);
        if (annotation != null) {
            try {
                value = (String) annotation.annotationType().getMethod(attributeName).invoke(annotation);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return value;
    }

    public Map<String, Boolean> getOrder() {
        return order;
    }

    public void setOrder(Map<String, Boolean> order) {
        this.order = order;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Map<String, String> getEq() {
        return eq;
    }

    public void setEq(Map<String, String> eq) {
        this.eq = eq;
    }

    public Map<String, String> getGt() {
        return gt;
    }

    public void setGt(Map<String, String> gt) {
        this.gt = gt;
    }

    public Map<String, String> getLt() {
        return lt;
    }

    public void setLt(Map<String, String> lt) {
        this.lt = lt;
    }

    public Map<String, String> getLike() {
        return like;
    }

    public void setLike(Map<String, String> like) {
        this.like = like;
    }

    public Map<String, List<String>> getIn() {
        return in;
    }

    public void setIn(Map<String, List<String>> in) {
        this.in = in;
    }

    public Map<String, String> getGe() {
        return ge;
    }

    public void setGe(Map<String, String> ge) {
        this.ge = ge;
    }

    public Map<String, String> getLe() {
        return le;
    }

    public void setLe(Map<String, String> le) {
        this.le = le;
    }
}
