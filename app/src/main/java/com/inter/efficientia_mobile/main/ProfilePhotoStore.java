package com.inter.efficientia_mobile.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.widget.ImageView;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

final class ProfilePhotoStore {
    private ProfilePhotoStore() { }

    static File photoFile(Context context) {
        return new File(context.getFilesDir(), "driver_profile_" + SessionManager.userId(context) + ".jpg");
    }

    static void show(Context context, ImageView imageView) {
        File file = photoFile(context);
        if (file.isFile()) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1024) {
                options.inSampleSize *= 2;
            }
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                Bitmap circularPhoto = circularCrop(bitmap);
                bitmap.recycle();
                imageView.setBackground(null);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setPadding(0, 0, 0, 0);
                imageView.setImageBitmap(circularPhoto);
                return;
            }
        }
        imageView.setBackgroundResource(R.drawable.main_home_profile_background);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int paddingDp = imageView.getId() == R.id.btnHomeProfile ? 7
                : imageView.getId() == R.id.imgDriverProfile ? 6 : 0;
        int padding = Math.round(paddingDp * context.getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setImageResource(R.drawable.ic_profile_placeholder);
    }

    private static Bitmap circularCrop(Bitmap source) {
        int size = Math.min(Math.min(source.getWidth(), source.getHeight()), 512);
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix transform = new Matrix();
        float scale = Math.max((float) size / source.getWidth(),
                (float) size / source.getHeight());
        transform.setScale(scale, scale);
        transform.postTranslate((size - source.getWidth() * scale) / 2f,
                (size - source.getHeight() * scale) / 2f);
        shader.setLocalMatrix(transform);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(shader);
        new Canvas(result).drawCircle(size / 2f, size / 2f, size / 2f, paint);
        return result;
    }

    static byte[] prepare(Context context, Uri source) throws IOException {
        ImageDecoder.Source decoderSource = ImageDecoder.createSource(context.getContentResolver(), source);
        Bitmap bitmap = ImageDecoder.decodeBitmap(decoderSource, (decoder, info, ignored) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            android.util.Size size = info.getSize();
            int largest = Math.max(size.getWidth(), size.getHeight());
            if (largest > 1024) decoder.setTargetSampleSize((int) Math.ceil(largest / 1024.0));
        });
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                throw new IOException("Não foi possível preparar a foto.");
            }
            return output.toByteArray();
        } finally {
            bitmap.recycle();
        }
    }

    static void saveBytes(Context context, byte[] bytes) throws IOException {
        File target = photoFile(context);
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(bytes);
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    static boolean remove(Context context) {
        File file = photoFile(context);
        return !file.exists() || file.delete();
    }
}
