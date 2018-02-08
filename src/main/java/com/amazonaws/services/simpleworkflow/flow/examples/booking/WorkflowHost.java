/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.simpleworkflow.flow.examples.booking;

import com.amazonaws.services.simpleworkflow.flow.examples.common.ConfigHelper;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.worker.WorkerOptions;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WorkflowHost {
    private static WorkflowService.Iface swfService;
    private static String domain;
    private static int domainRetentionPeriodInDays;
    private static Worker worker;
    private static WorkflowHost host;

    // Factory method for Workflow Host
    public synchronized static WorkflowHost getWorkflowHost() {
        if (host == null) {
            host = new WorkflowHost();
        }
        return host;
    }

    public static void main(String[] args) throws Exception {
    	ConfigHelper configHelper = loadConfiguration();
    	
        getWorkflowHost().startWorkflowWorker(configHelper);

        // Add a Shutdown hook to close WorkflowWorker
        addShutDownHook();
        
        System.out.println("Please press any key to terminate service.");
        try {
            System.in.read();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
        
    private void startWorkflowWorker(ConfigHelper configHelper) throws Exception {
        System.out.println("Starting Workflow Host Service...");

        String taskList = configHelper.getValueFromConfig(BookingConfigKeys.WORKFLOW_WORKER_TASKLIST);
        WorkerOptions workerOptions = new WorkerOptions();
        workerOptions.setDisableActivityWorker(true);
        worker = new Worker(swfService, domain, taskList, workerOptions);
        worker.addWorkflowType(BookingWorkflowImpl.class);
        // Start the worker threads
        worker.start();

        System.out.println("Workflow Host Service Started...");
    }

    private void stopHost() throws InterruptedException {
        System.out.println("Stopping Workflow Host Service...");
        worker.shutdown(Long.MAX_VALUE, TimeUnit.DAYS);
        System.out.println("Workflow Host Service Stopped...");
    }
    
    static ConfigHelper loadConfiguration() throws IllegalArgumentException, IOException{
        ConfigHelper configHelper = ConfigHelper.createConfig();

        // Create the client for Simple Workflow Service and S3 Service
        swfService = configHelper.createWorkflowClient();
        domain = configHelper.getDomain();
        domainRetentionPeriodInDays = configHelper.getDomainRetentionPeriodInDays();
//        configHelper.createS3Client();
        
        return configHelper;
    }
    
    static void addShutDownHook(){
  	  Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {
                try {
                    getWorkflowHost().stopHost();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));    	
  }
}
