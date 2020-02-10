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

import static org.openhab.binding.intesisbox.internal.IntesisBoxBindingConstants.*;
import static org.openhab.binding.intesisbox.internal.Message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IntesisBoxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class IntesisBoxHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(IntesisBoxHandler.class);

    private @Nullable IntesisBoxConfiguration config;

    public IntesisBoxHandler(Thing thing) {
        super(thing);
    }
   
    private Map<String, List<String>> limits = new HashMap<String, List<String>>();
    private double minTemp = 0.0d, maxTemp = 0.0d;
    @Nullable
    private String ipAddress;
    private int tcpPort;
    @Nullable
    private Socket tcpSocket = null;
    @Nullable
    private OutputStreamWriter tcpOutput = null;
    @Nullable
    private BufferedReader tcpInput = null;

    private boolean connected = false;
    @Nullable
    private ScheduledFuture<?> pollingTask;


    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!isConnected()) {
            return;
        }

        String value = "";

        switch (channelUID.getId()) {
            case ONOFF:
                if (command instanceof OnOffType) {
                    value = ((OnOffType) command == OnOffType.ON) ? "ON" : "OFF";
                }
                break;
            case SETPTEMP:
                if (command instanceof QuantityType) {
                    QuantityType<Temperature> celsiusTemperature = ((QuantityType<Temperature>) command)
                            .toUnit(SIUnits.CELSIUS);
                    if (celsiusTemperature != null) {
                        double doubleValue = celsiusTemperature.doubleValue();
                        if (doubleValue < minTemp) {
                            doubleValue = minTemp;
                        } else if (doubleValue > maxTemp) {
                            doubleValue = maxTemp;
                        }
                        value = String.valueOf((int) Math.round(doubleValue * 10));
                    }
                }
            case MODE:
            case FANSP:
            case VANEUD:
            case VANELR:
                if (command instanceof StringType) {
                    value = command.toString();
                    if (!checkLimit(channelUID.getId(), value)) {
                        value = "";
                    }
                }
                break;
            case AMBTEMP:
            case ERRSTATUS:
            case ERRCODE:
                break;
            default:
                logger.warn("Unknown channel {}", channelUID.getId());
                return;
        }

        if (command instanceof RefreshType) {
            sendQuery(channelUID.getId());
            return;
        }

        if (value.isEmpty()) {
            logger.warn("Could not send command {} to {}", command, channelUID.getId());
            return;
        }
        sendCommand(channelUID.getId(), value);
    }

    private void receivedUpdate(String function, String value) {
        logger.debug("receivedUpdate(): {} {}", function, value);
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), function.toLowerCase());
        switch (channelUID.getId()) {
            case ONOFF:
                updateState(channelUID, OnOffType.from(value));
                break;
            case SETPTEMP:
            case AMBTEMP:
                updateState(channelUID, new QuantityType<Temperature>(Double.valueOf(value) / 10.0d, SIUnits.CELSIUS));
                break;
            case MODE:
            case FANSP:
            case VANEUD:
            case VANELR:
                updateState(channelUID, new StringType(value));
                break;
        }

    }

    private synchronized boolean checkLimit(String function, String value) {
        if (!limits.containsKey(function)) {
            return false;
        }
        return limits.get(function).contains(value);
    }

    private void handleMessage(String data) {
    
        if (data.equals("ACK") || data.equals(null) || data.equals("")) {
            return;
        }
        

        Message message = Message.parse(data);
        switch (message.getCommand()) {
            case LIMITS:
                synchronized (this) {
                    if (message.getFunction().toLowerCase().equals(SETPTEMP)) {
                        List<Double> limits = message.getLimitsValue().stream().map(l -> Double.valueOf(l) / 10)
                                .collect(Collectors.toList());
                        if (limits.size() == 2) {
                            minTemp = limits.get(0);
                            maxTemp = limits.get(1);
                        }

                    } else {
                        limits.put(message.getFunction().toLowerCase(), message.getLimitsValue());
                    }
                }
                break;
            case CHN:
                receivedUpdate(message.getFunction(), message.getValue());
                break;

        }
        
    }

    private void sendAlive() {
        String data = String.format("GET,1:ONOFF\r\n", "onoff");
        write(data);
        logger.info("keep alive sent");
    }

    private void sendCommand(String function, String value) {
        String data = String.format("SET,1:%s,%s\r\n", function, value);
        write(data);
        logger.debug("sendCommand(): '{}' Command Sent - {} {}", function, value);
    }

    private void sendQuery(String function) {
        String data = String.format("GET,1:%s\r\n", function);
        write(data);
        logger.debug("sendQuery(): '{}' Command Sent - {}", function);
    }

    private boolean isConnected() {
        return this.connected;
    }

    private void setConnected(boolean connected) {
        this.connected = connected;
        updateStatus(connected ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        closeConnection();
        super.dispose();
    }

    private void stopPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(true);
            pollingTask = null;
        }
    }

    private synchronized void polling() {
        if (!isConnected()) {
            openConnection();   
        }
        sendAlive();
    }

    private void openConnection() {
        try {
            closeConnection();

            logger.debug("openConnection(): Connecting to IntesisBox ");

            tcpSocket = new Socket();
            SocketAddress tpiSocketAddress = new InetSocketAddress(ipAddress, tcpPort);
            tcpSocket.connect(tpiSocketAddress);
            tcpOutput = new OutputStreamWriter(tcpSocket.getOutputStream(), "US-ASCII");
            tcpInput = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));

            write("LIMITS:*\r\n");
            Thread tcpListener = new Thread(new TCPListener());
            tcpListener.start();

            setConnected(true);
        } catch (UnknownHostException unknownHostException) {
            logger.error("openConnection(): Unknown Host Exception: {}", unknownHostException.getMessage());
            setConnected(false);
        } catch (SocketException socketException) {
            logger.error("openConnection(): Socket Exception: {}", socketException.getMessage());
            setConnected(false);
        } catch (IOException ioException) {
            logger.error("openConnection(): IO Exception: {}", ioException.getMessage());
            setConnected(false);
        } catch (Exception exception) {
            logger.error("openConnection(): Unable to open a connection: {} ", exception.getMessage(), exception);
            setConnected(false);
        }
    }

    private void write(String data) {
        try {
            tcpOutput.write(data);
            tcpOutput.flush();
        } catch (IOException ioException) {
            logger.error("write(): {}", ioException.getMessage());
            setConnected(false);
        } catch (Exception exception) {
            logger.error("write(): Unable to write to socket: {} ", exception.getMessage(), exception);
            setConnected(false);
        }
    
    }

    public String read() {
        String message = "";
        try {
            
            message = tcpInput.readLine();
            logger.debug("read(): Message Received: {}", message);
        } catch (IOException ioException) {
            logger.error("read(): IO Exception: {}", ioException.getMessage());
            setConnected(false);
        } catch (Exception exception) {
            logger.error("read(): Exception: {} ", exception.getMessage(), exception);
            setConnected(false);
        }

        return message;
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        IntesisBoxConfiguration config = getConfigAs(IntesisBoxConfiguration.class);

        if (config.ipAddress != null) {
            ipAddress = config.ipAddress;
            tcpPort = config.port.intValue();

            updateStatus(ThingStatus.OFFLINE);
            if (pollingTask == null || pollingTask.isCancelled()) {
                pollingTask = scheduler.scheduleWithFixedDelay(this::polling, 0, 30, TimeUnit.SECONDS);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No IP address specified)");
        }
    }

    public void closeConnection() {
        try {
            if (tcpSocket != null) {
                logger.debug("closeConnection(): Closing Socket!");
                tcpSocket.close();
                tcpSocket = null;
            }
            if (tcpInput != null) {
                logger.debug("closeConnection(): Closing Output Writer!");
                tcpInput.close();
                tcpInput = null;
            }
            if (tcpOutput != null) {
                logger.debug("closeConnection(): Closing Input Reader!");
                tcpOutput.close();
                tcpOutput = null;
            }

            setConnected(false);
            logger.debug("closeConnection(): Closed TCP Connection!");
        } catch (IOException ioException) {
            logger.error("closeConnection(): Unable to close connection - {}", ioException.getMessage());
        } catch (Exception exception) {
            logger.error("closeConnection(): Error closing connection - {}", exception.getMessage());
        }
    }


    private class TCPListener implements Runnable {
        private final Logger logger = LoggerFactory.getLogger(TCPListener.class);

        /**
         * Run method. Runs the MessageListener thread
         */
        @Override
        public void run() {
            String messageLine;

            try {
                while (isConnected()) {
                    messageLine = read();
                    try {
                        handleMessage(messageLine);
                    } catch (Exception e) {
                        logger.error("TCPListener(): Message not handled by thing: {}", e.getMessage());
                        closeConnection();
                    }
                }
            } catch (Exception e) {
                logger.error("TCPListener(): Unable to read message: {} ", e.getMessage(), e);
                closeConnection();
            }
        }
    }
}
