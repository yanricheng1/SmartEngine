package com.alibaba.smart.framework.engine.service.command;

import java.io.InputStream;

import com.alibaba.smart.framework.engine.model.assembly.ProcessDefinitionSource;

/**
 *
 * 主要负责解析 XML，将其加载到单机内存里面。 需要注意的是,集群环境下需要注意内存数据的一致性.
 *
 * @author 高海军 帝奇  2016.11.11
 * @author ettear 2016.04.13
 */
public interface RepositoryCommandService {

    ProcessDefinitionSource deploy(String classPathResource) ;

    ProcessDefinitionSource deploy(String classPathResource,String tenantId) ;

    ProcessDefinitionSource deploy(InputStream inputStream) ;

    ProcessDefinitionSource deploy(InputStream inputStream,String tenantId) ;

    ProcessDefinitionSource deployWithUTF8Content(String uTF8ProcessDefinitionContent) ;

    ProcessDefinitionSource deployWithUTF8Content(String uTF8ProcessDefinitionContent,String tenantId) ;


}
