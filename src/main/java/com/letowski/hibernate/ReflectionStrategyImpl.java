package com.letowski.hibernate;

import javax.persistence.Column;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class ReflectionStrategyImpl implements ReflectionStrategy {
    private PropertyDescriptor propertyDescriptor(Class table, String fieldName) throws NoSuchMethodException, IntrospectionException {
        BeanInfo info = Introspector.getBeanInfo(table);
        for ( PropertyDescriptor pd : info.getPropertyDescriptors() )
            if (fieldName.equals(pd.getName())) return pd;
        throw new NoSuchMethodException(table+" has no field "+fieldName);
    }

    private Method reflectionGetterFlat(Class table, String fieldName) throws NoSuchMethodException, IntrospectionException {
        return this.propertyDescriptor(table, fieldName).getReadMethod();
    }

    private Method reflectionGetter(Class table, String fieldName) throws NoSuchMethodException, IntrospectionException {
        if (!fieldName.contains(".")) {
            return this.reflectionGetterFlat(table, fieldName);
        }
        String currentFieldName = fieldName.substring(0, fieldName.indexOf("."));
        fieldName = fieldName.substring(fieldName.indexOf(".") + 1);
        table = returnTypeFlat(table,currentFieldName);
        return reflectionGetter(table, fieldName);
    }

    private Class returnTypeFlat(Class table, String currentFieldName) throws NoSuchMethodException, IntrospectionException{
        Class result=null;
        Method method = this.reflectionGetterFlat(table, currentFieldName);
        try {
            final Type type = method.getGenericReturnType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                result = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(result==null){
            result=method.getReturnType();
        }
        return result;
    }

    public String returnType(Class table, String fieldName) {
        try {
            Class type = reflectionGetter(table, fieldName).getReturnType();
            return type.getSimpleName();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String columnName(Class table, String fieldName) {
        try {
            return reflectionGetter(table, fieldName).getAnnotation(Column.class).name();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String entityName(String fieldName) {
        if (!fieldName.contains(".")) {
            return "alias";
        }
        String[] pieces = fieldName.split("\\.");
        return pieces[pieces.length - 2];
    }


}
