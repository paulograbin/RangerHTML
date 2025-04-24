package com.paulograbin;

import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FakeController {


    public static void getLogin(@NotNull Context context) {
        Map<String, String> stringStringMap = new HashMap<>();

        stringStringMap.put("email", null);
        stringStringMap.put("firstName", "Anonymous");
        stringStringMap.put("nam", "Anonymous");
        stringStringMap.put("uid", "Anonymous");

        context.json(stringStringMap);
    }

    public static void getMiniCart(@NotNull Context context) {
        Map<String, String> stringStringMap = new HashMap<>();

        stringStringMap.put("email", null);
        stringStringMap.put("firstName", "Anonymous");
        stringStringMap.put("nam", "Anonymous");
        stringStringMap.put("uid", "Anonymous");

        context.json(stringStringMap);
    }

    public static void getInfo(@NotNull Context context) {
        Map<String, String> stringStringMap = new HashMap<>();

        stringStringMap.put("clearCartEnabled", "false");
        stringStringMap.put("clientJsDomain", "https://web.global-e.com");
        stringStringMap.put("clientJsUrl", " /scripts/merchants/globale.merchant.client.js");
        stringStringMap.put("cookieExpiration", "259200");
        stringStringMap.put("cookieName", "GlobalE_Data");
        stringStringMap.put("defaultCountry", " GB");
        stringStringMap.put("defaultCulture", null);
        stringStringMap.put("defaultCurrency", " GBP");
        stringStringMap.put("enabled", "true");
        stringStringMap.put("locationCountry", null);
        stringStringMap.put("locationCulture", null);
        stringStringMap.put("locationCurrency", null);
        stringStringMap.put("merchantId", "1344");
        stringStringMap.put("scriptVersion", "2.1.4");
        stringStringMap.put("sessionCountry", null);
        stringStringMap.put("sessionCulture", null);
        stringStringMap.put("sessionCurrency", null);

        context.json(stringStringMap);
    }
}
