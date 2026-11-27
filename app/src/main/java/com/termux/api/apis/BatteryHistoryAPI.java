package com.termux.api.apis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fork Spartan117x/termux-api (v0.53.0-spartan117x.2): comando nuevo
 * `termux-battery-history` (método "BatteryHistory").
 *
 * Cada llamada guarda una muestra de la batería (nivel, temperatura, estado,
 * si está enchufado, fecha/hora) en un archivo JSON privado de la app
 * ({@code battery-history.json}) y devuelve el historial completo como JSON.
 *
 * No hay servicio en segundo plano: el historial crece con cada llamada, por
 * lo que lo ideal es llamarlo desde un cron de Termux cada 5 minutos.
 * Se guardan como máximo 288 muestras (24 h a intervalos de 5 min); las
 * más viejas se van podando.
 *
 * Ejemplo de salida:
 * <pre>
 * [
 *   {
 *     "timestamp": 1795739525000,
 *     "time": "2026-11-26 18:32:05",
 *     "percentage": 88,
 *     "temperature": 34.0,
 *     "status": "DISCHARGING",
 *     "plugged": "UNPLUGGED"
 *   }
 * ]
 * </pre>
 */
public class BatteryHistoryAPI {

    private static final String LOG_TAG = "BatteryHistoryAPI";

    private static final String HISTORY_FILE_NAME = "battery-history.json";

    /** Máximo de muestras guardadas: 288 = 24 h a intervalos de 5 minutos. */
    private static final int MAX_SAMPLES = 288;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @SuppressLint("DefaultLocale")
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                JSONObject sample = readBatterySample(context);

                JSONArray history = loadHistory(context);
                history.put(sample);

                // Podar las muestras más viejas si nos pasamos del máximo.
                while (history.length() > MAX_SAMPLES) {
                    history.remove(0);
                }

                saveHistory(context, history);

                // Devolver el historial completo.
                out.beginArray();
                for (int i = 0; i < history.length(); i++) {
                    JSONObject entry = history.getJSONObject(i);
                    out.beginObject();
                    out.name("timestamp").value(entry.optLong("timestamp", 0));
                    out.name("time").value(entry.optString("time", ""));
                    out.name("percentage").value(entry.optInt("percentage", -1));
                    double temperature = entry.optDouble("temperature", Double.NaN);
                    if (Double.isNaN(temperature)) {
                        out.name("temperature").nullValue();
                    } else {
                        out.name("temperature").value(temperature);
                    }
                    out.name("status").value(entry.optString("status", "UNKNOWN"));
                    out.name("plugged").value(entry.optString("plugged", "UNKNOWN"));
                    out.endObject();
                }
                out.endArray();
            }
        });
    }

    /** Lee la muestra actual de la batería. */
    private static JSONObject readBatterySample(Context context) throws Exception {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) batteryStatus = new Intent();

        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        int percentage = -1;
        if (batteryManager != null) {
            int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (capacity != Integer.MIN_VALUE) percentage = capacity;
        }
        if (percentage == -1) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale > 0) percentage = (level * 100) / scale;
        }

        // Android devuelve la temperatura en décimas de grado Celsius (255 = 25.5 °C).
        int temperatureTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        double temperature = temperatureTenths != Integer.MIN_VALUE
            ? (double) Math.round(((double) temperatureTenths / 10f) * 10.0f) / 10.0f
            : Double.NaN;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String statusString;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = "CHARGING";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = "DISCHARGING";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = "FULL";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = "NOT_CHARGING";
                break;
            default:
                statusString = "UNKNOWN";
        }

        int pluggedInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String pluggedString;
        switch (pluggedInt) {
            case 0:
                pluggedString = "UNPLUGGED";
                break;
            case BatteryManager.BATTERY_PLUGGED_AC:
                pluggedString = "PLUGGED_AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                pluggedString = "PLUGGED_USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                pluggedString = "PLUGGED_WIRELESS";
                break;
            case BatteryManager.BATTERY_PLUGGED_DOCK:
                pluggedString = "PLUGGED_DOCK";
                break;
            default:
                pluggedString = "PLUGGED_" + pluggedInt;
        }

        long now = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(now));

        JSONObject sample = new JSONObject();
        sample.put("timestamp", now);
        sample.put("time", time);
        sample.put("percentage", percentage);
        sample.put("temperature", temperature);
        sample.put("status", statusString);
        sample.put("plugged", pluggedString);
        return sample;
    }

    /** Carga el historial guardado, o un arreglo vacío si no existe o está corrupto. */
    private static JSONArray loadHistory(Context context) {
        File file = new File(context.getFilesDir(), HISTORY_FILE_NAME);
        if (!file.exists()) return new JSONArray();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = in.read(bytes);
            if (read <= 0) return new JSONArray();
            return new JSONArray(new String(bytes, 0, read, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "No se pudo leer " + HISTORY_FILE_NAME + ", se empieza un historial nuevo: " + e);
            return new JSONArray();
        }
    }

    /** Guarda el historial en el almacenamiento privado de la app. */
    private static void saveHistory(Context context, JSONArray history) {
        File file = new File(context.getFilesDir(), HISTORY_FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(history.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "No se pudo guardar " + HISTORY_FILE_NAME + ": " + e);
        }
    }

}
