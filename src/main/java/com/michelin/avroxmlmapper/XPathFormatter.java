package com.michelin.avroxmlmapper;

import java.util.regex.Pattern;

public class XPathFormatter {

    /**
     * Helper method to format the xpath before evaluation :
     * - add the generic namespace in order to avoid to set a generic namespace in all our final code xpath expression
     * - re-write filters based on attribute in order to be case-insensitive on attribute name
     *
     * @param xpath xpath to format
     * @return the generic xpath
     */
    public static String format(String xpath) {
        if (xpath == null) {
            return null;
        }

        var excludePrefixes = new StringBuilder();
        for (var m = Pattern.compile("(\\w+)\\:").matcher(xpath); m.find(); ) {
            excludePrefixes.append(m.group().replace(":", "|"));
        }

        // regex means :
        // - ((\A)|[/\[]) : the first character of the match is the beginning of input, a slash or an opening bracket
        // - (?!and|or|not)(\w+) : any word, except the prefixes previously captured and operators 'and' and 'or'
        var tagToAliasPattern = "((\\A)|[/\\[])(?!" + excludePrefixes + "and|or|not)(\\w+)";

        // regex means :
        // - ((and|or|not)[ ]) : any combination of and/r/not with a subsequent whitespace
        // - (?!and|or|not)(\w+) : any word, except the prefixes previously captured and operators 'and' and 'or'
        var tagToAliasPattern2 = "((and|or|not)[ ])(?!" + excludePrefixes + "and|or|not)(\\w+)";


        String xpathPrefixed = Pattern.compile(tagToAliasPattern).matcher(xpath).replaceAll(m -> {
            String match = m.group();
            return match.substring(0, 1).matches("[/\\[]") ? match.substring(0, 1) + XMLUtilsConstants.NO_PREFIX_NS + ":" + match.substring(1) : XMLUtilsConstants.NO_PREFIX_NS + ":" + match;
        });

        String xpathPrefixed2 = Pattern.compile(tagToAliasPattern2).matcher(xpathPrefixed).replaceAll(m -> {
            String subMatch1 = m.group(1);
            String subMatch3 = m.group(3);

            return subMatch1 + XMLUtilsConstants.NO_PREFIX_NS + ":" +subMatch3;
        });

        // regex means the first character is a '@' followed by any word (i.e. an attribute)
        String xpathReplaced = Pattern.compile("@(\\w*)")
                .matcher(xpathPrefixed2)
                .replaceAll(m -> {
                    String match = m.group();
                    return "@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + match.toLowerCase().substring(1) + "']";
                });

        return xpathReplaced;

    }
}
