package com.paulograbin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlCheckerLogicTest {

    @Nested
    class RouteCookieTest {

        /*
         * These strings are Set-Cookie header values: name=value, optionally followed
         * by semicolon-separated attributes. Both "ROUTE=node-1" and
         * "ROUTE=.node-1; Path=/; Secure" are valid. A dot in the value and a
         * trailing semicolon are not required.
         *
         * "default" and "invalid" are malformed: they have no name=value pair.
         * The following are valid cookie syntax but unusable for server discovery:
         * - "SESSION=abc; Path=/": a different cookie, not ROUTE.
         * - "ROUTE=; Path=/": an empty value cannot identify a backend.
         * - "ROUTE=node-1; Max-Age=0": instructs the client to delete the cookie.
         */

        @Test
        void ignoresMissingUnrelatedAndMalformedCookies() {
            assertTrue(HtmlChecker.extractRouteCookie(List.of()).isEmpty());
            assertTrue(HtmlChecker.extractRouteCookie(List.of("SESSION=abc; Path=/", "default", "ROUTE=; Path=/")).isEmpty());
        }

        @Test
        void acceptsValuesWithoutDotsOrTrailingAttributes() {
            assertEquals("node-1", HtmlChecker.extractRouteCookie(List.of("ROUTE=node-1")).orElseThrow());
            assertEquals(".node-1", HtmlChecker.extractRouteCookie(List.of("ROUTE=.node-1; Path=/; Secure")).orElseThrow());
        }

        @Test
        void preservesTheFullCookieValue() {
            assertEquals("prefix.node-1", HtmlChecker.extractRouteCookie(List.of("ROUTE=prefix.node-1; Path=/")).orElseThrow());
        }

        @Test
        void skipsMalformedAndDeletedCookiesBeforeAValidCookie() {
            assertEquals("node-2", HtmlChecker.extractRouteCookie(List.of("invalid",
                    "ROUTE=node-1; Max-Age=0", "ROUTE=node-2")).orElseThrow());
        }
    }

    @Nested
    class FilterHtmlContentTest {

        @Test
        void removesCsrfLines() {
            String html = "<html>\n<meta name=\"CSRF\" content=\"token123\">\n<body>hello</body>\n</html>";

            String result = HtmlChecker.filterHtmlContent(html);

            assertFalse(result.contains("CSRF"));
            assertTrue(result.contains("<html>"));
            assertTrue(result.contains("<body>hello</body>"));
        }

        @Test
        void removesParagraphNowLines() {
            String html = "<div>content</div>\n<p>now 2024-01-15 10:30:00</p>\n<div>more</div>";

            String result = HtmlChecker.filterHtmlContent(html);

            assertFalse(result.contains("<p>now"));
            assertTrue(result.contains("<div>content</div>"));
            assertTrue(result.contains("<div>more</div>"));
        }

        @Test
        void keepsNormalLines() {
            String html = "<html>\n<head></head>\n<body>content</body>\n</html>";

            String result = HtmlChecker.filterHtmlContent(html);

            assertTrue(result.contains("<html>"));
            assertTrue(result.contains("<head></head>"));
            assertTrue(result.contains("<body>content</body>"));
            assertTrue(result.contains("</html>"));
        }

        @Test
        void handlesEmptyString() {
            String result = HtmlChecker.filterHtmlContent("");

            assertEquals(System.lineSeparator(), result);
        }

        @Test
        void removesMultipleCsrfLines() {
            String html = "<meta name=\"CSRF\" content=\"a\">\nkeep this\n<input type=\"hidden\" value=\"CSRF\">\nanother keeper";

            String result = HtmlChecker.filterHtmlContent(html);

            assertFalse(result.contains("CSRF"));
            assertTrue(result.contains("keep this"));
            assertTrue(result.contains("another keeper"));
        }

        @Test
        void preservesLineOrder() {
            String html = "first\nCSRF line\nsecond\n<p>now time</p>\nthird";

            String result = HtmlChecker.filterHtmlContent(html);

            int firstIdx = result.indexOf("first");
            int secondIdx = result.indexOf("second");
            int thirdIdx = result.indexOf("third");

            assertTrue(firstIdx < secondIdx);
            assertTrue(secondIdx < thirdIdx);
        }

        @Test
        void csrfCheckIsCaseSensitive() {
            String html = "has csrf lowercase\nhas CSRF uppercase";

            String result = HtmlChecker.filterHtmlContent(html);

            assertTrue(result.contains("has csrf lowercase"), "lowercase csrf should NOT be filtered");
            assertFalse(result.contains("has CSRF uppercase"), "uppercase CSRF should be filtered");
        }
    }

    @Nested
    class CalculateDeviationCountTest {

        @Test
        void identicalSizesReturnZero() {
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L, 1000L, 1000L));

            assertEquals(0, count);
        }

        @Test
        void closeValuesReturnZero() {
            // 1000 → 1050: 5%, 1050 → 1090: ~3.8%
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L, 1050L, 1090L));

            assertEquals(0, count);
        }

        @Test
        void largeGapReturnsOne() {
            // 1000 → 1200: 20%
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L, 1200L));

            assertEquals(1, count);
        }

        @Test
        void multipleDeviations() {
            // 100 → 200: 100%, 200 → 400: 100%
            int count = HtmlChecker.calculateDeviationCount(List.of(100L, 200L, 400L));

            assertEquals(2, count);
        }

        @Test
        void singleElementReturnsZero() {
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L));

            assertEquals(0, count);
        }

        @Test
        void emptyListReturnsZero() {
            int count = HtmlChecker.calculateDeviationCount(List.of());

            assertEquals(0, count);
        }

        @Test
        void exactlyTenPercentIsNotDeviation() {
            // 1000 → 1100: exactly 10%. Threshold is > 10.0, not >=
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L, 1100L));

            assertEquals(0, count);
        }

        @Test
        void justOverTenPercentIsDeviation() {
            // 1000 → 1101: 10.1%
            int count = HtmlChecker.calculateDeviationCount(List.of(1000L, 1101L));

            assertEquals(1, count);
        }

        @Test
        void unsortedInputIsSortedBeforeComparison() {
            // Input in reverse order — should still work correctly
            // 400, 100, 200 → sorted: 100, 200, 400 → two deviations
            int count = HtmlChecker.calculateDeviationCount(List.of(400L, 100L, 200L));

            assertEquals(2, count);
        }

        @Test
        void doesNotMutateInputList() {
            List<Long> input = new java.util.ArrayList<>(List.of(300L, 100L, 200L));
            List<Long> original = List.copyOf(input);

            HtmlChecker.calculateDeviationCount(input);

            assertEquals(original, input);
        }
    }
}
