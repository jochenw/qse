package com.github.jochenw.qse.is.core.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface Logger extends AutoCloseable {
	public void trace(String pMsg, Object... pArgs);
	public void debug(String pMsg, Object... pArgs);
	public void info(String pMsg, Object... pArgs);
	public void warn(String pMsg, Object... pArgs);
	public void error(String pMsg, Object... pArgs);
	public void fatal(String pMsg, Object... pArgs);

	public static Logger getNullLogger() {
		final InvocationHandler ih = new InvocationHandler() {
			@Override
			public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable {
				throw new IllegalStateException("Method not implemented: " + pMethod);
			}
		};
		final Class<?>[] interfaceClasses = { Logger.class };
		return (Logger) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaceClasses, ih);
	}
}
