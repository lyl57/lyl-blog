package com.lyl57.config;

import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Created by lyl57 on 2018/1/22
 */
@Configuration
public class ElasticsearchConfiguration implements FactoryBean<TransportClient>, InitializingBean, DisposableBean {
    Logger logger = Logger.getLogger(ElasticsearchConfiguration.class);
    @Value("${spring.data.elasticsearch.cluster-nodes}") //获取集群节点
    private String clusterNodes;

    @Value("${spring.data.elasticsearch.cluster-name}")//获取集群名称
    private String clusterName;

    private TransportClient client;

    @Override
    public void destroy() throws Exception {//销毁client
        try {
            logger.info("Closing elasticSearch client");
            if (client != null) {
                client.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing ElasticSearch client: ", e);
        }
    }

    @Override
    public TransportClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<TransportClient> getObjectType() {
        return TransportClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        buildClient();
    }

    //创建client
    protected void buildClient() {
        try {
            PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(settings());
            System.out.println("clusterNodes:" + clusterNodes);
            if (!"".equals(clusterNodes)) {
                for (String nodes : clusterNodes.split(",")) {
                    String InetSocket[] = nodes.split(":");
                    String Address = InetSocket[0];
                    Integer port = Integer.valueOf(InetSocket[1]);
                    preBuiltTransportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(Address), port));
                }
                client = preBuiltTransportClient;
                System.out.println("client" + client);
            }
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
            System.out.println("连接错误");
        }
    }

    //设置集群名称seetings
    private Settings settings() {
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName).build();
        client = new PreBuiltTransportClient(settings);
        System.out.println("clusterName:" + clusterName);
        return settings;
    }
}