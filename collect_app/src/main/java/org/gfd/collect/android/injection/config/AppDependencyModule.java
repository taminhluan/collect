package org.gfd.collect.android.injection.config;

import android.app.Application;
import android.content.Context;
import android.telephony.SmsManager;

import com.google.android.gms.analytics.Tracker;

import org.gfd.collect.android.application.Collect;
import org.gfd.collect.android.dao.FormsDao;
import org.gfd.collect.android.dao.InstancesDao;
import org.gfd.collect.android.events.RxEventBus;
import org.gfd.collect.android.http.CollectServerClient;
import org.gfd.collect.android.http.HttpClientConnection;
import org.gfd.collect.android.http.OpenRosaHttpInterface;
import org.gfd.collect.android.tasks.sms.SmsSubmissionManager;
import org.gfd.collect.android.tasks.sms.contracts.SmsSubmissionManagerContract;
import org.gfd.collect.android.utilities.PermissionUtils;
import org.gfd.collect.android.utilities.WebCredentialsUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Add dependency providers here (annotated with @Provides)
 * for objects you need to inject
 */
@Module
public class AppDependencyModule {

    @Provides
    public SmsManager provideSmsManager() {
        return SmsManager.getDefault();
    }

    @Provides
    SmsSubmissionManagerContract provideSmsSubmissionManager(Application application) {
        return new SmsSubmissionManager(application);
    }

    @Provides
    Context context(Application application) {
        return application;
    }

    @Provides
    public InstancesDao provideInstancesDao() {
        return new InstancesDao();
    }

    @Provides
    public FormsDao provideFormsDao() {
        return new FormsDao();
    }

    @Provides
    @Singleton
    RxEventBus provideRxEventBus() {
        return new RxEventBus();
    }

    @Provides
    OpenRosaHttpInterface provideHttpInterface() {
        return new HttpClientConnection();
    }

    @Provides
    CollectServerClient provideCollectServerClient(OpenRosaHttpInterface httpInterface, WebCredentialsUtils webCredentialsUtils) {
        return new CollectServerClient(httpInterface, webCredentialsUtils);
    }

    @Provides
    WebCredentialsUtils provideWebCredentials() {
        return new WebCredentialsUtils();
    }

    @Provides
    @Singleton
    public Tracker providesTracker(Application application) {
        return ((Collect) application).getDefaultTracker();
    }

    @Provides
    public PermissionUtils providesPermissionUtils() {
        return new PermissionUtils();
    }
}
