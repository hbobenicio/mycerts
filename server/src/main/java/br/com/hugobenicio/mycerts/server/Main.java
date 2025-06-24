package br.com.hugobenicio.mycerts.server;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.lang.String.format;

public class Main {

    private static final String TEXT_HTML_WITH_CHARSET =
            format("%s; charset=%s", ContentType.HTML, Charset.defaultCharset().name());

    public static void main(String[] args) {
        //TODO parameterize these configurations
        var host = "localhost";
        var port = 8080;

        // Template Engine
        Handlebars handlebars = handlebarsCreate();

        // Templates Compilation
        String templateFilePath = "";
        final Template indexTemplate;
        try {
            templateFilePath = "index.html";
            indexTemplate = handlebars.compile(templateFilePath);
        } catch (IOException e) {
            var msg = String.format("failed to compile handlebars template. path=\"%s\"", templateFilePath);
            throw new RuntimeException(msg, e);
        }

        // Javalin Configuration
        Javalin app = Javalin.create(config -> {
            config.useVirtualThreads = true;

            // Wait 5 seconds for existing requests to finish
            config.jetty.modifyServer(server -> server.setStopTimeout(5_000));

            // Webjar's resources will be served at /webjars (e.g. /webjars/bootstrap/...)
            // @see https://www.webjars.org/
            config.staticFiles.enableWebjars();

            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "/public";
                staticFileConfig.precompress = false;
            });
        });

        // Before Handlers
        app.before(RequestStateMiddleware::beforeAllHandler);
        app.before(AccessLogMiddleware::beforeAllHandler);

        // Endpoints Routing
        app.get("/", ctx -> {
            var data = new IndexTemplateData("World");
            String html = indexTemplate.apply(data);
            ctx.header("Content-type", TEXT_HTML_WITH_CHARSET).result(html);
        });

        // After Handlers
        app.after(AccessLogMiddleware::afterAllHandler);

        // Serving
        app.start(host, port);
    }

    /**
     * Produces the Handlebars instance that are able to load our hbs templates from resources.
     *
     * @return the Handlebars instance
     */
    private static Handlebars handlebarsCreate() {
        // Set the prefix and suffix for templates
        var prefix = "/templates"; // Path to the templates folder inside resources
        var suffix = ".hbs"; // File extension for template files
        TemplateLoader loader = new ClassPathTemplateLoader(prefix, suffix);
        return new Handlebars(loader);
    }
}
