package com.inter.efficientia_mobile.signature;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.inter.efficientia_mobile.auth.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public final class SignatureImageStore {
    private SignatureImageStore() {
    }

    public static String saveDriver(Context context, Bitmap image) throws IOException {
        File directory = new File(context.getFilesDir(), "signatures");
        return writePng(directory, "driver_" + SessionManager.userId(context) + ".png", image);
    }

    public static String driverPath(Context context) {
        File file = new File(new File(context.getFilesDir(), "signatures"),
                "driver_" + SessionManager.userId(context) + ".png");
        return file.isFile() ? file.getAbsolutePath() : null;
    }

    public static String saveRoute(Context context, Bitmap image) throws IOException {
        File directory = new File(context.getCacheDir(), "route_signatures");
        return writePng(directory, UUID.randomUUID() + ".png", image);
    }

    public static Bitmap read(String path) {
        return path == null ? null : BitmapFactory.decodeFile(path);
    }

    private static String writePng(File directory, String name, Bitmap image) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta de assinaturas.");
        }
        File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!image.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Não foi possível salvar a assinatura.");
            }
        }
        return file.getAbsolutePath();
    }
}
