/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.plexus.annotations.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.component.repository.ComponentRequirementList;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.StringLiteral;


public class JDTComponentGleaner {
  private static final String ANN_PACKAGE_NAME = "org.codehaus.plexus.component.annotations";

  public ComponentDescriptor glean(IType type, IProgressMonitor monitor) throws JavaModelException {
    
    IAnnotation compAnn = getAnnotation(type, type, "Component");

    if(compAnn == null) {
      return null; // not a component
    }

    ComponentDescriptor component = new ComponentDescriptor();

    Map<String, IMemberValuePair> annMembers = getMembersMap(compAnn);

    IMemberValuePair roleMember = annMembers.get("role");
    String[][] role = type.resolveType((String) roleMember.getValue());
    component.setRole(Signature.toQualifiedName(role[0]));

    component.setRoleHint(getStringValue(type, annMembers.get("hint")));
    component.setImplementation(type.getFullyQualifiedName());
    component.setVersion(getStringValue(type, annMembers.get("version")));
    component.setComponentType(getStringValue(type, annMembers.get("type")));
    component.setInstantiationStrategy(getStringValue(type, annMembers.get("instantiationStrategy")));
    component.setLifecycleHandler(getStringValue(type, annMembers.get("lifecycleHandler")));
    component.setComponentProfile(getStringValue(type, annMembers.get("profile")));
    component.setComponentComposer(getStringValue(type, annMembers.get("composer")));
    component.setComponentConfigurator(getStringValue(type, annMembers.get("configurator")));
    component.setComponentFactory(getStringValue(type, annMembers.get("factory")));
    component.setDescription(getStringValue(type, annMembers.get("description")));
    component.setAlias(getStringValue(type, annMembers.get("alias")));
    component.setIsolatedRealm(getBooleanValue(annMembers.get("isolatedRealm")));

    for(IField field : getFields(type, monitor)) {
      ComponentRequirement requirement = findRequirement(field, monitor);
      if(requirement != null) {
        component.addRequirement(requirement);
      }

      PlexusConfiguration config = findConfiguration(field);
      if(config != null) {
        addChildConfiguration(component, config);
      }
    }

    return component;
  }

  private ArrayList<IField> getFields(IType type, IProgressMonitor monitor) throws JavaModelException {
    ArrayList<IField> fields = new ArrayList<IField>();
    ITypeHierarchy hierarchy = type.newSupertypeHierarchy(monitor);
    IType curr = type;
    while(curr != null) {
      fields.addAll(Arrays.asList(curr.getFields()));
      curr = hierarchy.getSuperclass(curr);
    }
    return fields;
  }

  private void addChildConfiguration(ComponentDescriptor component, PlexusConfiguration config) {
    if(!component.hasConfiguration()) {
      component.setConfiguration(new XmlPlexusConfiguration("configuration"));
    }
    component.getConfiguration().addChild(config);
  }

  private PlexusConfiguration findConfiguration(IField field) throws JavaModelException {
    IType declaringType = field.getDeclaringType();

    IAnnotation anno = getAnnotation(declaringType, field, "Configuration");
    if(anno == null) {
      return null;
    }

    Map<String, IMemberValuePair> annMembers = getMembersMap(anno);

    String name = field.getElementName();
    IMemberValuePair nameMember = annMembers.get("name");
    if (nameMember != null) {
        name = (String) nameMember.getValue();
    }
    name = deHump(name);

    XmlPlexusConfiguration config = new XmlPlexusConfiguration(name);
    
    IMemberValuePair valueMember = annMembers.get("value");
    if (valueMember != null) {
      config.setValue((String) valueMember.getValue()); 
    }

    return config;
  }

  private boolean isRequirementListType(IType fieldType, IProgressMonitor monitor) throws JavaModelException {
    IType collection = fieldType.getJavaProject().findType("java.util.Collection");
    IType map = fieldType.getJavaProject().findType("java.util.Map");

    ITypeHierarchy hierarchy = fieldType.newSupertypeHierarchy(monitor);
    return hierarchy.contains(map) || hierarchy.contains(collection);
  }

