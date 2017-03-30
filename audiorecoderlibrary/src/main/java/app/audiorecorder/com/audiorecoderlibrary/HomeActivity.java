package app.audiorecorder.com.audiorecoderlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    Toolbar toolbar;
    TextView toolbar_title;

    ImageView imgRecordingPlay,imgRecordingMic,imgRecordingDelete;
    ImageView imgRecordingStop,imgRecordingPause;
    FloatingActionButton fabRecordingMic;
    Chronometer mChronometer;
    File folder;
    int count;
    MediaRecorder recorder=null;
    PlayAudio playAudio=null;
    private String mFileName;
    private String mFilePath;
    File f;
    boolean isRecording=false,isPlaying=false;
    RecordAudio recordAudio=null;
    long timeWhenPaused = 0;
    int isMicEnable=0;
    int isAudioPlay=0;
    MediaPlayer mediaPlayer;
    int audioLength=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        init();
        clickEvent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        100);
            }
        }
        folder = new File(Environment.getExternalStorageDirectory() + "/"+getResources().getString(R.string.app_name));

        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT>=19) {
            toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        }else{
            AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            layoutParams.height = getActionBarHeight(HomeActivity.this);
            toolbar.setLayoutParams(layoutParams);
        }
    }

    private void clickEvent() {
        fabRecordingMic.setOnClickListener(this);
        imgRecordingDelete.setOnClickListener(this);
        imgRecordingPlay.setOnClickListener(this);
        imgRecordingStop.setOnClickListener(this);
    }

    private void init() {
        toolbar=(Toolbar)findViewById(R.id.recording_toolbar);
        toolbar_title=(TextView)findViewById(R.id.toolbar_title);
        mChronometer=(Chronometer)findViewById(R.id.chronometer);
        imgRecordingPlay=(ImageView)findViewById(R.id.recording_imgPlay);
//        imgRecordingMic=(ImageView)findViewById(R.id.recording_imgMic);
        mediaPlayer=new MediaPlayer();
        fabRecordingMic=(FloatingActionButton)findViewById(R.id.recording_fab);
        imgRecordingDelete=(ImageView)findViewById(R.id.recording_imgDelete);
        imgRecordingStop=(ImageView)findViewById(R.id.recording_imgStop);

    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getActionBarHeight(Activity activity) {
        final TypedArray ta = activity.getTheme().obtainStyledAttributes(
                new int[] {android.R.attr.actionBarSize});
        int actionBarHeight = (int) ta.getDimension(0, 0);
        return actionBarHeight;
    }

    @Override
    public void onClick(View v) {
        if (v==fabRecordingMic){
            if (isMicEnable==0) {
                startRecording();
            }else if (isMicEnable==1){
                pauseRecording();
            }else if (isMicEnable==2){
                stopPlaying();
            }
        }else if (v==imgRecordingStop){
            stopRecording();
        }else if (v==imgRecordingPause){

        }else if (v==imgRecordingPlay){
            if (isAudioPlay==0){
                playRecording();
            }else{
                pausePlaying();
            }
        }else if (v==imgRecordingDelete){
            deleteRecording();
        }
    }

    /**
     * Play recorded audio
     */

    private void playRecording() {
        imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_white_24dp));
        isMicEnable=2;
        isAudioPlay=1;
        if (playAudio==null){
            playAudio = new PlayAudio();
            playAudio.execute();
        }else{
            if (mediaPlayer!=null) {
                mediaPlayer.seekTo(audioLength);
                mediaPlayer.start();
            }
        }
    }

    /**
     * Stop Recorded audio if audio is currently playing
     */
    private void stopPlaying() {
        imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
        isMicEnable=0;
        isAudioPlay=0;
        if (mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;
            audioLength=0;
            playAudio=null;

        }
    }

    /**
     * Pause Recorded audio if audio is currently playing
     */

    private void pausePlaying() {
        imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        isAudioPlay=0;
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            audioLength=mediaPlayer.getCurrentPosition();
        }
    }




    /**
     * stop audio recording
     */

    private void stopRecording() {
        if (recorder!=null){
            try {
                imgRecordingStop.setVisibility(View.GONE);
                imgRecordingPlay.setVisibility(View.VISIBLE);
                imgRecordingDelete.setAlpha(Float.parseFloat("1"));
                imgRecordingDelete.setClickable(true);
                imgRecordingPlay.setAlpha(Float.parseFloat("1"));
                imgRecordingPlay.setClickable(true);
                fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
                isMicEnable=0;
                recordAudio=null;
                playAudio=null;
                try {
                    if (isRecording) {
                        isRecording = false;
                        mChronometer.stop();
                        timeWhenPaused = 0;
                        recorder.stop();
                        recorder.reset();
                        recorder.release();
                    }
                }catch (IllegalStateException e){
                    e.printStackTrace();
                }

/*
                mChronometer.setBase(SystemClock.elapsedRealtime());
*/
                recorder = null;
                if (folder.exists() && folder.isDirectory()){
                    String[] fileContent=folder.list();
                    String[] recordedFile=new String[fileContent.length];
                    for(int j=0;j<fileContent.length;j++){
                        recordedFile[j]=Environment.getExternalStorageDirectory().getAbsolutePath()+ "/"+getResources().getString(R.string.app_name)+"/"+fileContent[j];
                    }
                    mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    mFilePath += "/"+getResources().getString(R.string.app_name);
                    String mergeFile=  Environment.getExternalStorageDirectory().getAbsolutePath()+ "/"+getResources().getString(R.string.app_name)+"/MainRecording.mp3";
                    File mFile=new File(mergeFile);
                    if (!mFile.exists()){
                        try {
                            mFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    ProgressDialog pbMergingDialog = new ProgressDialog(HomeActivity.this);
                    pbMergingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pbMergingDialog.setIndeterminate(true);
                    pbMergingDialog.show();
                    if(mergeMediaFiles(true,recordedFile,mergeFile)){
                        pbMergingDialog.hide();
                    }else{
                        pbMergingDialog.hide();
                    }
                }
            }catch (RuntimeException exception){
            }
        }
    }

    /**
     * set File path for recorded audio
     */
    public void setFileNameAndPath(){
        do{
            mFileName = getString(R.string.app_name_recording)+"-"+count+".mp3";
            mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFilePath += "/"+getResources().getString(R.string.app_name)+"/" + mFileName;
            f = new File(mFilePath);
            count++;
        }while (f.exists() && !f.isDirectory());
    }

    /**
     * Start / Resume Recording When
     */
    public void startRecording(){
        fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        isMicEnable=1;
        if (recordAudio==null) {
            if (folder.exists() && folder.isDirectory()) {
                //folder /SoundRecorder doesn't exist, create the folder
                String[] files = folder.list();
                for (int i = 0; i < files.length; i++) {
                    new File(folder, files[i]).delete();
                }
                folder.mkdir();
            } else {
                folder.mkdir();
            }
            imgRecordingPlay.setVisibility(View.GONE);
            imgRecordingStop.setVisibility(View.VISIBLE);
            imgRecordingDelete.setAlpha(Float.parseFloat("0.5"));
            imgRecordingDelete.setClickable(false);
            setFileNameAndPath();
            recordAudio=new RecordAudio();
            recordAudio.execute();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();
        }else{
            mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            mChronometer.start();
            setFileNameAndPath();
            recordAudio=new RecordAudio();
            recordAudio.execute();
        }
    }

    /**
     * Pause Audio recording
     */
    private void pauseRecording() {
        fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
        isMicEnable=0;
        if (recorder!=null){
//            imgRecordi ngPause.setVisibility(View.GONE);
//            imgRecordingMic.setVisibility(View.VISIBLE);
            timeWhenPaused = mChronometer.getBase() - SystemClock.elapsedRealtime();
            mChronometer.stop();
            recorder.stop();
            recorder.reset();
            recorder.release();
            isRecording=false;
//            recorder.pause();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==100){

        }
    }

    /**
     * Merge All audio recorded file into one audio
     * this method is used to fulfill pause/resume recording
     * @param isAudio
     * @param sourceFiles
     * @param targetFile
     * @return
     */
    public static boolean mergeMediaFiles(boolean isAudio, String sourceFiles[], String targetFile) {
        try {
            String mediaKey = isAudio ? "soun" : "vide";
            List<Movie> listMovies = new ArrayList<>();
            for (String filename : sourceFiles) {
                listMovies.add(MovieCreator.build(filename));
            }
            List<Track> listTracks = new LinkedList<>();
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals(mediaKey)) {
                        listTracks.add(track);
                    }
                }
            }
            Movie outputMovie = new Movie();
            if (!listTracks.isEmpty()) {
                outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
            }
            Container container = new DefaultMp4Builder().build(outputMovie);
            FileChannel fileChannel = new RandomAccessFile(String.format(targetFile), "rw").getChannel();
            container.writeContainer(fileChannel);
            fileChannel.close();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Delete Recorded audio From File
     */

    private void deleteRecording() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
        alertDialogBuilder.setMessage(getResources().getString(R.string.err_msg_delete_recording))
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String[] files = folder.list();
                                for (int i = 0; i < files.length; i++) {
                                    new File(folder, files[i]).delete();
                                }
                                if (mediaPlayer!=null) {
                                    if (mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release();
                                        audioLength = 0;
                                        playAudio = null;
                                    }
                                }
                                imgRecordingDelete.setAlpha(Float.parseFloat("0.5"));
                                imgRecordingDelete.setClickable(false);
                                imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                                imgRecordingPlay.setAlpha(Float.parseFloat("0.5"));
                                imgRecordingPlay.setClickable(false);
                                fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
                                isMicEnable = 0;
                                recordAudio = null;
                                mChronometer.stop();
                                mChronometer.setBase(SystemClock.elapsedRealtime());
                                dialog.cancel();
                            }
                        });
        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }



    /**
     * audio Recording functionality
     */

    private class RecordAudio extends AsyncTask<Void,Integer,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            isRecording=true;
            try{
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                recorder.setOutputFile(mFilePath);
                recorder.setAudioChannels(1);
                try {
                    recorder.prepare();
                } catch (IOException e) {
                    Log.e("ERROR", e.getMessage());
                }
                recorder.start();
            }catch (Throwable t){
                t.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Play recorded audio
     */
    private class PlayAudio extends AsyncTask<Void,Integer,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        }

        @Override
        protected Void doInBackground(Void... params) {
            String audioFile = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+getResources().getString(R.string.app_name)+"/MainRecording.mp3" ;
            try {
                mediaPlayer=new MediaPlayer();
                mediaPlayer.setDataSource(audioFile);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(audioLength);
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    imgRecordingPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                    fabRecordingMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
                    if (mp.isPlaying()){
                        mp.seekTo(0);
                        mp.stop();
                        mp.release();
                        mediaPlayer.release();
                        mediaPlayer=null;
                    }
                }
            });
            return null;
        }
    }
}
