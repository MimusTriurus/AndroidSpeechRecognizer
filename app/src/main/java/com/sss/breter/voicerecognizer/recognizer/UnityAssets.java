package com.sss.breter.voicerecognizer.recognizer;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by venen on 22.10.2016.
 */
public class UnityAssets {

    protected static final String TAG = UnityAssets.class.getSimpleName();
    public static final String ASSET_LIST_NAME = "assets.lst";
    public static final String SYNC_DIR = "sync";
    private final String EMPTY_CRC = "0";
    private final AssetManager assetManager;
    private final File externalDir;

    public UnityAssets(Context context) throws IOException {
        File appDir = context.getExternalFilesDir((String)null);
        if(null == appDir) {
            throw new IOException("cannot get external files dir, external storage state is " + Environment.getExternalStorageState());
        } else {
            this.externalDir = new File(appDir, SYNC_DIR);
            this.assetManager = context.getAssets();
        }
    }

    public UnityAssets(Context context, String dest) throws IOException {
        File appDir = context.getExternalFilesDir((String)null);
        if(null == appDir) {
            throw new IOException("cannot get external files dir, external storage state is " + Environment.getExternalStorageState());
        } else {
            this.externalDir = new File(appDir, dest);
            this.assetManager = context.getAssets();
        }
    }

    public File getExternalDir() {
        return this.externalDir;
    }

    public Map<String, String> getItems() throws IOException {
        HashMap items = new HashMap();
        Iterator i$ = this.readLines(this.openAsset(ASSET_LIST_NAME)).iterator();

        while(i$.hasNext()) {
            String path = (String)i$.next();
            // отключаем проверку контрольной суммы
            //InputStreamReader reader = new InputStreamReader(this.openAsset(path + ".md5"));
            //items.put(path, (new BufferedReader(reader)).readLine());
            InputStreamReader reader = new InputStreamReader(this.openAsset(path));
            items.put(path, EMPTY_CRC);
        }

        return items;
    }

    public Map<String, String> getExternalItems() {
        try {
            HashMap e = new HashMap();
            File assetFile = new File(this.externalDir, ASSET_LIST_NAME);
            Iterator i$ = this.readLines(new FileInputStream(assetFile)).iterator();

            while(i$.hasNext()) {
                String line = (String)i$.next();
                String[] fields = line.split(" ");
                // отключаем проверку контрольной суммы
                e.put(fields[0], EMPTY_CRC);
                //e.put(fields[0], fields[1]);
            }

            return e;
        } catch (IOException var6) {
            return Collections.emptyMap();
        }
    }

    public Collection<String> getItemsToCopy(String path) throws IOException {
        ArrayList items = new ArrayList();
        ArrayDeque queue = new ArrayDeque();
        queue.offer(path);

        while(!queue.isEmpty()) {
            path = (String)queue.poll();
            String[] list = this.assetManager.list(path);
            String[] arr$ = list;
            int len$ = list.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String nested = arr$[i$];
                queue.offer(nested);
            }

            if(list.length == 0) {
                items.add(path);
            }
        }

        return items;
    }

    private List<String> readLines(InputStream source) throws IOException {
        ArrayList lines = new ArrayList();
        BufferedReader br = new BufferedReader(new InputStreamReader(source));

        String line;
        while(null != (line = br.readLine())) {
            lines.add(line);
        }

        return lines;
    }

    private InputStream openAsset(String asset) throws IOException {
        return this.assetManager.open((new File(SYNC_DIR, asset)).getPath());
    }

    public void updateItemList(Map<String, String> items) throws IOException {
        File assetListFile = new File(this.externalDir, ASSET_LIST_NAME);
        PrintWriter pw = new PrintWriter(new FileOutputStream(assetListFile));
        Iterator i$ = items.entrySet().iterator();

        while(i$.hasNext()) {
            Map.Entry entry = (Map.Entry)i$.next();
            pw.format("%s %s\n", new Object[]{entry.getKey(), entry.getValue()});
        }

        pw.close();
    }

    public File copy(String asset) throws IOException {
        InputStream source = this.openAsset(asset);
        File destinationFile = new File(this.externalDir, asset);
        destinationFile.getParentFile().mkdirs();
        FileOutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];

        int nread;
        while((nread = source.read(buffer)) != -1) {
            if(nread == 0) {
                nread = source.read();
                if(nread < 0) {
                    break;
                }

                destination.write(nread);
            } else {
                destination.write(buffer, 0, nread);
            }
        }

        destination.close();
        return destinationFile;
    }

    public File syncAssets() throws IOException {
        ArrayList newItems = new ArrayList();
        ArrayList unusedItems = new ArrayList();
        Map items = this.getItems();
        Map externalItems = this.getExternalItems();
        Iterator i$ = items.keySet().iterator();

        while(true) {
            String path;
            while(i$.hasNext()) {
                path = (String)i$.next();
                if(((String)items.get(path)).equals(externalItems.get(path)) && (new File(this.externalDir, path)).exists())
                {
                    Log.i(TAG, String.format("Skipping asset %s: checksums are equal", new Object[]{path}));
                }
                else
                {
                    newItems.add(path);
                }
            }

            unusedItems.addAll(externalItems.keySet());
            unusedItems.removeAll(items.keySet());
            i$ = newItems.iterator();

            File file;
            while(i$.hasNext()) {
                path = (String)i$.next();
                file = this.copy(path);
                Log.i(TAG, String.format("Copying asset %s to %s", new Object[]{path, file}));
            }

            i$ = unusedItems.iterator();

            while(i$.hasNext()) {
                path = (String)i$.next();
                file = new File(this.externalDir, path);
                file.delete();
                Log.i(TAG, String.format("Removing asset %s", new Object[]{file}));
            }

            this.updateItemList(items);
            return this.externalDir;
        }
    }

}
