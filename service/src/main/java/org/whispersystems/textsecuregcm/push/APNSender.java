/*
 * Copyright (C) 2013 Open WhisperSystems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecuregcm.push;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.textsecuregcm.configuration.ApnConfiguration;
import org.whispersystems.textsecuregcm.push.RetryingApnsClient.ApnResult;
import org.whispersystems.textsecuregcm.redis.RedisOperation;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.AccountsManager;
import org.whispersystems.textsecuregcm.storage.Device;
import org.whispersystems.textsecuregcm.util.Constants;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class APNSender implements Managed {

    private static final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME);
    private static final Meter unregisteredEventStale = metricRegistry.meter(name(APNSender.class, "unregistered_event_stale"));
    private static final Meter unregisteredEventFresh = metricRegistry.meter(name(APNSender.class, "unregistered_event_fresh"));
    private final Logger logger = LoggerFactory.getLogger(APNSender.class);
    private final AccountsManager accountsManager;
    private final String bundleId;
    private final boolean sandbox;
    private final RetryingApnsClient apnsClient;
    private ExecutorService executor;
    private ApnFallbackManager fallbackManager;

    public APNSender(AccountsManager accountsManager, ApnConfiguration configuration)
            throws IOException {
        this.accountsManager = accountsManager;
        this.bundleId = configuration.getBundleId();
        this.sandbox = configuration.isSandboxEnabled();
        this.apnsClient = new RetryingApnsClient(configuration.getPushCertificate(),
                configuration.getPushKey(),
                sandbox);
    }

    @VisibleForTesting
    public APNSender(ExecutorService executor, AccountsManager accountsManager, RetryingApnsClient apnsClient, String bundleId, boolean sandbox) {
        this.executor = executor;
        this.accountsManager = accountsManager;
        this.apnsClient = apnsClient;
        this.sandbox = sandbox;
        this.bundleId = bundleId;
    }

    public ListenableFuture<ApnResult> sendMessage(final ApnMessage message) {
        String topic = bundleId;

        if (message.isVoip()) {
            topic = topic + ".voip";
        }

        ListenableFuture<ApnResult> future = apnsClient.send(message.getApnId(), topic,
                message.getMessage(),
                new Date(message.getExpirationTime()));

        Futures.addCallback(future, new FutureCallback<ApnResult>() {
            @Override
            public void onSuccess(@Nullable ApnResult result) {
                if (message.getChallengeData().isPresent()) return;

                if (result == null) {
                    logger.warn("*** RECEIVED NULL APN RESULT ***");
                } else if (result.getStatus() == ApnResult.Status.NO_SUCH_USER) {
                    handleUnregisteredUser(message.getApnId(), message.getNumber(), message.getDeviceId());
                } else if (result.getStatus() == ApnResult.Status.GENERIC_FAILURE) {
                    logger.warn("*** Got APN generic failure: " + result.getReason() + ", " + message.getNumber());
                }
            }

            @Override
            public void onFailure(@Nullable Throwable t) {
                logger.warn("Got fatal APNS exception", t);
            }
        }, executor);

        return future;
    }

    @Override
    public void start() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void stop() {
        this.executor.shutdown();
        this.apnsClient.disconnect();
    }

    public void setApnFallbackManager(ApnFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
    }

    private void handleUnregisteredUser(String registrationId, String number, long deviceId) {
//    logger.info("Got APN Unregistered: " + number + "," + deviceId);

        Optional<Account> account = accountsManager.get(number);

        if (!account.isPresent()) {
            logger.info("No account found: " + number);
            unregisteredEventStale.mark();
            return;
        }

        Optional<Device> device = account.get().getDevice(deviceId);

        if (!device.isPresent()) {
            logger.info("No device found: " + number);
            unregisteredEventStale.mark();
            return;
        }

        if (!registrationId.equals(device.get().getApnId()) &&
                !registrationId.equals(device.get().getVoipApnId())) {
            logger.info("Registration ID does not match: " + registrationId + ", " + device.get().getApnId() + ", " + device.get().getVoipApnId());
            unregisteredEventStale.mark();
            return;
        }

//    if (registrationId.equals(device.get().getApnId())) {
//      logger.info("APN Unregister APN ID matches! " + number + ", " + deviceId);
//    } else if (registrationId.equals(device.get().getVoipApnId())) {
//      logger.info("APN Unregister VoIP ID matches! " + number + ", " + deviceId);
//    }

        long tokenTimestamp = device.get().getPushTimestamp();

        if (tokenTimestamp != 0 && System.currentTimeMillis() < tokenTimestamp + TimeUnit.SECONDS.toMillis(10)) {
            logger.info("APN Unregister push timestamp is more recent: " + tokenTimestamp + ", " + number);
            unregisteredEventStale.mark();
            return;
        }

//    logger.info("APN Unregister timestamp matches: " + device.get().getApnId() + ", " + device.get().getVoipApnId());
//    device.get().setApnId(null);
//    device.get().setVoipApnId(null);
//    device.get().setFetchesMessages(false);
//    accountsManager.update(account.get());

//    if (fallbackManager != null) {
//      fallbackManager.cancel(new WebsocketAddress(number, deviceId));
//    }

        if (fallbackManager != null) {
            RedisOperation.unchecked(() -> fallbackManager.cancel(account.get(), device.get()));
            unregisteredEventFresh.mark();
        }
    }
}
