package com.letowski.hibernate;


import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

class RestrictionStrategyImpl implements RestrictionStrategy {
    protected Criteria criteria;
    private ReflectionStrategy reflectionStrategy;
    protected Set<String> aliases = new HashSet<>();
    protected static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
    protected Class table;

    @Override
    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public void setReflectionStrategy(ReflectionStrategy reflectionStrategy) {
        this.reflectionStrategy = reflectionStrategy;
    }

    public void setTable(Class table) {
        this.table = table;
    }

    public void addOrder(Map<String, Boolean> map) {
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
    public void addRestrictionsLike(Map<String, String> map) {
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
        switch (this.reflectionStrategy.returnType(this.table, key)) {
            case "Long": {
                try {
                    return Restrictions.sqlRestriction("{" + this.reflectionStrategy.entityName(key) + "}"
                            + "." + this.reflectionStrategy.columnName(this.table, key) + " LIKE '%" + Long.parseLong(value) + "%'");
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            case "String": {
                return Restrictions.like(restrictionKeyCrop(key), value, MatchMode.ANYWHERE).ignoreCase();
            }
        }
        return null;
    }

    public void addRestrictionsEq(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.reflectionStrategy.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        this.criteria.add(Restrictions.eq(restrictionKeyCrop(entry.getKey()), Long.parseLong(entry.getValue())));
                        break;
                    }
                    case "String": {
                        this.criteria.add(Restrictions.eq(restrictionKeyCrop(entry.getKey()), entry.getValue()));
                        break;
                    }
                }
            }
        }
    }

    public void addRestrictionsNullable(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (!entry.getKey().equals("")) {
                if(entry.getValue()) {
                    this.criteria.add(Restrictions.isNull(restrictionKeyCrop(entry.getKey())));
                }else {
                    this.criteria.add(Restrictions.isNotNull(restrictionKeyCrop(entry.getKey())));
                }
            }
        }
    }

    public void addRestrictionsGt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, true);
    }

    public void addRestrictionsLt(Map<String, String> map) {
        this.addRestrictionsLtGt(map, false);
    }

    protected void addRestrictionsLtGt(Map<String, String> map, boolean isLt) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.reflectionStrategy.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        if (isLt) {
                            this.criteria.add(Restrictions.lt(restrictionKeyCrop(entry.getKey()), Long.parseLong(entry.getValue())));
                        } else {
                            this.criteria.add(Restrictions.gt(restrictionKeyCrop(entry.getKey()), Long.parseLong(entry.getValue())));
                        }
                        break;
                    }
                    case "Timestamp": {
                        try {
                            Timestamp timestamp = new Timestamp(dateformat.parse(entry.getValue()).getTime());
                            if (isLt) {
                                this.criteria.add(Restrictions.lt(restrictionKeyCrop(entry.getKey()), timestamp));
                            } else {
                                this.criteria.add(Restrictions.gt(restrictionKeyCrop(entry.getKey()), timestamp));
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

    public void addRestrictionsGe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, true);
    }

    public void addRestrictionsLe(Map<String, String> map) {
        this.addRestrictionsLeGe(map, false);
    }

    protected void addRestrictionsLeGe(Map<String, String> map, boolean isLe) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals("")) {
                switch (this.reflectionStrategy.returnType(this.table, entry.getKey())) {
                    case "Long": {
                        if (isLe) {
                            this.criteria.add(Restrictions.le(restrictionKeyCrop(entry.getKey()), Long.parseLong(entry.getValue())));
                        } else {
                            this.criteria.add(Restrictions.ge(restrictionKeyCrop(entry.getKey()), Long.parseLong(entry.getValue())));
                        }
                        break;
                    }
                    case "Timestamp": {
                        try {
                            Timestamp timestamp = new Timestamp(dateformat.parse(entry.getValue()).getTime());
                            if (isLe) {
                                timestamp.setTime(timestamp.getTime()+24*60*60*1000);
                                this.criteria.add(Restrictions.le(restrictionKeyCrop(entry.getKey()), timestamp));
                            } else {
                                this.criteria.add(Restrictions.ge(restrictionKeyCrop(entry.getKey()), timestamp));
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

    public void addRestrictionsIn(Map<String, List<String>> map) {
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            values.remove("");

            switch (this.reflectionStrategy.returnType(this.table, entry.getKey())) {
                case "Long": {
                    List<Long> longValues = new ArrayList<Long>();

                    for (String value : values) {
                        longValues.add(Long.parseLong(value));
                    }

                    this.criteria.add(Restrictions.in(restrictionKeyCrop(entry.getKey()), longValues));
                    break;
                }

                case "Timestamp": {
                    try {
                        List<Timestamp> timestampValues = new ArrayList<Timestamp>();

                        for (String value : values) {
                            timestampValues.add(new Timestamp(dateformat.parse(value).getTime()));
                        }

                        this.criteria.add(Restrictions.in(restrictionKeyCrop(entry.getKey()), timestampValues));
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

    public void createAlias(Map<String, ?> map) {
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

    protected static String restrictionKeyCrop(String key){
        if(key.lastIndexOf(".")==key.indexOf(".")){
            return key;
        }
        String[] parts = key.split("\\.");
        return parts[parts.length-2]+"."+parts[parts.length-1];
    }
}
