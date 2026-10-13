package com.inter.efficientia_mobile.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Builds a readable PDF from the server report, including available signature PNGs. */
final class RouteReportPdf {
    private static final String[] ROLES = {"pecuarista", "motorista", "manobrista", "curraleiro"};
    private static final String[] LABELS = {"Pecuarista", "Motorista", "Manobrista", "Curraleiro"};

    private RouteReportPdf() { }

    static void open(Activity activity, JsonObject report, String authorization) {
        Toast.makeText(activity, "Preparando PDF do diário…", Toast.LENGTH_SHORT).show();
        RouteDiaryService api = RetrofitClient.getInstance().create(RouteDiaryService.class);
        Map<String, Bitmap> signatures = new HashMap<>();
        fetchSignature(activity, api, report, authorization, signatures, 0);
    }

    private static void fetchSignature(Activity activity, RouteDiaryService api, JsonObject report,
                                       String authorization, Map<String, Bitmap> images, int index) {
        if (index == ROLES.length) {
            try {
                File pdf = create(activity, report, images);
                Uri uri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".profilephotos", pdf);
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setDataAndType(uri, "application/pdf");
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(view, "Abrir diário em PDF"));
            } catch (IOException | RuntimeException error) {
                if (!activity.isFinishing()) new AlertDialog.Builder(activity)
                        .setTitle("Não foi possível abrir o PDF")
                        .setMessage("Tente novamente ou instale um leitor de PDF no aparelho.")
                        .setPositiveButton("Entendi", null).show();
            } finally {
                for (Bitmap bitmap : images.values()) bitmap.recycle();
            }
            return;
        }
        String role = ROLES[index];
        String field = "urlAssinatura" + Character.toUpperCase(role.charAt(0)) + role.substring(1);
        if (value(report, field).isEmpty()) {
            fetchSignature(activity, api, report, authorization, images, index + 1);
            return;
        }
        int id = report.get("id").getAsInt();
        Call<ResponseBody> call = "motorista".equals(role)
                ? api.driverSignature(authorization) : api.signature(authorization, id, role);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(body.byteStream());
                        if (bitmap != null) images.put(role, bitmap);
                    }
                }
                fetchSignature(activity, api, report, authorization, images, index + 1);
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                if (!activity.isFinishing() && !activity.isDestroyed())
                    fetchSignature(activity, api, report, authorization, images, index + 1);
            }
        });
    }

    private static File create(Activity activity, JsonObject report, Map<String, Bitmap> images)
            throws IOException {
        File directory = new File(activity.getCacheDir(), "route_reports");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Pasta de PDF indisponível.");
        File output = new File(directory, "diario-rota-" + report.get("id").getAsInt() + ".pdf");
        PdfDocument pdf = new PdfDocument();
        try {
            Writer writer = new Writer(pdf, report.get("id").getAsInt());
            writer.section("Dados da viagem");
            writer.row("Situação", value(report, "status"));
            writer.row("Origem", value(report, "nomeOrigem"));
            writer.row("Destino", value(report, "nomeDestino"));
            writer.row("GTA", value(report, "numeroGta"));
            writer.row("Nota fiscal", value(report, "numeroNotaFiscal"));
            writer.row("Data de embarque", value(report, "dataEmbarque"));
            writer.row("Horário de embarque", value(report, "horarioEmbarque"));
            writer.row("Saída da propriedade", value(report, "horarioSaidaPropriedade"));
            writer.row("Quilometragem de saída", value(report, "kmSaidaEmbarcadouro"));
            writer.row("Data de chegada", value(report, "dataChegadaUnidade"));
            writer.row("Horário de chegada", value(report, "horarioChegadaUnidade"));
            writer.row("Desembarque", value(report, "horarioDesembarque"));
            writer.row("Quilometragem de chegada", value(report, "kmChegadaDesembarcadouro"));
            writer.row("Curral", value(report, "numeroCurral"));
            writer.row("Sirene de ré funcionou", bool(report, "sireneReFuncionou"));
            writer.section("Animais");
            writer.row("Machos", value(report, "quantidadeMachos"));
            writer.row("Fêmeas", value(report, "quantidadeFemeas"));
            writer.row("Marrucos", value(report, "quantidadeMarrucos"));
            writer.row("Em pé", value(report, "quantidadeEmPe"));
            writer.row("Deitados", value(report, "quantidadeDeitado"));
            writer.row("Mortos", value(report, "quantidadeMorto"));
            writer.row("Emergência", value(report, "quantidadeEmergencia"));
            writer.row("Motivo de emergência", value(report, "motivoEmergencia"));
            writer.section("Ocorrências");
            JsonArray stops = array(report, "paradasImprevistas");
            for (int i = 0; i < stops.size(); i++) {
                JsonObject stop = stops.get(i).getAsJsonObject();
                writer.row("Parada " + (i + 1), first(value(stop, "descricaoMotivo"), value(stop, "motivo")));
                writer.row("Início / fim", value(stop, "dataHoraInicio") + " / " + value(stop, "dataHoraFim"));
            }
            JsonArray embark = array(report, "anomaliasEmbarque");
            for (int i = 0; i < embark.size(); i++) {
                JsonObject item = embark.get(i).getAsJsonObject();
                writer.row("Anomalia no embarque " + (i + 1),
                        first(value(item, "descricaoOutros"), value(item, "anomalia"))
                                + " - " + value(item, "quantidadeAnimais") + " animais");
            }
            JsonArray unloading = array(report, "anomaliasDesembarque");
            for (int i = 0; i < unloading.size(); i++) {
                JsonObject item = unloading.get(i).getAsJsonObject();
                writer.row("Anomalia no desembarque " + (i + 1),
                        first(value(item, "descricaoOutros"), value(item, "anomalia"))
                                + " - " + value(item, "quantidadeAnimais") + " animais");
            }
            writer.row("Comentários", value(report, "comentarios"));
            writer.section("Assinaturas");
            for (int i = 0; i < ROLES.length; i++) {
                String role = ROLES[i];
                String field = "urlAssinatura" + Character.toUpperCase(role.charAt(0)) + role.substring(1);
                writer.signature(LABELS[i], value(report, field), images.get(role));
            }
            writer.finish();
            try (FileOutputStream stream = new FileOutputStream(output)) { pdf.writeTo(stream); }
        } finally {
            pdf.close();
        }
        return output;
    }

    private static String value(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
    private static String first(String value, String fallback) { return value.isEmpty() ? fallback : value; }
    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }
    private static String bool(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsBoolean() ? "Sim" : "Não";
    }

    private static final class Writer {
        private final PdfDocument document;
        private final int reportId;
        private PdfDocument.Page page;
        private Canvas canvas;
        private int number;
        private float y;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Writer(PdfDocument document, int reportId) {
            this.document = document;
            this.reportId = reportId;
            newPage();
        }

        void section(String title) {
            ensure(44);
            y += 16;
            paint.setColor(Color.rgb(9, 58, 133));
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
            paint.setTextSize(14);
            canvas.drawText(title, 40, y, paint);
            y += 19;
            paint.setColor(Color.LTGRAY);
            canvas.drawLine(40, y, 555, y, paint);
            y += 15;
        }

        void row(String label, String value) {
            if (value == null || value.isBlank()) return;
            line(label + ": " + value);
        }

        void signature(String label, String url, Bitmap bitmap) {
            ensure(112);
            line(label + (url.isBlank() ? ": não registrada" : ":"));
            if (bitmap != null) {
                float scale = Math.min(160f / bitmap.getWidth(), 72f / bitmap.getHeight());
                float width = bitmap.getWidth() * scale;
                float height = bitmap.getHeight() * scale;
                canvas.drawBitmap(bitmap, null,
                        new android.graphics.RectF(47, y, 47 + width, y + height), paint);
                y += height + 13;
            } else if (!url.isBlank()) {
                line("Imagem indisponível para visualização offline.");
            }
        }

        private void line(String value) {
            paint.setColor(Color.rgb(32, 46, 69));
            paint.setTypeface(android.graphics.Typeface.create("sans-serif", 0));
            paint.setTextSize(10.5f);
            String remaining = value.replace('\n', ' ');
            while (!remaining.isEmpty()) {
                int count = paint.breakText(remaining, true, 510, null);
                if (count <= 0) break;
                if (count < remaining.length()) {
                    int space = remaining.lastIndexOf(' ', count);
                    if (space > 0) count = space;
                }
                ensure(17);
                canvas.drawText(remaining.substring(0, count).trim(), 44, y, paint);
                y += 17;
                remaining = remaining.substring(count).trim();
            }
        }

        private void ensure(float height) { if (y + height > 780) newPage(); }

        private void newPage() {
            if (page != null) document.finishPage(page);
            number++;
            page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, number).create());
            canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            paint.setColor(Color.rgb(8, 52, 123));
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-bold", 0));
            paint.setTextSize(18);
            canvas.drawText("Efficientia - Diário de rota", 40, 47, paint);
            paint.setTextSize(9);
            canvas.drawText("Relatório #" + reportId + "  |  Página " + number, 40, 815, paint);
            y = 72;
        }

        void finish() { document.finishPage(page); }
    }
}
