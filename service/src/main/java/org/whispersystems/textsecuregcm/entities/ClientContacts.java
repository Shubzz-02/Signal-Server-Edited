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
package org.whispersystems.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

public class ClientContacts {

    @JsonProperty
    private List<ClientContact> contacts;

    public ClientContacts(List<ClientContact> contacts) {
        if (contacts != null) this.contacts = contacts;
        else this.contacts = new LinkedList<>();
    }

    public ClientContacts() {
        this.contacts = new LinkedList<>();
    }

    public List<ClientContact> getContacts() {
        return contacts;
    }
}
