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
package org.whispersystems.textsecuregcm.storage;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.textsecuregcm.auth.AuthenticationCredentials;
import org.whispersystems.textsecuregcm.entities.SignedPreKey;
import org.whispersystems.textsecuregcm.util.Util;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class Device {

    public static final long MASTER_ID = 1;

    @JsonProperty
    private long id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String authToken;

    @JsonProperty
    private String salt;

    @JsonProperty
    private String signalingKey;

    @JsonProperty
    private String gcmId;

    @JsonProperty
    private String apnId;

    @JsonProperty
    private String voipApnId;

    @JsonProperty
    private long pushTimestamp;

    @JsonProperty
    private long uninstalledFeedback;

    @JsonProperty
    private boolean fetchesMessages;

    @JsonProperty
    private int registrationId;

    @JsonProperty
    private SignedPreKey signedPreKey;

    @JsonProperty
    private long lastSeen;

    @JsonProperty
    private long created;

    @JsonProperty
    private String userAgent;

    @JsonProperty
    private DeviceCapabilities capabilities;

    public Device() {
    }

    public Device(long id, String name, String authToken, String salt,
                  String signalingKey, String gcmId, String apnId,
                  String voipApnId, boolean fetchesMessages,
                  int registrationId, SignedPreKey signedPreKey,
                  long lastSeen, long created, String userAgent,
                  long uninstalledFeedback, DeviceCapabilities capabilities) {
        this.id = id;
        this.name = name;
        this.authToken = authToken;
        this.salt = salt;
        this.signalingKey = signalingKey;
        this.gcmId = gcmId;
        this.apnId = apnId;
        this.voipApnId = voipApnId;
        this.fetchesMessages = fetchesMessages;
        this.registrationId = registrationId;
        this.signedPreKey = signedPreKey;
        this.lastSeen = lastSeen;
        this.created = created;
        this.userAgent = userAgent;
        this.uninstalledFeedback = uninstalledFeedback;
        this.capabilities = capabilities;
    }

    public String getApnId() {
        return apnId;
    }

    public void setApnId(String apnId) {
        this.apnId = apnId;

        if (apnId != null) {
            this.pushTimestamp = System.currentTimeMillis();
        }
    }

    public String getVoipApnId() {
        return voipApnId;
    }

    public void setVoipApnId(String voipApnId) {
        this.voipApnId = voipApnId;
    }

    public long getUninstalledFeedbackTimestamp() {
        return uninstalledFeedback;
    }

    public void setUninstalledFeedbackTimestamp(long uninstalledFeedback) {
        this.uninstalledFeedback = uninstalledFeedback;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getCreated() {
        return this.created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getGcmId() {
        return gcmId;
    }

    public void setGcmId(String gcmId) {
        this.gcmId = gcmId;

        if (gcmId != null) {
            this.pushTimestamp = System.currentTimeMillis();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthenticationCredentials getAuthenticationCredentials() {
        return new AuthenticationCredentials(authToken, salt);
    }

    public void setAuthenticationCredentials(AuthenticationCredentials credentials) {
        this.authToken = credentials.getHashedAuthenticationToken();
        this.salt = credentials.getSalt();
    }

    public @Nullable
    DeviceCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(DeviceCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public String getSignalingKey() {
        return signalingKey;
    }

    public void setSignalingKey(String signalingKey) {
        this.signalingKey = signalingKey;
    }

    public boolean isEnabled() {
        boolean hasChannel = fetchesMessages || !Util.isEmpty(getApnId()) || !Util.isEmpty(getGcmId());

        return (id == MASTER_ID && hasChannel && signedPreKey != null) ||
                (id != MASTER_ID && hasChannel && signedPreKey != null && lastSeen > (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)));
    }

    public boolean getFetchesMessages() {
        return fetchesMessages;
    }

    public void setFetchesMessages(boolean fetchesMessages) {
        this.fetchesMessages = fetchesMessages;
    }

    public boolean isMaster() {
        return getId() == MASTER_ID;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public SignedPreKey getSignedPreKey() {
        return signedPreKey;
    }

    public void setSignedPreKey(SignedPreKey signedPreKey) {
        this.signedPreKey = signedPreKey;
    }

    public long getPushTimestamp() {
        return pushTimestamp;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Device)) return false;

        Device that = (Device) other;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) this.id;
    }

    public static class DeviceCapabilities {
        @JsonProperty
        private boolean uuid;

        @JsonProperty
        private boolean gv2;

        @JsonProperty
        private boolean storage;

        public DeviceCapabilities() {
        }

        public DeviceCapabilities(boolean uuid, boolean gv2, boolean storage) {
            this.uuid = uuid;
            this.gv2 = gv2;
            this.storage = storage;
        }

        public boolean isUuid() {
            return uuid;
        }

        public boolean isGv2() {
            return gv2;
        }

        public boolean isStorage() {
            return storage;
        }
    }

}
