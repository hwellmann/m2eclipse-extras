package org.sonatype.m2e.plexus.annotations.io;

/*
 * Copyright 2001-2006 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.component.repository.ComponentRequirementList;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


/**
 * @author Jason van Zyl 
 */
public class DefaultComponentDescriptorReader
{
    public static PlexusConfiguration buildConfiguration( String resourceName, Reader configuration )
        throws PlexusConfigurationException
    {
        try
        {
            PlexusConfiguration result; 
            try
            {
                Xpp3Dom dom = Xpp3DomBuilder.build( configuration );

                result = new XmlPlexusConfiguration( dom );
            }
            catch ( XmlPullParserException e )
            {
                throw new PlexusConfigurationException( "Failed to parse configuration resource!\nError was: \'"
                    + e.getLocalizedMessage() + "\'", e );
            }

            return result;
        }
        catch ( PlexusConfigurationException e )
        {
            throw new PlexusConfigurationException( "PlexusConfigurationException building configuration from: " + resourceName, e );
        }
        catch ( IOException e )
        {
            throw new PlexusConfigurationException( "IO error building configuration from: " + resourceName, e );
        }
    }


    public static ComponentDescriptor<?> buildComponentDescriptor( PlexusConfiguration configuration )
        throws PlexusConfigurationException

    {
        ComponentDescriptor<?> cd = new ComponentDescriptor();

        cd.setRole( configuration.getChild( "role" ).getValue() );

        cd.setImplementation( configuration.getChild( "implementation" ).getValue() );
        
        cd.setRoleHint( configuration.getChild( "role-hint" ).getValue() );

        cd.setVersion( configuration.getChild( "version" ).getValue() );

        cd.setComponentType( configuration.getChild( "component-type" ).getValue() );

        cd.setInstantiationStrategy( configuration.getChild( "instantiation-strategy" ).getValue() );

        cd.setLifecycleHandler( configuration.getChild( "lifecycle-handler" ).getValue() );

        cd.setComponentProfile( configuration.getChild( "component-profile" ).getValue() );

        cd.setComponentComposer( configuration.getChild( "component-composer" ).getValue() );

        cd.setComponentConfigurator( configuration.getChild( "component-configurator" ).getValue() );

        cd.setComponentFactory( configuration.getChild( "component-factory" ).getValue() );

        cd.setDescription( configuration.getChild( "description" ).getValue() );

        cd.setAlias( configuration.getChild( "alias" ).getValue() );

        String s = configuration.getChild( "isolated-realm" ).getValue();

        if ( s != null )
        {
            cd.setIsolatedRealm( s.equals( "true" ) ? true : false );
        }

        // ----------------------------------------------------------------------
        // Here we want to look for directives for inlining external
        // configurations. we probably want to take them from files or URLs.
        // ----------------------------------------------------------------------

        cd.setConfiguration( configuration.getChild( "configuration" ) );

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        PlexusConfiguration[] requirements = configuration.getChild( "requirements" ).getChildren( "requirement" );

        for ( int i = 0; i < requirements.length; i++ )
        {
            PlexusConfiguration requirement = requirements[i];

            ComponentRequirement cr;

            PlexusConfiguration[] hints = requirement.getChild( "role-hints" ).getChildren( "role-hint" );
            if ( hints != null && hints.length > 0 )
            {
                cr = new ComponentRequirementList();

                List<String> hintList = new LinkedList<String>();
                for ( PlexusConfiguration hint : hints )
                {
                    hintList.add( hint.getValue() );
                }

                ( (ComponentRequirementList) cr ).setRoleHints( hintList );
            }
            else
            {
                cr = new ComponentRequirement();

                cr.setRoleHint( requirement.getChild( "role-hint" ).getValue() );
            }

            cr.setRole( requirement.getChild( "role" ).getValue() );

            cr.setFieldName( requirement.getChild( "field-name" ).getValue() );

            cr.setOptional( Boolean.parseBoolean( requirement.getChild( "optional" ).getValue() ) );

            cd.addRequirement( cr );
        }

        return cd;
    }

    public static ComponentSetDescriptor buildComponentSet( PlexusConfiguration c )
        throws PlexusConfigurationException
    {
        ComponentSetDescriptor csd = new ComponentSetDescriptor();

        // ----------------------------------------------------------------------
        // Components
        // ----------------------------------------------------------------------

        PlexusConfiguration[] components = c.getChild( "components" ).getChildren( "component" );

        for ( int i = 0; i < components.length; i++ )
        {
            PlexusConfiguration component = components[i];

            csd.addComponentDescriptor( buildComponentDescriptor( component ) );
        }

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        PlexusConfiguration[] dependencies = c.getChild( "dependencies" ).getChildren( "dependency" );

        for ( int i = 0; i < dependencies.length; i++ )
        {
            PlexusConfiguration d = dependencies[i];

            ComponentDependency cd = new ComponentDependency();

            cd.setArtifactId( d.getChild( "artifact-id" ).getValue() );

            cd.setGroupId( d.getChild( "group-id" ).getValue() );

            String type = d.getChild( "type" ).getValue();
            if(type != null)
            {
                cd.setType( type );
            }

            cd.setVersion( d.getChild( "version" ).getValue() );

            csd.addDependency( cd );
        }

        return csd;
    }

}
