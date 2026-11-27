package com.termux.api.apis;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.api.util.ResultReturner.ResultJsonWriter;
import com.termux.shared.logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Fork Spartan117x/termux-api (v0.53.0-spartan117x.2): comando nuevo
 * `termux-salud` (método "Salud").
 *
 * Devuelve en un solo JSON el estado de salud del teléfono: batería
 * (nivel, temperatura, estado), RAM (total, disponible, usada) y si están
 * prendidos los servidores locales de IAs de Hiram (puertos 8080 heretic,
 * 8081 dolphin y 8082 wizardlm), por chequeo TCP a 127.0.0.1 con timeout
 * corto. El permiso INTERNET ya está en el manifiesto.
 *
 * Ejemplo de salida:
 * <pre>
 * {
 *   "battery": {
 *     "percentage": 88,
 *     "temperature": 34.0,
 *     "status": "DISCHARGING",
 *     "plugged": "UNPLUGGED"
 *   },
 *   "ram": {
 *     "total": 12213866496,
 *     "available": 6248000000,
 *     "used": 5965866496,
 *     "total_mb": 11648.0,
 *     "available_mb": 5958.0,
 *     "used_mb": 5689.0
 *   },
 *   "servers": [
 *     { "port": 8080, "name": "heretic", "open": false },
 *     { "port": 8081, "name": "dolphin", "open": false },
 *     { "port": 8082, "name": "wizardlm", "open": false }
 *   ]
 * }
 * </pre>
 */
public class SaludAPI {

    private static final String LOG_TAG = "SaludAPI";

    /** Timeout en ms para el chequeo TCP de cada puerto local. */
    private static final int SERVER_CHECK_TIMEOUT_MS = 500;

    /** Servidores locales de Hiram: puerto y nombre. */
    private static final int[] SERVER_PORTS = {8080, 8081, 8082};

    private static final String[] SERVER_NAMES = {"heretic", "dolphin", "wizardlm"};

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        ResultReturner.returnData(apiReceiver, intent, new ResultJsonWriter() {
            @SuppressLint("DefaultLocale")
            @Override
            public void writeJson(JsonWriter out) throws Exception {
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

                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                if (activityManager != null) activityManager.getMemoryInfo(memoryInfo);
                long ramTotal = memoryInfo.totalMem;
                long ramAvailable = memoryInfo.availMem;
                long ramUsed = ramTotal - ramAvailable;

                out.beginObject();

                out.name("battery");
                out.beginObject();
                out.name("percentage").value(percentage);
                if (Double.isNaN(temperature)) {
                    out.name("temperature").nullValue();
                } else {
                    out.name("temperature").value(temperature);
                }
                out.name("status").value(statusString);
                out.name("plugged").value(pluggedString);
                out.endObject();

                out.name("ram");
                out.beginObject();
                out.name("total").value(ramTotal);
                out.name("available").value(ramAvailable);
                out.name("used").value(ramUsed);
                out.name("total_mb").value(round1(ramTotal / 1048576.0));
                out.name("available_mb").value(round1(ramAvailable / 1048576.0));
                out.name("used_mb").value(round1(ramUsed / 1048576.0));
                out.endObject();

                out.name("servers");
                out.beginArray();
                for (int i = 0; i < SERVER_PORTS.length; i++) {
                    int port = SERVER_PORTS[i];
                    out.beginObject();
                    out.name("port").value(port);
                    out.name("name").value(SERVER_NAMES[i]);
                    out.name("open").value(isPortOpen(port));
                    out.endObject();
                }
                out.endArray();

                out.endObject();
            }
        });
    }

    private static double round1(double value) {
        return (double) Math.round(value * 10.0) / 10.0;
    }

    /** Chequeo TCP corto a 127.0.0.1: true si algo escucha en ese puerto. */
    private static boolean isPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), SERVER_CHECK_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
