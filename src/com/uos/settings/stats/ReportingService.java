/*
 * Copyright (C) 2015 The UOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uos.settings.stats;

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Log;
import android.provider.Settings;

import java.util.List;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "UOSStats";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String EXTRA_OPTING_OUT = "stats::opt_out";

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());
        String deviceROMName = Utilities.getROM();
        String deviceROMVersion = Utilities.getUOSVersion();

        final int uosStatsJob = AnonymousStats.getNextJobId(getApplicationContext());
        if (DEBUG) Log.d(TAG, "scheduling jobs id: " + uosStatsJob);

        // get snapshot and persist it

        PersistableBundle uosBundle = new PersistableBundle();
        uosBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        uosBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        uosBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        uosBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        uosBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        uosBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);
        uosBundle.putString(StatsUploadJobService.KEY_ROM_NAME, deviceROMName);
        uosBundle.putString(StatsUploadJobService.KEY_ROM_VERSION, deviceROMVersion);
        uosBundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        // set job types
        uosBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_UOS);

        // schedule uos stats upload
        js.schedule(new JobInfo.Builder(uosStatsJob, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(uosBundle)
                .setPersisted(true)
                .build());

        // reschedule
        AnonymousStats.updateLastSynced(this);
        ReportingServiceManager.setAlarm(this);
    }
}
