package com.example.camera4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageProcess {

    // put your server IP and Port here
    protected final String SERVER_URL = "http://<IP>:<Port>/";

    protected final Executor executor;
    protected final Context context;

    protected String filename;

    protected AtomicBoolean is_processed;

    public ImageProcess(Context context, String filename, AtomicBoolean is_processed) {
        executor = Executors.newSingleThreadExecutor();
        this.context = context;
        this.filename = filename;
        this.is_processed = is_processed;
    }

    public void processImage() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap rotatedBitmap = rotateBitmap(filename);


                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] image_bytes = byteArrayOutputStream.toByteArray();


                // Send the image to the server
                sendImageToServer(image_bytes);


            }
        });
    }

    protected void sendImageToServer(final byte[] imageBytes) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(imageBytes);
                outputStream.flush();
                outputStream.close();

                // TODO error, but image is sent
                int responseCode = connection.getResponseCode();
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    InputStream inputStream = connection.getInputStream();
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        response.append(line);
//                    }
//                    reader.close();
//                    inputStream.close();
////                    connection.disconnect();
//                    Log.d("Response", response.toString());
//                } else {
//                    Log.e("Response Error", "Failed with response code: " + responseCode);
//                }
            } catch (IOException e) {
                Log.e("Error", e.getMessage(), e);
            }


            if (Looper.myLooper() == Looper.getMainLooper()) {
                // We're on the main thread, no need to create a new looper
                Toast.makeText(context, "arait bra?", Toast.LENGTH_SHORT).show();
            } else {
                // We're on a background thread, need to create a new looper
                HandlerThread handlerThread = new HandlerThread("ImageProcessHandlerThread");
                handlerThread.start();
                Looper looper = handlerThread.getLooper();
                Handler handler = new Handler(looper);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "arait bra?", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        }).start();
    }


    // TODO GET picture from the server


    private Bitmap rotateBitmap(String filename) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);
        try {
            ExifInterface exif = new ExifInterface(filename);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
            }
            if (rotationAngle != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationAngle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}