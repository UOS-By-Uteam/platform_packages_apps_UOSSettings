/*
 * Copyright (C) 2016 The UOS Project
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

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.io.File;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class StatsUploadJobService extends JobService {

    private static final String TAG = StatsUploadJobService.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String KEY_JOB_TYPE = "job_type";
    public static final int JOB_TYPE_UOS = 1;

    public static final String KEY_UNIQUE_ID = "uniqueId";
    public static final String KEY_DEVICE_NAME = "deviceName";
    public static final String KEY_VERSION = "version";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_CARRIER = "carrier";
    public static final String KEY_CARRIER_ID = "carrierId";
    public static final String KEY_ROM_NAME = "romName";
    public static final String KEY_ROM_VERSION = "romVersion";
    public static final String KEY_TIMESTAMP = "timeStamp";

    private final Map<JobParameters, StatsUploadTask> mCurrentJobs
            = Collections.synchronizedMap(new ArrayMap<JobParameters, StatsUploadTask>());

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStartJob() called with " + "jobParameters = [" + jobParameters + "]");
        final StatsUploadTask uploadTask = new StatsUploadTask(jobParameters);
        mCurrentJobs.put(jobParameters, uploadTask);
        uploadTask.execute((Void) null);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (DEBUG)
            Log.d(TAG, "onStopJob() called with " + "jobParameters = [" + jobParameters + "]");

        final StatsUploadTask cancelledJob;
        cancelledJob = mCurrentJobs.remove(jobParameters);

        if (cancelledJob != null) {
            // cancel the ongoing background task
            cancelledJob.cancel(true);
            return true; // reschedule
        }

        return false;
    }

    private class StatsUploadTask extends AsyncTask<Void, Void, Boolean> {

        private JobParameters mJobParams;

        public StatsUploadTask(JobParameters jobParams) {
            this.mJobParams = jobParams;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            PersistableBundle extras = mJobParams.getExtras();

            String deviceId = extras.getString(KEY_UNIQUE_ID);
            String deviceName = extras.getString(KEY_DEVICE_NAME);
            String deviceVersion = extras.getString(KEY_VERSION);
            String deviceCountry = extras.getString(KEY_COUNTRY);
            String deviceCarrier = extras.getString(KEY_CARRIER);
            String deviceCarrierId = extras.getString(KEY_CARRIER_ID);
            String romName = extras.getString(KEY_ROM_NAME);
            String romVersion = extras.getString(KEY_ROM_VERSION);
            long timeStamp = extras.getLong(KEY_TIMESTAMP);

            boolean success = false;
            int jobType = extras.getInt(KEY_JOB_TYPE, -1);
            if (!isCancelled()) {
                switch (jobType) {

                    case JOB_TYPE_UOS:
                        try {
                            success = uploadToUOS(deviceId, deviceName, deviceVersion, deviceCountry,
                                    deviceCarrier, deviceCarrierId, romName, romVersion);
                        } catch (IOException e) {
                            Log.e(TAG, "Could not upload stats checkin to UOS server", e);
                            success = false;
                        }
                        break;
                }
            }
            if (DEBUG)
                Log.d(TAG, "job id " + mJobParams.getJobId() + ", has finished with success="
                        + success);
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mCurrentJobs.remove(mJobParams);
            jobFinished(mJobParams, !success);
        }
    }


    private boolean uploadToUOS(String deviceId, String deviceName, String deviceVersion,
                               String deviceCountry, String deviceCarrier, String deviceCarrierId, String romName, String romVersion)
            throws IOException {

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(getString(R.string.stats_uos_url) + "submit.php");
            boolean success = false;

            try {
                List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
    			kv.add(new BasicNameValuePair("device_hash", deviceId));
    			kv.add(new BasicNameValuePair("device_name", deviceName));
    			kv.add(new BasicNameValuePair("device_version", deviceVersion));
    			kv.add(new BasicNameValuePair("device_country", deviceCountry));
    			kv.add(new BasicNameValuePair("device_carrier", deviceCarrier));
    			kv.add(new BasicNameValuePair("device_carrier_id", deviceCarrierId));
                        kv.add(new BasicNameValuePair("rom_name", romName));
                        kv.add(new BasicNameValuePair("rom_version", romVersion));
    			kv.add(new BasicNameValuePair("sign_cert", "0"));

                httpPost.setEntity(new UrlEncodedFormEntity(kv));
                httpClient.execute(httpPost);

                success = true;
            } catch (IOException e) {
                Log.w(TAG, "Could not upload stats checkin", e);
            }

            return success;
    }

}
