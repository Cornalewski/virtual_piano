package com.example.virtual_piano;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton que gerencia o SoundPool global da aplicação.
 * Carrega, em background, todos os samples invisíveis (urgentes) e visíveis (menos urgentes).
 * Expõe métodos estáticos para tocar/parar sons por resourceId (R.raw.xxx).
 */
public class Sound_Manager {

    private static Sound_Manager instance;
    private SoundPool soundPool;
    private final Map<Integer, Integer> soundIdMap = new HashMap<>();   // rawResId -> soundPool soundId
    private final Map<Integer, Integer> streamIdMap = new HashMap<>();  // rawResId -> último streamId
    private final AtomicInteger loadedCount = new AtomicInteger(0);
    private int totalToLoadUrgentes;
    private boolean isUrgentesReady = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // IDs que queremos carregar como “urgentes” (notas invisíveis) e “secundárias” (visíveis)
    private Set<Integer> urgentes;
    private Set<Integer> secundarias;

    private Sound_Manager() { }

    public static synchronized Sound_Manager getInstance() {
        if (instance == null) {
            instance = new Sound_Manager();
        }
        return instance;
    }

    /**
     * Inicializa o SoundPool e começa a carregar os samples.
     * Deve ser chamado preferencialmente na Activity de seleção de nível (Level_selection),
     * antes de entrar em Play_music.
     *
     * @param context     Activity ou Application context
     * @param invisiveis  Conjunto de resourceIds (R.raw.xxx) das notas invisíveis que tocam automaticamente
     * @param visiveis    Conjunto de resourceIds (R.raw.xxx) das teclas que o usuário pode tocar
     */
    public void initialize(Context context, Set<Integer> invisiveis, Set<Integer> visiveis) {
        if (soundPool != null) {
            // Já inicializado antes; não recarrega tudo de novo
            return;
        }

        this.urgentes = invisiveis;
        this.secundarias = visiveis;
        this.totalToLoadUrgentes = invisiveis.size();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        // O listener só conta quantos “urgentes” foram carregados
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                int carreg = loadedCount.incrementAndGet();
                if (carreg == totalToLoadUrgentes) {
                    // ⇒ todas as notas invisíveis foram carregadas
                    isUrgentesReady = true;
                    Log.d("SoundManager", "Urgentes prontos para tocar!");
                }
            } else {
                Log.e("SoundManager", "Falha ao carregar sampleId=" + sampleId);
            }
        });

        // 1) Carrega primeiro os “urgentes” (notas invisíveis) – bloqueante superficial
        for (int rawRes : invisiveis) {
            int sid = soundPool.load(context, rawRes, 1);
            soundIdMap.put(rawRes, sid);
        }

        // 2) Em seguida, carrega em background os “secundários” (teclas visíveis)
        new Thread(() -> {
            for (int rawRes : visiveis) {
                int sid = soundPool.load(context, rawRes, 1);
                soundIdMap.put(rawRes, sid);
                // não incrementamos o loadedCount aqui, porque não são “urgentes”
            }
        }).start();
    }

    /**
     * Retorna true se todas as notas invisíveis (urgentes) já estão carregadas.
     * Caso contrário, o Play_music deve aguardar antes de tocar os sons automáticos.
     */
    public boolean areUrgentesReady() {
        return isUrgentesReady;
    }

    /**
     * Toca o som correspondente a resId. Se não estiver carregado ainda, não faz nada.
     *
     * @param rawRes R.raw.xxx do áudio
     */
    public void play(int rawRes) {
        if (soundPool == null) return;
        Integer sid = soundIdMap.get(rawRes);
        if (sid != null) {
            int stream = soundPool.play(sid, 1f, 1f, 1, 0, 1f);
            streamIdMap.put(rawRes, stream);
        }
    }

    /**
     * Para o som, se estiver tocando.
     *
     * @param rawRes R.raw.xxx do áudio
     */
    public void stop(int rawRes) {
        Integer stream = streamIdMap.get(rawRes);
        if (stream != null) {
            soundPool.stop(stream);
            streamIdMap.remove(rawRes);
        }
    }

    /**
     * Libera o SoundPool quando não for mais necessário (ex: ao sair do jogo completamente).
     */
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundIdMap.clear();
            streamIdMap.clear();
            isUrgentesReady = false;
            loadedCount.set(0);
        }
    }
}
