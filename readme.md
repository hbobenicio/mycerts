# mycerts

## Build

```bash
mvn package
```

## Autocompletion

```bash
# Generate the autocompletion script
java -cp target/mycerts.jar picocli.AutoComplete br.com.hugobenicio.mycerts.cmd.RootCommand

# Source it to your terminal session
. mycerts_completion

# You need to alias it for the program name to be compatible with the completion script
alias mycerts="java -jar target/mycerts.jar"

# you may now use autocompletion with tabs
```

> Picocli Autocompletion Support: https://picocli.info/autocomplete.html


## Commands

### Poke (aka SSLPoke)

```bash
java -jar target/mycerts.jar poke --host=www.foo.bar
java -jar target/mycerts.jar poke --host=www.foo.bar --port=8443
```
