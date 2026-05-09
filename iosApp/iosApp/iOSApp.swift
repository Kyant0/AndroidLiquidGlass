import SwiftUI
import shared


@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    @Environment(\.scenePhase)
    var scenePhase: ScenePhase

    init() {
        
    }

	var body: some Scene {
		WindowGroup {
            ContentView()
		}
	}
}


class AppDelegate: NSObject, UIApplicationDelegate {

}
