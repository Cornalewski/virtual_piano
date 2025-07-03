package com.example.virtual_piano;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
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
    private int activeStreamId = 0;
    Context context;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
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

    private Sound_Manager() {
    }

    public static synchronized Sound_Manager getInstance() {
        if (instance == null) {
            instance = new Sound_Manager();
        }
        return instance;
    }


    public void initialize(Context ctx, Set<Integer> invisiveis, Set<Integer> visiveis) {
        this.context = ctx.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (soundPool != null) {
            // Já inicializado antes; não recarrega tudo de novo
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .build();
        }
        this.urgentes = invisiveis;
        this.secundarias = visiveis;
        this.totalToLoadUrgentes = invisiveis.size();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(50)
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

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Recuperou o foco: volte ao volume normal
                            soundPool.setVolume(activeStreamId, 1f, 1f);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Perda temporária: pause ou abaixe volume
                            soundPool.setVolume(activeStreamId, 0.1f, 0.1f);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Perda longa: pare tudo e solte recursos
                            soundPool.autoPause();
                            abandonAudioFocus();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Pode “duck” (reduzir volume)
                            soundPool.setVolume(activeStreamId, 0.2f, 0.2f);
                            break;
                    }
                }
            };

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .build();
            return audioManager.requestAudioFocus(focusRequest)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            // Para APIs < 26
            int result = audioManager.requestAudioFocus(
                    afChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Supondo que você guardou o AudioFocusRequest em um campo chamado focusRequest
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(afChangeListener);
        }
    }

    /**
     * Toca o som correspondente a resId. Se não estiver carregado ainda, não faz nada.
     *
     * @param rawRes R.raw.xxx do áudio
     */
    public int play(int rawRes) {
        Integer sid = soundIdMap.get(rawRes);
        if (sid == null) return 0;
        return soundPool.play(sid, 1f, 1f, 1, 0, 1f);
    }
    public void stopStream(int streamId) {
        soundPool.stop(streamId);
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
            abandonAudioFocus();
        }
    }
}