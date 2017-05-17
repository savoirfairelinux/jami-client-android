package cx.ring.account;

/**
 * Created by hdsousa on 17-05-17.
 */

public interface ProfileCreationView {

    void displayProfileName(String profileName);

    void goToGallery();

    void goToPhotoCapture();

    void askStoragePermission();

    void askPhotoPermission();

    void goToNext();

    void goToLast();

}
