/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.managers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import pk.ajneb97.model.internal.UpdateCheckerResult;

public class UpdateCheckerManager {
    private String version;
    private String latestVersion;

    public UpdateCheckerManager(String version) {
        this.version = version;
    }

    public UpdateCheckerResult check() {
        try {
            HttpURLConnection con = (HttpURLConnection)new URL("https://api.spigotmc.org/legacy/update.php?resource=112616").openConnection();
            int timed_out = 1500;
            con.setConnectTimeout(timed_out);
            con.setReadTimeout(timed_out);
            this.latestVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            if (this.latestVersion.length() <= 7 && !this.version.equals(this.latestVersion)) {
                return UpdateCheckerResult.noErrors(this.latestVersion);
            }
            return UpdateCheckerResult.noErrors(null);
        }
        catch (Exception ex) {
            return UpdateCheckerResult.error();
        }
    }

    public String getLatestVersion() {
        return this.latestVersion;
    }
}

