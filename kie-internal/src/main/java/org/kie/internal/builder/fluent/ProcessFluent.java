/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.internal.builder.fluent;

import java.util.Map;

import org.kie.api.runtime.process.ProcessRuntime;

/**
 * see {@link ProcessRuntime}
 */
public interface ProcessFluent<T, U> {

    /**
     * Start a new process instance.  The process (definition) that should
     * be used is referenced by the given process id.
     *
     * @param processId  The id of the process that should be started
     * @return the <code>ProcessInstance</code> that represents the instance of the process that was started
     */
    T startProcess(String processId);

    /**
     * Start a new process instance.  The process (definition) that should
     * be used is referenced by the given process id.  Parameters can be passed
     * to the process instance (as name-value pairs), and these will be set
     * as variables of the process instance.
     *
     * @param processId  the id of the process that should be started
     * @param parameters  the process variables that should be set when starting the process instance
     * @return the <code>ProcessInstance</code> that represents the instance of the process that was started
     */
    T startProcess(String processId,
                                 Map<String, Object> parameters);

    /**
     * Creates a new process instance (but does not yet start it).  The process
     * (definition) that should be used is referenced by the given process id.
     * Parameters can be passed to the process instance (as name-value pairs),
     * and these will be set as variables of the process instance.  You should only
     * use this method if you need a reference to the process instance before actually
     * starting it.  Otherwise, use startProcess.
     *
     * @param processId  the id of the process that should be started
     * @param parameters  the process variables that should be set when creating the process instance
     * @return the <code>ProcessInstance</code> that represents the instance of the process that was created (but not yet started)
     */
    T createProcessInstance(String processId,
                                          Map<String, Object> parameters);

    /**
     * Starts the given process instance (which was created by using createProcesInstance
     * but not yet started).  This method can only be called once for each process
     * instance.  You should only use this method if you need a reference to the
     * process instance before actually starting it.  Otherwise, use startProcess.
     *
     * @param processInstanceId  the id of the process instance that needs to be started
     * @return the <code>ProcessInstance</code> that represents the instance of the process that was started
     */
    T startProcessInstance(String processInstanceId);

    /**
     * Signals the engine that an event has occurred. The type parameter defines
     * which type of event and the event parameter can contain additional information
     * related to the event.  All process instances that are listening to this type
     * of (external) event will be notified.  For performance reasons, this type of event
     * signaling should only be used if one process instance should be able to notify
     * other process instances. For internal event within one process instance, use the
     * signalEvent method that also include the processInstanceId of the process instance
     * in question.
     *
     * @param type the type of event
     * @param event the data associated with this event
     */
    T signalEvent(String type,
                     Object event);

    /**
     * Signals the process instance that an event has occurred. The type parameter defines
     * which type of event and the event parameter can contain additional information
     * related to the event.  All node instances inside the given process instance that
     * are listening to this type of (internal) event will be notified.  Note that the event
     * will only be processed inside the given process instance.  All other process instances
     * waiting for this type of event will not be notified.
     *
     * @param type the type of event
     * @param event the data associated with this event
     * @param processInstanceId the id of the process instance that should be signaled
     */
    T signalEvent(String type,
                     Object event,
                     String processInstanceId);

    /**
     * Aborts the process instance with the given id.  If the process instance has been completed
     * (or aborted), or the process instance cannot be found, this method will throw an
     * <code>IllegalArgumentException</code>.
     *
     * @param processInstanceId the id of the process instance
     */
    T abortProcessInstance(String processInstanceId);

    /**
     * Returns the <code>WorkItemManager</code> related to this session.  This can be used to
     * register new <code>WorkItemHandler</code>s or to complete (or abort) <code>WorkItem</code>s.
     *
     * @return the <code>WorkItemManager</code> related to this session
     */
    WorkItemManagerFluent<WorkItemManagerFluent, T, U> getWorkItemManager();
}
