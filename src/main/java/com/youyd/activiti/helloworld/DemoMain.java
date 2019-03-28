package com.youyd.activiti.helloworld;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DemoMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);
    public static void main(String[] args) throws ParseException {
        LOGGER.info("启动我们的程序");
        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();
        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动流程
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程 [{}]",processInstance.getProcessDefinitionKey());

        //处理流程任务

        processTask(processEngine, processInstance);
        LOGGER.info("结束我们的程序");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance!=null&&!processInstance.isEnded()){
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            LOGGER.info("待处理任务数量[{}]",list.size());
            for (Task task : list) {
                LOGGER.info("待处理任务[{}]", task.getName());
                Map<String, Object> variables = getMap(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }


        }

        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().list();
        for (Task task : list) {
            LOGGER.info("待处理任务[{}]",task.getName());
        }
        LOGGER.info("待处理任务数量[{}]", list.size());
        scanner.close();
    }

    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = Maps.newHashMap();

        for (FormProperty formProperty : formProperties) {
            String line = null;
            if(StringFormType.class.isInstance(formProperty.getType())){
                LOGGER.info("请输入 {} ?",formProperty.getName());
                line=scanner.nextLine();
                variables.put(formProperty.getId(), line);
            }else if(DateFormType.class.isInstance((formProperty.getType()))){
                LOGGER.info("请输入 {} ? 格式 (yyyy-MM-dd)",formProperty.getName());
                line=scanner.nextLine();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = simpleDateFormat.parse(line);
                variables.put(formProperty.getId(), date);
            }else{
                LOGGER.info("类型暂不支持【{}】",formProperty.getType());
            }
            LOGGER.info("您输入的内容是[{}]",line);
        }
        return variables;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deployment = repositoryService.createDeployment();
        deployment.addClasspathResource("MyProcess.bpmn20.xml");
        Deployment deployMent = deployment.deploy();
        String deployMentId = deployMent.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployMentId).singleResult();
        LOGGER.info("流程定义文件[{}],流程id[{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = processEngine.VERSION;
        LOGGER.info("流程引擎的名称[{}],版本[{}]",name,version);
        return processEngine;
    }

}
