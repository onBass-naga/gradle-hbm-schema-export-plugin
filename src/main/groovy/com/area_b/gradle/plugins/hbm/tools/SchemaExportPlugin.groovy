/*
* Copyright 2015 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.area_b.gradle.plugins.hbm.tools

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.hibernate.cfg.Configuration
import org.hibernate.tool.hbm2ddl.SchemaExport
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory

import javax.persistence.Entity

import static org.hibernate.cfg.AvailableSettings.DIALECT as HIBERNATE_DIALECT

/**
 * Created by naga on 2015/02/08.
 */
class SchemaExportPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.extensions.create("schemaExport", SchemaExportPluginExtension)

        project.task('schemaExport') << {

            def ext = project.schemaExport

            def resources = findresources(ext.classpath)
            def configuration = createConfiguration(resources, ext.dialectClass)

            new SchemaExport(configuration)
                    .setOutputFile(ext.outputFile)
                    .setDelimiter(ext.delimiter)
                    .create(true, false);
        }
    }

    def findresources(path) {

        def classpath = path.endsWith("/")? path : path + "/"
        def locationPattern = "classpath:${classpath}**/*.class";
        def resourcePatternResolver = new PathMatchingResourcePatternResolver();
        def resources = resourcePatternResolver.getResources(locationPattern);

        return resources
    }

    def createConfiguration(resources, dialectClass) {

        def dialectName = dialectClass.getCanonicalName()
        def configuration = new Configuration()
        configuration.setProperty(HIBERNATE_DIALECT, dialectName)
        def readerFactory = new SimpleMetadataReaderFactory()

        resources.each({ resource ->
            def metadataReader = readerFactory.getMetadataReader(resource)
            def metadata = metadataReader.getAnnotationMetadata()
            if (metadata.hasAnnotation(Entity.class.getName())) {

                URL[] urls = [resource.URL]
                def loader = URLClassLoader.newInstance(urls, getClass().getClassLoader())
                def clazz = loader.loadClass(metadata.getClassName())

                configuration.addAnnotatedClass(clazz)
            }
        })

        return configuration
    }
}

