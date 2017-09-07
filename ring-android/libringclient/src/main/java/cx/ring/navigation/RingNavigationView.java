package cx.ring.navigation;

/**
 * Created by hdesousa on 07/09/17.
 */

public interface RingNavigationView {

    void showViewModel(RingNavigationViewModel viewModel);

    void gotToImageCapture();

    void askCameraPermission();

    void goToGallery();

    void askGalleryPermission();

}
