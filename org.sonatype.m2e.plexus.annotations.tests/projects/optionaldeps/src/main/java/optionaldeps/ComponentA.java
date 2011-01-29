package optionaldeps;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=ComponentA.class)
public class ComponentA
{
    @Requirement(optional=true)
    private ComponentB optional;
}
