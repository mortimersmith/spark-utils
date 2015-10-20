package com.github.mortimersmith.sparkutils;

import com.google.gson.Gson;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import spark.Request;
import spark.Response;
import spark.Spark;

public class AnnotatedSpark
{
    private static final Gson GSON = new Gson();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Routed
    {
        String path();
        Result result() default Result.ARBITRARY;

        enum Result { ARBITRARY, JSON }
    }

    public interface Web
    {
        Request req();
        Response rsp();
    }

    private static class WebImpl implements Web
    {
        private final Request _req;
        private final Response _rsp;

        private WebImpl(Request req, Response rsp) { _req = req; _rsp = rsp; }

        public static Web of(Request req, Response rsp) { return new WebImpl(req, rsp); }

        public Request req() { return _req; }
        public Response rsp() { return _rsp; }
    }

    private static class Arg
    {
        public final int index;
        public final Class<?> cls;
        private Arg(int index, Class<?> cls) {
            this.index = index; this.cls = cls;
        }
        public static Arg of(int index) {
            return new Arg(index, null);
        }
        public static Arg of(int index, Class<?> cls) {
            return new Arg(index, cls);
        }
    }

    private static class Args
    {
        public final Arg web, params, post;
        private Args(Arg web, Arg params, Arg post) {
            this.web = web; this.params = params; this.post = post;
        }
        public static Args of(Arg web, Arg params, Arg post) {
            return new Args(web, params, post);
        }
    }

    public static void register(Object o)
    {
        register(o, o.getClass());
    }

    public static void register(Class<?> cls)
    {
        register(cls, cls);
    }

    public static void register(Object o, Class<?> cls)
    {
        for (Method m : cls.getDeclaredMethods()) {
            Routed route = m.getAnnotation(Routed.class);
            if (route == null) continue;
            Arg a_web = null, a_rsp = null, a_params = null, a_post = null;
            for (int i = 0; i < m.getParameterCount(); ++i) {
                Parameter p = m.getParameters()[i];
                if ("web".equals(p.getName())) a_web = Arg.of(i);
                else if ("params".equals(p.getName())) { a_params = Arg.of(i, p.getClass()); }
                else if ("post".equals(p.getName())) { a_post = Arg.of(i, p.getClass()); }
            }
            Args i = Args.of(a_web, a_params, a_post);
            Spark.post(route.path(),
                (Request req, Response rsp) -> {
                    Object[] args = new Object[m.getParameterCount()];
                    if (i.web != null) args[i.web.index] = WebImpl.of(req, rsp);
                    if (i.params != null) args[i.params.index] = Params.from(req, i.params.cls);
                    if (i.post != null) args[i.post.index] = GSON.fromJson(req.body(), i.post.cls);
                    Object result = m.invoke(o, args);
                    switch (route.result()) {
                        case JSON:
                            return GSON.toJson(result);
                        case ARBITRARY:
                        default:
                            return result;
                    }
                });
        }
    }
}
