#import "Lamp2Plugin.h"
#if __has_include(<lamp2/lamp2-Swift.h>)
#import <lamp2/lamp2-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "lamp2-Swift.h"
#endif

@implementation Lamp2Plugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftLamp2Plugin registerWithRegistrar:registrar];
}
@end
