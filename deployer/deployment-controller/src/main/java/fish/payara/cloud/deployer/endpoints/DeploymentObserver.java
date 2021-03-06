/*
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.cloud.deployer.endpoints;

import fish.payara.cloud.deployer.process.ChangeKind;
import fish.payara.cloud.deployer.process.LogProduced;
import fish.payara.cloud.deployer.process.StateChanged;

import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

/**
 *
 * @author jonathan coustick
 */
@ApplicationScoped
class DeploymentObserver {

    private ConcurrentHashMap<String, SseBroadcaster> broadcasts;
    private Jsonb jsonb;

    @Context
    Sse sse;

    protected DeploymentObserver() {
        broadcasts = new ConcurrentHashMap<>();
        jsonb = JsonbBuilder.create();
    }

    public void addRequest(SseEventSink eventSink, String processID) {
        SseBroadcaster broadcaster = broadcasts.computeIfAbsent(processID, p -> sse.newBroadcaster());
        broadcaster.register(eventSink);
    }

    void eventListener(@ObservesAsync StateChanged event) {
        String processID = event.getProcess().getId();

        var broadcaster = broadcasts.get(processID);
        if (broadcaster == null) {
            return;
        }
        OutboundSseEvent outboundEvent = createEvent(event);
        broadcaster.broadcast(outboundEvent);
        if (event.getKind().isTerminal()) {
            broadcasts.get(processID).close();
            broadcasts.remove(processID);
        }

    }

    private OutboundSseEvent createEvent(StateChanged event) {
        if (event instanceof LogProduced) {
            return createLogEvent((LogProduced)event);
        }
        return sse.newEvent(jsonb.toJson(event));
    }

    private OutboundSseEvent createLogEvent(LogProduced event) {
        return sse.newEvent("log", event.getChunk());
    }


}
