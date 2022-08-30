package green_green_avk.anotherterm.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchErrorException;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SshHostKeyRepository implements HostKeyRepository {

    public static final class Exception extends RuntimeException {
        public Exception(final Throwable e) {
            super(e);
        }

        public Exception(final String message) {
            super(message);
        }
    }

    private final SharedPreferences sp;

    public SshHostKeyRepository(@NonNull final Context ctx) {
        sp = ctx.getSharedPreferences(ctx.getPackageName() + "_hostkeys",
                Context.MODE_PRIVATE);
    }

    @NonNull
    private static String serializeKey(@NonNull final HostKey hostKey) {
        return hostKey.getType() + ":" + hostKey.getKey() + ":" + hostKey.getComment();
    }

    @Override
    public void add(@NonNull final HostKey hostKey, @NonNull final UserInfo userInfo) {
        final Set<String> keys =
                new HashSet<>(sp.getStringSet(hostKey.getHost(),
                        Collections.emptySet()));
        final SharedPreferences.Editor ed = sp.edit();
        keys.add(serializeKey(hostKey));
        ed.putStringSet(hostKey.getHost(), keys);
        ed.apply();
    }

    @NonNull
    private static SshHostKey parseKey(@NonNull final String host, @NonNull final String unparsed) {
        final String[] kss = unparsed.split(":", 3);
        try {
            return new SshHostKey(host, kss[0], kss[1], kss[2]);
        } catch (final JSchException | JSchErrorException e) {
            throw new SshHostKeyRepository.Exception(e);
        }
    }

    @Override
    public int check(@NonNull final String host, @NonNull final byte[] bytes) {
        final Set<String> keys = sp.getStringSet(host, Collections.emptySet());
        if (keys.isEmpty())
            return NOT_INCLUDED;
        for (final String ent : keys) {
            final SshHostKey k = parseKey(host, ent);
            if (Arrays.equals(k.getRawKey(), bytes)) {
                return OK;
            }
        }
        return CHANGED;
    }

    @NonNull
    public Set<SshHostKey> getHostKeySet(@NonNull final String host,
                                         @Nullable final String keyType) {
        final Set<String> keys = sp.getStringSet(host, Collections.emptySet());
        final Set<SshHostKey> r = new HashSet<>();
        for (final String ent : keys) {
            final SshHostKey k = parseKey(host, ent);
            if (keyType == null || keyType.equals(k.getType()))
                r.add(k);
        }
        return r;
    }

    @NonNull
    public Set<SshHostKey> getHostKeySet() {
        final Set<SshHostKey> r = new HashSet<>();
        for (final String host : sp.getAll().keySet()) {
            r.addAll(getHostKeySet(host, null));
        }
        return r;
    }

    @Override
    @NonNull
    public HostKey[] getHostKey() {
        return getHostKeySet().toArray(new HostKey[0]);
    }

    @Override
    @NonNull
    public HostKey[] getHostKey(@NonNull final String host, @Nullable final String keyType) {
        return getHostKeySet(host, keyType).toArray(new HostKey[0]);
    }

    @Override
    @NonNull
    public String getKnownHostsRepositoryID() {
        return "Main Hosts Repository";
    }

    @Override
    public void remove(@NonNull final String host, @Nullable final String keyType) {
        final Set<String> keys =
                new HashSet<>(sp.getStringSet(host, Collections.emptySet()));
        final SharedPreferences.Editor ed = sp.edit();
        for (final SshHostKey k : getHostKeySet(host, keyType)) {
            keys.remove(serializeKey(k));
            ed.putStringSet(host, keys);
        }
        ed.apply();
    }

    @Override
    public void remove(@NonNull final String host, @Nullable final String keyType,
                       @NonNull final byte[] bytes) {
        final Set<String> keys =
                new HashSet<>(sp.getStringSet(host, Collections.emptySet()));
        final SharedPreferences.Editor ed = sp.edit();
        for (final SshHostKey k : getHostKeySet(host, keyType)) {
            if (Arrays.equals(k.getRawKey(), bytes)) {
                keys.remove(serializeKey(k));
                ed.putStringSet(host, keys);
                ed.apply();
                return;
            }
        }
    }

    public void remove(@NonNull final SshHostKey key) {
        remove(key.getHost(), key.getType(), key.getRawKey());
    }
}
