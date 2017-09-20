package lac.com.imagegallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aicun on 9/7/2017.
 */

public class ImageGalleryFragment extends VisibleFragment {

    private static final String TAG = "ImageGalleryFragment";
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_MORE = 2;
    private static final int PAGE_SIZE = 10;

    private int currentPage = 1;

    private RecyclerView recyclerView;

    private List<ImageGalleryItem> images = new ArrayList<>();

    //private ThumbnailDownloader<ImageHolder> mThumbnailDownloader;

    public static ImageGalleryFragment getInstance() {
        return new ImageGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        fetchImage();

        Intent intent = PollService.newIntent(getActivity());
        getActivity().startService(intent);

        PollService.setServiceAlarm(getActivity(),true);

        Handler responseHandler = new Handler();
        /*mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<ImageHolder>() {
                    @Override
                    public void onThumbnailDownloaded(ImageHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bind(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();*/
        Log.i(TAG, "Background thread started");
    }

    private void fetchImage() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        if( query == null)
            new FetchImageTask().execute(new String[] {String.valueOf(currentPage),String.valueOf(PAGE_SIZE)});
        else
            new FetchImageTask().execute(new String[]{query});
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_gallery, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.fragment_image_gallery_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
                ImageAdapter adapter = (ImageAdapter) recyclerView.getAdapter();

                if (null == manager) {
                    throw new RuntimeException("you should call setLayoutManager() first!!");
                }
                if (manager instanceof LinearLayoutManager) {
                    int lastCompletelyVisibleItemPosition = ((LinearLayoutManager) manager).findLastCompletelyVisibleItemPosition();

                    if (adapter.getItemCount() > PAGE_SIZE &&
                            lastCompletelyVisibleItemPosition == adapter.getItemCount() - 1 &&
                            adapter.hasMoreImage()) {
                        //fetchImage();
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity(), query);
                new FetchImageTask().execute(new String[]{query});
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else  {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                fetchImage();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); // refresh menu
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /*recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = recyclerView.getWidth();
                int columns = width/300;

                recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), columns));
            }
        });*/
    }

    private void setupAdapter() {
        if (isAdded()) {
            recyclerView.setAdapter(new ImageAdapter(images));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
       // mThumbnailDownloader.clearQueue();
    }

    private class ImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        //private TextView imageTitleTextView;
        private ImageView imageImageView;
        private ImageGalleryItem mGalleryItem;

        public ImageHolder(View itemView) {
            super(itemView);
            //imageTitleTextView = (TextView) itemView.findViewById(R.id.image_title);
            imageImageView = (ImageView) itemView.findViewById(R.id.image_image);
            imageImageView.setOnClickListener(this);
        }

        public void bind(Drawable drawable) {
            //imageTitleTextView.setText(imageGalleryItem.getmCaption());
            imageImageView.setImageDrawable(drawable);
        }

        public void bindImageGallery(ImageGalleryItem item) {
            Picasso.with(getActivity()).load(item.getmUrl()).placeholder(R.drawable.wait).into(imageImageView);
            mGalleryItem = item;
        }

        @Override
        public void onClick(View v) {
            /*Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            startActivity(i);*/

            Intent intent = PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(intent);

        }
    }

    private class TextHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public TextHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }

        public void bind(String text) {
            textView.setText(text);
        }
    }

    private class ImageAdapter<T> extends RecyclerView.Adapter {

        private List<ImageGalleryItem> imageItems;

        public ImageAdapter(List<ImageGalleryItem> imageItems) {
            this.imageItems = imageItems;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if(viewType == TYPE_IMAGE) {
                View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_images, parent, false);
                return new ImageHolder(view);
            } else {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.show_loading_more,parent,false);
                return new TextHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(getItemViewType(position) == TYPE_MORE) {
                String text = "show more images";
                ((TextHolder)holder).bind(text);
            }else {
                ImageGalleryItem imageGalleryItem = imageItems.get(position);
                Drawable placeHolder = getResources().getDrawable(R.drawable.wait);
                ImageHolder imageHolder = (ImageHolder) holder;
                //imageHolder.bind(placeHolder);
                imageHolder.bindImageGallery(imageGalleryItem);
                //mThumbnailDownloader.queueThumbnail(imageHolder,imageGalleryItem.getmUrl());
            }
        }

        @Override
        public int getItemCount() {
            //return imageItems.size();
            return imageItems.size() < PAGE_SIZE ? imageItems.size() : imageItems.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if(!imageItems.isEmpty() &&  position < imageItems.size()) {
                return TYPE_IMAGE;
            }else {
                return TYPE_MORE;
            }
        }

        public boolean hasMoreImage() {
            return !imageItems.isEmpty();
        }
    }

    private class FetchImageTask extends AsyncTask<String, Void, List<ImageGalleryItem>> {

        @Override
        protected List<ImageGalleryItem> doInBackground(String... params) {
            if(params == null) return null;
            if(params.length == 1)
                return new FlickrFetchr().searchdGalleryItems(params[0]);
            return new FlickrFetchr().downloadGalleryItems(params[0],params[1]);
        }

        @Override
        protected void onPostExecute(List<ImageGalleryItem> imageGalleryItems) {
            images = imageGalleryItems;
            currentPage ++;
            setupAdapter();
        }
    }
}
