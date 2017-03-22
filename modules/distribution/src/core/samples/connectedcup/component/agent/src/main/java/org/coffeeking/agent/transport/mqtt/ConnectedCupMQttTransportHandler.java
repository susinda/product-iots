/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.coffeeking.agent.transport.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coffeeking.agent.transport.TransportHandlerException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

public class ConnectedCupMQttTransportHandler extends MQTTTransportHandler {

    private static Log log = LogFactory.getLog(ConnectedCupMQttTransportHandler.class);

    private static String iotServerSubscriber = UUID.randomUUID().toString().substring(0, 5);

    private static final String DEVICE_TYPE = "connectedcup";

    private static ConnectedCupMQttTransportHandler connectedCupMQttTransportHandler;

    private static String publishTopic = "%s/" + DEVICE_TYPE + "/%s";

    protected ConnectedCupMQttTransportHandler() {
        super(iotServerSubscriber, DEVICE_TYPE, "tcp://localhost:1886", "");
    }

    private ScheduledFuture<?> dataPushServiceHandler;

    public ScheduledFuture<?> getDataPushServiceHandler() {
        return dataPushServiceHandler;
    }

    public void setToken(String token) {
        setUsernameAndPassword(token, "");
    }

    @Override
    public void connect() {
        Runnable connect = new Runnable() {
            @Override
            public void run() {
                log.info("Trying to connect..");
                while (!isConnected()) {
                    try {
                        connectToQueue();
                    } catch (TransportHandlerException e) {
                        log.warn("Connection to MQTT Broker at: " +
                                mqttBrokerEndPoint + " failed");

                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException ex) {
                            log.error("MQTT-Subscriber: Thread Sleep Interrupt Exception");
                        }
                    }
                }

                log.info("Connected..");

            }
        };

        Thread connectorThread = new Thread(connect);
        connectorThread.start();

    }

    @Override
    public void processIncomingMessage(MqttMessage message, String... messageParams) {
    }

    public void publishToConnectedCup(String deviceOwner , String deviceId, String payLoad, String tenantDomain, int qos, boolean retained)
            throws TransportHandlerException{
        String topic = String.format(publishTopic, tenantDomain, deviceId);
        publishToQueue(topic, payLoad, qos, retained);
    }

    @Override
    public void disconnect() {
        Runnable stopConnection = new Runnable() {
            public void run() {
                while (isConnected()) {
                    try {
                        dataPushServiceHandler.cancel(true);
                        closeConnection();

                    } catch (MqttException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Unable to 'STOP' MQTT connection at broker at: " +
                                    mqttBrokerEndPoint);
                        }

                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException e1) {
                            log.error("MQTT-Terminator: Thread Sleep Interrupt Exception");
                        }
                    }
                }
            }
        };

        Thread terminatorThread = new Thread(stopConnection);
        terminatorThread.start();
    }


    @Override
    public void publishDeviceData() throws TransportHandlerException {

    }

    @Override
    public void publishDeviceData(MqttMessage publishData) throws TransportHandlerException {

    }

    @Override
    public void publishDeviceData(String... publishData) throws TransportHandlerException {

    }

    @Override
    public void processIncomingMessage() {

    }

    @Override
    public void processIncomingMessage(MqttMessage message) throws TransportHandlerException {

    }

    public static ConnectedCupMQttTransportHandler getInstance(){
        if(connectedCupMQttTransportHandler == null){
            connectedCupMQttTransportHandler = new ConnectedCupMQttTransportHandler();
        }
        return connectedCupMQttTransportHandler;
    }

}