package in.wission.comedyfeed;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class CommentDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_dialog);
    }

    public void onCloseCommentBox(View view) {
        finish();
    }

    public void onSubmitComment(View view) {
        finish();
    }
}
