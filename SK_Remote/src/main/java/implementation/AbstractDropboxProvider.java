package implementation;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import java.util.Objects;

abstract class AbstractDropboxProvider {

    /*
     * Pristupni KEY kojim se autorizujemo na DropBox nalog.
     * Kreiramo ga preko AppConsole:
     * https://www.dropbox.com/developers/apps
     * Najpre registrujemo aplikaciju, potom generisemo pristupni token.
     */
    private static final String ACCESS_TOKEN = "Bx_V6LLSvcAAAAAAAAAAEYf27080EU9GIPPgStczjAHSR-xZhBNxcrXE1HMIQ7cF";

    /*
     * Referenca na nalog
     */
    private DbxClientV2 client;

    protected DbxClientV2 getClient() {
        if(Objects.isNull(client)) connect();
        return client;
    }

    private void connect() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("SK_Remote").build();
        this.client = new DbxClientV2(config, ACCESS_TOKEN);
    }

}