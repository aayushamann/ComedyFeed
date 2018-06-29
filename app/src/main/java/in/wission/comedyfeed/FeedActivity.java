package in.wission.comedyfeed;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private FeedAdapter adapter;
    private List<FeedItem> feedItemList;
    private SwipeRefreshLayout refreshLayout;
    private String TAG = "FeedActivity";
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    private int refreshCount;
    private String searchNextPageToken;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        refreshCount = 0;
        loading = true;
        searchNextPageToken = "";
        refreshLayout = findViewById(R.id.swipe_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateListOnRefresh();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.feed_recycler_view);
        feedItemList = new ArrayList<>();
        adapter = new FeedAdapter(this, feedItemList);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();
                if (dy > 0 && loading) {
                    if ((visibleItemCount + pastVisibleItems) >= (totalItemCount - 1)) {
                        updateListOnScroll();
                    }
                }
            }
        });

        final TextView profileData = findViewById(R.id.profile_data);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();
        DatabaseReference database = mDatabase.getReference();

        database.child(Config.TABLE_USER).child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                String data = userProfile.name + " ("
                        + userProfile.gender.substring(0, 1)
                        + ", " + userProfile.age + ")";
                profileData.setText(data);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        LikeButton likeButton = findViewById(R.id.video_like_button);
        final TextView likeCountText = findViewById(R.id.like_count_text);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                int likes = Integer.parseInt(likeCountText.getText().toString());
                likeCountText.setText(String.valueOf(likes + 1));
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                int likes = Integer.parseInt(likeCountText.getText().toString());
                likeCountText.setText(String.valueOf(likes - 1));
            }
        });
        prepareList();
    }

    public void updateListOnScroll() {
        loading = false;
        refreshCount += 1;
        Log.d("FAct", "inside updateListOnScroll");
        FetchVideos fetchVideos = new FetchVideos(Config.LINK_TYPE_YOUTUBE_SEARCH,
                null, null);
        fetchVideos.execute();
    }

    public void updateListOnRefresh() {
        loading = true;
        feedItemList.clear();
        adapter.notifyDataSetChanged();
        FetchVideos fetchVideos = new FetchVideos(Config.LINK_TYPE_YOUTUBE_SEARCH,
                null, null);
        fetchVideos.execute();
        refreshLayout.setRefreshing(false);
    }

    public void prepareList() {
        FetchVideos fetchVideos = new FetchVideos(Config.LINK_TYPE_YOUTUBE_SEARCH, null, null);
        fetchVideos.execute();
    }

    public void onLogoutButton(View view) {
        mAuth.signOut();
        startActivity(new Intent(FeedActivity.this,
                LoginActivity.class));
        finish();
    }

    public void onOpenCommentBox(View view) {
        startActivity(new Intent(FeedActivity.this, CommentDialog.class));
    }

    private class FetchVideos extends AsyncTask<Void, Void, String> {
        private int linkType;
        private String playlistId;
        private String videoId;
        private String GET_URL;

        private FetchVideos(int linkType, String playlistId, String videoId) {
            this.linkType = linkType;
            this.playlistId = playlistId;
            this.videoId = videoId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("www.googleapis.com")
                    .appendPath("youtube")
                    .appendPath("v3");
            String PART_PARAM = "part";
            String MAX_RES_PARAM = "maxResults";
            String FIELD_PARAM = "fields";
            String KEY_PARAM = "key";
            String PAGE_TOKEN_PARAM = "pageToken";

            switch (linkType) {
                case Config.LINK_TYPE_YOUTUBE_SEARCH:
                    builder.appendPath("search")
                            .appendQueryParameter(PART_PARAM, "snippet")
                            .appendQueryParameter(MAX_RES_PARAM, "50")
                            .appendQueryParameter("order", "rating")
                            .appendQueryParameter("q", "comedy")
                            .appendQueryParameter(PAGE_TOKEN_PARAM, searchNextPageToken)
                            .appendQueryParameter("type", "playlist")
                            .appendQueryParameter(FIELD_PARAM, "items/id/playlistId,nextPageToken")
                            .appendQueryParameter(KEY_PARAM, Config.YOUTUBE_API_KEY);
                    break;
                case Config.LINK_TYPE_YOUTUBE_LIST:
                    builder.appendPath("playlistItems")
                            .appendQueryParameter(PART_PARAM, "snippet")
                            .appendQueryParameter(MAX_RES_PARAM, "10")
                            .appendQueryParameter("playlistId", playlistId)
                            .appendQueryParameter(FIELD_PARAM, "items/snippet/resourceId/videoId")
                            .appendQueryParameter(KEY_PARAM, Config.YOUTUBE_API_KEY);
                    break;
                case Config.LINK_TYPE_YOUTUBE_VIDEO:
                    builder.appendPath("videos")
                            .appendQueryParameter(PART_PARAM, "snippet,statistics")
                            .appendQueryParameter("id", videoId)
                            .appendQueryParameter(FIELD_PARAM, "items(id,snippet(thumbnails/default,title),statistics(commentCount,likeCount,viewCount))")
                            .appendQueryParameter(KEY_PARAM, Config.YOUTUBE_API_KEY);
                    break;
            }

            GET_URL = builder.build().toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            ArrayList<HashMap<String, String>> resultList = parseJSON(s);
            int resultSize = resultList.size();

            if (resultSize > 0) {
                switch (linkType) {
                    case Config.LINK_TYPE_YOUTUBE_SEARCH:
                        HashMap<String, String> result1 = resultList.get(refreshCount);
                        String playlistId = result1.get(Config.PLAYLIST_ID);
                        if (refreshCount >= 38) {
                            searchNextPageToken = result1.get(Config.SEARCH_NEXT_TOKEN);
                            refreshCount = 0;
                        }
                        FetchVideos fetchVideos1 = new FetchVideos(Config.LINK_TYPE_YOUTUBE_LIST, playlistId, null);
                        fetchVideos1.execute();
                        break;
                    case Config.LINK_TYPE_YOUTUBE_LIST:
                        for (int i = 0; i < resultSize; i++) {
                            HashMap<String, String> result2 = resultList.get(i);
                            String videoId = result2.get(Config.VIDEO_ID);
                            FetchVideos fetchVideos2 = new FetchVideos(Config.LINK_TYPE_YOUTUBE_VIDEO, null, videoId);
                            fetchVideos2.execute();
                        }
                        if (resultSize < 10) {
                            Log.d("FActListSize", String.valueOf(resultSize));
                            updateListOnScroll();
                        }
                        loading = true;
                        break;
                    case Config.LINK_TYPE_YOUTUBE_VIDEO:
                        HashMap<String, String> result3 = resultList.get(0);
                        String title = result3.get(Config.VIDEO_TITLE);
                        String url = result3.get(Config.VIDEO_THUMB_URL);
                        String viewCount = result3.get(Config.VIDEO_VIEW_COUNT);
                        String likeCount = result3.get(Config.VIDEO_LIKE_COUNT);
                        String commentCount = result3.get(Config.VIDEO_COMMENT_COUNT);

                        FeedItem feedItem = new FeedItem(videoId, title, viewCount, likeCount, commentCount, url);
                        feedItemList.add(feedItem);
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            RequestHandler requestHandler = new RequestHandler();
            return requestHandler.sendGetRequest(GET_URL);
        }

        private ArrayList<HashMap<String, String>> parseJSON(String json) {
            ArrayList<HashMap<String, String>> resultList = new ArrayList<>();
            try {
                JSONObject object = new JSONObject(json);
                JSONArray items = object.getJSONArray("items");
                int n = items.length();

                if (n != 0) {
                    switch (linkType) {
                        case Config.LINK_TYPE_YOUTUBE_SEARCH:
                            for (int i = 0; i < n; i++) {
                                JSONObject itemObject = items.getJSONObject(i);
                                JSONObject id = itemObject.getJSONObject("id");
                                HashMap<String, String> result = new HashMap<>();
                                result.put(Config.PLAYLIST_ID, id.getString(Config.PLAYLIST_ID));
                                result.put(Config.SEARCH_NEXT_TOKEN, object.getString(Config.SEARCH_NEXT_TOKEN));
                                resultList.add(result);
                            }
                            break;
                        case Config.LINK_TYPE_YOUTUBE_LIST:
                            for (int i = 0; i < n; i++) {
                                JSONObject itemObject = items.getJSONObject(i);
                                JSONObject snippet = itemObject.getJSONObject("snippet");
                                JSONObject resourceId = snippet.getJSONObject("resourceId");
                                HashMap<String, String> result = new HashMap<>();
                                result.put(Config.VIDEO_ID, resourceId.getString(Config.VIDEO_ID));
                                resultList.add(result);
                            }
                            break;
                        case Config.LINK_TYPE_YOUTUBE_VIDEO:
                            for (int i = 0; i < n; i++) {
                                JSONObject itemObject = items.getJSONObject(i);
                                JSONObject snippet = itemObject.getJSONObject("snippet");
                                JSONObject thumbnails = snippet.getJSONObject("thumbnails");
                                JSONObject defaultThumbnail = thumbnails.getJSONObject("default");
                                JSONObject statistics = itemObject.getJSONObject("statistics");

                                HashMap<String, String> result = new HashMap<>();
                                result.put(Config.VIDEO_TITLE, snippet.getString(Config.VIDEO_TITLE));
                                result.put(Config.VIDEO_THUMB_URL, defaultThumbnail.getString(Config.VIDEO_THUMB_URL));
                                result.put(Config.VIDEO_VIEW_COUNT, statistics.getString(Config.VIDEO_VIEW_COUNT));
                                result.put(Config.VIDEO_LIKE_COUNT, statistics.getString(Config.VIDEO_LIKE_COUNT));
                                result.put(Config.VIDEO_COMMENT_COUNT, statistics.getString(Config.VIDEO_COMMENT_COUNT));
                                resultList.add(result);
                            }
                            break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return resultList;
        }
    }
}
