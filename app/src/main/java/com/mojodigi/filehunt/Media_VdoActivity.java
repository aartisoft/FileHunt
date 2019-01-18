package com.mojodigi.filehunt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.mojodigi.filehunt.AddsUtility.SharedPreferenceUtil;
import com.mojodigi.filehunt.Class.Constants;
import com.mojodigi.filehunt.Utils.CustomProgressDialog;
import com.mojodigi.filehunt.Utils.Utility;
import com.mojodigi.filehunt.Utils.UtilityStorage;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Media_VdoActivity extends AppCompatActivity implements View.OnClickListener {

    private VideoView mVdoVideoView;
    private MediaController mediaController;
    private int stopPosition;
    private ImageView   mVideoShareImgView , mVideoInfoImgView , mVideoDeleteImgView,mVdoBackArrowImage ;

    private  Uri selectedVideoUri = null;
    private String selectedVdoPath = null;
    private Context mContext;
    private File file;
    private SharedPreferenceUtil addprefs;
    ArrayList<File> delete_list=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media__vdo);

        mContext=Media_VdoActivity.this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Utility.setstatusBarColorBelowM(Media_VdoActivity.this);
        }

        addprefs = new SharedPreferenceUtil(mContext);
        if (addprefs != null) {
            stopPosition = addprefs.getIntValue("position",0);
            //Log.d("", "savedInstanceState called" + stopPosition);
        }
        // If we have a saved state then we can restore it now
//        if (savedInstanceState != null) {
//            stopPosition = savedInstanceState.getInt("position");
//            //Log.d("", "savedInstanceState called" + stopPosition);
//        }

        initVdoView();
    }

    private void initVdoView() {

        mVdoVideoView = (VideoView) findViewById(R.id.mVdoVideoView);

        mVideoShareImgView =  findViewById(R.id.mVideoShareImgView);
        mVideoDeleteImgView =   findViewById(R.id.mVideoDeleteImgView);
        mVideoInfoImgView=findViewById(R.id.mVideoInfoImgView);
        mVdoBackArrowImage=findViewById(R.id.mVdoBackArrowImage);

        mVideoInfoImgView.setOnClickListener(this);
        mVideoDeleteImgView.setOnClickListener(this);
        mVideoShareImgView.setOnClickListener(this);
        mVdoBackArrowImage.setOnClickListener(this);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(mVdoVideoView);
        mediaController.setPadding(0, 0, 0, 95);


        Intent extrasIntent = getIntent();
        if (extrasIntent != null) {
            selectedVdoPath = extrasIntent.getStringExtra(Constants.selectedVdo);
            if(selectedVdoPath!=null)
            {
                 file =  new File(selectedVdoPath);
                 selectedVideoUri=Uri.parse(file.toString());
            }

        }
        if (selectedVideoUri != null) {

            mVdoVideoView.setMediaController(mediaController);
            mVdoVideoView.setVideoURI(selectedVideoUri);
            mVdoVideoView.requestFocus();
            mVdoVideoView.start();
        } else {

            Utility.dispToast(mContext, "can't play file");
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.mVdoBackArrowImage:
                finish();
                break;

            case R.id.mVideoShareImgView:

             if(file!=null && mContext!=null)
                 Utility.shareFile(mContext, file.getAbsolutePath());
             break;

            case R.id.mVideoInfoImgView:
                if(file!=null) {
                    addprefs.setValue(Constants.mediaType, "Video");
                    Intent intentImageGallary = new Intent(mContext, Media_InfoActivity.class);
                    intentImageGallary.putExtra(Constants.fileInfoPath, file.getAbsolutePath());
                    startActivity(intentImageGallary);
                }
                break;
            case R.id.mVideoDeleteImgView:
                if(file!=null) {
                    delete_list.add(file);
                    if(UtilityStorage.isWritableNormalOrSaf(file,mContext)) {
                        new DeleteFileTask(delete_list).execute();
                    }
                    else
                    {
                        UtilityStorage.guideDialogForLEXA(mContext,file.getParent(), Constants.FILE_DELETE_REQUEST_CODE);
                    }
                }
                break;

        }
    }



    @Override
    public void onPause() {
       // Log.d("", "onPause called");
        super.onPause();
        stopPosition = mVdoVideoView.getCurrentPosition();
        addprefs.setIntValue("position", stopPosition);
        //mVdoVideoView.pause();
    }

//    @Override
//    public void onStop() {
//        Log.d("", "onPause called");
//        super.onStop();
//        stopPosition = mVdoVideoView.getCurrentPosition();
//        addprefs.setIntValue("position", stopPosition);
//
//    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d("", "onResume called" + stopPosition);
       // mVdoVideoView.seekTo(stopPosition);
        mVdoVideoView.seekTo(addprefs.getIntValue("position",0));
        //mVdoVideoView.resume();
        mVdoVideoView.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mVdoVideoView.pause();
        stopPosition = mVdoVideoView.getCurrentPosition();
        outState.putInt("position", stopPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        stopPosition = savedInstanceState.getInt("position");
        Log.d("", "onRestore called" + stopPosition);
    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();
        return true;
    }

    private class DeleteFileTask extends AsyncTask<Void,Void,Integer>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            CustomProgressDialog.show(mContext,getResources().getString(R.string.deleting_file));
        }

        ArrayList<File> delete_list;
        DeleteFileTask( ArrayList<File> delete_list)
        {
            this.delete_list=delete_list;
        }


        @Override
        protected Integer doInBackground(Void... voids) {
            return deleteFile(delete_list);
        }

        @Override
        protected void onPostExecute(Integer FileCount) {
            super.onPostExecute(FileCount);
            Toast.makeText(mContext, FileCount+" file deleted", Toast.LENGTH_SHORT).show();
            CustomProgressDialog.dismiss();

            if(FileCount>0)
                finish();

        }
    }
    private int deleteFile( ArrayList<File> delete_list)
    {
        int count=0;

        for(int i=0;i<delete_list.size();i++)
        {
            File f=delete_list.get(i);
            if(f.exists()) {
                if (f.delete()) {
                    count++;
                    sendBroadcast(f);
                }
                //new
                else {
                    boolean st = UtilityStorage.isWritableNormalOrSaf(f, mContext);
                    System.out.println("" + st);
                    if (st) {
                        boolean status = UtilityStorage.deleteWithAccesFramework(mContext, f);
                        if (status) {
                            count++;
                            Utility.RunMediaScan(mContext, f);
                        }
                    } else {
                        //UtilityStorage.triggerStorageAccessFramework(mcontext);
                    }


                }
            }
            //new

        }


        return count;
    }
    private void sendBroadcast(File outputFile)
    {
        //  https://stackoverflow.com/questions/4430888/android-file-delete-leaves-empty-placeholder-in-gallery
        //this broadcast clear the deleted images from  android file system
        //it makes the MediaScanner service run again that keep  track of files in android
        // to  run it a permission  in manifest file has been given
        // <protected-broadcast android:name="android.intent.action.MEDIA_MOUNTED" />


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(outputFile);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            sendBroadcast(intent);
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode== Constants.FILE_DELETE_REQUEST_CODE) {
            boolean isPersistUriSet = UtilityStorage.setUriForStorage(requestCode, resultCode, data);
            if (isPersistUriSet && delete_list.size() > 0)
                new DeleteFileTask(delete_list).execute();
        }
    }

}




