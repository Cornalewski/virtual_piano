package com.example.virtual_piano;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Level_selection extends AppCompatActivity {
   Button b_selection,proximo;
   View card_box;
    public void mostrarCardBox(View v) {
        View cardBox = findViewById(R.id.card_view);
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cardBox.setVisibility(View.VISIBLE);
                    return true;

                case MotionEvent.ACTION_UP:
                    view.performClick(); // agora no lugar certo
                    return true;
            }
            return false;
        });
    }
    public void proxima_tela(){
      Intent it = new Intent(getBaseContext(),Play_music.class);
      startActivity(it);
    }
    public void Tela_musica(View v){

        v.setOnTouchListener((view,event) ->{
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    proxima_tela();
                    return  true;
            }
            return false;
        });
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.selection_screen);
        b_selection = findViewById(R.id.Level_1);
        mostrarCardBox(b_selection);
        proximo = findViewById(R.id.iniciar);
        Tela_musica(proximo);

    }
}
