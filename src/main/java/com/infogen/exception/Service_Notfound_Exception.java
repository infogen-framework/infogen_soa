package com.infogen.exception;

import com.infogen.InfoGen_CODE;

/**
 * 依赖服务在注册中心/本地缓存没有找到的异常
 * 
 * @author larry/larrylv@outlook.com/创建时间 2015年6月18日 下午4:51:31
 * @since 1.0
 * @version 1.0
 */
public class Service_Notfound_Exception extends InfoGen_Exception {
	private static final long serialVersionUID = -8990952704800171175L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infogen.rpc.exception.InfoGen_RPC_Exception#code()
	 */
	@Override
	public Integer code() {
		return InfoGen_CODE.notfound_service.code;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infogen.rpc.exception.InfoGen_RPC_Exception#name()
	 */
	@Override
	public String name() {
		return InfoGen_CODE.notfound_service.name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.infogen.rpc.exception.InfoGen_RPC_Exception#note()
	 */
	@Override
	public String message() {
		return InfoGen_CODE.notfound_service.message;
	}

}
