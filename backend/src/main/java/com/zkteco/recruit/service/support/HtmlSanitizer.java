package com.zkteco.recruit.service.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * XSS 过滤（§16.2）。
 * <p>
 * 富文本（岗位职责、任职要求）保留基础排版标签；纯文本字段（首页文案）一律去标签。
 */
public final class HtmlSanitizer {

    private static final Safelist RICH_TEXT = Safelist.none()
            .addTags("p", "br", "ul", "ol", "li", "strong", "em", "b", "i", "h3", "h4")
            .addProtocols("a", "href", "http", "https");

    private HtmlSanitizer() {
    }

    /** 富文本：保留白名单标签，去掉所有属性与脚本 */
    public static String richText(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input, RICH_TEXT);
    }

    /** 纯文本：彻底去标签，并还原实体，保留换行 */
    public static String plainText(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = Jsoup.clean(input, Safelist.none());
        return Jsoup.parse(cleaned).text();
    }
}
