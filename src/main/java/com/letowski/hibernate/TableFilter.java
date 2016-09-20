package com.letowski.hibernate;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

//voodoo people
public class TableFilter {
    protected static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    protected Map<String, String> eq = new HashMap<String, String>();
    protected Map<String, String> gt = new HashMap<String, String>();
    protected Map<String, String> lt = new HashMap<String, String>();
    protected Map<String, String> ge = new HashMap<String, String>();
    protected Map<String, String> le = new HashMap<String, String>();
    protected Map<String, String> like = new HashMap<String, String>();
    protected Map<String, Boolean> order = new HashMap<String, Boolean>();
    protected Map<String, List<String>> in = new HashMap<String, List<String>>();
    protected Map<String, Boolean> nullable = new HashMap<String, Boolean>();
    protected int offset = 0;
    protected int limit = 20;
    protected Criteria criteria;
    protected Session session;
    protected Class table;
    protected Set<String> aliases = new HashSet<>();


    protected static String showSql(Criteria criteria){
        try {
            CriteriaImpl c = (CriteriaImpl) criteria;
            SessionImpl s = (SessionImpl) c.getSession();
            SessionFactoryImplementor factory = (SessionFactoryImplementor) s.getSessionFactory();
            String[] implementors = factory.getImplementors(c.getEntityOrClassName());
            LoadQueryInfluencers lqis = new LoadQueryInfluencers();
            CriteriaLoader loader = new CriteriaLoader((OuterJoinLoadable) factory.getEntityPersister(implementors[0]), factory, c, implementors[0], lqis);
            Field f = OuterJoinLoader.class.getDeclaredField("sql");
            f.setAccessible(true);
            return (String) f.get(loader);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public String showSql(){
        return showSql(this.criteria);
    }

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

    protected String entityName(String fieldName) {
        if (!fieldName.contains(".")) {
            return "alias";
        }
        String[] pieces = fieldName.split("\\.");
        return pieces[pieces.length - 2];
    }

    protected void prepare() throws Exception {
        this.createAlias(eq);
        this.createAlias(gt);
        this.createAlias(lt);
        this.createAlias(like);
        this.createAlias(order);
        this.createAlias(in);
        this.createAlias(ge);
        this.createAlias(le);
        this.createAlias(nullable);
        this.criteria.setFirstResult(offset);
        this.criteria.setMaxResults(limit);

        this.addRestrictionsEq(this.eq);
        this.addRestrictionsGt(this.gt);
        this.addRestrictionsLt(this.lt);
        this.addRestrictionsLike(this.like);
        this.addRestrictionsIn(this.in);
        this.addRestrictionsGe(this.ge);
        this.addRestrictionsLe(this.le);
        this.addRestrictionsNullable(this.nullable);

        this.addOrder(this.order);
        this.criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
    }

    public List run(Class table, Session session) throws Exception {
        this.createCriteria(table,session);
        this.prepare();
        List list=this.criteria.list();
        session.close();
        return list;
    }

    public Criteria manual(Class table, Session session, Criteria criteria) throws Exception{
        this.createCriteria(table, session, criteria);
        this.prepare();
        return this.criteria;
    }

    protected void createCriteria(Class table, Session session){
        this.session = session;
        this.table = table;
        this.criteria = this.session.createCriteria(this.table);
    }
    protected void createCriteria(Class table, Session session, Criteria criteria){
        this.session = session;
        this.table = table;
        this.criteria = criteria;
    }

    protected void addOrder(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            String key=entry.getKey();
            if(key.contains(".")){
                String[] pieces = key.split("\\.");
                if(pieces.length>2){
                    key = pieces[pieces.length-2] + "." + pieces[pieces.length-1];
                }
            }
            this.criteria.addOrder(entry.getValue() ? Order.asc(key) : Order.desc(key));
        }
    }

    /**
     * Like with Long is written not for all SQL-dialects
     * It will be working in Oracle, MySQL and maybe something else
     *
     * @param map key-value storage to search by
     */
    protected void addRestrictionsLike(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                if(!entry.getKey().contains(",")) {
                    Criterion criterion = this.restrictionLike(entry.getKey(), entry.getValue());
                    if(criterion!=null){
                        this.criteria.add(criterion);
                    }
                }else{
                    String[] pieces = entry.getKey().split(",");
                    List<Criterion> criterionList = new ArrayList<>();
                    for(String piece : pieces){
                        if(!piece.equals("")){
                            Criterion criterion = this.restrictionLike(piece, entry.getValue());
                            if(criterion!=null){
                                criterionList.add(criterion);
                            }
                        }
                    }
                    if(criterionList.size()>0){
                        Criterion[] criterions = new Criterion[criterionList.size()];
                        this.criteria.add(Restrictions.or(criterionList.toArray(criterions)));
                    }
                }
            }
        }
    }

    protected Criterion restrictionLike(String key, String value) {
        switch (this.returnType(this.table, key)) {
            case "Long": {
                try {
                    return Restrictions.sqlRestriction("{" + this.entityName(key) + "}"
                            + "." + this.columnName(this.table, key) + " LIKE '%" + value + "%'");
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            case "String": {
                return Restrictions.like(key, value, MatchMode.ANYWHERE).ignoreCase();
            }
        }
        return null;
    }

    protected void addRestrictionsEq(Map<String, String> map) {
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
    protected void addRestrictionsNullable(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (!entry.getKey().equals("")) {
                if(entry.getValue()) {
                    this.criteria.add(Restrictions.isNull(entry.getKey()));
                }else {
                    this.criteria.add(Restrictions.isNotNull(entry.getKey()));
                }
            }
        }
    }

    protected void addRestrictionsGt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, true);
    }

    protected void addRestrictionsLt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, false);
    }

    protected void addRestrictionsLtGt(Map<String, String> map, boolean isLt) {
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

    protected void addRestrictionsGe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, true);
    }

    protected void addRestrictionsLe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, false);
    }

    protected void addRestrictionsLeGe(Map<String, String> map, boolean isLe) {
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
                                timestamp.setTime(timestamp.getTime()+24*60*60*1000);
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

    protected void addRestrictionsIn(Map<String, List<String>> map) {
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

    protected void createAlias(Map<String, ?> map) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if(!entry.getKey().contains(",")) {
                this.createAlias(entry.getKey());
            }else{
                String[] pieces = entry.getKey().split(",");
                for (String piece : pieces){
                    if(!piece.equals("")){
                        this.createAlias(piece);
                    }
                }
            }
        }
    }

    protected static String joinPartOfArray(String[] pieces, int position, String separator){
        String result="";
        for(int i = 0; i <= position; i++) {
            result = (result.equals("")) ? pieces[i] : result+separator+pieces[i];
        }
        return result;
    }

    protected void createAlias(String string){
        if (string.contains(".")) {
            String[] pieces = string.split("\\.");
            for(int i = 0; i < pieces.length-1; i++) {
                String tableName = joinPartOfArray(pieces,i,".");
                String tableNameStripped = pieces[i];
                if (!this.aliases.contains(tableName)) {
                    this.criteria.createAlias(tableName, tableNameStripped);
                    this.aliases.add(tableName);
                }
            }
        }
    }

    protected String getClassAnnotationValue(Class classType, Class annotationType, String attributeName) {
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

    public Map<String, Boolean> getNullable() {
        return nullable;
    }

    public void setNullable(Map<String, Boolean> nullable) {
        this.nullable = nullable;
    }
}
