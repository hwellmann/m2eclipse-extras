package org.maven.ide.eclipse.plexus.annotations.tests;

import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.component.repository.io.PlexusTools;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

public class P001SimpleTest extends AsbtractMavenProjectTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);
  }
  
  @Override
  protected void tearDown() throws Exception {
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);

    super.tearDown();
  }

  public void testBuild() throws Exception {
    IProject project = createExisting("plexus-annotations-p001", "projects/simple/plexus-annotations-p001");
    waitForJobsToComplete();

    IFile metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
    assertFalse(metadata.exists());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    metadata = project.getFile("target/classes/META-INF/plexus/components.xml");

    assertMetadata(metadata);

    // now delete and run incremental build
    
    metadata.delete(true, monitor);

    metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
    assertFalse(metadata.exists());

    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
    assertMetadata(metadata);
    
  }

  public void testIncrementalChange() throws Exception {
    IProject project = createExisting("plexus-annotations-p001", "projects/simple/plexus-annotations-p001");
    waitForJobsToComplete();

    IFile metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
    assertFalse(metadata.exists());

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    metadata = project.getFile("target/classes/META-INF/plexus/components.xml");

    assertMetadata(metadata);
    
    // now change content and run incremental build
    copyContent(project, "src/main/java/simple/p001/P001SimpleImpl.java-changed", "src/main/java/simple/p001/P001SimpleImpl.java");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    ComponentSetDescriptor set = readComponentSet(metadata);
    List<ComponentDescriptor<?>> components = set.getComponents();
    ComponentDescriptor comp = components.get(0);
    List<ComponentRequirement> requirements = comp.getRequirements();
    ComponentRequirement requirement = requirements.get(0);

    assertEquals("changed", requirement.getRoleHint());
  }

  public void testIntraProjectDependencies() throws Exception {
    IProject project = createExisting("plexus-annotations-p002", "projects/simple/plexus-annotations-p002");
    waitForJobsToComplete();

    project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IFile metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
    ComponentDescriptor comp02 = getComponentDescriptor(readComponentSet(metadata), "simple.p002.P002Simple02Impl");
    assertRequirement(comp02, "r01", "default");
    IFile testMetadata = project.getFile("target/test-classes/META-INF/plexus/components.xml");
    ComponentDescriptor compTest = getComponentDescriptor(readComponentSet(testMetadata), "simple.p002.test.P002SimpleTestImpl");
    assertRequirement(compTest, "r01", "default");

    // change parent class, check changes are reflected in subclass components
    copyContent(project, "src/main/java/simple/p002/P002Simple01Impl.java-changed", "src/main/java/simple/p002/P002Simple01Impl.java");
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
    comp02 = getComponentDescriptor(readComponentSet(metadata), "simple.p002.P002Simple02Impl");
    assertRequirement(comp02, "r01", "r01");
    testMetadata = project.getFile("target/test-classes/META-INF/plexus/components.xml");
    compTest = getComponentDescriptor(readComponentSet(testMetadata), "simple.p002.test.P002SimpleTestImpl");
    assertRequirement(compTest, "r01", "r01");
  
  }
  
  public void testInterProjectDependencies() throws Exception {
    IProject project02 = createExisting("plexus-annotations-p002", "projects/simple/plexus-annotations-p002");
    IProject project03 = createExisting("plexus-annotations-p003", "projects/simple/plexus-annotations-p003");
    waitForJobsToComplete();

    workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    IFile metadata = project03.getFile("target/classes/META-INF/plexus/components.xml");
    ComponentDescriptor comp02 = getComponentDescriptor(readComponentSet(metadata), "simple.p003.P003Simple01Impl");
    assertRequirement(comp02, "r01", "default");
    IFile testMetadata = project03.getFile("target/test-classes/META-INF/plexus/components.xml");
    ComponentDescriptor compTest = getComponentDescriptor(readComponentSet(testMetadata), "simple.p003.test.P003SimpleTestImpl");
    assertRequirement(compTest, "r01", "default");

    // change parent class, check changes are reflected in subclass components
    copyContent(project02, "src/main/java/simple/p002/P002Simple01Impl.java-changed", "src/main/java/simple/p002/P002Simple01Impl.java");
    workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

    metadata = project03.getFile("target/classes/META-INF/plexus/components.xml");
    comp02 = getComponentDescriptor(readComponentSet(metadata), "simple.p003.P003Simple01Impl");
    assertRequirement(comp02, "r01", "r01");
    testMetadata = project03.getFile("target/test-classes/META-INF/plexus/components.xml");
    compTest = getComponentDescriptor(readComponentSet(testMetadata), "simple.p003.test.P003SimpleTestImpl");
    assertRequirement(compTest, "r01", "r01");
    
  }

  public void testCustomMetadataMerge() throws Exception {
      IProject project04 = createExisting("plexus-annotations-p004", "projects/simple/plexus-annotations-p004");
      waitForJobsToComplete();

      workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
      workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

      assertNoErrors( project04 );

      IFile metadata = project04.getFile("target/classes/META-INF/plexus/components.xml");
      ComponentSetDescriptor componentSet = readComponentSet(metadata);

      assertEquals(2, componentSet.getComponents().size());
  }

  public void testConstantFieldValues() throws Exception {
      IProject project05 = createExisting("plexus-annotations-p005", "projects/simple/plexus-annotations-p005");
      waitForJobsToComplete();

      workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
      workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

      assertNoErrors( project05 );

      IFile metadata = project05.getFile("target/classes/META-INF/plexus/components.xml");
      ComponentDescriptor comp = getComponentDescriptor(readComponentSet(metadata), "simple.p005.P005SimpleImpl");
      
      assertEquals("fooled", comp.getRoleHint());
  }

  public void testDeepInheritance() throws Exception {
      IProject project06 = createExisting("plexus-annotations-p006", "projects/simple/plexus-annotations-p006");
      waitForJobsToComplete();

      workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
      workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

      assertNoErrors( project06 );

      IFile metadata = project06.getFile("target/classes/META-INF/plexus/components.xml");
      ComponentDescriptor comp = getComponentDescriptor(readComponentSet(metadata), "simple.p006.s1.s2.P006S2");

      assertEquals("simple.p006.s1.s2.P006S2", comp.getRole());
  }

  public void testOptionalRequirement() throws Exception {
      IProject project = importProject("projects/optionaldeps/pom.xml", new ResolverConfiguration());
      waitForJobsToComplete();

      workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
      workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

      assertNoErrors( project );

      IFile metadata = project.getFile("target/classes/META-INF/plexus/components.xml");
      ComponentDescriptor comp = getComponentDescriptor(readComponentSet(metadata), "optionaldeps.ComponentA");

      List<ComponentRequirement> requirements = comp.getRequirements();
      assertEquals("optionaldeps.ComponentB", requirements.get(0).getRole());
      assertTrue(requirements.get(0).isOptional());
  }

  private void assertRequirement(ComponentDescriptor comp, String fieldName, String roleHint) {
    List<ComponentRequirement> requirements = comp.getRequirements();
    for (ComponentRequirement requirement : requirements) {
      if (fieldName.equals(requirement.getFieldName())) {
        assertEquals(roleHint, requirement.getRoleHint());
        return;
      }
    }
    fail("Requirement" + fieldName);
  }

  private ComponentDescriptor getComponentDescriptor(ComponentSetDescriptor set, String impl) {
    List<ComponentDescriptor<?>> components = set.getComponents();
    for (ComponentDescriptor component : components) {
      if (impl.equals(component.getImplementation())) {
        return component;
      }
    }
    return null;
  }

  private void assertMetadata(IFile metadata) throws IOException, CoreException, PlexusConfigurationException {
    assertTrue(metadata.exists());

    ComponentSetDescriptor set = readComponentSet(metadata);
    List<ComponentDescriptor<?>> components = set.getComponents();
    assertEquals(1, components.size());

    ComponentDescriptor comp = components.get(0);
    assertEquals("simple.p001.P001Simple", comp.getRole());
    assertEquals("simple.p001.P001SimpleImpl", comp.getImplementation());

    List<ComponentRequirement> requirements = comp.getRequirements();
    assertEquals(1, requirements.size());
    
    ComponentRequirement requirement = requirements.get(0);
    assertEquals("simple.p001.P001Simple", requirement.getRole());
    assertEquals("default", requirement.getRoleHint());
  }

  private ComponentSetDescriptor readComponentSet(IFile metadata) throws IOException, CoreException, PlexusConfigurationException {
    XmlStreamReader reader = ReaderFactory.newXmlReader(metadata.getContents());
    try {
      PlexusConfiguration plexusConfiguration = PlexusTools.buildConfiguration("test resource", reader);
      return PlexusTools.buildComponentSet( plexusConfiguration );
    } finally {
      reader.close();
    }
  }
}
