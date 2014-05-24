package com.toluene.hotswapbatis;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.toluene.hotswapbatis.util.FileUtils;
import com.toluene.hotswapbatis.util.ReflectionUtil;

@Intercepts({
		@Signature(type = Executor.class, method = "query", args = {
				MappedStatement.class, Object.class, RowBounds.class,
				ResultHandler.class }),
		@Signature(type = Executor.class, method = "update", args = {
				MappedStatement.class, Object.class }) })
public class SwapPlugin implements Interceptor {

	private Map<String, Long> fileLastModified = new HashMap<String, Long>();

	public Object intercept(Invocation invocation) throws Throwable {
		MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
		String msId = ms.getId();
		msId = msId.substring(0, msId.lastIndexOf("."));
		Class<?> type = FileUtils.findClass(msId, true, this.getClass()
				.getClassLoader());
		if (type == null) {
			// we can do nothing here;
			throw new RuntimeException();
		}
		String fileFullName = msId.replaceAll("\\.", "/");
		fileFullName += ".xml";
		if (hasModified(fileFullName)) {
			cleanData(invocation, ms, type);
		}
		return invocation.proceed();
	}

	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	public void setProperties(Properties properties) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unchecked")
	private void cleanData(Invocation invocation, MappedStatement ms,
			Class<?> type) {
		Object target = invocation.getTarget();
		Object delegate = ReflectionUtil.getFieldValue(target, "delegate");
		Object configuration = ReflectionUtil.getFieldValue(delegate,
				"configuration");
		MapperRegistry mapperRegistry = (MapperRegistry) ReflectionUtil
				.getFieldValue(configuration, "mapperRegistry");
		Map<String, MappedStatement> mappedStatements = (HashMap<String, MappedStatement>) ReflectionUtil
				.getFieldValue(configuration, "mappedStatements");
		Set<String> keyShouldBeDel = new HashSet<String>();
		Set<String> keySet = mappedStatements.keySet();
		Iterator<String> it = keySet.iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (mappedStatements.get(key) == ms
					|| mappedStatements.get(key).getResource()
							.equals(ms.getResource())) {
				keyShouldBeDel.add(key);
			}
		}
		Iterator<String> itd = keyShouldBeDel.iterator();
		while (itd.hasNext()) {
			Object key = itd.next();
			mappedStatements.remove(key);
		}
		Map<Class<?>, MapperProxyFactory<?>> knownMappers = (Map<Class<?>, MapperProxyFactory<?>>) ReflectionUtil
				.getFieldValue(mapperRegistry, "knownMappers");
		Set<String> loadedResources = (HashSet<String>) ReflectionUtil
				.getFieldValue(configuration, "loadedResources");
		String xmlResource = type.getName().replace('.', '/') + ".xml";
		loadedResources.remove(type.toString());
		loadedResources.remove(xmlResource);
		knownMappers.remove(type);
		mapperRegistry.addMapper(type);
	}

	private boolean hasModified(String fileFullName) {
		URL url = FileUtils.getResource(fileFullName, this.getClass()
				.getClassLoader());
		File file = null;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (file == null)
			throw new RuntimeException();
		Long lastModified = fileLastModified.get(fileFullName);
		boolean reloadFlag = (lastModified == null)
				|| (lastModified < file.lastModified());
		if (lastModified == null) {
			fileLastModified.put(fileFullName, file.lastModified());
		}
		return reloadFlag;
	}

}