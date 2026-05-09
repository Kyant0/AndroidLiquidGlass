import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.kyant.backdrop.catalog.MainContent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController =
    ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
        },
    ) {
        MainContent()
    }