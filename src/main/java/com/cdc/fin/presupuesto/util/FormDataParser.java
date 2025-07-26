package com.cdc.fin.presupuesto.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FormDataParser {
    public static Map<String, String> parse(String formData) {
        return Arrays.stream(formData.split("&"))
                     .map(s -> s.split("=", 2))
                     .collect(Collectors.toMap(
                         a -> URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                         a -> a.length > 1 ? URLDecoder.decode(a[1], StandardCharsets.UTF_8) : ""
                     ));
    }
}
