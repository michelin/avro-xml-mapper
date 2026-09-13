/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.michelin.avroxmlmapper.utility;

import com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for formatting XPath expressions. */
public class XPathFormatter {

    /** Private constructor. */
    private XPathFormatter() {}

    /**
     * Formats an XPath expression before evaluation. Adds the generic namespace and makes attribute filters
     * case-insensitive.
     *
     * @param xpath The XPath expression to format.
     * @return The formatted XPath expression.
     */
    public static String format(String xpath) {
        if (xpath == null) {
            return null;
        }

        StringBuilder excludePrefixes = new StringBuilder();
        for (Matcher m = Pattern.compile("(\\w+)\\:").matcher(xpath); m.find(); ) {
            excludePrefixes.append(m.group().replace(":", "|"));
        }

        // regex means :
        // - ((\A)|[/\[]) : the first character of the match is the beginning of input, a slash or an opening bracket
        // - (?!and|or|not)(\w+) : any word, except the prefixes previously captured and operators 'and' and 'or'
        String tagToAliasPattern = "((\\A)|[/\\[])(?!" + excludePrefixes + "and|or|not)(\\w+)";

        // regex means :
        // - ((and|or|not)[ ]) : any combination of and/r/not with a subsequent whitespace
        // - (?!and|or|not)(\w+) : any word, except the prefixes previously captured and operators 'and' and 'or'
        String tagToAliasPattern2 = "((and|or|not)[ ])(?!" + excludePrefixes + "and|or|not)(\\w+)";

        String xpathPrefixed = Pattern.compile(tagToAliasPattern).matcher(xpath).replaceAll(m -> {
            String match = m.group();
            return match.substring(0, 1).matches("[/\\[]")
                    ? match.charAt(0) + AvroXmlMapperConstants.NO_PREFIX_NS + ":" + match.substring(1)
                    : AvroXmlMapperConstants.NO_PREFIX_NS + ":" + match;
        });

        String xpathPrefixed2 = Pattern.compile(tagToAliasPattern2)
                .matcher(xpathPrefixed)
                .replaceAll(m -> {
                    String subMatch1 = m.group(1);
                    String subMatch3 = m.group(3);

                    return subMatch1 + AvroXmlMapperConstants.NO_PREFIX_NS + ":" + subMatch3;
                });

        // regex means the first character is a '@' followed by any word (i.e. an attribute)

        return Pattern.compile("@(\\w*)").matcher(xpathPrefixed2).replaceAll(m -> {
            String match = m.group();
            return "@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '"
                    + match.toLowerCase().substring(1) + "']";
        });
    }
}
