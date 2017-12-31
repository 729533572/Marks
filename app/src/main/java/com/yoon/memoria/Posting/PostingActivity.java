package com.yoon.memoria.Posting;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.tedpark.tedpermission.rx2.TedRx2Permission;
import com.yoon.memoria.R;
import com.yoon.memoria.Util.Util;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PostingActivity extends AppCompatActivity implements PostingContract.View {

    private FirebaseUser user;
    private PostingPresenter presenter;

    private final int GALLERY_CODE=1112;

    @BindView(R.id.postingToolbar) Toolbar toolbar;
    @BindView(R.id.post_image) ImageView imageView;
    @BindView(R.id.postEdit) EditText editText;

    private ProgressDialog progressDialog;

    private Intent intent;
    private CalendarDay day;

    private String uid;
    private String nickname;
    private String filename;
    private String content;
    private double latitude;
    private double longitude;
    private String date;

    private Uri uri;
    private String filePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posting);
        ButterKnife.bind(this);
        user = FirebaseAuth.getInstance().getCurrentUser();
        presenter = new PostingPresenter(this);

        initToolbar();
        init();
    }

    public void init(){
        imageView.setOnClickListener(view -> {
            TedRx2Permission.with(this)
                    .setRationaleTitle(R.string.rationale_title)
                    .setRationaleMessage(R.string.rationale_picture_message)
                    .setDeniedMessage(R.string.rationale_denied_message)
                    .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .request()
                    .subscribe(tedPermissionResult -> {
                        if (tedPermissionResult.isGranted()) {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, GALLERY_CODE);
                        } else {
                            Util.makeToast(this, "권한 거부\n" + tedPermissionResult.getDeniedPermissions().toString());
                        }
                    }, throwable -> {
                    }, () -> {
                    });
        });
        intent = getIntent();

        uid = getUid();
        nickname = "태중이";
        latitude = intent.getDoubleExtra("latitude",0);
        longitude = intent.getDoubleExtra("longitude",0);
        day = CalendarDay.today();
        date = day.getYear() +"," + (day.getMonth()+1) + "," + day.getDay();
    }

    public void initToolbar(){
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id)
        {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_post:
                if(filePath.length() <= 0)
                    Util.makeToast(this, "사진이 없습니다!");
                else {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle("업로드중...");
                    progressDialog.show();
                    presenter.fileUpload(uri, progressDialog);
                    break;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void post_OK(){
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    @Override
    public void success() {
        filename = presenter.getFilename();
        content = editText.getText().toString();
        progressDialog.dismiss();
        presenter.post_to_firebase(uid,nickname,date,latitude,longitude,filename,content);
    }

    @Override
    public void failed() {
        Util.makeToast(this,"업로드 실패!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == GALLERY_CODE && resultCode == Activity.RESULT_OK){
            uri = data.getData();
            filePath = getRealPathFromURIPath(uri, PostingActivity.this);
            File file = new File(filePath);
            if(file.length()>5*2E20){
                Util.makeToast(this,"이미지 크기는 최대 5MB 입니다");
            }else {
                Glide.with(this)
                        .load(file)
                        .override(Util.dpToPixel(this,260),Util.dpToPixel(this,260))
                        .fitCenter()
                        .into(imageView);
            }
        }
    }

    private String getRealPathFromURIPath(Uri contentURI, Activity activity) {
        Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
