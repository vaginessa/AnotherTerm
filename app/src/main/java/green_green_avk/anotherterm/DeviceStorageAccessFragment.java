package green_green_avk.anotherterm;

import android.Manifest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public final class DeviceStorageAccessFragment extends PermissionsAccessFragment {
    private static final String[] PERMS =
            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};

    @NonNull
    @Override
    public String[] getPerms() {
        return PERMS;
    }

    @Override
    public int getLayout() {
        return R.layout.device_storage_access_fragment;
    }
}
