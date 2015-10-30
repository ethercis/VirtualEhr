/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Copyright
package com.ethercis.partyservice;


import org.openehr.rm.common.generic.PartyIdentified;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public interface I_PartyIdentifiedService {

    UUID retrievePartyByIdentifier(String idCode, String issuer);

    UUID getOrCreateParty(PartyIdentified partyIdentified);

    UUID getOrCreateParty(String name, String idCode, String issuer, String assigner, String typeName);

    Integer deleteParty(UUID id);
}
