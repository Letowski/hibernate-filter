package com.letowski.hibernate;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import java.lang.reflect.Field;
import java.util.*;

//voodoo people
public class TableFilter {
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
    private ReflectionStrategy reflectionStrategy;
    private RestrictionStrategy restrictionStrategy;

    public TableFilter() {
        reflectionStrategy = new ReflectionStrategyImpl();
        restrictionStrategy = new RestrictionStrategyImpl();
        restrictionStrategy.setReflectionStrategy(reflectionStrategy);
    }

    public void setReflectionStrategy(ReflectionStrategy reflectionStrategy) {
        this.reflectionStrategy = reflectionStrategy;
    }

    public void setRestrictionStrategy(RestrictionStrategy restrictionStrategy) {
        this.restrictionStrategy = restrictionStrategy;
    }

    public static String showSql(Criteria criteria){
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


    protected void prepare() throws Exception {
        if(this.getLike()!=null && !this.getLike().isEmpty()) {
            List<String> emptyKeys = new ArrayList<>();
            this.getLike().forEach((key,value) -> {
                if(value==null || value.equals("")){
                    emptyKeys.add(key);
                }
            });
            emptyKeys.forEach(key -> this.getLike().remove(key));
        }

        this.restrictionStrategy.createAlias(eq);
        this.restrictionStrategy.createAlias(gt);
        this.restrictionStrategy.createAlias(lt);
        this.restrictionStrategy.createAlias(like);
        this.restrictionStrategy.createAlias(order);
        this.restrictionStrategy.createAlias(in);
        this.restrictionStrategy.createAlias(ge);
        this.restrictionStrategy.createAlias(le);
        this.restrictionStrategy.createAlias(nullable);
        this.criteria.setFirstResult(offset);
        this.criteria.setMaxResults(limit);

        this.restrictionStrategy.addRestrictionsEq(this.eq);
        this.restrictionStrategy.addRestrictionsGt(this.gt);
        this.restrictionStrategy.addRestrictionsLt(this.lt);
        this.restrictionStrategy.addRestrictionsLike(this.like);
        this.restrictionStrategy.addRestrictionsIn(this.in);
        this.restrictionStrategy.addRestrictionsGe(this.ge);
        this.restrictionStrategy.addRestrictionsLe(this.le);
        this.restrictionStrategy.addRestrictionsNullable(this.nullable);

        this.restrictionStrategy.addOrder(this.order);
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

    public Criteria manual(Class table, Session session) throws Exception {
        this.createCriteria(table, session);
        this.prepare();
        return this.criteria;
    }

    protected void createCriteria(Class table, Session session){
        this.session = session;
        this.table = table;
        this.criteria = this.session.createCriteria(this.table);
        this.restrictionStrategy.setCriteria(criteria);
        this.restrictionStrategy.setTable(table);
    }

    protected void createCriteria(Class table, Session session, Criteria criteria){
        this.session = session;
        this.table = table;
        this.criteria = criteria;
        this.restrictionStrategy.setCriteria(criteria);
        this.restrictionStrategy.setTable(table);
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
