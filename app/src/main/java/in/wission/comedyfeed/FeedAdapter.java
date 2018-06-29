package in.wission.comedyfeed;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.like.LikeButton;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.MyViewHolder> {

    private Context mContext;
    private List<FeedItem> feedItemList;

    public FeedAdapter(Context mContext, List<FeedItem> feedItemList) {
        this.mContext = mContext;
        this.feedItemList = feedItemList;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView viewCount;
        TextView likeCount;
        TextView commentCount;
        TextView videoTitle;
        ImageView playButton;

        public MyViewHolder(View view) {
            super(view);
            viewCount = view.findViewById(R.id.view_count);
            likeCount = view.findViewById(R.id.like_count);
            commentCount = view.findViewById(R.id.comment_count);
            videoTitle = view.findViewById(R.id.video_title);
            playButton = view.findViewById(R.id.play_button);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        FeedItem feedItem = feedItemList.get(position);
        final String videoId = feedItem.getVideoId();
        String videoTitle = feedItem.getVideoTitle();
        String viewCount = feedItem.getViewCount();
        final String likeCount = feedItem.getLikeCount();
        String commentCount = feedItem.getCommentCount();
        String url = feedItem.getThumbnailUrl();

        holder.playButton.setImageDrawable(null);
        Glide.with(mContext).load(url).into(holder.playButton);

        holder.videoTitle.setText(videoTitle);
        holder.viewCount.setText(viewCount);
        holder.likeCount.setText(likeCount);
        holder.commentCount.setText(commentCount);

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CardView playerCardView = ((Activity) mContext).findViewById(R.id.player_card_view);
                playerCardView.setVisibility(View.VISIBLE);
                final YouTubePlayerFragment youTubePlayerFragment = YouTubePlayerFragment.newInstance();
                ((Activity) mContext).getFragmentManager().beginTransaction()
                        .replace(R.id.youtube_holder, youTubePlayerFragment)
                        .commit();
                youTubePlayerFragment.initialize(Config.YOUTUBE_API_KEY, new YouTubePlayer.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
                        youTubePlayer.cueVideo(videoId);
                        youTubePlayer.setShowFullscreenButton(false);
                        LikeButton likeButton = ((Activity) mContext).findViewById(R.id.video_like_button);
                        TextView likeCountText = ((Activity) mContext).findViewById(R.id.like_count_text);
                        TextView commentText = ((Activity) mContext).findViewById(R.id.comment_button);

                        likeButton.setVisibility(View.VISIBLE);
                        likeCountText.setVisibility(View.VISIBLE);
                        commentText.setVisibility(View.VISIBLE);
                        likeCountText.setText(likeCount);
                    }

                    @Override
                    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return feedItemList.size();
    }
}
