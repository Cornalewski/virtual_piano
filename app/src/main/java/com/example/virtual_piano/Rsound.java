package com.example.virtual_piano;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Rsound {
    public static String[] store(String path) {
        String[] saida = null;
        try {
            File som = new File(path);
            Scanner myReader = new Scanner(som);
            StringBuilder aux = new StringBuilder();

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                aux.append(data);
            }
            saida = aux.toString().split(" ");
            myReader.close();
            return saida;

        } catch (FileNotFoundException e) {
            System.out.println("erro na leitura da musica.");
            e.printStackTrace();
        }
        return saida;

    }
}
