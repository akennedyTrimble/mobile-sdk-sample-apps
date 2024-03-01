package com.mapssampleapp;

import android.app.Application;

import com.facebook.react.ReactApplication;
import com.maps.react.TrimbleMapsPackages;
import com.reactnativecommunity.checkbox.ReactCheckBoxPackage;
import com.reactnativerestart.RestartPackage;
import com.oblador.vectoricons.VectorIconsPackage;
import com.rnfs.RNFSPackage;
import com.reactnativecommunity.slider.ReactSliderPackage;
import com.swmansion.rnscreens.RNScreensPackage;
import com.swmansion.gesturehandler.RNGestureHandlerPackage;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.th3rdwave.safeareacontext.SafeAreaContextPackage;
import com.facebook.soloader.SoLoader;

import org.devio.rn.splashscreen.SplashScreenReactPackage;

import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application implements ReactApplication {

  private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
    @Override
    public boolean getUseDeveloperSupport() {
      return BuildConfig.DEBUG;
    }

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new ReactCheckBoxPackage(),
        new RestartPackage(),
        new VectorIconsPackage(),
        new RNFSPackage(),
        new ReactSliderPackage(),
        new TrimbleMapsPackages(),
        new RNScreensPackage(),
        new RNGestureHandlerPackage(),
        new SafeAreaContextPackage(),
        new MapsSampleAppPackage(),
        new SplashScreenReactPackage()
      );
    }

        @Override
        protected String getJSMainModuleName() {
          return "index";
        }
      };

  @Override
  public ReactNativeHost getReactNativeHost() {
    return mReactNativeHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, /* native exopackage */ false);
  }
}
