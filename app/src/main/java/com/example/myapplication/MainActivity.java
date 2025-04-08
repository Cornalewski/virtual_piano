package com.example.myapplication;
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



public class MainActivity extends AppCompatActivity {

    Button Bdo;
    MediaPlayer player ;

    public void d0listener(View view) {

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tocarSom();
                        return true;

                    case MotionEvent.ACTION_UP:
                        pararSom();
                        return true;

                }
                return false;
            }
        });
    }

   private void tocarSom()
   {
    if(player== null)
    {
        player = MediaPlayer.create(this, R.raw.c33);
        player.setLooping(true);
    }
    player.start();
   }
   private void pararSom()
   {
       if(player != null) {
           player.pause();
           player.seekTo(0);
       }
   }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bdo = findViewById(R.id.c3);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


}