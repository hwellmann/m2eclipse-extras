/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.plexus.annotations.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.io.PlexusTools;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.metadata.ComponentDescriptorWriter;
import org.codehaus.plexus.metadata.DefaultComponentDescriptorWriter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaModelException;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.plexus.annotations.PlexusAnnotationsCore;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.util.Util;


public class PlexusMetadata {

  private static class Record {
    boolean testMetadata;

    IResource resource;

    Set<IResource> resources;

    ComponentDescriptor descriptor;

    boolean isValid() {
      return descriptor != null;
    }
  }

  private Map<IProject, Map<IResource, Record>> projects = new HashMap<IProject, Map<IResource, Record>>();

  public void addDescriptor(IResource resource, Set<IResource> resources, ComponentDescriptor decriptor, boolean testMetadata)
      throws JavaModelException {
    Record record = new Record();
    record.testMetadata = testMetadata;
    record.resource = resource;
    record.resources = resources;
    record.descriptor = decriptor;

    IProject project = resource.getProject();
    Map<IResource, Record> projectRecords = projects.get(project);
    if(projectRecords == null) {
      projectRecords = new HashMap<IResource, Record>();
      projects.put(project, projectRecords);
    }
    projectRecords.put(resource, record);
  }

  public void invalidateMetadata(IResource resource) {
    invalidateMetadata(new HashSet<IResource>(), resource);
  }

  private void invalidateMetadata(Set<IResource> invalidated, IResource resource) {
    if(!invalidated.add(resource)) {
      return;
    }

    Map<IResource, Record> projectRecords = projects.get(resource.getProject());

    if(projectRecords == null) {
      return;
    }

    Record record = projectRecords.get(resource);

    if(record != null) {
      record.descriptor = null;
    }

    for(IResource dependent : getDependentResources(resource)) {
      invalidateMetadata(invalidated, dependent);
    }
  }

  private Set<IResource> getDependentResources(IResource resource) {
    Set<IResource> resources = new HashSet<IResource>();

    for(Map<IResource, Record> projectRecords : projects.values()) {
      for(Map.Entry<IResource, Record> entry : projectRecords.entrySet()) {
        if(entry.getValue().resources.contains(resource)) {
          resources.add(entry.getKey());
        }
      }
    }
    return resources;
  }

  public Set<IProject> writeMetadata(IProject project, boolean testMetadata) throws CoreException {
    Set<IProject> dependencies = new HashSet<IProject>();

    List<ComponentDescriptor<?>> descriptors = new ArrayList<ComponentDescriptor<?>>();

    Map<IResource, Record> projectRecords = projects.get(project);
    if (projectRecords == null) {
      System.err.println(project.toString() + "  WTF?");
      return dependencies;
    }
    
    Iterator<Entry<IResource, Record>> iterator = projectRecords.entrySet().iterator();
    while(iterator.hasNext()) {
      Entry<IResource, Record> entry = iterator.next();
      IResource resource = entry.getKey();

      if(!resource.exists()) {
        iterator.remove();
        continue;
      }

      Record record = entry.getValue();

      if (record.testMetadata != testMetadata) {
        continue;
      }

      if(!record.isValid()) {
        throw new IllegalStateException("Inconsistent plexus metadata state");
      }

      for(IResource dependency : record.resources) {
        dependencies.add(dependency.getProject());
      }

      descriptors.add(record.descriptor);
    }

    writeDescriptor(project, descriptors, testMetadata);

    return dependencies;
  }

