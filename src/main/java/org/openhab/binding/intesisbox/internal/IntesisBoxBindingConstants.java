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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IntesisBoxBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class IntesisBoxBindingConstants {

    private static final String BINDING_ID = "intesisbox";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_INTESISBOX = new ThingTypeUID(BINDING_ID, "intesisbox");

    // List of all Channel ids
    public static final String ONOFF = "onoff";
    public static final String MODE = "mode";
    public static final String SETPTEMP = "setptemp";
    public static final String AMBTEMP = "ambtemp";
    public static final String FANSP = "fansp";
    public static final String VANEUD = "vaneud";
    public static final String VANELR = "vanelr";
    public static final String ERRSTATUS = "errstatus";
    public static final String ERRCODE = "errcode";
}
