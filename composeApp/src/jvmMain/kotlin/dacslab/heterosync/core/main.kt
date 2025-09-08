package dacslab.heterosync.core

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dacslab.heterosync.ui.desktop.DesktopApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HeteroSync - 다중 디바이스 동기화",
        state = WindowState(width = 1200.dp, height = 900.dp)
    ) {
        DesktopApp()
    }
}