  private void writeDescriptor(IProject project, final List<ComponentDescriptor<?>> descriptors, boolean testDescriptor)
      throws CoreException {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    NullProgressMonitor monitor = new NullProgressMonitor();
    IMavenProjectFacade facade = projectManager.create(project, monitor);

    if(facade == null) {
      return;
    }

    // XXX this may or may not properly merge handwritten descriptor
    // XXX use mojo configuration!
    IFolder staticMetadataFolder = project.getFolder(testDescriptor ? "src/test/resources/META-INF/plexus"
        : "src/main/resources/META-INF/plexus");
    if (staticMetadataFolder.isAccessible()) {
      staticMetadataFolder.accept(new IResourceVisitor() {
        public boolean visit(IResource resource) throws CoreException {
          if (resource instanceof IFile && resource.getName().endsWith(".xml") && !resource.getName().equals("plexus.xml")) {
            IFile file = (IFile) resource;
            addDescriptors(descriptors, file);
          }
          return true;
        }
      }, IResource.DEPTH_ONE, false);
    }

    IPath outputLocation = testDescriptor ? facade.getTestOutputLocation() : facade.getOutputLocation();

    String encoding = "UTF-8";

    IFolder outputFolder = project.getFolder(outputLocation.append("META-INF/plexus").removeFirstSegments(1));
    IFile file = outputFolder.getFile("components.xml");

    if(!descriptors.isEmpty()) {
      Util.createFolder(outputFolder, true);

      ComponentDescriptorWriter writer = new DefaultComponentDescriptorWriter();

      ComponentSetDescriptor set = new ComponentSetDescriptor();
      set.setComponents(descriptors);
      set.setDependencies(Collections.EMPTY_LIST);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try {
        Writer os = new OutputStreamWriter(buffer, "UTF-8");
        try {
          writer.writeDescriptorSet(os, set, false);
          os.flush();
        } finally {
          IOUtil.close(os);
        }
      } catch(Exception e) {
        throw new CoreException(new Status(IStatus.ERROR, PlexusAnnotationsCore.PLUGIN_ID,
            "Exception writing components.xml", e));
      }

      ByteArrayInputStream source = new ByteArrayInputStream(buffer.toByteArray());
      if(!file.exists()) {
        file.create(source, IResource.FORCE | IResource.DERIVED, monitor);
      } else {
        file.setContents(source, true, false, monitor);
      }
      file.setCharset(encoding, monitor);
    } else {
      if(file.exists()) {
        file.delete(true, monitor);
      }
    }

  }

  public List<IResource> getStaleResources(IProject project, boolean testMetadata) {
    ArrayList<IResource> resources = new ArrayList<IResource>();
    Map<IResource, Record> projectRecords = projects.get(project);
    if (projectRecords != null) {
      for(Record record : projectRecords.values()) {
        if(record.testMetadata == testMetadata && !record.isValid()) {
          resources.add(record.resource);
        }
      }
    }
    return resources;
  }

  public void remove(IResource resource) {
    Map<IResource, Record> projectRecords = projects.get(resource.getProject());
    if(projectRecords != null) {
      projectRecords.remove(resource);
    }
  }

  public void clean(IProject project) {
    projects.remove(project);
  }

  private void addDescriptors(List<ComponentDescriptor<?>> descriptors, IFile file) {

    PlexusConfiguration componentDescriptorConfiguration;
    String source = file.getLocation().toOSString();
    try {
      Reader componentDescriptorReader = ReaderFactory.newXmlReader(file.getContents());
      try {
        componentDescriptorConfiguration = PlexusTools.buildConfiguration(source, componentDescriptorReader);
      } finally {
        IOUtil.close(componentDescriptorReader);
      }
    } catch(Exception e) {
      MavenLogger.log("Could not read " + source, e);
      return;
    }

    PlexusConfiguration[] componentConfigurations = componentDescriptorConfiguration.getChild("components")
        .getChildren("component");

    for(int i = 0; i < componentConfigurations.length; i++ ) {
      PlexusConfiguration componentConfiguration = componentConfigurations[i];

      ComponentDescriptor componentDescriptor;

      try {
        componentDescriptor = PlexusTools.buildComponentDescriptor(componentConfiguration);
      } catch(PlexusConfigurationException e) {
        MavenLogger.log("Could not process " + source, e);
        return;
      }

      componentDescriptor.setSource(source);

      componentDescriptor.setComponentType("plexus");

      descriptors.add(componentDescriptor);
    }
  }

  public boolean isFullBuildRequired(IProject project) {
    return projects.get(project) == null;
  }

}
