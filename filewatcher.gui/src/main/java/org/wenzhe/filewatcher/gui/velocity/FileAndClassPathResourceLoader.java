package org.wenzhe.filewatcher.gui.velocity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

public class FileAndClassPathResourceLoader extends FileResourceLoader {

	private final List<String> paths = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public void init(ExtendedProperties configuration) {
		super.init(configuration);
		paths.addAll(configuration.getVector("path"));
	}

	@Override
	public InputStream getResourceStream(String templateName) throws ResourceNotFoundException {
		try {
			InputStream resourceStream = super.getResourceStream(templateName);
			if (resourceStream != null) {
				return resourceStream;
			}
		} catch (ResourceNotFoundException e) {
			for (String path : paths) {
				if (!path.endsWith("/")) {
					path += "/";
				}
				InputStream stream = getClass().getResourceAsStream(path + templateName);
				if (stream != null) {
					return stream;
				}
        stream = getClass().getClassLoader().getResourceAsStream(path + templateName);
        if (stream != null) {
          return stream;
        }
			}
		}
		throw new ResourceNotFoundException("Resource not found in classpath nor fileSystem. " + templateName);
	}

}
