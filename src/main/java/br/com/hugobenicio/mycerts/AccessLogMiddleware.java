package br.com.hugobenicio.mycerts;

import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogMiddleware {

    private static final Logger logger = LoggerFactory.getLogger("access");

    public static void beforeAllHandler(Context ctx) {
        logger.atInfo().setMessage("pre-request.")
                .addKeyValue("rid", RequestStateMiddleware.getRid(ctx))
                .addKeyValue("path", ctx.req().getPathInfo())
                .log();
    }

    public static void afterAllHandler(Context ctx) {
        logger.atInfo().setMessage("pos-request.")
                .addKeyValue("rid", RequestStateMiddleware.getRid(ctx))
                .addKeyValue("status", ctx.res().getStatus())
                .log();
    }
}
