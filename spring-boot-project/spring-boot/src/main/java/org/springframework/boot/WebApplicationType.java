/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * An enumeration of possible types of web application.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @since 2.0.0
 */
public enum WebApplicationType {

	/**
	 * The application should not run as a web application and should not start an
	 * embedded web server.
	 */
	NONE,

	/**
	 * The application should run as a servlet-based web application and should start an
	 * embedded servlet web server.
	 */
	SERVLET,

	/**
	 * The application should run as a reactive web application and should start an
	 * embedded reactive web server.
	 */
	REACTIVE;

	private static final String[] SERVLET_INDICATOR_CLASSES = { "jakarta.servlet.Servlet",
			"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final String WEBMVC_INDICATOR_CLASS = "org.springframework.web.servlet.DispatcherServlet";

	private static final String WEBFLUX_INDICATOR_CLASS = "org.springframework.web.reactive.DispatcherHandler";

	private static final String JERSEY_INDICATOR_CLASS = "org.glassfish.jersey.servlet.ServletContainer";

	static WebApplicationType deduceFromClasspath() {
		if (ClassUtils.isPresent(WEBFLUX_INDICATOR_CLASS, null) && !ClassUtils.isPresent(WEBMVC_INDICATOR_CLASS, null)
				&& !ClassUtils.isPresent(JERSEY_INDICATOR_CLASS, null)) {
			// org.springframework.web.reactive.DispatcherHandler 存在
			// org.springframework.web.servlet.DispatcherServlet 不存在
			// org.glassfish.jersey.servlet.ServletContainer 不存在 代表为响应式WebApplication
			//应用程序应作为响应式 Web 应用程序运行，并启动嵌入式响应式 Web 服务器。
			return WebApplicationType.REACTIVE;
		}
		for (String className : SERVLET_INDICATOR_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				// jakarta.servlet.Servlet
				// org.springframework.web.context.ConfigurableWebApplicationContezxt 这两个类不存在
				// 不是 Web 应用程序运行，也不应启动嵌入式 Web 服务器
				return WebApplicationType.NONE;
			}
		}
		// 应用程序作为基于 servlet 的 Web 应用程序运行，并启动嵌入式 servlet Web 服务器。
		return WebApplicationType.SERVLET;
	}

	static class WebApplicationTypeRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			for (String servletIndicatorClass : SERVLET_INDICATOR_CLASSES) {
				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
			}
			registerTypeIfPresent(JERSEY_INDICATOR_CLASS, classLoader, hints);
			registerTypeIfPresent(WEBFLUX_INDICATOR_CLASS, classLoader, hints);
			registerTypeIfPresent(WEBMVC_INDICATOR_CLASS, classLoader, hints);
		}

		private void registerTypeIfPresent(String typeName, ClassLoader classLoader, RuntimeHints hints) {
			if (ClassUtils.isPresent(typeName, classLoader)) {
				hints.reflection().registerType(TypeReference.of(typeName));
			}
		}

	}

}
