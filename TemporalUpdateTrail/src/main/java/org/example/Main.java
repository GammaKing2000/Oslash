package org.example;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    static final String TASK_QUEUE = "HelloChildTaskQueue";
    static final String WORKFLOW_ID = "HelloChildWorkflow";

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);

        @SignalMethod
        void createChild(String flowId, String Data);
    }

    @WorkflowInterface
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String Greeting, String name);
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        String greeting = "hello World!";

        @Override
        public String getGreeting(String name) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(greeting+" made by main workflow      "+dtf.format(now));
            return greeting;
        }

        @Override
        public void createChild(String flowId, String data) {
            var newStub = Workflow.newContinueAsNewStub(GreetingWorkflow.class, ContinueAsNewOptions.newBuilder().setTaskQueue(flowId).build());
            newStub.getGreeting(data);
        }
    }

    public static class GreetingChildImpl implements GreetingChild {

        @Override
        public String composeGreeting(String greeting, String name) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            String path = "C:\\Users\\suman\\Documents\\Misc\\Temporal Trail\\";
            File file = new File(path + greeting+name+dtf.format(now)+".txt");
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(greeting + " " + name + "!" + "         " + dtf.format(now));
            return greeting + " " + name + "!";
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
        factory.start();

        GreetingWorkflow workflow = client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(WORKFLOW_ID)
                        .setCronSchedule("* * * * *")
                        .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                        .setWorkflowRunTimeout(Duration.ofSeconds(61))
                        .setTaskQueue(TASK_QUEUE)
                        .build());
        String greeting = workflow.getGreeting("World");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(greeting + "         " + dtf.format(now));
        System.exit(0);
    }

    public static ChildWorkflowOptions childOptions(String flowId) {
        return ChildWorkflowOptions.newBuilder()
                .setWorkflowId(flowId)
                .setCronSchedule("* * * * *")
                .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                .setWorkflowRunTimeout(Duration.ofSeconds(61))
                .build();
    }

}
/**
 * @WorkflowInterface public interface GreetingWorkflow {
 * @WorkflowMethod void setGreet(String name);
 * @SignalMethod void changeGreeting(String workflowId, int workflowNumber, String data);
 * }
 * public static void main(String[] args) {
 * WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
 * WorkflowClient client = WorkflowClient.newInstance(service);
 * GreetingWorkflow workflowById = client.newWorkflowStub(GreetingWorkflow.class, "InitialWorkflowId0");
 * workflowById.changeGreeting("1",1, "Universe");
 * <p>
 * }
 **/