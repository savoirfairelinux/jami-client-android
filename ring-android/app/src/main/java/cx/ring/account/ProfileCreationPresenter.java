package cx.ring.account;

import javax.inject.Inject;

import cx.ring.mvp.RootPresenter;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;

/**
 * Created by hdsousa on 17-05-17.
 */

public class ProfileCreationPresenter extends RootPresenter<ProfileCreationView> {

    public static final String TAG = ProfileCreationPresenter.class.getSimpleName();

    protected DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    public ProfileCreationPresenter(DeviceRuntimeService deviceRuntimeService) {
        this.mDeviceRuntimeService = deviceRuntimeService;
    }

    public void initPresenter() {
        //~ Checking the state of the READ_CONTACTS permission
        if (mDeviceRuntimeService.hasContactPermission()) {
            String profileName = mDeviceRuntimeService.getProfileName();
            if (profileName != null) {
                getView().displayProfileName(profileName);
            }
        } else {
            Log.d(TAG, "READ_CONTACTS permission is not granted.");
        }
    }

    public void galleryClick() {
        boolean hasPermission = mDeviceRuntimeService.hasGalleryPermission();
        if (hasPermission) {
            getView().goToGallery();
        } else {
            getView().askStoragePermission();
        }
    }

    public void cameraClick() {
        boolean hasPermission = mDeviceRuntimeService.hasVideoPermission() &&
                mDeviceRuntimeService.hasPhotoPermission();
        if (hasPermission) {
            getView().goToPhotoCapture();
        } else {
            getView().askPhotoPermission();
        }
    }

    public void nextClick() {
        getView().goToNext();
    }

    public void lastClick() {
        getView().goToLast();
    }

    @Override
    public void afterInjection() {

    }
}
