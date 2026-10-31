package com.inter.efficientia_mobile.main;

import android.text.Html;
import android.text.Spanned;

import java.util.regex.Pattern;

/** Renders the basic Markdown used in CinttIA replies without loading remote HTML. */
final class CinttiaMarkdown {
    private static final Pattern CODE = Pattern.compile("`([^`\\n]+)`");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");

    private CinttiaMarkdown() { }

    static Spanned render(String markdown) {
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) html.append("<br>");
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("# ") || trimmed.startsWith("## ")
                    || trimmed.startsWith("### ")) {
                line = trimmed.replaceFirst("^#{1,3}\\s+", "");
                html.append("<b>");
                appendInline(html, line);
                html.append("</b>");
            } else if (trimmed.matches("^[-*]\\s+.*")) {
                html.append("&#8226; ");
                appendInline(html, trimmed.substring(2));
            } else {
                appendInline(html, line);
            }
        }
        return Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_COMPACT);
    }

    private static void appendInline(StringBuilder html, String line) {
        String escaped = Html.escapeHtml(line);
        escaped = CODE.matcher(escaped).replaceAll("<tt>$1</tt>");
        escaped = BOLD.matcher(escaped).replaceAll("<b>$1</b>");
        escaped = ITALIC.matcher(escaped).replaceAll("<i>$1</i>");
        html.append(escaped);
    }
}