  private ComponentRequirement findRequirement(IField field, IProgressMonitor monitor) throws JavaModelException {
    IJavaProject javaProject = field.getJavaProject();
    IType declaringType = field.getDeclaringType();

    IAnnotation anno = getAnnotation(declaringType, field, "Requirement");
    if(anno == null) {
      return null;
    }

    Map<String, IMemberValuePair> annMembers = getMembersMap(anno);

    IType fieldType = resolve(declaringType, Signature.toString(field.getTypeSignature()));

    ComponentRequirement requirement;

    if(isRequirementListType(fieldType, monitor)) {
      requirement = new ComponentRequirementList();

      String[] hints = null;
      IMemberValuePair member = annMembers.get("hints");
      if (member != null) {
        Object[] os = (Object[]) member.getValue();
        hints = new String[os.length];
        for (int i = 0; i < os.length; i++) {
          hints[i] = (String) os[i];
        }
      }

      if(hints != null && hints.length > 0) {
        ((ComponentRequirementList) requirement).setRoleHints(Arrays.asList(hints));
      }
    } else {
      requirement = new ComponentRequirement();

      requirement.setRoleHint(getStringValue(declaringType, annMembers.get("hint")));
    }

    String role = null;
    IMemberValuePair roleMember = annMembers.get("role");
    if(roleMember != null) {
      String fqnRole = Signature.toQualifiedName(declaringType.resolveType((String) roleMember.getValue())[0]);
      if(!"java.lang.Object".equals(fqnRole)) {
        role = javaProject.findType(fqnRole).getFullyQualifiedName();
      }
    }

    if(role == null) {
      role = fieldType.getFullyQualifiedName();
    }
    
    requirement.setRole(role);

    requirement.setFieldName(field.getElementName());

    requirement.setFieldMappingType(fieldType.getFullyQualifiedName());

    return requirement;
  }

  private boolean getBooleanValue(IMemberValuePair member) {
    if(member == null || member.getValueKind() != IMemberValuePair.K_BOOLEAN) {
      return false;
    }
    return ((Boolean) member.getValue()).booleanValue();
  }

  private String getStringValue(IType type, IMemberValuePair member) throws JavaModelException {
    if(member == null) {
      return null;
    }
    if (member.getValueKind() == IMemberValuePair.K_STRING) {
        String value = (String) member.getValue();
        return "".equals(value) ? null : value;
    } else if (member.getValueKind() == IMemberValuePair.K_QUALIFIED_NAME) {
        String qname = (String) member.getValue();

        int dot = qname.lastIndexOf( '.' );
        String fieldTypeName = qname.substring( 0, dot );
        String fieldName = qname.substring( dot + 1 );

        IType fieldType = resolve( type, fieldTypeName );
        IField field = fieldType.getField( fieldName );

        String constant = (String) field.getConstant();

        // TODO is there an easier way?
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(constant.toCharArray());
        parser.setKind( ASTParser.K_EXPRESSION );
        StringLiteral literal = (StringLiteral) parser.createAST(null);

        return literal.getLiteralValue();
    }
    return null;
  }

  private Map<String, IMemberValuePair> getMembersMap(IAnnotation ann) throws JavaModelException {
    LinkedHashMap<String, IMemberValuePair> result = new LinkedHashMap<String, IMemberValuePair>();
    IMemberValuePair[] members = ann.getMemberValuePairs();
    for(IMemberValuePair member : members) {
      result.put(member.getMemberName(), member);
    }
    return result;
  }

  private IAnnotation getAnnotation(IType type, IAnnotatable element, String name) throws JavaModelException {
    /*
     * Oddly enough, "element.getAnnotation(name)" returns annotations that
     * were deleted from the code and such deleted annotations claim to exist
     * (i.e. ann.exists() returns true)
     */
    for( IAnnotation ann : element.getAnnotations()) {
      String annName = ann.getElementName();
      if (annName.equals(ANN_PACKAGE_NAME + "." + name)) {
        return ann;
      } else if (annName.equals(name)) {
        String[][] resolved = type.resolveType(ann.getElementName());
        if(resolved != null && resolved.length > 0 && ANN_PACKAGE_NAME.equals(resolved[0][0])) {
          return ann;
        }
      }
    }
    return null;
  }

  private IType resolve(IType type, String typeName) throws JavaModelException {
    String[][] resolved = type.resolveType(typeName);
    if(resolved.length <= 0) {
      return null;
    }
    return type.getJavaProject().findType(Signature.toQualifiedName(resolved[0]));
  }

  protected String deHump(final String string) {
    // assert string != null;

    StringBuffer buff = new StringBuffer();

    for (int i = 0; i < string.length(); i++) {
        if (i != 0 && Character.isUpperCase(string.charAt(i))) {
            buff.append('-');
        }

        buff.append(string.charAt(i));
    }

    return buff.toString().trim().toLowerCase();
  }
}
