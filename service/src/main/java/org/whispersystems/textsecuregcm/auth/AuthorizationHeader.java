/**
 * Copyright (C) 2013 Open WhisperSystems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecuregcm.auth;


import org.whispersystems.textsecuregcm.util.Base64;
import org.whispersystems.textsecuregcm.util.Util;

import java.io.IOException;

public class AuthorizationHeader {

    private final AmbiguousIdentifier identifier;
    private final long deviceId;
    private final String password;

    private AuthorizationHeader(AmbiguousIdentifier identifier, long deviceId, String password) {
        this.identifier = identifier;
        this.deviceId = deviceId;
        this.password = password;
    }

    public static AuthorizationHeader fromUserAndPassword(String user, String password) throws InvalidAuthorizationHeaderException {
        try {
            String[] numberAndId = user.split("\\.");
            return new AuthorizationHeader(new AmbiguousIdentifier(numberAndId[0]),
                    numberAndId.length > 1 ? Long.parseLong(numberAndId[1]) : 1,
                    password);
        } catch (NumberFormatException nfe) {
            throw new InvalidAuthorizationHeaderException(nfe);
        }
    }

    public static AuthorizationHeader fromFullHeader(String header) throws InvalidAuthorizationHeaderException {
        try {
            if (header == null) {
                throw new InvalidAuthorizationHeaderException("Null header");
            }

            String[] headerParts = header.split(" ");

            if (headerParts == null || headerParts.length < 2) {
                throw new InvalidAuthorizationHeaderException("Invalid authorization header: " + header);
            }

            if (!"Basic".equals(headerParts[0])) {
                throw new InvalidAuthorizationHeaderException("Unsupported authorization method: " + headerParts[0]);
            }

            String concatenatedValues = new String(Base64.decode(headerParts[1]));

            if (Util.isEmpty(concatenatedValues)) {
                throw new InvalidAuthorizationHeaderException("Bad decoded value: " + concatenatedValues);
            }

            String[] credentialParts = concatenatedValues.split(":");

            if (credentialParts == null || credentialParts.length < 2) {
                throw new InvalidAuthorizationHeaderException("Badly formated credentials: " + concatenatedValues);
            }

            return fromUserAndPassword(credentialParts[0], credentialParts[1]);
        } catch (IOException ioe) {
            throw new InvalidAuthorizationHeaderException(ioe);
        }
    }

    public AmbiguousIdentifier getIdentifier() {
        return identifier;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public String getPassword() {
        return password;
    }
}
