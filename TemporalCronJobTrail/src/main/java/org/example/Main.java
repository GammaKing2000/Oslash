package org.example;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main {

    static final String TASK_QUEUE = "InitialTaskQueue";
    static final String WORKFLOW_ID = "InitialWorkflowId";
    static ArrayList<WorkflowOptions> workflowOptionsList = new ArrayList<>();

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        void setGreet(String name);

        @SignalMethod
        void changeGreeting(String workflowId, int workflowNumber, String data, WorkflowClient client);
    }

    @ActivityInterface
    public interface GreetingActivities {
        void greet(String greeting);

    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        String name;

        private final GreetingActivities activities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build());

        @Override
        public void setGreet(String name) {
            this.name = name;
            makeGreet();
        }

        //SIGNAL HANDLER
        @Override
        public void changeGreeting(String workflowId, int workflowNumber, String data, WorkflowClient client) {
            workflowOptionsList.add(makeOptions(workflowId));
            var workflowStub = client.newWorkflowStub(GreetingWorkflow.class, workflowOptionsList.get(workflowNumber));
            workflowStub.setGreet(data);
        }

        public void makeGreet() {
            activities.greet("Hello " + this.name + "!");
        }
    }

    static class GreetingsActivitiesImpl implements GreetingActivities {
        @Override
        public void greet(String greeting) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(
                    "From " + Activity.getExecutionContext().getInfo().getWorkflowId() + ": " + greeting + "      " + dtf.format(now) + "     " + workflowOptionsList.toString()
            );
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingsActivitiesImpl());

        factory.start();

        workflowOptionsList.add(makeOptions(WORKFLOW_ID + "0"));

        GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptionsList.get(0));

        try {
            WorkflowExecution execution = WorkflowClient.start(workflow::setGreet, "World");
            System.out.println("Started " + execution);
        } catch (WorkflowExecutionAlreadyStarted e) {
            // Thrown when a workflow with the same WORKFLOW_ID is currently running
            System.out.println("Already running as " + e.getExecution());
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static WorkflowOptions makeOptions(String workflowId) {
        return WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .setCronSchedule("* * * * *")
                .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                .setWorkflowRunTimeout(Duration.ofSeconds(60))
                .build();
    }
}