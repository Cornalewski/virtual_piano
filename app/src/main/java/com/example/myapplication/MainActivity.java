package com.example.myapplication;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    Button Bdo;
    MediaPlayer player = new MediaPlayer();

    public void dSom(View view) {
        int som = 0;


        if(view.getId() == R.id.c4){
            som = R.raw.c4;
        }
        else if(view.getId() == R.id.d4){
            som = R.raw.d4;
        } else if (view.getId() == R.id.e4) {
            som = R.raw.e4;
        } else if (view.getId() == R.id.f4) {
            som = R.raw.f4;
        }

        if (som != 0) {
            tocarSom(som);
        }
    }
    public void tocarSom(int som)
    {
        if (player.isPlaying()) {
            player.stop();
        }
        player.reset();

        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(som);
            if (afd != null) {
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                player.prepare();
                player.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bdo = findViewById(R.id.c4);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


}
