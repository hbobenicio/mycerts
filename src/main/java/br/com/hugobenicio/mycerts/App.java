package br.com.hugobenicio.mycerts;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.javalin.Javalin;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        // Template Engine
        Handlebars handlebars = handlebarsCreate();

        // Templates
        Template indexTemplate = handlebars.compile("index.html");

        // Javalin Configuration
        Javalin app = Javalin.create(config -> {
            config.useVirtualThreads = true;
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
            ctx.header("Content-type", "text/html; charset=utf-8").result(html);
        });

        // After Handlers
        app.after(AccessLogMiddleware::afterAllHandler);

        // Serving
        app.start("127.0.0.1", 8080);
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
