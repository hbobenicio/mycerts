package br.com.hugobenicio.mycerts.server;

public class IndexTemplateData {

    private final String greeting;

    public IndexTemplateData(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return this.greeting;
    }

    public boolean isProduction() {
        //TODO make config a dependency and check for it
        return false;
    }
}

