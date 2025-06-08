package br.com.hugobenicio.mycerts;

import io.javalin.http.Context;

import java.util.UUID;

public class RequestStateMiddleware {

    public static final String ATTR_RID = "rid";

    public static void beforeAllHandler(Context ctx) {
        var rid = UUID.randomUUID();
        ctx.req().setAttribute(ATTR_RID, rid);
    }

    public static UUID getRid(Context ctx) {
        return (UUID) ctx.req().getAttribute(ATTR_RID);
    }
}
