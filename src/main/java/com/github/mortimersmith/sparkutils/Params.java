package com.github.mortimersmith.sparkutils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import spark.Request;

public class Params
{
    public static <T> T from(Request req, Class<T> cls)
    {
        try {
            T t = cls.getConstructor().newInstance();
            req.params().forEach((k, v) -> set(t, k, v));
            req.queryMap().toMap().forEach((k, vs) -> set(t, k, vs));
            return t;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(Object o, String key, String[] values)
    {
        Field f;
        try { f = o.getClass().getDeclaredField(key); }
        catch (NoSuchFieldException ex) { return; }
        if (f == null) return;
        set(o, f, key, values);
    }

    private static void set(Object o, Field f, String key, String[] values)
    {
        try {
            if (String[].class.isAssignableFrom(f.getType())) {
                f.set(o, values);
            } else if (int[].class == f.getType() || Integer[].class == f.getType()) {
                f.set(o, Arrays.stream(values).mapToInt(Integer::parseInt).toArray());
            } else if (long.class == f.getType() || Long.class == f.getType()) {
                f.set(o, Arrays.stream(values).mapToLong(Long::parseLong).toArray());
            } else if (double.class == f.getType() || Double.class == f.getType()) {
                f.set(o, Arrays.stream(values).mapToDouble(Double::parseDouble).toArray());
            } else if (values.length == 1) {
                set(o, f, key, values[0]);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(Object o, String key, String value)
    {
        Field f;
        try { f = o.getClass().getDeclaredField(key.substring(1)); }
        catch (NoSuchFieldException ex) { return; }
        if (f == null) return;
        set(o, f, key, value);
    }

    private static void set(Object o, Field f, String key, String value)
    {
        try {
            if (String.class.isAssignableFrom(f.getType())) {
                f.set(o, value);
            } else if (int.class == f.getType() || Integer.class == f.getType()) {
                try { f.setInt(o, Integer.parseInt(value)); }
                catch (NumberFormatException ex) { throw new RuntimeException("invalid value: " + value); }
            } else if (long.class == f.getType() || Long.class == f.getType()) {
                try { f.setLong(o, Long.parseLong(value)); }
                catch (NumberFormatException ex) { throw new RuntimeException("invalid value: " + value); }
            } else if (float.class == f.getType() || Float.class == f.getType()) {
                try { f.setFloat(o, Float.parseFloat(value)); }
                catch (NumberFormatException ex) { throw new RuntimeException("invalid value: " + value); }
            } else if (double.class == f.getType() || Double.class == f.getType()) {
                try { f.setDouble(o, Double.parseDouble(value)); }
                catch (NumberFormatException ex) { throw new RuntimeException("invalid value: " + value); }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
