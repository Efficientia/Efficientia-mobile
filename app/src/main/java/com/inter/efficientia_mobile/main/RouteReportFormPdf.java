package com.inter.efficientia_mobile.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** A4 version of the paper loading/unloading report used by the operation. */
final class RouteReportFormPdf {
    private static final int BLUE = Color.rgb(14, 55, 122);
    private static final int GREEN = Color.rgb(77, 216, 103);
    private static final int INK = Color.rgb(31, 39, 48);
    private static final int BORDER = Color.rgb(119, 128, 139);
    private static final float LEFT = 24;
    private static final float RIGHT = 571;
    private static final float COLUMN = 273.5f;

    private final Activity activity;
    private final JsonObject report;
    private final Map<String, Bitmap> signatures;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Canvas canvas;

    private RouteReportFormPdf(Activity activity, JsonObject report, Map<String, Bitmap> signatures) {
        this.activity = activity;
        this.report = report;
        this.signatures = signatures;
    }

    static File create(Activity activity, JsonObject report, Map<String, Bitmap> signatures)
            throws IOException {
        File directory = new File(activity.getCacheDir(), "route_reports");
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IOException("Pasta de PDF indisponível.");
        File output = new File(directory, "diario-rota-" + value(report, "id") + ".pdf");
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.Page page = document.startPage(
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create());
            RouteReportFormPdf form = new RouteReportFormPdf(activity, report, signatures);
            form.canvas = page.getCanvas();
            form.draw();
            document.finishPage(page);
            try (FileOutputStream stream = new FileOutputStream(output)) {
                document.writeTo(stream);
            }
        } finally {
            document.close();
        }
        return output;
    }

    private void draw() {
        canvas.drawColor(Color.WHITE);
        header();
        details();
        loadingAnimals();
        loadingInformation();
        signature("pecuarista", "Assinatura do responsável (fazenda)", LEFT, 331, RIGHT - LEFT, 53);
        tripInformation();
        unloadingInformation();
        animalCondition();
        comments();
        signature("motorista", "Assinatura do motorista", LEFT, 660, 182, 134);
        signature("manobrista", "Assinatura do manobrista", LEFT + 182, 660, 182, 134);
        signature("curraleiro", "Assinatura do recebedor (curraleiro)", LEFT + 364, 660, 183, 134);
        footer();
    }

    private void header() {
        fill(BLUE);
        canvas.drawRoundRect(new RectF(LEFT, 22, RIGHT, 77), 5, 5, paint);
        Drawable logo = ContextCompat.getDrawable(activity, R.drawable.logo_effi);
        if (logo != null) {
            logo.setBounds(32, 26, 78, 72);
            logo.draw(canvas);
        }
        text("Efficient", 82, 49, 18, Color.WHITE, true);
        float suffixX = 82 + measure("Efficient", 18, true);
        text("ia", suffixX, 49, 18, GREEN, true);
        text("RELATÓRIO DE EMBARQUE E DESEMBARQUE", 82, 66, 10, Color.WHITE, true);
        text("Nº " + display(value(report, "id")), RIGHT - 61, 38, 8, Color.WHITE, false);
    }

    private void details() {
        sectionTitle("DADOS DA VIAGEM", 78, 96);
        twoFields(96, "Fazenda / origem", origin(), "Unidade / destino", destination());
        twoFields(114, "Nome pecuarista", value(report, "nomePecuarista"),
                "Código motorista", value(report, "motoristaId"));
        twoFields(132, "Placa cavalo", value(report, "placaCavalo"),
                "Placa carreta", value(report, "placaCarreta"));
        twoFields(150, "Data de embarque", date(value(report, "dataEmbarque")),
                "Horário de embarque", time(value(report, "horarioEmbarque")));
        twoFields(168, "Saída da propriedade", time(value(report, "horarioSaidaPropriedade")),
                "Nº nota fiscal", value(report, "numeroNotaFiscal"));
        twoFields(186, "Nº GTA", value(report, "numeroGta"),
                "Km saída do embarcadouro", value(report, "kmSaidaEmbarcadouro"));
    }

    private void loadingAnimals() {
        sectionTitle("ANIMAIS / EMBARQUE", 204, 221);
        fourValues(221, "Machos", value(report, "quantidadeMachos"),
                "Fêmeas", value(report, "quantidadeFemeas"),
                "Marrucos", value(report, "quantidadeMarrucos"),
                "Total", value(report, "totalAnimais"));
    }

    private void loadingInformation() {
        sectionTitle("INFORMAÇÕES EMBARQUE", 239, 255);
        JsonArray anomalies = array(report, "anomaliasEmbarque");
        checkboxRow(255, anomalies, "Animal sangrando", "sangr", "Excesso de magreza", "magrez", "Cutucões fortes", "cutuc");
        checkboxRow(274, anomalies, "Animal mancando", "manc", "Animal sujo / pisoteio", "pisote", "Quebra da cauda", "cauda");
        checkboxRow(293, anomalies, "Gaiola muito cheia", "gaiola", "Chute / paulada / ferrão", "chute", "Arraste", "arraste");
        checkboxRow(312, anomalies, "Nenhum", "__empty__", "Outro", "outro", "", "");
    }

    private void tripInformation() {
        sectionTitle("INFORMAÇÕES VIAGEM", 384, 400);
        JsonArray stops = array(report, "paradasImprevistas");
        field(LEFT, 400, 131, "Parada imprevista?", stops.size() == 0 ? "Não" : "Sim");
        field(LEFT + 131, 400, RIGHT - LEFT - 131, "Motivo / descrição",
                stops.size() == 0 ? "" : first(value(stops.get(0).getAsJsonObject(), "descricaoMotivo"),
                        value(stops.get(0).getAsJsonObject(), "motivo")));
        twoFields(418, "Data de chegada na unidade", date(value(report, "dataChegadaUnidade")),
                "Horário de chegada", time(value(report, "horarioChegadaUnidade")));
        twoFields(436, "Km chegada desembarcadouro", value(report, "kmChegadaDesembarcadouro"),
                "Horário de desembarque", time(value(report, "horarioDesembarque")));
    }

    private void unloadingInformation() {
        sectionTitle("INFORMAÇÕES DESEMBARQUE", 454, 470);
        JsonArray anomalies = array(report, "anomaliasDesembarque");
        checkboxRow(470, anomalies, "Gaiola danificada", "gaiola", "Porteiras não abrem", "porteira", "Uso abusivo de choque", "choque");
        checkboxRow(489, anomalies, "Problema no veículo", "veiculo", "Outros atos de abuso", "abuso", "Nenhum", "__empty__");
        field(LEFT, 508, 170, "Nº curral", value(report, "numeroCurral"));
        field(LEFT + 170, 508, RIGHT - LEFT - 170, "Sirene de ré funcionou?",
                bool(value(report, "sireneReFuncionou")));
        field(LEFT, 526, RIGHT - LEFT, "Outras anomalias", describe(anomalies));
    }

    private void animalCondition() {
        sectionTitle("INFORMAÇÕES ANIMAIS", 544, 560);
        fourValues(560, "Em pé", value(report, "quantidadeEmPe"),
                "Deitados", value(report, "quantidadeDeitado"),
                "Mortos", value(report, "quantidadeMorto"),
                "Emergência", value(report, "quantidadeEmergencia"));
        field(LEFT, 578, RIGHT - LEFT, "Motivo da emergência", value(report, "motivoEmergencia"));
    }

    private void comments() {
        sectionTitle("COMENTÁRIOS E OBSERVAÇÕES", 596, 612);
        outline(LEFT, 612, RIGHT, 660);
        String comments = value(report, "comentarios").replace('\n', ' ').trim();
        drawWrapped(comments, LEFT + 6, 626, RIGHT - LEFT - 12, 10, 3);
    }

    private void footer() {
        line(LEFT, 802, RIGHT, 802);
        text("Relatório Efficientia • Documento gerado a partir dos dados do diário de rota.",
                LEFT + 4, 814, 7.5f, BORDER, false);
        text("ID " + display(value(report, "id")), RIGHT - 57, 814, 7.5f, BORDER, false);
    }

    private void signature(String role, String label, float x, float top, float width, float height) {
        outline(x, top, x + width, top + height);
        Bitmap bitmap = signatures.get(role);
        if (bitmap != null) {
            Rect ink = inkBounds(bitmap);
            if (ink != null) {
                float availableWidth = width - 22;
                float availableHeight = height - 34;
                float scale = Math.min(availableWidth / ink.width(), availableHeight / ink.height());
                float imageWidth = ink.width() * scale;
                float imageHeight = ink.height() * scale;
                float left = x + (width - imageWidth) / 2;
                float imageTop = top + 3 + (availableHeight - imageHeight) / 2;
                Paint signaturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                // The capture screen writes white strokes on transparent PNGs.
                signaturePaint.setColorFilter(new PorterDuffColorFilter(BLUE, PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(bitmap, ink,
                        new RectF(left, imageTop, left + imageWidth, imageTop + imageHeight),
                        signaturePaint);
            }
        }
        line(x + 10, top + height - 22, x + width - 10, top + height - 22);
        fitted(label, x + 7, top + height - 8, width - 14, 8.3f, INK, false);
    }

    private static Rect inkBounds(Bitmap bitmap) {
        int left = bitmap.getWidth();
        int top = bitmap.getHeight();
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if ((bitmap.getPixel(x, y) >>> 24) < 24) continue;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        if (right < left || bottom < top) return null;
        return new Rect(Math.max(0, left - 3), Math.max(0, top - 3),
                Math.min(bitmap.getWidth(), right + 4), Math.min(bitmap.getHeight(), bottom + 4));
    }

    private void sectionTitle(String title, float top, float bottom) {
        fill(Color.rgb(230, 238, 247));
        canvas.drawRect(LEFT, top, RIGHT, bottom, paint);
        outline(LEFT, top, RIGHT, bottom);
        float width = measure(title, 9, true);
        text(title, LEFT + (RIGHT - LEFT - width) / 2, bottom - 4, 9, BLUE, true);
    }

    private void twoFields(float top, String firstLabel, String firstValue,
                           String secondLabel, String secondValue) {
        field(LEFT, top, COLUMN, firstLabel, firstValue);
        field(LEFT + COLUMN, top, COLUMN, secondLabel, secondValue);
    }

    private void fourValues(float top, String l1, String v1, String l2, String v2,
                            String l3, String v3, String l4, String v4) {
        float width = (RIGHT - LEFT) / 4;
        field(LEFT, top, width, l1, v1);
        field(LEFT + width, top, width, l2, v2);
        field(LEFT + 2 * width, top, width, l3, v3);
        field(LEFT + 3 * width, top, width, l4, v4);
    }

    private void field(float x, float top, float width, String label, String value) {
        outline(x, top, x + width, top + 18);
        fitted(label, x + 4, top + 7, width - 8, 6.9f, BLUE, true);
        fitted(display(value), x + 4, top + 16, width - 8, 8.4f, INK, false);
    }

    private void checkboxRow(float top, JsonArray values,
                             String label1, String token1, String label2, String token2,
                             String label3, String token3) {
        float width = (RIGHT - LEFT) / 3;
        checkbox(LEFT, top, width, label1, contains(values, token1));
        checkbox(LEFT + width, top, width, label2, contains(values, token2));
        checkbox(LEFT + 2 * width, top, width, label3, contains(values, token3));
    }

    private void checkbox(float x, float top, float width, String label, boolean checked) {
        outline(x, top, x + width, top + 19);
        if (label.isEmpty()) return;
        outline(x + 5, top + 5, x + 14, top + 14);
        if (checked) {
            text("X", x + 6.5f, top + 13, 9, BLUE, true);
        }
        fitted(label, x + 19, top + 13, width - 23, 7.7f, INK, false);
    }

    private void drawWrapped(String value, float x, float baseline, float width,
                             float size, int maxLines) {
        if (value.isEmpty()) return;
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(INK);
        String remaining = value;
        for (int i = 0; i < maxLines && !remaining.isEmpty(); i++) {
            int count = paint.breakText(remaining, true, width, null);
            if (count <= 0) return;
            if (count < remaining.length()) {
                int wordEnd = remaining.lastIndexOf(' ', count);
                if (wordEnd > 0) count = wordEnd;
            }
            String part = remaining.substring(0, count).trim();
            if (i == maxLines - 1 && count < remaining.length()) {
                while (!part.isEmpty() && measure(part + "…", size, false) > width)
                    part = part.substring(0, part.length() - 1);
                part += "…";
            }
            canvas.drawText(part, x, baseline + i * 13, paint);
            remaining = remaining.substring(count).trim();
        }
    }

    private void outline(float left, float top, float right, float bottom) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.55f);
        paint.setColor(BORDER);
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void line(float startX, float startY, float endX, float endY) {
        paint.setColor(BORDER);
        paint.setStrokeWidth(0.6f);
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    private void fill(int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    private void text(String value, float x, float baseline, float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        canvas.drawText(value, x, baseline, paint);
    }

    private void fitted(String value, float x, float baseline, float width,
                        float size, int color, boolean bold) {
        text(ellipsize(value, width, size, bold), x, baseline, size, color, bold);
    }

    private String ellipsize(String value, float width, float size, boolean bold) {
        if (measure(value, size, bold) <= width) return value;
        String result = value;
        while (!result.isEmpty() && measure(result + "…", size, bold) > width)
            result = result.substring(0, result.length() - 1);
        return result + "…";
    }

    private float measure(String value, float size, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return paint.measureText(value);
    }

    private String origin() { return first(value(report, "nomeOrigem"), value(report, "fazendaId")); }
    private String destination() { return first(value(report, "nomeDestino"), value(report, "unidadeFrigorificaId")); }

    private static String value(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private static String first(String value, String fallback) { return value.trim().isEmpty() ? fallback : value; }
    private static String display(String value) { return value == null || value.trim().isEmpty() ? "-" : value; }
    private static String bool(String value) { return "true".equals(value) ? "Sim" : "false".equals(value) ? "Não" : ""; }
    private static String time(String value) { return value.length() >= 5 ? value.substring(0, 5) : value; }
    private static String date(String value) {
        if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) return value;
        return value.substring(8) + "/" + value.substring(5, 7) + "/" + value.substring(0, 4);
    }
    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }
    private static boolean contains(JsonArray array, String token) {
        if ("__empty__".equals(token)) return array.size() == 0;
        if (token.isEmpty()) return false;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            String content = element.toString().toLowerCase(Locale.ROOT);
            if (content.contains(token)) return true;
        }
        return false;
    }
    private static String describe(JsonArray array) {
        StringBuilder result = new StringBuilder();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String description = first(value(item, "descricaoOutros"), value(item, "anomalia"));
            if (description.trim().isEmpty()) continue;
            if (result.length() > 0) result.append("; ");
            result.append(description);
        }
        return result.toString();
    }
}
