package br.com.hugobenicio.mycerts.core.pem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is a simple PEM splitter that just tries to split pem blocks and give them to jvm X.509 certificate factory
 * (which supports PEM parsing of certificates even excluding comments)
 */
public class PemSplitter {

    private static final Pattern blockFooterPattern = Pattern.compile("(.+?)-----END (.+?)-----", Pattern.DOTALL);

    public PemSplitter() {}

    public List<PemBlock> split(String input) {
        List<PemBlock> blocks = new ArrayList<>();

        var matcher = blockFooterPattern.matcher(input);
        while (matcher.find()) {
            String block = matcher.group();
            String blockType = matcher.group(2);
            blocks.add(new PemBlock(blockType, block));
        }

        return blocks;
    }
}
