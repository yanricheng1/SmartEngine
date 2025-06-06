package com.alibaba.smart.framework.engine.test.service;

import com.alibaba.smart.framework.engine.constant.DeploymentStatusConstant;
import com.alibaba.smart.framework.engine.constant.RequestMapSpecialKeyConstant;
import com.alibaba.smart.framework.engine.model.instance.DeploymentInstance;
import com.alibaba.smart.framework.engine.model.instance.ExecutionInstance;
import com.alibaba.smart.framework.engine.model.instance.InstanceStatus;
import com.alibaba.smart.framework.engine.model.instance.ProcessInstance;
import com.alibaba.smart.framework.engine.model.instance.TaskInstance;
import com.alibaba.smart.framework.engine.service.param.command.CreateDeploymentCommand;
import com.alibaba.smart.framework.engine.service.param.query.ProcessInstanceQueryParam;
import com.alibaba.smart.framework.engine.service.param.query.TaskInstanceQueryParam;
import com.alibaba.smart.framework.engine.test.DatabaseBaseTestCase;
import com.alibaba.smart.framework.engine.test.process.helper.CustomExceptioinProcessor;
import com.alibaba.smart.framework.engine.test.process.helper.CustomVariablePersister;
import com.alibaba.smart.framework.engine.test.process.helper.DefaultMultiInstanceCounter;
import com.alibaba.smart.framework.engine.test.process.helper.DoNothingLockStrategy;
import com.alibaba.smart.framework.engine.test.process.helper.dispatcher.DefaultTaskAssigneeDispatcher;
import com.alibaba.smart.framework.engine.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ContextConfiguration("/spring/application-test.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ProcessServiceWithTenantIdTest extends DatabaseBaseTestCase {

    @Override
    protected void initProcessConfiguration() {
        super.initProcessConfiguration();
        processEngineConfiguration.setExceptionProcessor(new CustomExceptioinProcessor());
        processEngineConfiguration.setTaskAssigneeDispatcher(new DefaultTaskAssigneeDispatcher());
        processEngineConfiguration.setMultiInstanceCounter(new DefaultMultiInstanceCounter());
        processEngineConfiguration.setVariablePersister(new CustomVariablePersister());
        processEngineConfiguration.setLockStrategy(new DoNothingLockStrategy());
    }


    @Test
    public void test() throws Exception {

        String tenantId = "-10";

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("multi-instance-test.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");
        createDeploymentCommand.setTenantId(tenantId);

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");
        request.put("processVariable","processVariableValue");
        request.put(RequestMapSpecialKeyConstant.TENANT_ID, tenantId);


        ProcessInstance processInstance = processCommandService.start(
            deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
            ,request  );
        Assert.assertNotNull(processInstance);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());

        processInstance =   processQueryService.findById(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());
        Assert.assertEquals("multi-instance-user-task:1.0.2",processInstance.getProcessDefinitionIdAndVersion());
        Assert.assertEquals("multi-instance-user-task",processInstance.getProcessDefinitionId());
        Assert.assertEquals("1.0.2",processInstance.getProcessDefinitionVersion());

        Assert.assertEquals("123",processInstance.getStartUserId());
        Assert.assertEquals(InstanceStatus.running,processInstance.getStatus());
        Assert.assertNotNull(processInstance.getCompleteTime());
        Assert.assertNotNull(processInstance.getStartTime());
        Assert.assertNull(processInstance.getBizUniqueId());

        processCommandService.abort(processInstance.getInstanceId(),"reason",tenantId);
        processInstance =   processQueryService.findById(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(InstanceStatus.aborted,processInstance.getStatus());
        Assert.assertEquals("reason",processInstance.getReason());

        List<ExecutionInstance> executionInstanceList = executionQueryService.findActiveExecutionList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,executionInstanceList.size());
        List<TaskInstance> taskInstanceList =   taskQueryService.findAllPendingTaskList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,taskInstanceList.size());

    }


    @Test
    public void testAbortWithOneArg() throws Exception {

        String tenantId = "-11";

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("multi-instance-test.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");
        createDeploymentCommand.setTenantId(tenantId);

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");
        request.put("processVariable","processVariableValue");
        request.put(RequestMapSpecialKeyConstant.TENANT_ID,tenantId);


        ProcessInstance processInstance = processCommandService.start(
            deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
            ,request  );
        Assert.assertNotNull(processInstance);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());


        processCommandService.abort(processInstance.getInstanceId(),"",tenantId);
        processInstance =   processQueryService.findById(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(InstanceStatus.aborted,processInstance.getStatus());
        Assert.assertEquals("",processInstance.getReason());

        List<ExecutionInstance> executionInstanceList = executionQueryService.findActiveExecutionList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,executionInstanceList.size());
        List<TaskInstance> taskInstanceList =   taskQueryService.findAllPendingTaskList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,taskInstanceList.size());

    }


    @Test
    public void testAbortWithHashMap() throws Exception {

        String tenantId = "-12";

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("multi-instance-test.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");
        createDeploymentCommand.setTenantId(tenantId);

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");
        request.put("processVariable","processVariableValue");
        request.put(RequestMapSpecialKeyConstant.TENANT_ID,tenantId);


        ProcessInstance processInstance = processCommandService.start(
            deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
            ,request  );
        Assert.assertNotNull(processInstance);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());

        Map<String,Object> abortRequest = new HashMap<String,Object>();
        abortRequest.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_ABORT_REASON,"abort_reason");
        abortRequest.put(RequestMapSpecialKeyConstant.TASK_INSTANCE_TAG,"abort_tag");
        abortRequest.put(RequestMapSpecialKeyConstant.TENANT_ID,tenantId);

        processCommandService.abort(processInstance.getInstanceId(),abortRequest);

        processInstance =   processQueryService.findById(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(InstanceStatus.aborted,processInstance.getStatus());
        Assert.assertEquals("abort_reason",processInstance.getReason());


        List<ExecutionInstance> executionInstanceList = executionQueryService.findAll(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(4,executionInstanceList.size());

        executionInstanceList = executionQueryService.findActiveExecutionList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,executionInstanceList.size());
        List<TaskInstance> taskInstanceList =   taskQueryService.findAllPendingTaskList(processInstance.getInstanceId(),tenantId);
        Assert.assertEquals(0,taskInstanceList.size());



        TaskInstanceQueryParam taskInstanceQueryParam = new TaskInstanceQueryParam ();
        ArrayList<String> longs = new ArrayList<String>();
        longs.add(processInstance.getInstanceId());
        taskInstanceQueryParam.setProcessInstanceIdList(longs);

        List<TaskInstance> taskInstances =  taskQueryService.findList(taskInstanceQueryParam);
        Assert.assertNotNull(taskInstances);
        TaskInstance taskInstance = taskInstances.get(0);
        Assert.assertEquals(InstanceStatus.aborted.name(), taskInstance.getStatus());
        Assert.assertEquals("abort_tag", taskInstance.getTag());

    }

    @Test
    public void testAbortWithHashMapNoTenantId() throws Exception {

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("multi-instance-test.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");
        request.put("processVariable","processVariableValue");


        ProcessInstance processInstance = processCommandService.start(
                deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
                ,request  );
        Assert.assertNotNull(processInstance);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());

        Map<String,Object> abortRequest = new HashMap<String,Object>();
        abortRequest.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_ABORT_REASON,"abort_reason");
        abortRequest.put(RequestMapSpecialKeyConstant.TASK_INSTANCE_TAG,"abort_tag");

        processCommandService.abort(processInstance.getInstanceId(),abortRequest);

        processInstance =   processQueryService.findById(processInstance.getInstanceId(),null);
        Assert.assertEquals(InstanceStatus.aborted,processInstance.getStatus());
        Assert.assertEquals("abort_reason",processInstance.getReason());


        List<ExecutionInstance> executionInstanceList = executionQueryService.findAll(processInstance.getInstanceId(),null);
        Assert.assertEquals(4,executionInstanceList.size());

        executionInstanceList = executionQueryService.findActiveExecutionList(processInstance.getInstanceId(),null);
        Assert.assertEquals(0,executionInstanceList.size());
        List<TaskInstance> taskInstanceList =   taskQueryService.findAllPendingTaskList(processInstance.getInstanceId(),null);
        Assert.assertEquals(0,taskInstanceList.size());



        TaskInstanceQueryParam taskInstanceQueryParam = new TaskInstanceQueryParam ();
        ArrayList<String> longs = new ArrayList<String>();
        longs.add(processInstance.getInstanceId());
        taskInstanceQueryParam.setProcessInstanceIdList(longs);

        List<TaskInstance> taskInstances =  taskQueryService.findList(taskInstanceQueryParam);
        Assert.assertNotNull(taskInstances);
        TaskInstance taskInstance = taskInstances.get(0);
        Assert.assertEquals(InstanceStatus.aborted.name(), taskInstance.getStatus());
        Assert.assertEquals("abort_tag", taskInstance.getTag());

    }

    @Test
    public void testAbortWithHashMapWithoutTenantId() throws Exception {

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("multi-instance-test.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");
        request.put("processVariable","processVariableValue");


        ProcessInstance processInstance = processCommandService.start(
                deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
                ,request  );
        Assert.assertNotNull(processInstance);
        Assert.assertEquals("group",processInstance.getProcessDefinitionType());

        Map<String,Object> abortRequest = new HashMap<String,Object>();
        abortRequest.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_ABORT_REASON,"abort_reason");
        abortRequest.put(RequestMapSpecialKeyConstant.TASK_INSTANCE_TAG,"abort_tag");

        processCommandService.abort(processInstance.getInstanceId(),abortRequest);

        processInstance =   processQueryService.findById(processInstance.getInstanceId(),null);
        Assert.assertEquals(InstanceStatus.aborted,processInstance.getStatus());
        Assert.assertEquals("abort_reason",processInstance.getReason());


        List<ExecutionInstance> executionInstanceList = executionQueryService.findAll(processInstance.getInstanceId(),null);
        Assert.assertEquals(4,executionInstanceList.size());

        executionInstanceList = executionQueryService.findActiveExecutionList(processInstance.getInstanceId(),null);
        Assert.assertEquals(0,executionInstanceList.size());
        List<TaskInstance> taskInstanceList =   taskQueryService.findAllPendingTaskList(processInstance.getInstanceId(),null);
        Assert.assertEquals(0,taskInstanceList.size());



        TaskInstanceQueryParam taskInstanceQueryParam = new TaskInstanceQueryParam ();
        ArrayList<String> longs = new ArrayList<String>();
        longs.add(processInstance.getInstanceId());
        taskInstanceQueryParam.setProcessInstanceIdList(longs);

        List<TaskInstance> taskInstances =  taskQueryService.findList(taskInstanceQueryParam);
        Assert.assertNotNull(taskInstances);
        TaskInstance taskInstance = taskInstances.get(0);
        Assert.assertEquals(InstanceStatus.aborted.name(), taskInstance.getStatus());
        Assert.assertEquals("abort_tag", taskInstance.getTag());

    }

    @Test
    public void testQuery() throws Exception {

        String tenantId = "-14";

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("compatible-any-passed.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");
        createDeploymentCommand.setTenantId(tenantId);

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");
        request.put(RequestMapSpecialKeyConstant.TENANT_ID,tenantId);


        ProcessInstance  processInstance1 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstance  processInstance2 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstance  processInstance3 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );


        ProcessInstance  processInstance4 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstanceQueryParam processInstanceQueryParam1 = new ProcessInstanceQueryParam ();
        processInstanceQueryParam1.setProcessDefinitionType("group");
        processInstanceQueryParam1.setStatus(InstanceStatus.running.name());
        processInstanceQueryParam1.setStartUserId("123");
        processInstanceQueryParam1.setProcessDefinitionIdAndVersion("Process_1:1.0.0");
        processInstanceQueryParam1.setTenantId(tenantId);
        List<ProcessInstance> processInstanceList1 = processQueryService.findList(processInstanceQueryParam1);
        Assert.assertEquals(4,processInstanceList1.size());


        request.put(RequestMapSpecialKeyConstant.PROCESS_BIZ_UNIQUE_ID,"uniqueId");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");

        processCommandService.start(
            deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
            ,request  );

        ProcessInstanceQueryParam processInstanceQueryParam = new ProcessInstanceQueryParam ();
        processInstanceQueryParam.setProcessDefinitionType("group");
        processInstanceQueryParam.setStatus(InstanceStatus.running.name());
        processInstanceQueryParam.setStartUserId("123");
        processInstanceQueryParam.setProcessDefinitionIdAndVersion("Process_1:1.0.0");
        processInstanceQueryParam.setTenantId(tenantId);


        //processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(0);
        processInstanceQueryParam.setPageSize(10);

        List<ProcessInstance> processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(5,processInstanceList.size());

        processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(0);
        processInstanceQueryParam.setPageSize(10);
        processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(1,processInstanceList.size());

        processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(1);
        processInstanceQueryParam.setPageSize(10);
        processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(0,processInstanceList.size());

    }

    @Test
    public void testQueryNoTenantId() throws Exception {

        //3. 部署流程定义
        CreateDeploymentCommand createDeploymentCommand = new CreateDeploymentCommand();
        String content = IOUtil.readResourceFileAsUTF8String("compatible-any-passed.bpmn20.xml");
        createDeploymentCommand.setProcessDefinitionContent(content);
        createDeploymentCommand.setDeploymentUserId("123");
        createDeploymentCommand.setDeploymentStatus(DeploymentStatusConstant.ACTIVE);
        createDeploymentCommand.setProcessDefinitionDesc("desc");
        createDeploymentCommand.setProcessDefinitionName("name");
        createDeploymentCommand.setProcessDefinitionType("group");
        createDeploymentCommand.setProcessDefinitionCode("code");

        DeploymentInstance deploymentInstance =  deploymentCommandService.createDeployment(createDeploymentCommand);

        Assert.assertEquals("code",deploymentInstance.getProcessDefinitionCode());

        //4.启动流程实例

        Map<String, Object> request = new HashMap();
        request.put(RequestMapSpecialKeyConstant.PROCESS_INSTANCE_START_USER_ID,"123");


        ProcessInstance  processInstance1 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstance  processInstance2 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstance  processInstance3 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );


        ProcessInstance  processInstance4 = processCommandService.startWith(deploymentInstance.getInstanceId(),request  );

        ProcessInstanceQueryParam processInstanceQueryParam1 = new ProcessInstanceQueryParam ();
        processInstanceQueryParam1.setProcessDefinitionType("group");
        processInstanceQueryParam1.setStatus(InstanceStatus.running.name());
        processInstanceQueryParam1.setStartUserId("123");
        processInstanceQueryParam1.setProcessDefinitionIdAndVersion("Process_1:1.0.0");
        List<ProcessInstance> processInstanceList1 = processQueryService.findList(processInstanceQueryParam1);
        Assert.assertEquals(4,processInstanceList1.size());


        request.put(RequestMapSpecialKeyConstant.PROCESS_BIZ_UNIQUE_ID,"uniqueId");
        request.put(RequestMapSpecialKeyConstant.PROCESS_DEFINITION_TYPE,"group");

        processCommandService.start(
                deploymentInstance.getProcessDefinitionId(), deploymentInstance.getProcessDefinitionVersion()
                ,request  );

        ProcessInstanceQueryParam processInstanceQueryParam = new ProcessInstanceQueryParam ();
        processInstanceQueryParam.setProcessDefinitionType("group");
        processInstanceQueryParam.setStatus(InstanceStatus.running.name());
        processInstanceQueryParam.setStartUserId("123");
        processInstanceQueryParam.setProcessDefinitionIdAndVersion("Process_1:1.0.0");


        //processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(0);
        processInstanceQueryParam.setPageSize(10);

        List<ProcessInstance> processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(5,processInstanceList.size());

        processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(0);
        processInstanceQueryParam.setPageSize(10);
        processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(1,processInstanceList.size());

        processInstanceQueryParam.setBizUniqueId("uniqueId");
        processInstanceQueryParam.setPageOffset(1);
        processInstanceQueryParam.setPageSize(10);
        processInstanceList = processQueryService.findList(processInstanceQueryParam);
        Assert.assertEquals(0,processInstanceList.size());

    }

}