package com.infogen;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.protobuf.BlockingService;
import com.infogen.aop.AOP;
import com.infogen.configuration.InfoGen_Configuration;
import com.infogen.http.InfoGen_Jetty;
import com.infogen.rpc.InfoGen_RPC;
import com.infogen.server.management.InfoGen_Loaded_Handle_Server;
import com.infogen.server.management.InfoGen_Server_Management;
import com.infogen.server.model.RemoteServer;
import com.infogen.tracking.annotation.Execution;
import com.infogen.tracking.event_handle.InfoGen_AOP_Handle_Execution;

/**
 * 启动infogen服务
 * 
 * @author larry/larrylv@outlook.com/创建时间 2015年5月19日 下午2:58:57
 * @since 1.0
 * @version 1.0
 */
public class InfoGen {
	private static final Logger LOGGER = LogManager.getLogger(InfoGen.class.getName());

	private InfoGen() {
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final String VERSION = "V2.6.11R161118";
	private static InfoGen_Server_Management CACHE_SERVER = InfoGen_Server_Management.getInstance();

	// //////////////////////////////////////////配置/////////////////////////////////////////////////////
	private static InfoGen_Configuration infogen_configuration = null;

	public static InfoGen_Configuration getInfogen_configuration() {
		return infogen_configuration;
	}

	/**
	 * 绑定 InfoGen 配置
	 * 
	 * @param infogen_configuration
	 *            配置文件
	 * @return InfoGen
	 */
	public static InfoGen create(InfoGen_Configuration infogen_configuration) {
		InfoGen.infogen_configuration = infogen_configuration;
		return new InfoGen();
	}

	/**
	 * 加载 InfoGen 配置
	 * 
	 * @param infogen_configuration_path
	 *            配置文件
	 * @return InfoGen
	 */
	public static InfoGen create(String infogen_configuration_path) {
		try {
			InfoGen.infogen_configuration = new InfoGen_Configuration().initialization(infogen_configuration_path);
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("初始化 infogen_configuration 失败", e);
			System.exit(-1);
		}
		return new InfoGen();
	}

	// ////////////////////////////////////////// AOP /////////////////////////////////////////////////////
	private static Boolean isAOP = false;

	/**
	 * 启动 AOP 功能 ， 默认会在 InfoGen_HTTP_Filter 初始化的时候加载
	 */
	public static void aop() {
		if (isAOP) {
			LOGGER.warn("AOP 已经开启");
		}
		isAOP = true;
		LOGGER.info("开启 AOP");
		AOP.getInstance().advice();
	}

	////////////////////////////////////// 获取服务 /////////////////////////////////////////////////////
	// 获取一个服务的缓存数据,如果没有则初始化拉取这个服务,并指定节点拉取完成的事件
	public static RemoteServer get_server(String server_name) {
		RemoteServer server = CACHE_SERVER.depend_server.get(server_name);
		if (server != null) {
			return server;
		}
		return init_server(server_name, (native_server) -> {
		});
	}

	// 获取一个服务的缓存数据,如果没有则初始化拉取这个服务,并指定节点拉取完成的事件
	public static RemoteServer get_server(String server_name, InfoGen_Loaded_Handle_Server server_loaded_handle) {
		RemoteServer server = CACHE_SERVER.depend_server.get(server_name);
		if (server != null) {
			return server;
		}
		return init_server(server_name, server_loaded_handle);
	}

	// 初始化服务,每个服务只会拉取一次
	public static RemoteServer init_server(String server_name, InfoGen_Loaded_Handle_Server server_loaded_handle) {
		RemoteServer server = CACHE_SERVER.cache_server_single(server_name, server_loaded_handle);
		if (server != null) {
			return server;
		}
		LOGGER.warn("没有找到可用服务:".concat(server_name));
		return server;
	}

	// ///////////////////////////////////////启动模块////////////////////////////////////////////////////////
	private Boolean isStart = false;
	private Boolean isRegister = false;

	/**
	 * 启动 InfoGen 的 AOP 和 服务治理
	 * 
	 * @return InfoGen 对象
	 * @throws IOException
	 *             网络异常
	 * @throws URISyntaxException
	 *             路径异常
	 */
	public InfoGen start() throws IOException, URISyntaxException {
		if (isStart) {
			LOGGER.warn("InfoGen 已经启动并开启监听服务");
			return this;
		}
		isStart = true;

		LOGGER.info("InfoGen 启动并开启监听服务");
		// 初始化缓存的服务
		CACHE_SERVER.init(infogen_configuration, () -> {// zookeeper
			// 因连接session过期重启后定制处理
			if (isRegister) {
				CACHE_SERVER.create_node(infogen_configuration.register_node);
			}
			// 这期间漏掉的Watch消息回调无法恢复 重新加载所有的服务和配置
			CACHE_SERVER.reload_all_server_flag = true;
		});
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("InfoGen关闭并关闭监听服务");
		}));
		return this;
	}

	/**
	 * 注册当前服务的节点
	 * 
	 * @return InfoGen 对象
	 */
	public InfoGen register() {
		if (!isStart) {
			LOGGER.error("InfoGen服务没有开启- InfoGen.create('infogen.properties').start();");
			System.exit(-1);
		}

		LOGGER.info("注册当前服务");
		isRegister = true;
		CACHE_SERVER.create_server(infogen_configuration.register_server, false);
		CACHE_SERVER.create_node(infogen_configuration.register_node);
		return this;
	}

	/**
	 * 注册当前服务并提交当前服务的方法列表
	 * 
	 * @return InfoGen 对象
	 */
	public InfoGen register_service() {
		if (!isStart) {
			LOGGER.error("InfoGen服务没有开启- InfoGen.create('infogen.properties').start();");
			System.exit(-1);
		}

		LOGGER.info("注册当前服务");
		CACHE_SERVER.create_server(infogen_configuration.register_server, true);
		LOGGER.info("提交当前服务的方法列表");
		CACHE_SERVER.create_service_functions(infogen_configuration.service_functions);
		return this;
	}

	/**
	 * 开启 @Execution 的日志追踪功能 使用 InfoGen_AOP_Handle_Execution
	 * 
	 * @return InfoGen
	 */
	public InfoGen track() {
		AOP.getInstance().add_advice_method(Execution.class, new InfoGen_AOP_Handle_Execution());
		return this;
	}

	private InfoGen_Jetty infogen_http;

	public InfoGen_Jetty getInfogen_http() {
		return infogen_http;
	}

	/**
	 * 开启 Jetty 服务
	 * 
	 * @return InfoGen 对象
	 */
	public InfoGen http() {
		infogen_http = InfoGen_Jetty.getInstance().start(infogen_configuration.register_node.getHttp_port());
		return this;
	}

	private InfoGen_RPC infogen_rpc;

	public InfoGen_RPC getInfogen_rpc() {
		return infogen_rpc;
	}

	/**
	 * 开启 RPC 服务
	 * 
	 * @return InfoGen 对象
	 */
	public InfoGen rpc() {
		try {
			infogen_rpc = InfoGen_RPC.getInstance().start(infogen_configuration.register_node.getRpc_port());
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
		return this;
	}

	/**
	 * 注册一个 RPC 方法
	 * 
	 * @param service
	 *            BlockingService
	 * @return InfoGen 对象
	 */
	public InfoGen registerService(final BlockingService service) {
		if (infogen_rpc != null) {
			infogen_rpc.registerService(service);
		}
		return this;
	}

	/**
	 * 阻塞当前线程
	 * 
	 * @throws InterruptedException
	 *             异常
	 */
	public void join() throws InterruptedException {
		Thread.currentThread().join();
	}
}
