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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Cody Cutrer - Initial contribution
 */
public class Message {
    public static final String ID = "ID";
    public static final String INFO = "INFO";
    public static final String SET = "SET";
    public static final String CHN = "CHN";
    public static final String GET = "GET";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String CFG = "CFG";
    public static final String LIMITS = "LIMITS";
    public static final String DISCOVER = "DISCOVER";

    private static final Pattern REGEX = Pattern.compile(
            "^(ID|INFO|SET|CHN|GET|LOGIN|LOGOUT|CFG|LIMITS)(?:,(\\d+))?:(ONOFF|MODE|SETPTEMP|FANSP|VANEUD|VANELR|AMBTEMP|ERRSTATUS|ERRCODE),([A-Z0-9,\\[\\]]+)$");

    private final String command;
    private final String acNum;
    private final String function;
    private final String value;

    private Message(String command, String acNum, String function, String value) {
        this.command = command;
        this.acNum = acNum;
        this.function = function;
        this.value = value;
    }

    public String getCommand() {
        return command;
    }

    public String getFunction() {
        return function;
    }

    public String getValue() {
        return value;
    }

    public List<String> getLimitsValue() {
        return Arrays.asList(value.substring(1, value.length() - 1).split(","));
    }

    public static Message parse(String message) {
        Matcher m = REGEX.matcher(message);
        if (!m.find()) {
            return null;
        }

        return new Message(m.group(1), m.group(2), m.group(3), m.group(4));
    }
}
