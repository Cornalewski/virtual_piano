package com.example.virtual_piano;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Level_selection extends AppCompatActivity {
   Button b_selection,proximo;
   TextView tv;
   View card_box;
    public static String[] removeElement(String[] arr, int index) {
        if (arr == null || index >= arr.length) {
            return arr; // Or throw an exception for invalid input
        }
       if (index < 0){
           index = arr.length -1;
       }
        String[] newArray = new String[arr.length - 1];
        System.arraycopy(arr, 0, newArray, 0, index);
        System.arraycopy(arr, index + 1, newArray, index, arr.length - index - 1);
        return newArray;
    }
    public void mostrarCardBox(View v,String path) {
        View cardBox = findViewById(R.id.card_view);
        tv = findViewById(R.id.texto_caixa);
        String[] temp = (path.split("[._]"));
        temp = removeElement(temp,-1);
        tv.setText(String.join(" ",temp).toUpperCase());
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
    public void proxima_tela(String path){
      Intent it = new Intent(getBaseContext(),Play_music.class);
      it.putExtra("Partitura",path);
      setIntent(it);
      startActivity(it);
    }
    public void Tela_musica(View v,String path){
        mostrarCardBox(b_selection,path);
        v.setOnTouchListener((view,event) ->{

            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    proxima_tela(path);
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
        proximo = findViewById(R.id.iniciar);
        Tela_musica(proximo,"ode_alegria.txt");




    }
}
