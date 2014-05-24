package com.toluene.hotswapbatis.util;

import java.net.URL;

public class FileUtils {

	public static URL getResource(String resource, ClassLoader classLoader) {
		return getResource(resource, getClassLoaders(classLoader));
	}

	public static Class<?> findClass(String className, boolean initialize,
			ClassLoader classLoader) {
		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		ClassLoader[] classLoaders = new ClassLoader[] { classLoader,
				Thread.currentThread().getContextClassLoader(),
				FileUtils.class.getClassLoader(), systemClassLoader };
		for (ClassLoader cl : classLoaders) {
			if (null != cl) {
				Class<?> returnValue = null;
				try {
					returnValue = Class.forName(className, initialize, cl);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (null != returnValue)
					return returnValue;
			}
		}
		return null;
	}

	private static ClassLoader[] getClassLoaders(ClassLoader classLoader) {
		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		return new ClassLoader[] { classLoader,
				Thread.currentThread().getContextClassLoader(),
				FileUtils.class.getClassLoader(), systemClassLoader };
	}

	static URL getResource(String resource, ClassLoader[] classLoader) {
		for (ClassLoader cl : classLoader) {
			if (null != cl) {
				// try to find the resource as passed
				URL returnValue = cl.getResource(resource);
				// now, some class loaders want this leading "/", so we'll add
				// it and try again if we didn't find the resource
				if (null == returnValue)
					returnValue = cl.getResource("/" + resource);

				if (null != returnValue)
					return returnValue;
			}
		}
		return null;
	}
}
