/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.intesisbox.internal;

/**
 * The {@link IntesisBoxConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Cody Cutrer - Initial contribution
 */
public class IntesisBoxConfiguration {
    public String macAddress;
    public String ipAddress;
    public Integer port;
}
