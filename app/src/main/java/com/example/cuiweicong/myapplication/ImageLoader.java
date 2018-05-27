package com.example.cuiweicong.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {
    private LruCache<String, Bitmap> lruCache;
    private static ImageLoader instance;
    private DiskLruCache diskLruCache;
    private static final int DISK_CACHE_INDEX = 1;
    private static final int IO_BUFFERED_SIZE = 1024 * 8;
    private static final int MESSAGE_IMAGE_RESULT = 1;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int KEEP_ALIVE = 60;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2;
    private static ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    private ImageResizer imageResizer;
    private Handler mainHandler;

    private ImageLoader(Context context) {
        imageResizer = new ImageResizer();
        int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
        lruCache = new LruCache<String, Bitmap>(memoryCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(context, "bitmap");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdir();
        }
        int diskCacheSize = 1024 * 1024 * 50;
        if (diskCacheDir.getUsableSpace() > diskCacheSize) try {
            diskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, diskCacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MESSAGE_IMAGE_RESULT) {
                    ImageResult imageResult = (ImageResult) msg.obj;
                    imageResult.imageView.setImageBitmap(imageResult.bitmap);
                    imageResult.imageView.setTag(imageResult.uri);
                }
            }
        };

        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "ImageLoader" + count.getAndIncrement());
            }
        };
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), threadFactory);
    }

    public static void init(Context context){
        instance = new ImageLoader(context);
    }

    public static ImageLoader getInstance() {
        return instance;
    }

    public void addBitmapToMemoryCache(String url, Bitmap bitmap){
        if (url == null || url.length() == 0) {
            throw new RuntimeException("url不能为空");
        }
        String key = hashKeyFromUrl(url);
        if (key != null) {
            lruCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key){
        return lruCache.get(key);
    }

    public Bitmap loadBitmapFromMemoryCache(String url){
        String key = hashKeyFromUrl(url);
        if (key != null) {
            return getBitmapFromMemoryCache(key);
        }
        return null;
    }

    private String hashKeyFromUrl(String url){
        String cacheKey;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            cacheKey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = url;
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xff & aByte);
            if (hex.length() == 0) {
                sb.append("0");
            } else {
                sb.append(hex);
            }
        }
        return sb.toString();
    }

    public File getDiskCacheDir(Context context, String path){
        String cachePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + path);
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w("tag", "Load bitmap from main thread is not recommend");
        }
        if (diskLruCache == null){
            return null;
        }
        if (url == null || url.length() == 0) {
            return null;
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
        if (snapshot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            Bitmap bitmap = imageResizer.decodeSampleBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(url, bitmap);
            }
            return bitmap;
        }
        return null;
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Can not visit network form UI thread!");
        }
        if (url == null || url.length() == 0) {
            throw new RuntimeException("url 不能为空");
        }
        if (diskLruCache == null) {
            return null;
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = diskLruCache.edit(key);
        OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
        if (downLoadBitmapFromNet(url, outputStream)) {
            editor.commit();
        } else {
            editor.abort();
        }
        diskLruCache.flush();
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private boolean downLoadBitmapFromNet(String url, OutputStream outputStream){
        HttpURLConnection httpURLConnection = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            URL httpUrl = new URL(url);
            httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), IO_BUFFERED_SIZE);
            bufferedOutputStream = new BufferedOutputStream(outputStream, IO_BUFFERED_SIZE);
            int b;
            while ((b = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(b);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 同步加载图片，需要在工作线程运行
     * @param url 图片地址
     * @param reqWidth 宽
     * @param reqHeight 高
     * @return bitmap
     */
    public Bitmap loadBitmap(String url, int reqWidth, int reqHeight){
        if (url == null) {
            return null;
        }
        String key = hashKeyFromUrl(url);
        Bitmap bitmap = loadBitmapFromMemoryCache(key);
        if (bitmap != null) {
            return bitmap;
        }
        try {
            bitmap = loadBitmapFromDiskCache(url, reqWidth, reqHeight);
            if (bitmap != null) {
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
            if (bitmap != null) {
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return downLoadBitmapFromUrl(url);
    }

    private Bitmap downLoadBitmapFromUrl(String url){
        Bitmap bitmap = null;
        HttpURLConnection httpURLConnection = null;
        InputStream in = null;
        try {
            URL httpUrl = new URL(url);
            httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
            in = httpURLConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(in);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight){
        if (uri == null) {
            throw new RuntimeException("url 不能为空！");
        }
        Bitmap bitmap = loadBitmapFromMemoryCache(uri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);
                if (bitmap != null) {
                    Message message = mainHandler.obtainMessage();
                    message.obj = new ImageResult(imageView, uri, bitmap);
                    message.what = MESSAGE_IMAGE_RESULT;
                    mainHandler.sendMessage(message);
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(runnable);
    }

    private static class ImageResult{
        private ImageView imageView;
        private String uri;
        private Bitmap bitmap;

        public ImageResult(ImageView imageView, String uri, Bitmap bitmap) {
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }

}