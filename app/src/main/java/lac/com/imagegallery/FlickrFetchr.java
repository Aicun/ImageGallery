package lac.com.imagegallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aicun on 9/7/2017.
 */

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String FLICKR_KEY = "158f924fd6c5e33a8611c8bdd7625286";

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", FLICKR_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try {
            //connect to url now
            InputStream inputStream = urlConnection.getInputStream();

            if(urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(urlConnection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            int byteRead;
            byte[] buffer = new byte[1024];
            while((byteRead = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer,0,byteRead);
            }
            byteArrayOutputStream.close();
            return  byteArrayOutputStream.toByteArray();
        } finally {
            urlConnection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<ImageGalleryItem> downloadGalleryItems(String page, String per_page) {

        List<ImageGalleryItem> imageItems = new ArrayList<>();

        try {

            Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                    .appendQueryParameter("method", FETCH_RECENTS_METHOD);
            uriBuilder.appendQueryParameter("page",page);
            uriBuilder.appendQueryParameter("per_page",per_page);


            String jsonString = getUrlString(uriBuilder.build().toString());

            JSONObject jsonBody = new JSONObject(jsonString);

            parseItems(imageItems,jsonBody);

            Log.i(TAG, "Received json string: " + jsonString);
        } catch (IOException e) {
            Log.e(TAG, "Get json data failed", e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "Parse json data failed", e);
            e.printStackTrace();
        }
        return imageItems;
    }

    public List<ImageGalleryItem> searchdGalleryItems(String text) {

        List<ImageGalleryItem> imageItems = new ArrayList<>();

        try {
            Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                    .appendQueryParameter("method", SEARCH_METHOD);
            uriBuilder.appendQueryParameter("text",text);

            String jsonString = getUrlString(uriBuilder.build().toString());

            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(imageItems,jsonBody);

            Log.i(TAG, "Received json string: " + jsonString);
        } catch (IOException e) {
            Log.e(TAG, "Get json data failed", e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG, "Parse json data failed", e);
            e.printStackTrace();
        }
        return imageItems;
    }

    private void parseItems(List<ImageGalleryItem> items, JSONObject jsonBody) throws JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i=0;i<photoJsonArray.length();i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            ImageGalleryItem item = new ImageGalleryItem();
            item.setmId(photoJsonObject.getString("id"));
            item.setmCaption(photoJsonObject.getString("title"));
            if(!photoJsonObject.has("url_s")) continue;;
            item.setmUrl(photoJsonObject.getString("url_s"));
            item.setmOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }
    }
}
