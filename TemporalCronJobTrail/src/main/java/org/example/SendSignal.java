package org.example;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
public class SendSignal {
    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);

        @SignalMethod
        void createChild(String flowId, String Data);
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        GreetingWorkflow workflowById = client.newWorkflowStub(GreetingWorkflow.class, "HelloChildWorkflow");
        workflowById.createChild("flowOuter", "Universe");
    }
}
